package com.example.etapa1.data.remote

import retrofit2.Response
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface SonrisasApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<LoginResponseDto>

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequestDto): Response<RefreshResponseDto>

    @POST("auth/logout")
    suspend fun logout(@Body request: RefreshRequestDto): Response<Unit>

    @POST("auth/device-token")
    suspend fun registerDeviceToken(@Body request: DeviceTokenRequestDto): Response<Unit>

    @DELETE("auth/device-token")
    suspend fun unregisterDeviceToken(@Body request: DeviceTokenRequestDto): Response<Unit>

    @GET("auth/me")
    suspend fun me(): Response<UserDto>

    @GET("rooms")
    suspend fun getRooms(): Response<List<RoomDto>>

    @GET("rooms/{roomId}/children")
    suspend fun getChildrenByRoom(
        @Path("roomId") roomId: String,
        @Query("isPresent") isPresent: Boolean? = null
    ): Response<List<ChildDto>>

    @GET("parents/me/children")
    suspend fun getMyChildren(): Response<List<ChildDto>>

    @GET("children/{childId}")
    suspend fun getChild(@Path("childId") childId: String): Response<ChildDto>

    @GET("children/{childId}/full-profile")
    suspend fun getChildFullProfile(@Path("childId") childId: String): Response<ChildFullProfileDto>

    @PUT("children/{childId}/full-profile")
    suspend fun updateChildFullProfile(
        @Path("childId") childId: String,
        @Body request: ChildProfileUpdateDto
    ): Response<ChildFullProfileDto>

    @GET("children/{childId}/weekly-summaries")
    suspend fun getWeeklySummaries(@Path("childId") childId: String): Response<List<WeeklySummaryDto>>

    /** RF-19: meses disponibles (YYYY-MM) y reporte de un mes. */
    @GET("children/{childId}/monthly-reports")
    suspend fun getMesesReporte(@Path("childId") childId: String): Response<List<String>>

    @GET("children/{childId}/monthly-reports/{month}")
    suspend fun getReporteMensual(
        @Path("childId") childId: String,
        @Path("month") mes: String
    ): Response<ReporteMensualDto>

    @PUT("children/{childId}/weekly-summaries/{summaryId}/feedback")
    suspend fun sendWeeklySummaryFeedback(
        @Path("childId") childId: String,
        @Path("summaryId") summaryId: String,
        @Body request: WeeklySummaryFeedbackDto
    ): Response<WeeklySummaryDto>

    @GET("children/{childId}/bitacoras")
    suspend fun getBitacoras(@Path("childId") childId: String): Response<List<BitacoraDiaDto>>

    @GET("children/{childId}/educator")
    suspend fun getChildEducator(@Path("childId") childId: String): Response<EducatorDto>

    @POST("attendance/ingreso")
    suspend fun registrarIngreso(@Body request: IngresoDto): Response<AttendanceResponseDto>

    @POST("attendance/egreso")
    suspend fun registrarEgreso(@Body request: EgresoDto): Response<AttendanceResponseDto>

    @GET("attendance/records")
    suspend fun getAttendanceRecords(
        @Query("childId") childId: String? = null,
        @Query("date") date: String? = null
    ): Response<List<AttendanceRecordDto>>

    @GET("announcements")
    suspend fun getAnnouncements(): Response<List<AnnouncementDto>>

    @GET("announcements/unread-count")
    suspend fun getUnreadAnnouncementsCount(): Response<UnreadCountDto>

    @PATCH("announcements/{id}/read")
    suspend fun markAnnouncementAsRead(@Path("id") id: String): Response<Unit>

    @GET("suggestions")
    suspend fun getSuggestions(
        @Query("status") status: String? = null,
        @Query("category") category: String? = null,
        @Query("search") search: String? = null
    ): Response<List<SuggestionDto>>

    @POST("suggestions")
    suspend fun createSuggestion(@Body request: CreateSuggestionDto): Response<SuggestionDto>

    @PATCH("suggestions/{id}/status")
    suspend fun updateSuggestionStatus(
        @Path("id") id: String,
        @Body request: UpdateSuggestionStatusDto
    ): Response<SuggestionDto>

    @GET("lost-objects")
    suspend fun getLostObjects(
        @Query("category") category: String? = null,
        @Query("isClaimed") isClaimed: Boolean? = null,
        @Query("search") search: String? = null
    ): Response<List<LostItemDto>>

    @PATCH("lost-objects/{id}/claim")
    suspend fun claimLostObject(@Path("id") id: String): Response<Unit>

    // ---------------------------------------------------------------- grupos con familias
    @GET("family-groups")
    suspend fun getFamilyGroups(): Response<List<FamilyGroupDto>>

    @GET("family-groups/candidates")
    suspend fun getFamilyGroupCandidates(): Response<List<FamilyGroupCandidateDto>>

    @POST("family-groups")
    suspend fun createFamilyGroup(@Body request: CreateFamilyGroupDto): Response<FamilyGroupDto>

    @GET("family-groups/{groupId}/messages")
    suspend fun getFamilyGroupMessages(@Path("groupId") groupId: String): Response<List<FamilyGroupMessageDto>>

    @POST("family-groups/{groupId}/messages")
    suspend fun sendFamilyGroupMessage(
        @Path("groupId") groupId: String,
        @Body request: SendFamilyGroupMessageDto
    ): Response<FamilyGroupMessageDto>

    @Multipart
    @POST("media/upload")
    suspend fun uploadMedia(
        @Part file: MultipartBody.Part,
        @Part("folder") folder: RequestBody
    ): Response<MediaUploadDto>

    // ---------------------------------------------------------------- observaciones semanales
    @GET("weekly-observations")
    suspend fun getWeeklyObservations(): Response<List<WeeklyObservationDto>>

    @POST("children/{childId}/weekly-observations")
    suspend fun addWeeklyObservation(
        @Path("childId") childId: String,
        @Body request: CreateWeeklyObservationDto
    ): Response<WeeklyObservationDto>

    // ---------------------------------------------------------------- planeación y menú
    @GET("daily-plans")
    suspend fun getDailyPlans(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("roomId") roomId: String? = null
    ): Response<List<DailyPlanDto>>

    @PUT("daily-plans/{date}")
    suspend fun saveDailyPlan(
        @Path("date") date: String,
        @Body request: SaveDailyPlanDto
    ): Response<DailyPlanDto>

    // ---------------------------------------------------------------- registro diario
    @GET("daily-logs")
    suspend fun getDailyLogs(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<List<DailyLogDto>>

    @PUT("children/{childId}/daily-logs/{date}/ingreso")
    suspend fun saveIngresoLog(
        @Path("childId") childId: String,
        @Path("date") date: String,
        @Body request: IngresoLogDto
    ): Response<DailyLogDto>

    @PUT("children/{childId}/daily-logs/{date}/estadia")
    suspend fun saveEstadiaLog(
        @Path("childId") childId: String,
        @Path("date") date: String,
        @Body request: EstadiaLogDto
    ): Response<DailyLogDto>

    @PUT("children/{childId}/daily-logs/{date}/egreso")
    suspend fun saveEgresoLog(
        @Path("childId") childId: String,
        @Path("date") date: String,
        @Body request: EgresoLogDto
    ): Response<DailyLogDto>

    // ---------------------------------------------------------------- mensajería de personal y chat global
    @GET("staff/members")
    suspend fun getStaffMembers(): Response<List<StaffMemberDto>>

    @GET("chats/workers")
    suspend fun getWorkerChats(): Response<List<WorkerChatDto>>

    @POST("chats/workers")
    suspend fun createWorkerGroup(@Body request: CreateWorkerGroupDto): Response<WorkerChatDto>

    @GET("chats/workers/{chatId}/messages")
    suspend fun getWorkerChatMessages(@Path("chatId") chatId: String): Response<List<WorkerChatMessageDto>>

    @POST("chats/workers/{chatId}/messages")
    suspend fun sendWorkerMessage(
        @Path("chatId") chatId: String,
        @Body request: SendWorkerMessageDto
    ): Response<WorkerChatMessageDto>

    @PUT("chats/workers/{chatId}/read")
    suspend fun markWorkerChatRead(@Path("chatId") chatId: String): Response<Unit>

    @GET("chats/staff-global/messages")
    suspend fun getStaffGlobalChat(): Response<StaffGlobalChatDto>

    @POST("chats/staff-global/messages")
    suspend fun sendStaffGlobalMessage(@Body request: SendWorkerMessageDto): Response<WorkerChatMessageDto>

    // ---------------------------------------------------------------- chat bitácora individual y línea de tiempo
    @GET("rooms/{roomId}/parent-chats")
    suspend fun getParentChatsByRoom(@Path("roomId") roomId: String): Response<List<ParentChatSummaryDto>>

    @GET("chats/bitacora/{childId}/messages")
    suspend fun getBitacoraMessages(@Path("childId") childId: String): Response<List<BitacoraMessageDto>>

    @POST("chats/bitacora/{childId}/messages")
    suspend fun sendBitacoraMessage(
        @Path("childId") childId: String,
        @Body request: SendBitacoraMessageDto
    ): Response<BitacoraMessageDto>

    @PUT("chats/bitacora/{childId}/read")
    suspend fun markBitacoraChatRead(@Path("childId") childId: String): Response<Unit>

    @GET("children/{childId}/timeline")
    suspend fun getChildTimeline(
        @Path("childId") childId: String,
        @Query("date") date: String? = null
    ): Response<List<TimelineItemDto>>

    @POST("children/{childId}/activities")
    suspend fun createActivity(
        @Path("childId") childId: String,
        @Body request: CreateActivityDto
    ): Response<NewActivityResponseDto>

    @POST("children/{childId}/timeline/events")
    suspend fun createTimelineEvent(
        @Path("childId") childId: String,
        @Body request: QuickEventDto
    ): Response<TimelineItemDto>

    // ---------------------------------------------------------------- RF-11: envíos de la cola sin conexión
    // Llevan Idempotency-Key: si un reintento llega dos veces, el servidor no duplica el registro.

    @POST("attendance/ingreso")
    suspend fun registrarIngresoCola(
        @Header("Idempotency-Key") llave: String,
        @Body request: IngresoDto
    ): Response<AttendanceResponseDto>

    @POST("attendance/egreso")
    suspend fun registrarEgresoCola(
        @Header("Idempotency-Key") llave: String,
        @Body request: EgresoDto
    ): Response<AttendanceResponseDto>

    @POST("children/{childId}/activities")
    suspend fun createActivityCola(
        @Header("Idempotency-Key") llave: String,
        @Path("childId") childId: String,
        @Body request: CreateActivityDto
    ): Response<NewActivityResponseDto>

    @POST("children/{childId}/timeline/events")
    suspend fun createTimelineEventCola(
        @Header("Idempotency-Key") llave: String,
        @Path("childId") childId: String,
        @Body request: QuickEventDto
    ): Response<TimelineItemDto>

    @POST("chats/bitacora/{childId}/messages")
    suspend fun sendBitacoraMessageCola(
        @Header("Idempotency-Key") llave: String,
        @Path("childId") childId: String,
        @Body request: SendBitacoraMessageDto
    ): Response<BitacoraMessageDto>

    // ---------------------------------------------------------------- RF-10: borrar una foto
    @DELETE("media/{fileId}")
    suspend fun eliminarArchivo(
        @Path("fileId") fileId: String,
        @Query("reason") motivo: String = ""
    ): Response<EliminarArchivoDto>

    // ---------------------------------------------------------------- Gestión de Avisos
    @POST("announcements")
    suspend fun createAnnouncement(@Body request: CreateAnnouncementDto): Response<AnnouncementDto>

    @PUT("announcements/{id}")
    suspend fun updateAnnouncement(
        @Path("id") id: String,
        @Body request: UpdateAnnouncementDto
    ): Response<AnnouncementDto>

    @DELETE("announcements/{id}")
    suspend fun deleteAnnouncement(@Path("id") id: String): Response<Unit>

    // ---------------------------------------------------------------- Gestión de Objetos Perdidos
    @POST("lost-objects")
    suspend fun createLostObject(@Body request: CreateLostObjectDto): Response<LostItemDto>

    @PUT("lost-objects/{id}")
    suspend fun updateLostObject(
        @Path("id") id: String,
        @Body request: UpdateLostObjectDto
    ): Response<LostItemDto>

    @DELETE("lost-objects/{id}")
    suspend fun deleteLostObject(@Path("id") id: String): Response<Unit>

    // ---------------------------------------------------------------- Gestión de Sugerencias
    @DELETE("suggestions/{id}")
    suspend fun deleteSuggestion(@Path("id") id: String): Response<Unit>

    // ---------------------------------------------------------------- Panel de Administración (/admin)
    @GET("admin/users")
    suspend fun getAdminUsers(
        @Query("role") role: String? = null,
        @Query("search") search: String? = null
    ): Response<List<UserDto>>

    @POST("admin/users")
    suspend fun createAdminUser(@Body request: AdminUserCreateDto): Response<UserDto>

    @PUT("admin/users/{id}")
    suspend fun updateAdminUser(
        @Path("id") id: String,
        @Body request: AdminUserUpdateDto
    ): Response<UserDto>

    @DELETE("admin/users/{id}")
    suspend fun deleteAdminUser(@Path("id") id: String): Response<Unit>

    @GET("admin/logs")
    suspend fun getAdminLogs(
        @Query("category") category: String? = null,
        @Query("search") search: String? = null
    ): Response<List<AdminLogDto>>

    @GET("admin/stats")
    suspend fun getAdminStats(): Response<AdminStatsDto>

    @GET("admin/children")
    suspend fun getAdminAllChildren(): Response<List<AdminChildSummaryDto>>

    @POST("admin/users/{userId}/children")
    suspend fun createAndLinkChild(
        @Path("userId") userId: String,
        @Body request: AdminCreateChildDto
    ): Response<AdminChildSummaryDto>

    @POST("admin/users/{userId}/children/{childId}")
    suspend fun linkChildToParent(
        @Path("userId") userId: String,
        @Path("childId") childId: String
    ): Response<AdminChildSummaryDto>

    @DELETE("admin/users/{userId}/children/{childId}")
    suspend fun unlinkChildFromParent(
        @Path("userId") userId: String,
        @Path("childId") childId: String
    ): Response<Unit>

    @PUT("admin/children/{childId}")
    suspend fun updateAdminChild(
        @Path("childId") childId: String,
        @Body request: AdminUpdateChildDto
    ): Response<AdminChildSummaryDto>
}


