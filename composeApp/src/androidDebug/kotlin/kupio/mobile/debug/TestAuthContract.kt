package kupio.mobile.debug

internal object TestAuthContract {
    const val SeedAuthAction = "kupio.mobile.test.SEED_AUTH"
    const val ClearAuthAction = "kupio.mobile.test.CLEAR_AUTH"
    const val ExtraUserId = "userId"
    const val ExtraEmail = "email"
    const val ExtraUsername = "username"
    const val AccessToken = "appium-access-token"
    const val RefreshToken = "appium-refresh-token"
    const val TokenLifetimeSeconds = 60L * 60L
}
