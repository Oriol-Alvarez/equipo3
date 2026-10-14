package com.example.etapa1.data.remote

import com.example.etapa1.data.remote.RemoteSync.toIso
import com.example.etapa1.data.repository.impl.NetworkDailyLogRepositoryImpl
import com.example.etapa1.data.repository.impl.NetworkDailyPlanRepositoryImpl
import com.example.etapa1.data.repository.impl.NetworkWeeklyObservationsRepositoryImpl
import com.example.etapa1.domain.SimpleDate
import com.example.etapa1.model.DailyMenu
import com.example.etapa1.model.DailyPlan
import com.example.etapa1.model.EgresoLog
import com.example.etapa1.model.FoodConsumptionOption
import com.example.etapa1.model.IngresoLog
import com.example.etapa1.model.MedicationPrescriptionOption
import com.example.etapa1.model.MoodState
import com.example.etapa1.model.ObservationAuthorRole
import com.example.etapa1.model.PlannedActivity
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.net.Socket
import java.util.UUID

/**
 * Prueba de integración contra el backend real (docker compose up en backend/).
 * Usa los mismos DTOs y repositorios de red que la app.
 *
 * Crea datos de prueba en la base, así que solo corre si se pide explícitamente:
 *   (PowerShell)  $env:SONRISAS_IT="1"; .\gradlew.bat testDebugUnitTest --tests "*BackendIntegrationTest"
 * Si no está la variable o el servidor no responde en localhost:8000, las pruebas se omiten.
 */
class BackendIntegrationTest {

    companion object {
        private const val BASE_URL = "http://localhost:8000/api/v1/"
        private var serverUp = false
        private lateinit var educadora: SonrisasApi
        private lateinit var familia: SonrisasApi

        @BeforeClass
        @JvmStatic
        fun setUp() {
            serverUp = System.getenv("SONRISAS_IT") == "1" && try {
                Socket("localhost", 8000).use { true }
            } catch (_: Exception) {
                false
            }
            if (!serverUp) return
            educadora = apiFor(login("educadora@sonrisas.edu", "EDUCADORA"))
            familia = apiFor(login("familiar@sonrisas.edu", "FAMILIAR"))
        }

        private fun apiFor(token: String?): SonrisasApi {
            val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor { token }).build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(NetworkClient.json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(SonrisasApi::class.java)
        }

        private fun login(email: String, role: String): String = runBlocking {
            val res = apiFor(null).login(LoginRequestDto(email, "Password123!", role))
            assertTrue("login $email: ${res.code()}", res.isSuccessful)
            res.body()!!.token
        }
    }

    /** Espera a que termine la llamada en segundo plano del repositorio. */
    private fun eventually(what: String, check: () -> Boolean) {
        repeat(50) {
            if (check()) return
            Thread.sleep(100)
        }
        throw AssertionError("No se cumplió: $what")
    }

    @Test
    fun gruposConFamilias_contratoCompleto() = runBlocking {
        assumeTrue(serverUp)
        val candidates = educadora.getFamilyGroupCandidates().body()!!
        assertTrue(candidates.any { it.childId == "mateo_garcia" })

        val created = educadora.createFamilyGroup(CreateFamilyGroupDto("Prueba app", listOf("mateo_garcia"))).body()
        assertNotNull(created)

        val sent = familia.sendFamilyGroupMessage(created!!.id, SendFamilyGroupMessageDto("Hola desde la app")).body()
        assertEquals("Hola desde la app", sent!!.text)

        val groups = familia.getFamilyGroups().body()!!
        assertEquals("el grupo con el mensaje más reciente va primero", created.id, groups.first().id)
        val messages = familia.getFamilyGroupMessages(created.id).body()!!
        assertEquals("Hola desde la app", messages.last().text)
    }

    @Test
    fun observacionesSemanales_repositorioDeRed() {
        assumeTrue(serverUp)
        val repo = NetworkWeeklyObservationsRepositoryImpl(familia)
        val week = "Semana app ${UUID.randomUUID()}"
        val result = repo.addObservation("mateo_garcia", week, ObservationAuthorRole.FAMILIA, "Familia", "Comió muy bien")
        assertTrue(result.isSuccess)
        eventually("la observación se guarda en el servidor") {
            repo.observations.value.any { it.weekKey == week && !it.id.startsWith("wobs_local") }
        }
        assertEquals(
            ObservationAuthorRole.FAMILIA,
            repo.observations.value.first { it.weekKey == week }.authorRole
        )
    }

    @Test
    fun planeacion_repositorioDeRed() {
        assumeTrue(serverUp)
        val repo = NetworkDailyPlanRepositoryImpl(educadora)
        val date = SimpleDate.today()
        val title = "Actividad ${UUID.randomUUID().toString().take(6)}"
        repo.savePlan(
            DailyPlan(
                date = date,
                menu = DailyMenu("Avena", "Manzana", "Sopa de verduras"),
                activities = listOf(PlannedActivity("09:00", title))
            )
        )
        eventually("la planeación se guarda en el servidor") {
            runBlocking { familia.getDailyPlans(date.toIso(), date.toIso()).body()!! }
                .any { plan -> plan.activities.any { it.title == title } }
        }
        val familyRepo = NetworkDailyPlanRepositoryImpl(familia)
        eventually("la familia ve la planeación") { familyRepo.planFor(date)?.activities?.any { it.title == title } == true }
    }

    @Test
    fun registroDiario_repositorioDeRed() {
        assumeTrue(serverUp)
        val repo = NetworkDailyLogRepositoryImpl(educadora)
        val date = SimpleDate.today()
        assertTrue(
            repo.saveIngreso(
                "lucia_garcia", date,
                IngresoLog(time = "08:05", deliveredBy = "Mamá", medicationWithPrescription = MedicationPrescriptionOption.NO_APLICA)
            ).isSuccess
        )
        assertTrue(
            repo.updateEstadia("lucia_garcia", date) {
                it.copy(breakfast = FoodConsumptionOption.PIDIO_MAS, mood = MoodState.FELIZ, napMinutes = 50)
            }.isSuccess
        )
        assertTrue(repo.saveEgreso("lucia_garcia", date, EgresoLog(time = "15:30", collectedBy = "Papá")).isSuccess)

        eventually("los tres apartados quedan en el servidor") {
            val server = runBlocking { familia.getDailyLogs(date.toIso(), date.toIso()).body()!! }
                .find { it.childId == "lucia_garcia" }
            server?.ingreso?.time == "08:05" &&
                server.estadia?.breakfast == "PIDIO_MAS" &&
                server.egreso?.collectedBy == "Papá"
        }

        // Otro repositorio (como otro teléfono) lee lo mismo ya convertido a los enums de la app
        val other = NetworkDailyLogRepositoryImpl(familia)
        eventually("se carga el registro") { other.logFor("lucia_garcia", date) != null }
        val log = other.logFor("lucia_garcia", date)!!
        assertEquals(FoodConsumptionOption.PIDIO_MAS, log.estadia?.breakfast)
        assertEquals(MedicationPrescriptionOption.NO_APLICA, log.ingreso?.medicationWithPrescription)
        assertEquals("15:30", log.egreso?.time)
    }
}
