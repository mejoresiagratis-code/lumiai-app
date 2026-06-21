package com.mejoresiagratis.lumiai.domain.model

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;
    fun next(): ThemeMode = entries[(ordinal + 1) % entries.size]
}
