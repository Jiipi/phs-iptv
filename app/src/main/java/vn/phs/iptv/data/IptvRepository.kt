package vn.phs.iptv.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import vn.phs.iptv.BuildConfig
import vn.phs.iptv.data.local.ProvisioningDataStore
import vn.phs.iptv.data.remote.IptvApi
import vn.phs.iptv.data.remote.dto.ApiError
import vn.phs.iptv.data.remote.dto.ContentResponse
import vn.phs.iptv.data.remote.dto.MeRequest
import vn.phs.iptv.data.remote.dto.MeResponse
import vn.phs.iptv.data.remote.dto.RegisterRequest
import vn.phs.iptv.data.remote.dto.ScreenResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IptvRepository @Inject constructor(
    private val api: IptvApi,
    private val store: ProvisioningDataStore,
    private val json: Json,
) {
    suspend fun preparePairing(): MeResponse {
        val deviceId = store.getOrCreateDeviceId()
        if (store.deviceSecret() != null) {
            try {
                return checkPairing()
            } catch (e: Exception) {
                val apiErr = apiError(e)
                if (apiErr?.mess == "systemalert.iptv.device.invalid" ||
                    apiErr?.mess == "systemalert.iptv.auth.denied" ||
                    (e is HttpException && (e.code() == 401 || e.code() == 404))
                ) {
                    // PMS deleted or invalidated this device — clear stale secret/token so we re-register below!
                    store.clearDeviceCredentials()
                } else {
                    throw e
                }
            }
        }

        val response = api.register(
            RegisterRequest(
                deviceId = deviceId,
                appVersion = BuildConfig.VERSION_NAME,
            )
        )
        response.deviceSecret?.let { store.setDeviceSecret(it) }
        return MeResponse(
            status = response.status,
            displayCode = response.displayCode,
        )
    }

    suspend fun checkPairing(): MeResponse {
        val deviceId = store.deviceId()
            ?: throw DeviceCredentialException("Thiết bị chưa được đăng ký.")
        val secret = store.deviceSecret()
            ?: throw DeviceCredentialException("Thiết bị bị mất khóa ghép nối.")

        return api.me(
            MeRequest(
                deviceId = deviceId,
                deviceSecret = secret,
                appVersion = BuildConfig.VERSION_NAME,
            )
        ).also { response ->
            response.deviceToken?.let { store.setDeviceToken(it) }
            response.roomNo?.let { store.setRoomNo(it) }
        }
    }

    suspend fun hasDeviceToken(): Boolean {
        store.migrateLegacyKeys()
        return store.deviceToken() != null
    }

    suspend fun clearDeviceToken() = store.clearDeviceToken()

    suspend fun clearDeviceCredentials() = store.clearDeviceCredentials()

    suspend fun screen(): ScreenResponse = api.screen(bearer())

    suspend fun content(): ContentResponse = api.content(bearer())

    fun apiError(throwable: Throwable): ApiError? {
        val body = (throwable as? HttpException)
            ?.response()
            ?.errorBody()
            ?.string()
            ?: return null
        return runCatching { json.decodeFromString<ApiError>(body) }.getOrNull()
    }

    private suspend fun bearer(): String {
        val token = store.deviceToken()
            ?: throw DeviceCredentialException("Thiết bị chưa được cấp quyền truy cập phòng.")
        return "Bearer $token"
    }
}

class DeviceCredentialException(message: String) : IllegalStateException(message)
