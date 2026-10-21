package com.example.etapa1

import android.app.Application
import android.content.Context
import com.example.etapa1.data.export.AndroidLogSheetExporter
import com.example.etapa1.data.export.LogSheetExporter
import com.example.etapa1.data.remote.NetworkClient
import com.example.etapa1.data.remote.SubidaArchivos
import com.example.etapa1.data.remote.TokenManager
import com.example.etapa1.data.repository.AnnouncementRepository
import com.example.etapa1.data.repository.AttendanceRepository
import com.example.etapa1.data.repository.AuthRepository
import com.example.etapa1.data.repository.BitacoraRepository
import com.example.etapa1.data.repository.ChildRepository
import com.example.etapa1.data.repository.DailyLogRepository
import com.example.etapa1.data.repository.AdminRepository
import com.example.etapa1.data.repository.DailyPlanRepository
import com.example.etapa1.data.repository.FamilyGroupsRepository
import com.example.etapa1.data.repository.LostObjectsRepository
import com.example.etapa1.data.repository.MessagesRepository
import com.example.etapa1.data.repository.RoomRepository
import com.example.etapa1.data.repository.SuggestionsRepository
import com.example.etapa1.data.repository.WeeklyObservationsRepository
import com.example.etapa1.data.repository.impl.MockAdminRepositoryImpl
import com.example.etapa1.data.repository.impl.MockAnnouncementRepositoryImpl
import com.example.etapa1.data.repository.impl.MockAttendanceRepositoryImpl
import com.example.etapa1.data.repository.impl.MockAuthRepositoryImpl
import com.example.etapa1.data.repository.impl.MockBitacoraRepositoryImpl
import com.example.etapa1.data.repository.impl.MockChildRepositoryImpl
import com.example.etapa1.data.repository.impl.MockDailyLogRepositoryImpl
import com.example.etapa1.data.repository.impl.MockDailyPlanRepositoryImpl
import com.example.etapa1.data.repository.impl.MockFamilyGroupsRepositoryImpl
import com.example.etapa1.data.repository.impl.MockLostObjectsRepositoryImpl
import com.example.etapa1.data.repository.impl.MockMessagesRepositoryImpl
import com.example.etapa1.data.repository.impl.MockRoomRepositoryImpl
import com.example.etapa1.data.repository.impl.MockSuggestionsRepositoryImpl
import com.example.etapa1.data.repository.impl.MockWeeklyObservationsRepositoryImpl
import com.example.etapa1.data.repository.impl.NetworkAdminRepositoryImpl
import com.example.etapa1.data.repository.impl.NetworkAnnouncementRepositoryImpl
import com.example.etapa1.data.repository.impl.NetworkAttendanceRepositoryImpl
import com.example.etapa1.data.repository.impl.NetworkAuthRepositoryImpl
import com.example.etapa1.data.repository.impl.NetworkChildRepositoryImpl
import com.example.etapa1.data.repository.impl.NetworkDailyLogRepositoryImpl
import com.example.etapa1.data.repository.impl.NetworkDailyPlanRepositoryImpl
import com.example.etapa1.data.repository.impl.NetworkFamilyGroupsRepositoryImpl
import com.example.etapa1.data.repository.impl.NetworkLostObjectsRepositoryImpl
import com.example.etapa1.data.repository.impl.NetworkRoomRepositoryImpl
import com.example.etapa1.data.repository.impl.NetworkSuggestionsRepositoryImpl
import com.example.etapa1.data.remote.ChatConexion
import com.example.etapa1.data.remote.OfflineStore
import com.example.etapa1.data.repository.impl.NetworkBitacoraRepositoryImpl
import com.example.etapa1.data.repository.impl.NetworkMessagesRepositoryImpl
import com.example.etapa1.data.repository.impl.NetworkWeeklyObservationsRepositoryImpl

open class AppContainer(val context: Context) {

    companion object {
        /**
         * INTERRUPTOR DE FUENTE DE DATOS:
         * - false: Conecta a la API REST del backend FastAPI (por defecto http://10.0.2.2:8000/api/v1/)
         * - true : Utiliza 100% datos mock en memoria sin depender de red ni servidor
         */
        var USE_MOCKS: Boolean = false
    }

