package com.example.etapa1.data.remote

import com.example.etapa1.data.remote.RemoteSync.toIso
import com.example.etapa1.data.repository.impl.NetworkBitacoraRepositoryImpl
import com.example.etapa1.data.repository.impl.NetworkChildRepositoryImpl
import com.example.etapa1.data.repository.impl.NetworkDailyLogRepositoryImpl
import com.example.etapa1.data.repository.impl.NetworkLostObjectsRepositoryImpl
import com.example.etapa1.data.repository.impl.NetworkSuggestionsRepositoryImpl
import com.example.etapa1.data.repository.impl.NetworkDailyPlanRepositoryImpl
import com.example.etapa1.data.repository.impl.NetworkWeeklyObservationsRepositoryImpl
import com.example.etapa1.domain.SimpleDate
import com.example.etapa1.model.DailyMenu
import com.example.etapa1.model.DailyPlan
import com.example.etapa1.model.EgresoLog
import com.example.etapa1.model.FoodConsumptionOption
import com.example.etapa1.model.IngresoLog
import com.example.etapa1.model.LostItem
import com.example.etapa1.model.ParentSuggestion
import com.example.etapa1.model.SuggestionCategory
import com.example.etapa1.model.SuggestionStatus
import com.example.etapa1.model.TimelineItem
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
        repo.abrirSala("sala_1a")
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
        familyRepo.abrirSala("sala_1a")
        eventually("la familia ve la planeación") { familyRepo.planFor(date)?.activities?.any { it.title == title } == true }
    }

    @Test
    fun registroDiario_repositorioDeRed() {
        assumeTrue(serverUp)
        val repo = NetworkDailyLogRepositoryImpl(educadora)
        val date = SimpleDate.today()
        val ingreso = IngresoLog(
            time = "08:05", assistantName = "Laura Gómez", deliveredBy = "Mamá", receivedBy = "Laura Gómez",
            isSick = true, medicationWithPrescription = MedicationPrescriptionOption.SI,
            hasSymptoms = false, arrivedClean = true, backpackComplete = true, goodPhysicalCondition = true
        )
        val egreso = EgresoLog(
            time = "15:30", goodPhysicalCondition = true, collectedBy = "Papá",
            deliveredClean = true, belongingsComplete = true, deliveredByStaff = "Laura Gómez"
        )
        repo.saveIngreso("lucia_garcia", date, ingreso).getOrThrow()
        repo.updateEstadia("lucia_garcia", date) {
            it.copy(breakfast = FoodConsumptionOption.PIDIO_MAS, mood = MoodState.FELIZ, slept = true, napMinutes = 50)
        }.getOrThrow()
        repo.saveEgreso("lucia_garcia", date, egreso).getOrThrow()

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
        assertEquals(MedicationPrescriptionOption.SI, log.ingreso?.medicationWithPrescription)
        assertEquals(50, log.estadia?.napMinutes)
        assertEquals("15:30", log.egreso?.time)
    }

    @Test
    fun bitacora_cadaMensajeLlegaAlNinoCorrecto() {
        assumeTrue(serverUp)
        val repo = NetworkBitacoraRepositoryImpl(educadora, null, ChatConexion(OkHttpClient()))
        val texto = "Mensaje para Lucía ${UUID.randomUUID().toString().take(6)}"

        repo.abrir("lucia_garcia")
        repo.addChatMessage(childId = "lucia_garcia", text = texto)
        eventually("el mensaje se guarda en el chat de Lucía") {
            runBlocking { educadora.getBitacoraMessages("lucia_garcia").body()!! }.any { it.message == texto }
        }
        // Antes todos los mensajes se iban al chat de Mateo
        val deMateo = runBlocking { educadora.getBitacoraMessages("mateo_garcia").body()!! }
        assertTrue(deMateo.none { it.message == texto })

        // Al abrir a otro niño no aparece el mensaje de Lucía
        repo.abrir("sofia_lopez")
        eventually("la línea de tiempo de Sofía no trae lo de Lucía") {
            repo.childIdActual.value == "sofia_lopez" &&
                repo.timeline.value.none { it is TimelineItem.ChatMessage && it.message == texto }
        }
    }

    @Test
    fun ninosSalasYResumenSemanal_repositorioDeRed() {
        assumeTrue(serverUp)
        val sala = NetworkChildRepositoryImpl(educadora)
        sala.abrirSala("sala_2b")
        eventually("niños de la Sala 2B") { sala.childrenOfRoom.value.isNotEmpty() }
        assertTrue(sala.childrenOfRoom.value.all { it.roomId == "sala_2b" && it.roomText == "Sala 2B" })

        val familiaRepo = NetworkChildRepositoryImpl(familia)
        familiaRepo.refresh()
        eventually("hijos de la familia") { familiaRepo.parentChildren.value.any { it.id == "mateo_garcia" } }
        val mateo = familiaRepo.parentChildren.value.first { it.id == "mateo_garcia" }
        familiaRepo.cargarDetalle(mateo.id)
        eventually("resúmenes del servidor") { familiaRepo.getWeeklySummariesForChild(mateo).size == 4 }
        val semana = familiaRepo.getWeeklySummariesForChild(mateo)[1]
        assertTrue(semana.executiveSummary.isNotBlank())

        familiaRepo.markWeeklySummaryReadStatus(mateo.id, semana.weekRangeText, true, "ahora")
        eventually("la lectura queda en el servidor") {
            runBlocking { educadora.getWeeklySummaries(mateo.id).body()!! }.first { it.id == semana.id }.isReadByParent
        }
        familiaRepo.markWeeklySummaryReadStatus(mateo.id, semana.weekRangeText, false)
        eventually("y se puede quitar") {
            !runBlocking { educadora.getWeeklySummaries(mateo.id).body()!! }.first { it.id == semana.id }.isReadByParent
        }
    }

    @Test
    fun objetosPerdidos_repositorioDeRed() {
        assumeTrue(serverUp)
        val titulo = "Gorra ${UUID.randomUUID().toString().take(6)}"
        val repoFamilia = NetworkLostObjectsRepositoryImpl(familia)
        repoFamilia.publishLostItem(
            LostItem(id = "", title = titulo, description = "Azul con su nombre", category = "Ropa", room = "Sala 1A")
        )
        eventually("se publica en el servidor") {
            repoFamilia.lostItems.value.any { it.title == titulo && !it.id.startsWith("temp_") }
        }
        val id = repoFamilia.lostItems.value.first { it.title == titulo }.id

        val repoEducadora = NetworkLostObjectsRepositoryImpl(educadora)
        eventually("la educadora lo ve") { repoEducadora.lostItems.value.any { it.id == id } }
        repoEducadora.deleteLostItem(id)
        eventually("se elimina en el servidor") {
            runBlocking { educadora.getLostObjects().body()!! }.none { it.id == id }
        }
    }

    @Test
    fun sugerencias_repositorioDeRed() {
        assumeTrue(serverUp)
        val asunto = "Asunto ${UUID.randomUUID().toString().take(6)}"
        val repoFamilia = NetworkSuggestionsRepositoryImpl(familia)
        repoFamilia.addSuggestion(
            ParentSuggestion(
                id = "", parentName = "", childName = "", roomName = "", date = "", time = "",
                category = SuggestionCategory.GENERAL, subject = asunto, content = "¿Hay junta el viernes?",
                status = SuggestionStatus.PENDIENTE, response = null, responseDate = null, responderName = null
            ),
            childId = "mateo_garcia"
        )
        eventually("la sugerencia llega con el niño real") {
            repoFamilia.suggestions.value.any { it.subject == asunto && it.childName == "Mateo García López" }
        }
        val id = repoFamilia.suggestions.value.first { it.subject == asunto }.id

        val repoEducadora = NetworkSuggestionsRepositoryImpl(educadora)
        eventually("la educadora la ve") { repoEducadora.suggestions.value.any { it.id == id } }
        repoEducadora.updateSuggestionStatus(id, SuggestionStatus.ATENDIDA, "Sí, a las 5")
        eventually("la respuesta queda firmada por quien respondió") {
            runBlocking { familia.getSuggestions().body()!! }.first { it.id == id }.responderName == "Laura Gómez (Educadora)"
        }
    }
}
