package vn.phs.iptv.data.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import vn.phs.iptv.domain.GuestProfile
import javax.inject.Inject

// FCM data message handler (PRD §8).
// Parses incoming messages and emits to GuestEventBus so AppStateMachine can react.
//
// FCM payload fields expected for guest.checked_in:
//   type, stayId, guestName, guestTitle, roomNo, nationality, isBirthday, videoUrl (optional)
@AndroidEntryPoint
class PhsFcmService : FirebaseMessagingService() {

    @Inject lateinit var guestEventBus: GuestEventBus

    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"] ?: return
        Log.d(TAG, "FCM received: type=$type")

        when (type) {
            "guest.checked_in"  -> handleCheckedIn(message.data)
            "guest.checked_out" -> handleCheckedOut()
            "folio.updated"     -> Log.d(TAG, "folio.updated — FolioScreen refresh (F4, not implemented)")
            else                -> Log.w(TAG, "Unknown FCM type: $type")
        }
    }

    private fun handleCheckedIn(data: Map<String, String>) {
        val guest = GuestProfile(
            name        = data["guestName"]  ?: "",
            title       = data["guestTitle"] ?: "",
            roomNo      = data["roomNo"]     ?: "",
            nationality = data["nationality"] ?: "VNM",
            isBirthday  = data["isBirthday"] == "true",
        )
        val stayId   = data["stayId"]   ?: ""
        val videoUrl = data["videoUrl"]
        Log.d(TAG, "guest.checked_in: room=${guest.roomNo}, guest=${guest.name}, stayId=$stayId")
        guestEventBus.emit(GuestEvent.CheckedIn(guest, stayId, videoUrl))
    }

    private fun handleCheckedOut() {
        Log.d(TAG, "guest.checked_out")
        guestEventBus.emit(GuestEvent.CheckedOut)
    }

    companion object {
        private const val TAG = "PhsFcmService"
    }
}
