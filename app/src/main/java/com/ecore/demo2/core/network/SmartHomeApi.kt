package com.ecore.demo2.core.network

import com.ecore.demo2.core.network.dto.AlertDto
import com.ecore.demo2.core.network.dto.AuthResponseDto
import com.ecore.demo2.core.network.dto.BudgetDto
import com.ecore.demo2.core.network.dto.BudgetRequestDto
import com.ecore.demo2.core.network.dto.ChatRequestDto
import com.ecore.demo2.core.network.dto.ChatResponseDto
import com.ecore.demo2.core.network.dto.DashboardDto
import com.ecore.demo2.core.network.dto.DeviceDto
import com.ecore.demo2.core.network.dto.ElectricalReadingDto
import com.ecore.demo2.core.network.dto.HistoryRecordDto
import com.ecore.demo2.core.network.dto.LoginRequestDto
import com.ecore.demo2.core.network.dto.PasswordResetRequestDto
import com.ecore.demo2.core.network.dto.RegisterRequestDto
import com.ecore.demo2.core.network.dto.SystemStatusDto
import com.ecore.demo2.core.network.dto.UserDto
import com.ecore.demo2.core.network.dto.WaterReadingDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Contrato REST con el backend local (FastAPI). Rutas relativas (sin "/" inicial)
 * para respetar la URL base configurada en Ajustes. Documentado en docs/API.md.
 */
interface SmartHomeApi {

    // ---- Auth
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequestDto): AuthResponseDto

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequestDto): AuthResponseDto

    @GET("api/auth/me")
    suspend fun me(): UserDto

    @POST("api/auth/logout")
    suspend fun logout()

    @POST("api/auth/password-reset")
    suspend fun requestPasswordReset(@Body body: PasswordResetRequestDto)

    // ---- Datos
    @GET("api/dashboard")
    suspend fun dashboard(): DashboardDto

    @GET("api/electricity/current")
    suspend fun currentElectricity(): ElectricalReadingDto

    @GET("api/electricity/history")
    suspend fun electricityHistory(@Query("period") period: String): List<ElectricalReadingDto>

    @GET("api/water/current")
    suspend fun currentWater(): WaterReadingDto

    @GET("api/water/history")
    suspend fun waterHistory(@Query("period") period: String): List<WaterReadingDto>

    @GET("api/devices")
    suspend fun devices(): List<DeviceDto>

    @GET("api/devices/{id}")
    suspend fun device(@Path("id") id: String): DeviceDto

    @GET("api/alerts")
    suspend fun alerts(): List<AlertDto>

    @PATCH("api/alerts/{id}/read")
    suspend fun markAlertRead(@Path("id") id: String): AlertDto

    @GET("api/budgets")
    suspend fun budgets(): List<BudgetDto>

    @POST("api/budgets")
    suspend fun createBudget(@Body body: BudgetRequestDto): BudgetDto

    @PUT("api/budgets/{id}")
    suspend fun updateBudget(@Path("id") id: String, @Body body: BudgetRequestDto): BudgetDto

    @GET("api/history")
    suspend fun history(@Query("period") period: String): List<HistoryRecordDto>

    @POST("api/assistant/chat")
    suspend fun chat(@Body body: ChatRequestDto): ChatResponseDto

    @GET("api/system/status")
    suspend fun systemStatus(): SystemStatusDto
}
