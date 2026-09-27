package com.twoDLedger.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class ActivationRequest(
    @field:Json(name = "cd_key") val cd_key: String,
    @field:Json(name = "device_fingerprint") val device_fingerprint: String,
    @field:Json(name = "device_model") val device_model: String? = null
)

@JsonClass(generateAdapter = true)
data class ActivationResponse(
    @field:Json(name = "status") val status: String? = null,
    @field:Json(name = "token") val token: String? = null,
    @field:Json(name = "expires_at") val expires_at: Long? = null,
    @field:Json(name = "device_migrated") val device_migrated: Boolean? = null,
    @field:Json(name = "remaining_days") val remaining_days: Int? = null,
    @field:Json(name = "message") val message: String? = null,
    @field:Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class CheckStatusRequest(
    @field:Json(name = "cd_key") val cd_key: String,
    @field:Json(name = "device_fingerprint") val device_fingerprint: String
)

@JsonClass(generateAdapter = true)
data class CheckStatusResponse(
    @field:Json(name = "status") val status: String? = null,
    @field:Json(name = "token") val token: String? = null,
    @field:Json(name = "expires_at") val expires_at: Long? = null,
    @field:Json(name = "message") val message: String? = null,
    @field:Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class VerifyLicenseRequest(
    @field:Json(name = "cd_key") val cd_key: String,
    @field:Json(name = "device_fingerprint") val device_fingerprint: String
)

@JsonClass(generateAdapter = true)
data class VerifyLicenseResponse(
    @field:Json(name = "valid") val valid: Boolean = false,
    @field:Json(name = "reason") val reason: String? = null,
    @field:Json(name = "message") val message: String? = null,
    @field:Json(name = "expires_at") val expires_at: Long? = null
)

@JsonClass(generateAdapter = true)
data class RestoreLicenseRequest(
    @field:Json(name = "device_fingerprint") val device_fingerprint: String,
    @field:Json(name = "device_model") val device_model: String? = null
)

@JsonClass(generateAdapter = true)
data class RestoreLicenseResponse(
    @field:Json(name = "status") val status: String? = null,
    @field:Json(name = "token") val token: String? = null,
    @field:Json(name = "cd_key") val cd_key: String? = null,
    @field:Json(name = "expires_at") val expires_at: Long? = null,
    @field:Json(name = "message") val message: String? = null,
    @field:Json(name = "error") val error: String? = null
)

interface LicenseApi {
    @POST("/activate")
    suspend fun activateLicense(@Body request: ActivationRequest): Response<ActivationResponse>

    @POST("/check-status")
    suspend fun checkStatus(@Body request: CheckStatusRequest): Response<CheckStatusResponse>

    @POST("/verify")
    suspend fun verifyLicense(@Body request: VerifyLicenseRequest): Response<VerifyLicenseResponse>

    @POST("/restore")
    suspend fun restoreLicense(@Body request: RestoreLicenseRequest): Response<RestoreLicenseResponse>
}

