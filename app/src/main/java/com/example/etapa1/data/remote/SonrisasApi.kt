package com.example.etapa1.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
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

    @GET("lost-objects")
    suspend fun getLostObjects(
        @Query("category") category: String? = null,
        @Query("isClaimed") isClaimed: Boolean? = null,
        @Query("search") search: String? = null
    ): Response<List<LostItemDto>>

    @PATCH("lost-objects/{id}/claim")
    suspend fun claimLostObject(@Path("id") id: String): Response<Unit>
}
