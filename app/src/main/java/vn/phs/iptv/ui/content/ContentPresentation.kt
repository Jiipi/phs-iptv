package vn.phs.iptv.ui.content

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import vn.phs.iptv.data.remote.dto.ContentServiceDto
import vn.phs.iptv.data.remote.dto.I18nTextDto
import vn.phs.iptv.domain.AppLanguage

fun I18nTextDto.forLang(language: AppLanguage): String = when (language) {
    AppLanguage.EN -> en.ifBlank { vi }
    AppLanguage.RU -> ru.ifBlank { vi }
    AppLanguage.VI -> vi
}

fun JsonElement?.forLang(language: AppLanguage): String {
    val element = this ?: return ""
    if (element is JsonPrimitive) return element.contentOrNull.orEmpty()
    val value = element as? JsonObject ?: return ""
    val vi = (value["vi"] as? JsonPrimitive)?.contentOrNull.orEmpty()
    val selected = when (language) {
        AppLanguage.EN -> (value["en"] as? JsonPrimitive)?.contentOrNull.orEmpty()
        AppLanguage.RU -> (value["ru"] as? JsonPrimitive)?.contentOrNull.orEmpty()
        AppLanguage.VI -> vi
    }
    return selected.ifBlank { vi }
}

fun List<JsonElement>.firstMediaUrl(): String? = firstNotNullOfOrNull { it.contentUrl() }

/**
 * A `tiles[]` entry as the UI needs it, regardless of which shape PMS sent.
 * `["bill", "services"]` and `[{"key":"bill", "title":{...}}]` both land here.
 * [title]/[subtitle]/[imageUrl] are blank for the bare-key form — the caller
 * falls back to the localized labels in `UiStrings.tiles`.
 */
data class ContentTile(
    val key: String,
    val target: String,
    val title: String = "",
    val subtitle: String = "",
    val imageUrl: String = "",
)

/** Bare key, or object with key/action/target — anything else is dropped. */
fun JsonElement.asContentTile(language: AppLanguage): ContentTile? {
    if (this is JsonPrimitive) {
        val key = contentOrNull?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return ContentTile(key = key, target = key.tileTarget())
    }
    val obj = this as? JsonObject ?: return null
    fun str(name: String) = (obj[name] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
    val key = str("key")
    val target = str("target").ifBlank { str("action") }.ifBlank { key }
    if (key.isBlank() && target.isBlank()) return null
    return ContentTile(
        key = key.ifBlank { target },
        target = target.tileTarget(),
        title = obj["title"].forLang(language),
        subtitle = obj["subtitle"].forLang(language).ifBlank { obj["description"].forLang(language) },
        imageUrl = str("imageUrl").ifBlank { str("image") },
    )
}

private fun String.tileTarget(): String = trim().lowercase().replace('-', '_')

/** PMS tile key → the `UiStrings.tiles` / `Demo.homeTiles` id carrying label + artwork. */
fun String.tileLabelKey(): String = when (tileTarget()) {
    "tv", "live_tv", "livetv" -> "tv"
    "bill", "folio" -> "bill"
    "order", "service", "room_service", "roomservice", "dining" -> "service"
    "voice", "assistant" -> "voice"
    "services", "hotel_services", "facilities" -> "services"
    "help", "hotel", "info", "hotel_info" -> "help"
    else -> tileTarget()
}

fun ContentServiceDto.resolvedImageUrl(): String? =
    imageUrl.ifBlank { image }.takeIf { it.isNotBlank() } ?: media.firstMediaUrl()

fun ContentServiceDto.resolvedHours(): String = when {
    hours.isNotBlank() -> hours
    openTime.isNotBlank() && closeTime.isNotBlank() -> "$openTime – $closeTime"
    openTime.isNotBlank() -> openTime
    closeTime.isNotBlank() -> closeTime
    else -> ""
}

/** PMS sends facility/amenity codes. Unknown codes are intentionally not displayed. */
fun facilityLabel(code: String, language: AppLanguage): String? {
    val labels = when (code.trim().lowercase().replace('-', '_')) {
        "wifi", "free_wifi" -> Triple("Wi-Fi", "Wi-Fi", "Wi-Fi")
        "pool", "swimming_pool" -> Triple("Hồ bơi", "Swimming pool", "Бассейн")
        "gym", "fitness", "fitness_center" -> Triple("Phòng tập", "Fitness centre", "Фитнес-центр")
        "spa", "wellness" -> Triple("Spa & chăm sóc sức khỏe", "Spa & wellness", "Спа")
        "restaurant", "dining" -> Triple("Nhà hàng", "Restaurant", "Ресторан")
        "bar" -> Triple("Quầy bar", "Bar", "Бар")
        "parking", "free_parking" -> Triple("Bãi đỗ xe", "Parking", "Парковка")
        "airport_shuttle", "shuttle" -> Triple("Đưa đón sân bay", "Airport shuttle", "Трансфер")
        "laundry" -> Triple("Giặt là", "Laundry", "Прачечная")
        "elevator", "lift" -> Triple("Thang máy", "Elevator", "Лифт")
        "air_conditioning", "air_conditioner" -> Triple("Điều hòa", "Air conditioning", "Кондиционер")
        "front_desk", "reception", "concierge" -> Triple("Lễ tân", "Reception", "Стойка регистрации")
        "room_service" -> Triple("Dịch vụ phòng", "Room service", "Обслуживание номеров")
        "breakfast" -> Triple("Bữa sáng", "Breakfast", "Завтрак")
        "beach", "private_beach" -> Triple("Bãi biển", "Beach", "Пляж")
        else -> return null
    }
    return when (language) {
        AppLanguage.VI -> labels.first
        AppLanguage.EN -> labels.second
        AppLanguage.RU -> labels.third
    }
}

private fun JsonElement.contentUrl(): String? = when (this) {
    is JsonPrimitive -> contentOrNull?.takeIf { it.isNotBlank() }
    is JsonObject -> sequenceOf("url", "imageUrl", "secureUrl", "src")
        .mapNotNull { key -> (this[key] as? JsonPrimitive)?.contentOrNull }
        .firstOrNull { it.isNotBlank() }
    else -> null
}
