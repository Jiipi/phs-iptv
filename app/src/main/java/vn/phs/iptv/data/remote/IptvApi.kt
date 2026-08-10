package vn.phs.iptv.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import vn.phs.iptv.data.remote.dto.MeRequest
import vn.phs.iptv.data.remote.dto.MeResponse
import vn.phs.iptv.data.remote.dto.ContentResponse
import vn.phs.iptv.data.remote.dto.RegisterRequest
import vn.phs.iptv.data.remote.dto.RegisterResponse
import vn.phs.iptv.data.remote.dto.ScreenResponse

// Current PHS-Lite IPTV contract. Room data is authorized only by a revocable
// device JWT; the permanent device ID/secret are used exclusively for pairing.
interface IptvApi {

    @POST("api/Iptv/Device/Register")
    suspend fun register(
        @Body body: RegisterRequest,
    ): RegisterResponse

    @POST("api/Iptv/Device/Me")
    suspend fun me(
        @Body body: MeRequest,
    ): MeResponse

    @GET("api/Iptv/Screen")
    suspend fun screen(
        @Header("Authorization") bearer: String,
    ): ScreenResponse

    @GET("api/Iptv/Content")
    suspend fun content(
        @Header("Authorization") bearer: String,
    ): ContentResponse
}
