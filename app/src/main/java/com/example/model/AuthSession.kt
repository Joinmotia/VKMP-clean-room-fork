package com.example.model

enum class AuthMethod(val titleRu: String) {
    MAX_MESSENGER("Вход через MAX"),
    VK_TOKEN("Токен VK / Kate"),
    GUEST_OFFLINE("Офлайн / Демо")
}

data class AuthSession(
    val isLoggedIn: Boolean = false,
    val userId: Long = 0L,
    val userName: String = "Гость",
    val userAvatar: String? = null,
    val accessToken: String? = null,
    val authMethod: AuthMethod = AuthMethod.GUEST_OFFLINE,
    val maxPhoneNumber: String? = null,
    val isMaxVerified: Boolean = false
)
