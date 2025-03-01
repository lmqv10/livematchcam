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

    private val ACCOUNT_GOOGLE =  stringPreferencesKey("GoogleAccount")
    private val ACCOUNT_KEY =  stringPreferencesKey("AccountKey")

    val accountGoogle: Flow<String> = context.accountDataStore
        .data.map { preferences -> preferences[ACCOUNT_GOOGLE] ?: "" }

    suspend fun setAccountGoogle(account: String?) {
        context.accountDataStore.edit { preferences -> preferences[ACCOUNT_GOOGLE] = account ?: "" }
    }

    val accountKey: Flow<String> = context.accountDataStore
        .data.map { preferences -> preferences[ACCOUNT_KEY] ?: "" }

    suspend fun setAccountKey(account: String?) {
        context.accountDataStore.edit { preferences -> preferences[ACCOUNT_KEY] = account ?: "" }
    }
}
