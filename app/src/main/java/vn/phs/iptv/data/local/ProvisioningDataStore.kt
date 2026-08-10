package vn.phs.iptv.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.provisioningDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "provisioning")

@Singleton
class ProvisioningDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore get() = context.provisioningDataStore

    // The v1 contract stores exactly these four values and no guest/folio content.
    private val keyDeviceId = stringPreferencesKey("device_id")
    private val keyDeviceSecret = stringPreferencesKey("device_secret")
    private val keyDeviceToken = stringPreferencesKey("device_token")
    private val keyRoomNo = stringPreferencesKey("room_no")

    suspend fun deviceId(): String? = dataStore.data.first()[keyDeviceId]

    suspend fun deviceSecret(): String? = dataStore.data.first()[keyDeviceSecret]

    suspend fun setDeviceSecret(secret: String) {
        dataStore.edit { it[keyDeviceSecret] = secret }
    }

    suspend fun deviceToken(): String? = dataStore.data.first()[keyDeviceToken]

    suspend fun setDeviceToken(token: String) {
        dataStore.edit { it[keyDeviceToken] = token }
    }

    suspend fun clearDeviceToken() {
        dataStore.edit { it.remove(keyDeviceToken) }
    }

    suspend fun clearDeviceSecret() {
        dataStore.edit { it.remove(keyDeviceSecret) }
    }

    suspend fun clearDeviceCredentials() {
        dataStore.edit {
            it.remove(keyDeviceToken)
            it.remove(keyDeviceSecret)
            it.remove(keyRoomNo)
        }
    }

    suspend fun roomNo(): String? = dataStore.data.first()[keyRoomNo]

    suspend fun setRoomNo(roomNo: String) {
        dataStore.edit { it[keyRoomNo] = roomNo }
    }

    /** Returns the permanent device ID, creating it only on the first app run. */
    suspend fun getOrCreateDeviceId(): String {
        val existing = dataStore.data.first()[keyDeviceId]
        if (existing != null) return existing
        val newId = UUID.randomUUID().toString()
        dataStore.edit { it[keyDeviceId] = newId }
        return newId
    }

    /** Removes keys left by PRD v0.4 so the store contains only the four v1 keys. */
    suspend fun migrateLegacyKeys() {
        val branchId = stringPreferencesKey("branch_id")
        val roomId = stringPreferencesKey("room_id")
        val lastPlayedStayId = stringPreferencesKey("last_played_stay_id")
        val current = dataStore.data.first()
        if (branchId !in current && roomId !in current && lastPlayedStayId !in current) return
        dataStore.edit { prefs ->
            prefs.remove(branchId)
            prefs.remove(roomId)
            prefs.remove(lastPlayedStayId)
        }
    }
}
