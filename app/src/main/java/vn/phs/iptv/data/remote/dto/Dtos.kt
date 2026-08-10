package vn.phs.iptv.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ── PHS-Lite IPTV API ────────────────────────────────────────────────────────

@Serializable
data class RegisterRequest(
    val deviceId: String,
    val appVersion: String,
)

@Serializable
data class RegisterResponse(
    val displayCode: String,
    val status: String,
    val deviceSecret: String? = null,
)

@Serializable
data class MeRequest(
    val deviceId: String,
    val deviceSecret: String,
    val appVersion: String,
)

@Serializable
data class MeResponse(
    val status: String,
    val displayCode: String,
    val branchId: String? = null,
    val roomId: String? = null,
    val roomNo: String? = null,
    val deviceToken: String? = null,
)

@Serializable
data class ScreenResponse(
    val roomNo: String,
    val occupied: Boolean,
    val hotel: ScreenHotelDto,
    val qrUrl: String? = null,
    val breakfast: BreakfastDto,
    val stay: StayDto? = null,
    val folio: ScreenFolioDto? = null,
)

@Serializable
data class ScreenHotelDto(
    val name: String,
    val wifiSsid: String,
    val wifiPassword: String,
    val hotline: String,
    val breakfastTime: String,
    val welcomeNote: String,
)

@Serializable
data class BreakfastDto(
    val enabled: Boolean,
    val cutoffTime: String,
    val serviceDate: String,
    val eligible: Boolean,
    val alreadySelected: Boolean,
    val canOrder: Boolean,
)

@Serializable
data class StayDto(
    val guestName: String,
    val adults: Int,
    val children: Int,
    val arrival: String,
    val departure: String,
    val nights: Int,
)

@Serializable
data class ScreenFolioDto(
    val room: Long,
    val service: Long,
    val surcharge: Long,
    val discount: Long,
    val deposit: Long,
    val total: Long,
)

@Serializable
data class ContentResponse(
    val version: String,
    val hotel: ContentHotelDto,
    val roomType: ContentRoomTypeDto? = null,
    val video: ContentVideoDto = ContentVideoDto(),
    val services: List<ContentServiceDto> = emptyList(),
    // PMS sends either bare keys (["bill","services"]) or full objects. Kept as raw
    // JsonElement so one shape change can never fail the whole ContentResponse parse.
    val tiles: List<JsonElement> = emptyList(),
    val texts: ContentTextsDto = ContentTextsDto(),
)

@Serializable
data class ContentHotelDto(
    val name: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val logo: String = "",
    val description: String = "",
    val media: List<JsonElement> = emptyList(),
    val facilities: List<String> = emptyList(),
    val checkInTime: String = "",
    val checkOutTime: String = "",
    val policies: List<String> = emptyList(),
    val wifi: ContentWifiDto? = null,
    val location: ContentLocationDto? = null,
)

@Serializable
data class ContentWifiDto(
    val ssid: String = "",
    val password: String = "",
)

@Serializable
data class ContentLocationDto(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val lat: Double? = null,
    val lng: Double? = null,
)

@Serializable
data class ContentRoomTypeDto(
    val roomTypeId: Long? = null,
    val roomType: String = "",
    val description: String = "",
    val media: List<JsonElement> = emptyList(),
    val amenities: List<String> = emptyList(),
    val sizeSqm: Double? = null,
    val bedConfig: List<JsonElement> = emptyList(),
    val view: String = "",
    val personNum: Int? = null,
    val bedNum: Int? = null,
)

@Serializable
data class ContentVideoDto(
    val introUrl: String = "",
    val idleUrl: String = "",
)

@Serializable
data class ContentServiceDto(
    val title: I18nTextDto = I18nTextDto(),
    val description: I18nTextDto = I18nTextDto(),
    val hours: String = "",
    val imageUrl: String = "",
    val image: String = "",
    val media: List<JsonElement> = emptyList(),
    val openTime: String = "",
    val closeTime: String = "",
)

@Serializable
data class ContentTextsDto(
    val welcome: I18nTextDto = I18nTextDto(),
    val idleNote: I18nTextDto = I18nTextDto(),
)

@Serializable
data class I18nTextDto(
    val vi: String = "",
    val en: String = "",
    val ru: String = "",
)

@Serializable
data class ApiError(
    val mess: String = "",
    val defaultMessage: String = "",
)
