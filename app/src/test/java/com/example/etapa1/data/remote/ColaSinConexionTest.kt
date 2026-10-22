package com.example.etapa1.data.remote

import com.example.etapa1.model.Horas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * RF-11 / RNF-05: la cola sin conexión guarda, reintenta y no duplica.
 * Se prueba contra un servidor simulado (MockWebServer), sin teléfono.
 */
class ColaSinConexionTest {

    private lateinit var servidor: MockWebServer
    private lateinit var api: SonrisasApi
    private lateinit var scope: CoroutineScope
    private val json = NetworkClient.json

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        val cliente = OkHttpClient.Builder().readTimeout(2, TimeUnit.SECONDS).build()
        api = Retrofit.Builder()
            .baseUrl(servidor.url("/api/v1/"))
            .client(cliente)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SonrisasApi::class.java)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @After
    fun cerrar() {
        scope.cancel()
        servidor.shutdown()
    }

    private fun cola(almacen: AlmacenPendientes = AlmacenPendientesEnMemoria(), usuario: String? = "usr_educadora") =
        ColaSinConexion(api, { usuario }, { true }, almacen, scope, procesarAlIniciar = false)

    private fun actividad(descripcion: String, usuario: String = "usr_educadora") = Pendiente(
        tipo = TipoPendiente.ACTIVIDAD,
        userId = usuario,
        childId = "mateo_garcia",
        cuerpo = json.encodeToString(
            CreateActivityDto(time = "10:30 AM", category = "ALIMENTACION", title = "Colación", description = descripcion)
        )
    )

    private val respuestaActividad = """
        {"id":"act_1","status":"CREADA","timelineItem":{"type":"ACTIVITY_CARD","id":"act_1","time":"10:30 AM"}}
    """.trimIndent()

    @Test
    fun sin_internet_se_conserva_y_al_volver_se_manda_con_la_misma_llave() = runBlocking {
        val almacen = AlmacenPendientesEnMemoria()
        val c = cola(almacen)
        val p = actividad("Comió todo")
        c.guardarEnCola(p)

        // 1) Sin internet: la conexión se corta y el registro se queda guardado
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        c.enviarTodo()
        assertEquals(listOf(p), almacen.guardados)
        assertTrue(c.sinConexion.value)

        // 2) Vuelve el internet: se manda y sale de la cola
        servidor.enqueue(MockResponse().setResponseCode(201).setBody(respuestaActividad))
        c.enviarTodo()
        assertTrue(almacen.guardados.isEmpty())

        // El reintento lleva como Idempotency-Key el id con que se guardó: si el primer envío
        // sí hubiera llegado, el servidor devolvería lo mismo sin duplicar (test_entrega_final.py)
        servidor.takeRequest() // el intento que se cortó
        val reintento = servidor.takeRequest()
        assertEquals("/api/v1/children/mateo_garcia/activities", reintento.path)
        assertEquals(p.id, reintento.getHeader("Idempotency-Key"))
    }

    @Test
    fun se_mandan_en_el_orden_en_que_se_registraron() = runBlocking {
        val c = cola()
        val a = actividad("primera")
        val b = actividad("segunda")
        c.guardarEnCola(a)
        c.guardarEnCola(b)
        repeat(2) { servidor.enqueue(MockResponse().setResponseCode(201).setBody(respuestaActividad)) }
        c.enviarTodo()
        assertEquals(a.id, servidor.takeRequest().getHeader("Idempotency-Key"))
        assertEquals(b.id, servidor.takeRequest().getHeader("Idempotency-Key"))
        assertTrue(c.pendientes.value.isEmpty())
    }

    @Test
    fun la_cola_sobrevive_si_se_cierra_la_app() = runBlocking {
        val almacen = AlmacenPendientesEnMemoria()
        val p = actividad("guardada antes de cerrar")
        cola(almacen).guardarEnCola(p)

        // Una cola nueva (app abierta otra vez) lee lo que quedó guardado
        val reabierta = cola(almacen)
        assertEquals(listOf(p), reabierta.pendientes.value)
    }

    @Test
    fun sesion_vencida_no_borra_lo_pendiente() = runBlocking {
        val almacen = AlmacenPendientesEnMemoria()
        val c = cola(almacen)
        val p = actividad("con token vencido")
        c.guardarEnCola(p)
        servidor.enqueue(MockResponse().setResponseCode(401))
        c.enviarTodo()
        assertEquals(listOf(p), almacen.guardados)
    }

    @Test
    fun error_del_servidor_se_reintenta_despues() = runBlocking {
        val almacen = AlmacenPendientesEnMemoria()
        val c = cola(almacen)
        c.guardarEnCola(actividad("servidor caído"))
        servidor.enqueue(MockResponse().setResponseCode(503))
        c.enviarTodo()
        assertEquals(1, almacen.guardados.size)
    }

    @Test
    fun dato_invalido_se_quita_para_no_atorar_la_cola() = runBlocking {
        val almacen = AlmacenPendientesEnMemoria()
        val c = cola(almacen)
        c.guardarEnCola(actividad("inválida"))
        c.guardarEnCola(actividad("válida"))
        servidor.enqueue(MockResponse().setResponseCode(422).setBody("""{"field":"time","error":"hora inválida"}"""))
        servidor.enqueue(MockResponse().setResponseCode(201).setBody(respuestaActividad))
        c.enviarTodo()
        assertTrue(almacen.guardados.isEmpty())
        assertEquals(2, servidor.requestCount)
    }

    @Test
    fun no_se_mandan_registros_de_otra_persona() = runBlocking {
        val almacen = AlmacenPendientesEnMemoria()
        val c = cola(almacen, usuario = "usr_otra")
        c.guardarEnCola(actividad("de la educadora", usuario = "usr_educadora"))
        c.enviarTodo()
        assertEquals(0, servidor.requestCount)
        assertEquals(1, almacen.guardados.size)
    }

    @Test
    fun hora_del_formulario_se_muestra_en_12_horas() {
        assertEquals("08:15 AM", Horas.a12h("08:15"))
        assertEquals("12:30 PM", Horas.a12h("12:30"))
        assertEquals("12:05 AM", Horas.a12h("00:05"))
        assertEquals("02:30 PM", Horas.a12h("14:30"))
        assertEquals("11:59 PM", Horas.a12h("23:59"))
        // Si ya viene en 12 h o no se entiende, no se toca
        assertEquals("02:30 PM", Horas.a12h("02:30 PM"))
    }
}
