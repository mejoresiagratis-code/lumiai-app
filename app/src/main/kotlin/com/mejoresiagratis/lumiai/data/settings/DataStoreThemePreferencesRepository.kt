package com.mejoresiagratis.lumiai.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mejoresiagratis.lumiai.domain.model.AccentColor
import com.mejoresiagratis.lumiai.domain.model.AccentStyle
import com.mejoresiagratis.lumiai.domain.model.ThemeMode
import com.mejoresiagratis.lumiai.domain.repository.ThemePreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import com.mejoresiagratis.lumiai.domain.repository.AuthRepository
import com.mejoresiagratis.lumiai.domain.model.AuthUser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreThemePreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val auth: AuthRepository
) : ThemePreferencesRepository {

    private val themeKey = stringPreferencesKey("theme_mode")
    private val accentOwnerKey = stringPreferencesKey("accent_owner")
    private val accentKey = stringPreferencesKey("accent_color")
    private val accentStyleKey = stringPreferencesKey("accent_style")
    private val reduceMotionKey = booleanPreferencesKey("a11y_reduce_motion")
    private val highContrastKey = booleanPreferencesKey("a11y_high_contrast")
    private val hapticsKey = booleanPreferencesKey("a11y_haptics")
    private val autoLockScreenKey = booleanPreferencesKey("a11y_auto_lock_screen")

    // Never assign the legacy device-wide value to an arbitrary account.
    private fun themeKeyFor(user: AuthUser?) = stringPreferencesKey("theme_mode_v2:${owner(user)}")

    override val themeMode: Flow<ThemeMode> = combine(auth.currentUser, dataStore.data) { user, p ->
        runCatching { ThemeMode.valueOf(p[themeKeyFor(user)] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        val user = auth.currentUser.first()
        val expectedUid = user?.uid
        if (auth.currentUid() != expectedUid) return
        dataStore.edit { p ->
            if (auth.currentUid() != expectedUid || owner(auth.currentUser.first()) != owner(user)) return@edit
            p[themeKeyFor(user)] = mode.name
            p.remove(themeKey)
        }
    }

    override suspend fun resetGuestTheme() {
        dataStore.edit { it.remove(themeKeyFor(null)) }
    }

    private fun owner(user: AuthUser?): String =
        user?.takeUnless { it.isAnonymous }?.let { "account:${it.uid}" } ?: "guest"

    private suspend fun editAccent(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        val current = auth.currentUser.first()
        val expectedUid = current?.uid
        if (auth.currentUid() != expectedUid) return
        val expectedOwner = owner(current)
        dataStore.edit { p ->
            if (auth.currentUid() != expectedUid) return@edit
            if (p[accentOwnerKey] != expectedOwner) {
                p.remove(accentKey)
                p.remove(accentStyleKey)
                p[accentOwnerKey] = expectedOwner
            }
            block(p)
        }
    }

    override val accentColor: Flow<AccentColor> = combine(auth.currentUser, dataStore.data) { user, p ->
        if (p[accentOwnerKey] != owner(user)) AccentColor.BLUE
        else runCatching { AccentColor.valueOf(p[accentKey] ?: AccentColor.BLUE.name) }
            .getOrDefault(AccentColor.BLUE)
    }

    override suspend fun setAccentColor(accent: AccentColor) = editAccent { it[accentKey] = accent.name }

    override val accentStyle: Flow<AccentStyle> = combine(auth.currentUser, dataStore.data) { user, p ->
        if (p[accentOwnerKey] != owner(user)) AccentStyle.VIVID
        else runCatching { AccentStyle.valueOf(p[accentStyleKey] ?: AccentStyle.VIVID.name) }
            .getOrDefault(AccentStyle.VIVID)
    }

    override suspend fun setAccentStyle(style: AccentStyle) = editAccent { it[accentStyleKey] = style.name }

    override suspend fun resetAccent() {
        // Explicit sign-out discards the old selection; logging back in cannot resurrect it.
        editAccent { p -> p.remove(accentKey); p.remove(accentStyleKey) }
    }

    override suspend fun resetAccentIfMatches(accent: AccentColor) {
        editAccent { p ->
            if (p[accentKey] == accent.name) {
                p.remove(accentKey)
                p.remove(accentStyleKey)
            }
        }
    }

    // Accesibilidad (Capa B). Defaults: desactivados (se respeta tambien el sistema).
    override val reduceMotion: Flow<Boolean> = dataStore.data.map { p -> p[reduceMotionKey] ?: false }

    override suspend fun setReduceMotion(value: Boolean) {
        dataStore.edit { it[reduceMotionKey] = value }
    }

    override val highContrast: Flow<Boolean> = dataStore.data.map { p -> p[highContrastKey] ?: false }

    override suspend fun setHighContrast(value: Boolean) {
        dataStore.edit { it[highContrastKey] = value }
    }

    // Vibracion: por defecto activada (sin efecto en dispositivos sin vibrador).
    override val haptics: Flow<Boolean> = dataStore.data.map { p -> p[hapticsKey] ?: true }

    override suspend fun setHaptics(value: Boolean) {
        dataStore.edit { it[hapticsKey] = value }
    }

    // Auto-bloqueo del modo Pantalla: desactivado por defecto (el usuario opta por activarlo).
    override val autoLockScreen: Flow<Boolean> = dataStore.data.map { p -> p[autoLockScreenKey] ?: false }

    override suspend fun setAutoLockScreen(value: Boolean) {
        dataStore.edit { it[autoLockScreenKey] = value }
    }
}
