package com.example.etapa1.data.remote

import retrofit2.Response
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
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
        @Query("to") to: String? = null
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

    @GET("chats/workers/{chatId}/messages")
    suspend fun getWorkerChatMessages(@Path("chatId") chatId: String): Response<List<WorkerChatMessageDto>>

    @POST("chats/workers/{chatId}/messages")
    suspend fun sendWorkerMessage(
        @Path("chatId") chatId: String,
        @Body request: SendWorkerMessageDto
    ): Response<WorkerChatMessageDto>

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
}

