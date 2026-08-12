package com.example.pythagoros.data.prefs

import android.content.Context

/**
 * Небольшие локальные настройки: пройден ли первый запуск, выбран ли язык, есть ли подписка.
 *
 * Статус Pro здесь — кэш ответа биллинга: источником истины остаётся Google Play,
 * но экранам нужно знать состояние сразу, до ответа сети.
 */
class AppPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("pythagoros", Context.MODE_PRIVATE)

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean(KeyOnboarding, false)
        set(value) = prefs.edit().putBoolean(KeyOnboarding, value).apply()

    var languageCode: String
        get() = prefs.getString(KeyLanguage, "ru").orEmpty()
        set(value) = prefs.edit().putString(KeyLanguage, value).apply()

    var isPro: Boolean
        get() = prefs.getBoolean(KeyPro, false)
        set(value) = prefs.edit().putBoolean(KeyPro, value).apply()

    var userId: String
        get() = prefs.getString(KeyUserId, "").orEmpty()
        set(value) = prefs.edit().putString(KeyUserId, value).apply()

    var userPhone: String
        get() = prefs.getString(KeyUserPhone, "").orEmpty()
        set(value) = prefs.edit().putString(KeyUserPhone, value).apply()

    var userEmail: String
        get() = prefs.getString(KeyUserEmail, "").orEmpty()
        set(value) = prefs.edit().putString(KeyUserEmail, value).apply()

    var userDisplayName: String
        get() = prefs.getString(KeyUserDisplayName, "").orEmpty()
        set(value) = prefs.edit().putString(KeyUserDisplayName, value).apply()

    var sessionToken: String
        get() = prefs.getString(KeySessionToken, "").orEmpty()
        set(value) = prefs.edit().putString(KeySessionToken, value).apply()

    val isRegistered: Boolean
        get() = sessionToken.isNotBlank() && userId.isNotBlank()

    private companion object {
        const val KeyOnboarding = "onboarding_completed"
        const val KeyLanguage = "language"
        const val KeyPro = "is_pro"
        const val KeyUserId = "user_id"
        const val KeyUserPhone = "user_phone"
        const val KeyUserEmail = "user_email"
        const val KeyUserDisplayName = "user_display_name"
        const val KeySessionToken = "session_token"
    }
}
