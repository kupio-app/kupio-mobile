# Kupio Mobile Frontend

Kupio Mobile is a Kotlin Multiplatform mobile application targeting Android and iOS with shared UI built in Compose Multiplatform.

This repository intentionally starts with a small, opinionated skeleton. The goal is to give the team one clear way to structure new code without overbuilding infrastructure before the first real features exist.

## Technology Stack

- Kotlin Multiplatform + Compose Multiplatform for shared UI
- Material 3 as theme and design-token infrastructure, not as the final visual identity
- Voyager for navigation
- Koin for dependency injection
- AndroidX Lifecycle ViewModel for screen logic
- DataStore Preferences for app preferences
- Compose Multiplatform resources for localization

Deferred until the first real feature needs them:

- Ktor for HTTP and WebSockets
- Room KMP for relational and offline-first app data
- Coil 3 for image loading
- Okio for file storage
- Secure storage for tokens and secrets
- Push notification and permissions libraries

These are intentionally deferred because they need feature-driven wiring. Adding them too early would make the starter heavier without increasing clarity.

## Architecture

The project uses a lightweight screen-level MVI approach:

- each screen has immutable `State`
- user interactions are modeled as sealed `Action`
- one-shot UI commands are modeled as sealed `Effect`
- each screen owns one `ViewModel`
- composables render state and emit actions
- navigation is triggered from effects, not from business logic directly

This is intentionally MVI-lite, not a generic framework. The code should stay explicit and easy to follow.

## Project Structure

The project stays in a single shared Gradle module for now: `:composeApp`.

Shared code in `commonMain` is organized like this:

- `kupio.mobile.app`
- `kupio.mobile.core.designsystem`
- `kupio.mobile.core.navigation`
- `kupio.mobile.core.presentation`
- `kupio.mobile.core.di`
- `kupio.mobile.core.preferences`
- `kupio.mobile.features.home`
- `kupio.mobile.features.settings`

Rule of thumb:

- `core` contains app-wide infrastructure reused by multiple features
- `features` contains product functionality and screen-specific logic

Do not create `core.network`, `core.database`, or `core.storage` until real feature work needs them.

## UI and Design System

Compose Multiplatform is shared across Android and iOS.

Material is used only for:

- theme structure
- color scheme support
- typography and spacing tokens
- accessibility-friendly defaults where useful

Visible UI should gradually move toward custom shared components such as:

- `KupioScaffold`
- `KupioButton`
- `KupioText`

This avoids the app looking like a stock Android Material app on iOS while still keeping a shared design system.

## Preferences, Database, and Secure Storage

Use DataStore Preferences for small app preferences:

- theme mode
- language override
- onboarding flags
- simple UI settings

Use Room later for relational or offline-first app data:

- listings cache
- favourites
- draft listings
- sync metadata
- chat cache

Use secure storage later for secrets:

- access token
- refresh token
- any credential-like data

## Localization

Project code and documentation stay in English.

The app itself is prepared for:

- English
- Slovak

Localization uses shared Compose resources.

## TODO Policy

Starter code is allowed to be incomplete only when it is clearly marked.

Every meaningful temporary or simplified implementation must include a `TODO` that explains what should happen next, for example:

- replace a temporary implementation with a real backend-backed one
- expand minimal design system components
- replace starter navigation with the real feature graph
- move simplified logic into a proper feature-specific implementation

Do not add vague or decorative `TODO`s.

## Build and Run

Build Android debug:

```shell
./gradlew :composeApp:assembleDebug
```

Open and run iOS from Xcode:

- open `iosApp/iosApp.xcodeproj`
- run the `iosApp` target

## Current Scope of the Starter

The starter intentionally demonstrates only:

- shared app bootstrapping
- theme setup
- navigation between `home` and `settings`
- screen-level MVI conventions
- Koin DI setup
- DataStore-backed theme preference
- EN/SK localization pattern

No backend integration is included in the starter.
