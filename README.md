# Kupio Mobile

Kupio Mobile is a Kotlin Multiplatform mobile application targeting Android and iOS with shared UI built in Compose Multiplatform.

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

## MVI and MVVM

The project uses `ViewModel` as the host for screen logic, but the state flow inside the screen follows `MVI-lite`.

That means:

- `MVVM` answers where screen state and logic live: in a `ViewModel`
- `MVI` answers how state flows: `Action -> state update -> UI render -> Effect`

So these are not competing choices in this project. We use:

- `ViewModel` as the implementation container
- `State`, `Action`, and `Effect` as the screen contract
- one `onAction(...)` entry point for user interactions

This is different from classic callback-heavy MVVM where a screen exposes many public methods such as:

- `onBackClick()`
- `onThemeChanged()`
- `loadData()`

Instead, the preferred pattern is:

- one immutable `State`
- one sealed `Action`
- one sealed `Effect`
- one `ViewModel` coordinating the flow

In short: this project uses `ViewModel` plus `MVI-lite`, not plain classic MVVM.

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

### Feature Layers

Real features will gradually adopt `presentation`, `data`, and sometimes `domain` packages inside the feature.

Example:

- `features/listings/presentation`
- `features/listings/data`
- `features/listings/domain`

The starter does not include these folders everywhere yet because empty architectural folders add noise before the first real feature exists.

#### Presentation

`presentation` should exist in every real feature.

It contains:

- screens and composables
- `ViewModel`
- `State`, `Action`, and `Effect`
- UI-specific models and mappers
- feature-local reusable UI pieces

#### Data

`data` should be added when a feature starts working with backend, database, files, or platform storage.

It contains:

- repository implementations
- API services
- DTOs
- local and remote data sources
- database entities
- mapper implementations

#### Domain

`domain` is optional and should be added only when the feature has enough business logic to justify it.

It contains:

- business models
- validation rules
- use cases / interactors
- business logic that should not depend on UI or storage

Do not force `domain` into every feature from day one. Use it when the feature becomes complex enough that separating business rules improves clarity.

Practical rule:

- always add `presentation`
- add `data` when the feature talks to backend, database, or storage
- add `domain` when business logic becomes non-trivial

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
