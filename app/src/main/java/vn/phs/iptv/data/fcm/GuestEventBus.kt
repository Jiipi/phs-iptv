package vn.phs.iptv.data.fcm

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import vn.phs.iptv.domain.GuestProfile
import javax.inject.Inject
import javax.inject.Singleton

sealed interface GuestEvent {
    data class CheckedIn(
        val guest: GuestProfile,
        val stayId: String,
        val videoUrl: String? = null,
    ) : GuestEvent
    data object CheckedOut : GuestEvent
}

// Application-scoped event bus bridging PhsFcmService (Service) → AppStateMachine (ViewModel).
@Singleton
class GuestEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<GuestEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<GuestEvent> = _events.asSharedFlow()

    fun emit(event: GuestEvent) {
        _events.tryEmit(event)
    }
}
