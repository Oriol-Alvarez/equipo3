package com.example.etapa1.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import retrofit2.Response
import java.io.IOException
import java.util.UUID

/** Qué tipo de registro quedó pendiente de enviar. */
@Serializable
enum class TipoPendiente { INGRESO, EGRESO, ACTIVIDAD, EVENTO, MENSAJE_BITACORA }

/**
 * Un registro hecho en el teléfono que todavía no llega al servidor.
 *
 * [id] viaja como cabecera Idempotency-Key: si el envío llegó pero se perdió la respuesta,
 * el reintento no duplica el registro en el servidor (RF-11 / RNF-05).
 */
@Serializable
data class Pendiente(
    val id: String = UUID.randomUUID().toString(),
    val tipo: TipoPendiente,
    val userId: String,
    val childId: String,
    /** Cuerpo de la petición en JSON (IngresoDto, CreateActivityDto, etc.). */
    val cuerpo: String,
    val creadoEn: Long = System.currentTimeMillis()
)

sealed interface ResultadoEnvio {
    val pendiente: Pendiente

    /** El servidor lo guardó; [respuesta] es el cuerpo que devolvió. */
    data class Enviado(override val pendiente: Pendiente, val respuesta: Any?) : ResultadoEnvio

    /** El servidor lo rechazó por un dato inválido; reintentarlo no serviría. */
    data class Rechazado(override val pendiente: Pendiente, val motivo: String) : ResultadoEnvio
}

/**
 * RF-11 "Funciona sin internet": cola de registros pendientes de la educadora.
 *
 * 1. Cada registro (ingreso, egreso, actividad, evento o mensaje) se guarda primero en el
 *    teléfono, cifrado, y aparece en pantalla de inmediato.
 * 2. Se manda al servidor en el mismo orden en que se hizo.
 * 3. Si no hay internet o el servidor no responde, se queda guardado (aunque se cierre la app)
 *    y se reintenta cuando vuelve la red o cada 30 segundos.
 * 4. Si la sesión venció (401) se conserva y se manda cuando la misma persona vuelve a entrar.
 */
