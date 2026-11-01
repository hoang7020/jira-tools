package com.jiratools.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class UserPreferencesTest {
    @Test
    fun themeModeHasSystemLightAndDarkChoices() {
        assertEquals(
            listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK),
            ThemeMode.entries,
        )
    }

    @Test
    fun themeModeDefaultsToSystem() {
        assertEquals(ThemeMode.SYSTEM, UserPreferences().themeMode)
    }
}
