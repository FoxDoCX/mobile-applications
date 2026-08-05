package ru.recalltoast.app.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object SettingsKeys {
    val SETTINGS_JSON = stringPreferencesKey("settings_json")
    val SCHEMA_VERSION = intPreferencesKey("schema_version")
    val LAST_EVENT_AT = longPreferencesKey("last_event_at")
    val SERVICE_RUNNING = booleanPreferencesKey("service_running_hint")
}