    var useMocks: Boolean = USE_MOCKS

    val tokenManager by lazy { TokenManager(context) }
    val offlineStore by lazy { OfflineStore(context) }
    val api by lazy { NetworkClient.createApi(tokenManager) }
    val subidaArchivos by lazy { SubidaArchivos(api, context) }
    val chatConexion: ChatConexion by lazy {
        ChatConexion(NetworkClient.createOkHttpClient(tokenManager))
    }

    // Repositorios Mock originales (preservados sin modificaciones)
    private val mockAuthRepository by lazy { MockAuthRepositoryImpl() }
    private val mockRoomRepository by lazy { MockRoomRepositoryImpl() }
    private val mockChildRepository by lazy { MockChildRepositoryImpl() }
    private val mockAttendanceRepository by lazy { MockAttendanceRepositoryImpl() }
    private val mockBitacoraRepository by lazy {
        MockBitacoraRepositoryImpl().apply {
            onActivityCreated = { childId ->
                mockChildRepository.cargarDetalle(childId)
            }
        }
    }
    private val mockLostObjectsRepository by lazy { MockLostObjectsRepositoryImpl() }
    private val mockSuggestionsRepository by lazy { MockSuggestionsRepositoryImpl() }
    private val mockMessagesRepository by lazy { MockMessagesRepositoryImpl() }
    private val mockAnnouncementRepository by lazy { MockAnnouncementRepositoryImpl() }
    private val mockFamilyGroupsRepository by lazy { MockFamilyGroupsRepositoryImpl() }
    private val mockWeeklyObservationsRepository by lazy { MockWeeklyObservationsRepositoryImpl() }
    private val mockDailyPlanRepository by lazy { MockDailyPlanRepositoryImpl() }
    private val mockDailyLogRepository by lazy { MockDailyLogRepositoryImpl() }
    private val mockAdminRepository by lazy { MockAdminRepositoryImpl() }

    // Repositorios de Red conectados a FastAPI
    private val networkAuthRepository by lazy { NetworkAuthRepositoryImpl(api, tokenManager) }
    private val networkRoomRepository by lazy { NetworkRoomRepositoryImpl(api) }
    private val networkChildRepository by lazy { NetworkChildRepositoryImpl(api, tokenManager) }
    private val networkAttendanceRepository by lazy { NetworkAttendanceRepositoryImpl(api) }
    private val networkAnnouncementRepository by lazy { NetworkAnnouncementRepositoryImpl(api, chatConexion) }
    private val networkSuggestionsRepository by lazy { NetworkSuggestionsRepositoryImpl(api) }
    private val networkLostObjectsRepository by lazy { NetworkLostObjectsRepositoryImpl(api, subidaArchivos) }
    private val networkFamilyGroupsRepository by lazy { NetworkFamilyGroupsRepositoryImpl(api, tokenManager, context, chatConexion) }
    private val networkWeeklyObservationsRepository by lazy { NetworkWeeklyObservationsRepositoryImpl(api) }
    private val networkDailyPlanRepository by lazy { NetworkDailyPlanRepositoryImpl(api) }
    private val networkDailyLogRepository by lazy { NetworkDailyLogRepositoryImpl(api) }
    private val networkMessagesRepository by lazy { NetworkMessagesRepositoryImpl(api, tokenManager, chatConexion) }
    private val networkBitacoraRepository by lazy {
        NetworkBitacoraRepositoryImpl(api, tokenManager, chatConexion, subidaArchivos).apply {
            onActivityCreated = { childId ->
                networkChildRepository.cargarDetalle(childId)
            }
        }
    }
    private val networkAdminRepository by lazy { NetworkAdminRepositoryImpl(api) }

    // Repositorios expuestos a la aplicación (conmutación transparente según `useMocks`)
    open val authRepository: AuthRepository
        get() = if (useMocks) mockAuthRepository else networkAuthRepository

    open val adminRepository: AdminRepository
        get() = if (useMocks) mockAdminRepository else networkAdminRepository

