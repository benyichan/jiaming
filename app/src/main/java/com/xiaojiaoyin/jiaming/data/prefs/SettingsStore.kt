package com.xiaojiaoyin.jiaming.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** 可选民俗模块开关（design.md §7：全部默认关闭，开启时展示免责） */
class SettingsStore(context: Context) {
    private val ds = context.dataStore

    companion object {
        val KEY_BAZI = booleanPreferencesKey("module_bazi")
        val KEY_ZODIAC = booleanPreferencesKey("module_zodiac")
    }

    val baziEnabled: Flow<Boolean> = ds.data.map { it[KEY_BAZI] ?: false }
    val zodiacEnabled: Flow<Boolean> = ds.data.map { it[KEY_ZODIAC] ?: false }

    suspend fun setBazi(v: Boolean) = ds.edit { it[KEY_BAZI] = v }
    suspend fun setZodiac(v: Boolean) = ds.edit { it[KEY_ZODIAC] = v }
}
