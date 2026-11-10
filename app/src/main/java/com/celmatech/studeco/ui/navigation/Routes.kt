package com.celmatech.studeco.ui.navigation

sealed class Route(val path: String) {
    data object Splash : Route("splash")
    data object Login : Route("login")
    data object Signup : Route("signup")
    data object ResetPassword : Route("reset_password")
    data object EmailVerification : Route("email_verification")
    data object Main : Route("main")
    data object Feed : Route("feed")
    data object Communities : Route("communities")
    data object Messages : Route("messages")
    data object Study : Route("study")
    data object Profile : Route("profile")
    data object Marketplace : Route("marketplace")
    data object Opportunities : Route("opportunities")
    data object Planner : Route("planner")
    data object AiTutor : Route("ai_tutor")
    data object Settings : Route("settings")
}