    open val roomRepository: RoomRepository
        get() = if (useMocks) mockRoomRepository else networkRoomRepository

    open val childRepository: ChildRepository
        get() = if (useMocks) mockChildRepository else networkChildRepository

    open val attendanceRepository: AttendanceRepository
        get() = if (useMocks) mockAttendanceRepository else networkAttendanceRepository

    open val announcementRepository: AnnouncementRepository
        get() = if (useMocks) mockAnnouncementRepository else networkAnnouncementRepository

    open val suggestionsRepository: SuggestionsRepository
        get() = if (useMocks) mockSuggestionsRepository else networkSuggestionsRepository

    open val lostObjectsRepository: LostObjectsRepository
        get() = if (useMocks) mockLostObjectsRepository else networkLostObjectsRepository

    open val bitacoraRepository: BitacoraRepository
        get() = if (useMocks) mockBitacoraRepository else networkBitacoraRepository

    open val messagesRepository: MessagesRepository
        get() = if (useMocks) mockMessagesRepository else networkMessagesRepository

    open val familyGroupsRepository: FamilyGroupsRepository
        get() = if (useMocks) mockFamilyGroupsRepository else networkFamilyGroupsRepository

    open val weeklyObservationsRepository: WeeklyObservationsRepository
        get() = if (useMocks) mockWeeklyObservationsRepository else networkWeeklyObservationsRepository

    open val dailyPlanRepository: DailyPlanRepository
        get() = if (useMocks) mockDailyPlanRepository else networkDailyPlanRepository

    open val dailyLogRepository: DailyLogRepository
        get() = if (useMocks) mockDailyLogRepository else networkDailyLogRepository

    fun asegurarConexionChat() {
        val token = tokenManager.token
        if (!useMocks && !token.isNullOrBlank()) {
            chatConexion.conectar(NetworkClient.baseUrl, token)
        }
    }

    fun desconectarChat() {
        chatConexion.desconectar()
    }

    /**
     * Al iniciar sesión: registra este teléfono para recibir notificaciones de la cuenta.
     * Solo funciona si la app se compiló con google-services.json (Firebase configurado).
     */
    fun registrarTelefonoParaPush() {
        if (useMocks) return
        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) return
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { tarea ->
                val telefono = tarea.result
                if (tarea.isSuccessful && !telefono.isNullOrBlank()) {
                    com.example.etapa1.data.remote.PushNotificationHelper.registerDeviceToken(api, tokenManager, telefono)
                }
            }
        } catch (_: Exception) {
            // Firebase sin configurar: la app funciona igual, solo sin notificaciones push
        }
    }

    /** Al iniciar sesión: pide al servidor los datos del usuario que acaba de entrar. */
    fun recargarTodo() {
        if (useMocks) return
        // Las salas solo las consulta el personal; para la familia el servidor responde 403
        if (!tokenManager.userRole.equals("FAMILIAR", ignoreCase = true)) roomRepository.refresh()
        childRepository.refresh()
        attendanceRepository.refresh()
        announcementRepository.refresh()
        suggestionsRepository.refresh()
        lostObjectsRepository.refresh()
        messagesRepository.refresh()
        familyGroupsRepository.refresh()
        weeklyObservationsRepository.refresh()
        dailyPlanRepository.refresh()
        dailyLogRepository.refresh()
    }

    /** Al cerrar sesión: borra todo lo cargado para que el siguiente usuario no vea datos ajenos. */
    fun limpiarTodo() {
        if (useMocks) return
        roomRepository.limpiar()
        childRepository.limpiar()
        attendanceRepository.limpiar()
        announcementRepository.limpiar()
        suggestionsRepository.limpiar()
        lostObjectsRepository.limpiar()
        messagesRepository.limpiar()
        familyGroupsRepository.limpiar()
        weeklyObservationsRepository.limpiar()
        dailyPlanRepository.limpiar()
        dailyLogRepository.limpiar()
        bitacoraRepository.limpiar()
    }

    val logSheetExporter: LogSheetExporter by lazy { AndroidLogSheetExporter(context) }
}

class SonrisasApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
