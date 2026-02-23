package com.ftpserver.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ftpserver.app.ui.screens.MainScreen
import com.ftpserver.app.ui.screens.OnboardingScreen
import com.ftpserver.app.ui.screens.SettingsScreen
import com.ftpserver.app.ui.screens.ThemeMode
import com.ftpserver.app.ui.theme.FTPServerTheme
import com.ftpserver.app.ui.theme.PresetColors
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ============== ONBOARDING SCREEN TESTS ==============

    @Test
    fun onboardingScreen_displaysTitle() {
        composeTestRule.setContent {
            FTPServerTheme(seedColor = PresetColors[0], isDark = false) {
                OnboardingScreen(onStart = {})
            }
        }

        composeTestRule.onNodeWithText("ftp server").assertIsDisplayed()
    }

    @Test
    fun onboardingScreen_displaysDescription() {
        composeTestRule.setContent {
            FTPServerTheme(seedColor = PresetColors[0], isDark = false) {
                OnboardingScreen(onStart = {})
            }
        }

        composeTestRule.onNodeWithText("transfer files via ftp server directly from your android device")
            .assertIsDisplayed()
    }

    @Test
    fun onboardingScreen_displaysStartButton() {
        composeTestRule.setContent {
            FTPServerTheme(seedColor = PresetColors[0], isDark = false) {
                OnboardingScreen(onStart = {})
            }
        }

        composeTestRule.onNodeWithText("start").assertIsDisplayed()
    }

    // ============== MAIN SCREEN TESTS ==============

    @Test
    fun mainScreen_displaysDisabledStatus_whenServerNotRunning() {
        composeTestRule.setContent {
            FTPServerTheme(seedColor = PresetColors[0], isDark = false) {
                MainScreen(
                    isRunning = false,
                    password = "",
                    username = "",
                    onStartServer = {},
                    onStopServer = {},
                    getIPAddress = { "192.168.1.1" },
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("disabled").assertIsDisplayed()
    }

    @Test
    fun mainScreen_displaysActiveStatus_whenServerRunning() {
        composeTestRule.setContent {
            FTPServerTheme(seedColor = PresetColors[0], isDark = false) {
                MainScreen(
                    isRunning = true,
                    password = "testpass",
                    username = "testuser",
                    onStartServer = {},
                    onStopServer = {},
                    getIPAddress = { "192.168.1.1" },
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("active").assertIsDisplayed()
    }

    @Test
    fun mainScreen_displaysPowerButton() {
        composeTestRule.setContent {
            FTPServerTheme(seedColor = PresetColors[0], isDark = false) {
                MainScreen(
                    isRunning = false,
                    password = "",
                    username = "",
                    onStartServer = {},
                    onStopServer = {},
                    getIPAddress = { "192.168.1.1" },
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Power").assertIsDisplayed()
    }

    @Test
    fun mainScreen_displaysSettingsButton() {
        composeTestRule.setContent {
            FTPServerTheme(seedColor = PresetColors[0], isDark = false) {
                MainScreen(
                    isRunning = false,
                    password = "",
                    username = "",
                    onStartServer = {},
                    onStopServer = {},
                    getIPAddress = { "192.168.1.1" },
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }

    @Test
    fun mainScreen_displaysWifiIcon() {
        composeTestRule.setContent {
            FTPServerTheme(seedColor = PresetColors[0], isDark = false) {
                MainScreen(
                    isRunning = false,
                    password = "",
                    username = "",
                    onStartServer = {},
                    onStopServer = {},
                    getIPAddress = { "192.168.1.1" },
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Wi-Fi").assertIsDisplayed()
    }

    @Test
    fun mainScreen_displaysNetworkInfo_whenServerNotRunning() {
        composeTestRule.setContent {
            FTPServerTheme(seedColor = PresetColors[0], isDark = false) {
                MainScreen(
                    isRunning = false,
                    password = "",
                    username = "",
                    onStartServer = {},
                    onStopServer = {},
                    getIPAddress = { "192.168.1.1" },
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("network").assertIsDisplayed()
        composeTestRule.onNodeWithText("192.168.1.1").assertIsDisplayed()
    }

    @Test
    fun mainScreen_displaysServerDetails_whenServerRunning() {
        composeTestRule.setContent {
            FTPServerTheme(seedColor = PresetColors[0], isDark = false) {
                MainScreen(
                    isRunning = true,
                    password = "mypassword",
                    username = "myuser",
                    onStartServer = {},
                    onStopServer = {},
                    getIPAddress = { "192.168.1.100" },
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("192.168.1.100").assertIsDisplayed()
        composeTestRule.onNodeWithText("myuser").assertIsDisplayed()
        composeTestRule.onNodeWithText("mypassword").assertIsDisplayed()
    }

    // ============== SETTINGS SCREEN TESTS ==============

    @Test
    fun settingsScreen_displaysTitle() {
        composeTestRule.setContent {
            FTPServerTheme(seedColor = PresetColors[0], isDark = false) {
                SettingsScreen(
                    seedColor = PresetColors[0],
                    themeMode = ThemeMode.SYSTEM,
                    onSeedColorChange = {},
                    onThemeModeChange = {},
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("settings").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysBackButton() {
        composeTestRule.setContent {
            FTPServerTheme(seedColor = PresetColors[0], isDark = false) {
                SettingsScreen(
                    seedColor = PresetColors[0],
                    themeMode = ThemeMode.SYSTEM,
                    onSeedColorChange = {},
                    onThemeModeChange = {},
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysAppearanceSection() {
        composeTestRule.setContent {
            FTPServerTheme(seedColor = PresetColors[0], isDark = false) {
                SettingsScreen(
                    seedColor = PresetColors[0],
                    themeMode = ThemeMode.SYSTEM,
                    onSeedColorChange = {},
                    onThemeModeChange = {},
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("appearance").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysThemeOption() {
        composeTestRule.setContent {
            FTPServerTheme(seedColor = PresetColors[0], isDark = false) {
                SettingsScreen(
                    seedColor = PresetColors[0],
                    themeMode = ThemeMode.SYSTEM,
                    onSeedColorChange = {},
                    onThemeModeChange = {},
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("theme").assertIsDisplayed()
        composeTestRule.onNodeWithText("system").assertIsDisplayed()
        composeTestRule.onNodeWithText("light").assertIsDisplayed()
        composeTestRule.onNodeWithText("dark").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysAccentColorOption() {
        composeTestRule.setContent {
            FTPServerTheme(seedColor = PresetColors[0], isDark = false) {
                SettingsScreen(
                    seedColor = PresetColors[0],
                    themeMode = ThemeMode.SYSTEM,
                    onSeedColorChange = {},
                    onThemeModeChange = {},
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("accent color").assertIsDisplayed()
    }
}