class ColaSinConexion(
    private val api: SonrisasApi,
    /** Id de quien tiene la sesión (solo se mandan sus registros). */
    private val usuarioActual: () -> String?,
    /** true si hay token para hablar con el servidor. */
    private val haySesion: () -> Boolean,
    private val almacen: AlmacenPendientes,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    /** Si se da, se vigila la red para mandar en cuanto vuelva el internet. */
    contextoRed: Context? = null,
    procesarAlIniciar: Boolean = true
) {
    /** Constructor de la app: guarda la cola cifrada y usa la sesión del [TokenManager]. */
    constructor(context: Context, api: SonrisasApi, tokenManager: TokenManager) : this(
        api = api,
        usuarioActual = { tokenManager.userId },
        haySesion = { !tokenManager.token.isNullOrBlank() },
        almacen = AlmacenPendientesCifrado(context),
        contextoRed = context
    )

    private val json = NetworkClient.json
    private val candado = Any()

    private val _pendientes = MutableStateFlow(almacen.cargar())
    val pendientes: StateFlow<List<Pendiente>> = _pendientes.asStateFlow()

    private val _resultados = MutableSharedFlow<ResultadoEnvio>(extraBufferCapacity = 64)
    val resultados: SharedFlow<ResultadoEnvio> = _resultados.asSharedFlow()

    /** true si el último intento no pudo llegar al servidor. */
    private val _sinConexion = MutableStateFlow(false)
    val sinConexion: StateFlow<Boolean> = _sinConexion.asStateFlow()

    private val enviando = Mutex()
    private var reintento: Job? = null

    init {
        contextoRed?.let(::vigilarRed)
        if (procesarAlIniciar) procesar()
    }

    /** Pendientes de la sesión actual para un niño (para mostrarlos al abrir su bitácora). */
    fun pendientesDe(childId: String): List<Pendiente> {
        val usuario = usuarioActual()
        return _pendientes.value.filter { it.childId == childId && it.userId == usuario }
    }

    /** Guarda el registro en el teléfono y trata de mandarlo. */
    fun agregar(pendiente: Pendiente) {
        guardarEnCola(pendiente)
        procesar()
    }

    /** Solo lo guarda (las pruebas deciden cuándo mandar con [enviarTodo]). */
    internal fun guardarEnCola(pendiente: Pendiente) {
        synchronized(candado) {
            _pendientes.value = _pendientes.value + pendiente
            almacen.guardar(_pendientes.value)
        }
    }

    private fun quitar(id: String) {
        synchronized(candado) {
            _pendientes.value = _pendientes.value.filterNot { it.id == id }
            almacen.guardar(_pendientes.value)
        }
    }

    /** Intenta mandar todo lo pendiente (no hace nada si ya hay un envío en curso). */
    fun procesar(): Job = scope.launch {
        if (!enviando.tryLock()) return@launch
        try {
            enviarPendientes()
        } finally {
            enviando.unlock()
        }
    }

    /** Manda ya lo pendiente; si hay otro envío en curso, espera a que termine. */
    internal suspend fun enviarTodo() = enviando.withLock { enviarPendientes() }

    private suspend fun enviarPendientes() {
        val usuario = usuarioActual() ?: return
        if (!haySesion()) return
        for (p in _pendientes.value.filter { it.userId == usuario }) {
            val respuesta: Response<*> = try {
                enviar(p)
            } catch (e: IOException) {
                Log.i(TAG, "Sin conexión; ${_pendientes.value.size} registro(s) esperan: ${e.message}")
                _sinConexion.value = true
                programarReintento()
                return
            } catch (e: Exception) {
                Log.w(TAG, "Registro ${p.id} inválido, se descarta: ${e.message}")
                quitar(p.id)
                _resultados.emit(ResultadoEnvio.Rechazado(p, e.message ?: "Registro inválido"))
                continue
            }
            _sinConexion.value = false
            val codigo = respuesta.code()
            when {
                respuesta.isSuccessful -> {
                    quitar(p.id)
                    _resultados.emit(ResultadoEnvio.Enviado(p, respuesta.body()))
                }
                codigo == 401 -> return // se manda cuando la misma persona vuelva a iniciar sesión
                codigo == 408 || codigo == 429 || codigo >= 500 -> {
                    programarReintento()
                    return
                }
                else -> {
                    val motivo = NetworkErrorHelper.parseResponse(respuesta)
                    Log.w(TAG, "El servidor rechazó ${p.tipo} ${p.id}: $motivo (HTTP $codigo)")
                    quitar(p.id)
                    _resultados.emit(ResultadoEnvio.Rechazado(p, motivo))
                }
            }
        }
    }

    private suspend fun enviar(p: Pendiente): Response<*> = when (p.tipo) {
        TipoPendiente.INGRESO -> api.registrarIngresoCola(p.id, json.decodeFromString<IngresoDto>(p.cuerpo))
        TipoPendiente.EGRESO -> api.registrarEgresoCola(p.id, json.decodeFromString<EgresoDto>(p.cuerpo))
        TipoPendiente.ACTIVIDAD ->
            api.createActivityCola(p.id, p.childId, json.decodeFromString<CreateActivityDto>(p.cuerpo))
        TipoPendiente.EVENTO ->
            api.createTimelineEventCola(p.id, p.childId, json.decodeFromString<QuickEventDto>(p.cuerpo))
        TipoPendiente.MENSAJE_BITACORA ->
            api.sendBitacoraMessageCola(p.id, p.childId, json.decodeFromString<SendBitacoraMessageDto>(p.cuerpo))
    }

    private fun programarReintento() {
        if (reintento?.isActive == true) return
        reintento = scope.launch {
            delay(REINTENTO_MS)
            procesar()
        }
    }

    private fun vigilarRed(context: Context) {
        val conectividad = context.getSystemService(ConnectivityManager::class.java) ?: return
        try {
            conectividad.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    procesar()
                }

                override fun onLost(network: Network) {
                    _sinConexion.value = true
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo vigilar la red: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "ColaSinConexion"
        private const val REINTENTO_MS = 30_000L
    }
}

/** Dónde se guarda la cola para que sobreviva aunque se cierre la app o se apague el teléfono. */
interface AlmacenPendientes {
    fun cargar(): List<Pendiente>
    fun guardar(lista: List<Pendiente>)
}

/** En el teléfono: archivo cifrado con EncryptedSharedPreferences. */
class AlmacenPendientesCifrado(context: Context) : AlmacenPendientes {
    private val store = OfflineStore(context, "sonrisas_cola_pendientes")
    override fun cargar(): List<Pendiente> = store.load<List<Pendiente>>(CLAVE) ?: emptyList()
    override fun guardar(lista: List<Pendiente>) = store.save(CLAVE, lista)

    private companion object {
        const val CLAVE = "pendientes"
    }
}

/** En memoria (pruebas). */
class AlmacenPendientesEnMemoria(inicial: List<Pendiente> = emptyList()) : AlmacenPendientes {
    @Volatile
    var guardados: List<Pendiente> = inicial
        private set

    override fun cargar(): List<Pendiente> = guardados
    override fun guardar(lista: List<Pendiente>) {
        guardados = lista
    }
}
