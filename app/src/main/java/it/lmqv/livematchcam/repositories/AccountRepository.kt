package it.lmqv.livematchcam.repositories

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.accountDataStore: DataStore<Preferences> by preferencesDataStore(name = "account")

class AccountRepository(private val context: Context) {

    private val ACCOUNT_NAME =  stringPreferencesKey("AccountName")
    private val ACCOUNT_KEY =  stringPreferencesKey("AccountKey")

    val accountName: Flow<String> = context.accountDataStore
        .data.map { preferences -> preferences[ACCOUNT_NAME] ?: "" }

    suspend fun setAccountName(account: String?) {
        context.accountDataStore.edit { preferences -> preferences[ACCOUNT_NAME] = account ?: "" }
    }

    // Lecco: "846af82686b3429a85e9a2d9a14ed79a"
    // Picco: "99261ce7d8b94fc395301f57d9e61ffd"
    val accountKey: Flow<String> = context.accountDataStore
        .data.map { preferences -> "846af82686b3429a85e9a2d9a14ed79a" }
        //.data.map { preferences -> "99261ce7d8b94fc395301f57d9e61ffd" }
        //.data.map { preferences -> preferences[ACCOUNT_KEY] ?: "" }

    suspend fun setAccountKey(account: String?) {
        context.accountDataStore.edit { preferences -> preferences[ACCOUNT_KEY] = account ?: "" }
    }

    suspend fun clear() {
        context.accountDataStore.edit { clear() }
    }
}
