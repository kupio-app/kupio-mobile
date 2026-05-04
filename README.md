# Kupio Mobile

Kupio Mobile is a Kotlin Multiplatform mobile application targeting Android and iOS with shared UI built in Compose Multiplatform.

## Tech Stack

- Kotlin Multiplatform
- Compose Multiplatform
- Material 3
- Voyager
- Koin
- Ktor Client
- kotlinx.serialization
- DataStore
- KVault

## Local Setup

### Android

Create or update the ignored `local.properties` file in the project root.

You can start from:

```bash
cp local.properties.example local.properties
```

Example local Android config:

```properties
KUPIO_BACKEND_BASE_URL=http://10.0.2.2:9988
KUPIO_GOOGLE_SERVER_CLIENT_ID=your-web-client-id.apps.googleusercontent.com
```

Notes:

- `10.0.2.2` is the Android emulator address for a backend running on your host machine.
- Debug builds allow cleartext only for local development hosts through the debug network security config.
- Release Android builds require non-blank `KUPIO_BACKEND_BASE_URL` and `KUPIO_GOOGLE_SERVER_CLIENT_ID`.
- Runtime config can also be supplied via Gradle properties or environment variables.

### iOS

Create an ignored local iOS config file:

```bash
cp iosApp/Configuration/Config.local.example.xcconfig iosApp/Configuration/Config.local.xcconfig
```

Example local iOS config:

```xcconfig
TEAM_ID=

KUPIO_BACKEND_BASE_URL = http:/$()/localhost:9988
KUPIO_GOOGLE_IOS_CLIENT_ID=your-ios-client-id.apps.googleusercontent.com
KUPIO_GOOGLE_SERVER_CLIENT_ID=your-web-client-id.apps.googleusercontent.com
KUPIO_GOOGLE_REVERSED_CLIENT_ID=com.googleusercontent.apps.your-ios-client-id
```

Notes:

- `http:/$()/localhost:9988` is the xcconfig-safe way to write `http://localhost:9988`.
- `Config.xcconfig` includes `Config.local.xcconfig` when present.
- Keep `Config.local.xcconfig` untracked because it is machine/local-environment specific.
- Before shipping iOS release builds, provide production values through CI or release-specific config instead of local defaults.
