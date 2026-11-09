package com.johnpaulpatigas.weightcast.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

interface UserPreferencesRepository {
    val preferredUnit: Flow<WeightUnit>
    suspend fun setPreferredUnit(unit: WeightUnit)
}

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferencesRepository {

    private object PreferencesKeys {
        val PREFERRED_WEIGHT_UNIT = stringPreferencesKey("preferred_weight_unit")
    }

    override val preferredUnit: Flow<WeightUnit> = context.dataStore.data.map { preferences ->
        val unitStr = preferences[PreferencesKeys.PREFERRED_WEIGHT_UNIT] ?: WeightUnit.KG.name
        WeightUnit.fromString(unitStr)
    }

    override suspend fun setPreferredUnit(unit: WeightUnit) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREFERRED_WEIGHT_UNIT] = unit.name
        }
    }
}
