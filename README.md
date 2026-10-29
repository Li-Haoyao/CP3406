# Collab Radar

Collab Radar is a native Android utility app for browsing game and brand collaborations at a glance. It groups events into drinks, dining, fashion, retail, and games/anime, with dates, regional availability, rewards, and participation guides.

Version **0.3.0** is built with Kotlin, Jetpack Compose, and Material Design 3. It adds account support, a home-screen region selector, and four interface languages.

## Features

- **Regional discovery:** switch between China, Malaysia, and Singapore directly from the home screen. The selection immediately filters events and is retained after restarting. The default region is China.
- **Quick browsing:** search by game, brand, or collaboration; filter by category or game; sort by ending date or publication date; and optionally include ended events.
- **Participation guides:** expand event cards to view dates, locations, requirements, rewards, and participation steps.
- **Me:** manage account details, saved collaborations, discovery preferences, language, and appearance.
- **Four languages:** English is the default. Simplified Chinese, Malay, and Tamil are available under **Me > Appearance & language > Language**. Interface text and built-in demo guides are localised; game and brand names retain their original names.
- **Guest mode:** browse and save events without signing in. Preferences and guest saves are stored on the device.
- **Accounts:** with the included account service configured, users can register with an email and password, sign in, edit their display name, change their password, sign out, delete their account, and synchronise saved collaborations.
- **Account synchronisation:** saving or removing an event while signed in updates the server. Other devices receive changes when signing in, restoring a session, or selecting **Sync account**.
- **Separate saves:** guest saves and account saves remain separate. Signing out restores the device's guest saves; they are not automatically merged into an account.
- **Appearance:** light and dark modes.
- **Event feeds:** load an HTTPS JSON feed that follows the supplied event schema.

## Current status

The built-in events are clearly labelled **fictional demonstrations**. The app does not automatically collect real collaboration announcements, and no public account server is included or deployed.

Without an account-service URL, demo browsing and guest saves work normally. Account forms can be viewed, but submission is disabled with an explanation that account services are not connected.

The event-feed client is implemented, but this version does not include a default live event-feed endpoint. A compatible HTTPS feed must be supplied to demonstrate live event loading.

## Architecture

The app uses a single activity with two bottom-navigation destinations: **Discover** and **Me**. Account actions use dialogs within Me.

- **UI:** Compose screens and reusable components display state and forward user actions.
- **ViewModel:** `RadarViewModel` manages UI state, event filtering, loading, account actions, and save operations. The activity collects its state with lifecycle awareness.
- **Repositories:** event and account repositories handle data access. Retrofit and OkHttp perform network requests.
- **Dependency injection:** `RadarApplication` and `AppContainer` provide repositories and stores to the ViewModel through explicit constructor injection.
- **Local storage:** device preferences and guest saves are persisted locally. Session tokens use Android Keystore-backed encryption.
- **Account service:** the `server/` directory contains a FastAPI and SQLite implementation with account and favourites endpoints.

## Project structure

Kotlin sources are under `app/src/main/java/au/edu/jcu/assessment/utilityapp/`.

| Path | Purpose |
| --- | --- |
| `RadarApplication.kt` | Application entry point and dependency container |
| `MainActivity.kt` | Activity entry point, theme, and state collection |
| `data/` | Event and account APIs, repositories, stores, and demo data |
| `model/` | Event, region, language, preference, and account models |
| `ui/` | Discover, Me, account dialogs, reusable components, and theme |
| `app/src/main/res/values*/strings.xml` | Localised interface resources |
| `api/` | Example event feed and JSON schema |
| `server/` | Account service, startup instructions, and API tests |

## Open and run in Android Studio

1. Extract the project archive.
2. Select **File > Open** and choose the **CollabRadar** directory containing `settings.gradle.kts`.
3. Select **JDK 17** as the Gradle JDK.
4. Install **Android SDK Platform 36** and **Build Tools 35.0.0**, then allow Gradle Sync to finish. Gradle Wrapper 8.13 is included.
5. Select the **app** run configuration and an emulator or device running **Android 8.0 / API 26 or later**.
6. Select **Run**. The app initially opens in English and displays demo events.

To change language, open **Me > Appearance & language > Language**.

## Enable registration and sign-in

### Windows and the Android Emulator

1. Install Python 3.12 and confirm that `py -3 --version` works in a terminal.
2. Run `server/run_server.bat`. Wait for the dependencies to install and for Uvicorn to start at `http://127.0.0.1:8000`. Keep this window open.
3. On the computer, open `http://127.0.0.1:8000/health` and check that the response includes `"status": "ok"`.
4. Add the following line to the project's root `gradle.properties`:

```properties
RADAR_ACCOUNT_BASE_URL=http://10.0.2.2:8000/
```

5. Sync Gradle, rebuild, and run the app again.
6. Open **Me > Create account**. Successful registration signs the user in.

The debug configuration allows HTTP for the emulator host address `10.0.2.2` and local loopback addresses. Changes to the account URL require rebuilding the app.

### Physical devices and cross-device access

Deploy the account service to an HTTPS host and configure the actual service root:

```properties
RADAR_ACCOUNT_BASE_URL=https://accounts.your-domain.com/
```

This is an example address, not a deployed service. All devices must use the same service. The emulator-specific address `10.0.2.2` does not work on physical phones, and release builds require HTTPS.

See `server/README.md` for service setup and endpoint details.

## Account behaviour and limitations

- Passwords must contain 12-128 characters. The service stores Argon2id password hashes.
- Email addresses act as login identifiers. Email ownership is not verified in this version.
- Sessions expire after seven days. The Android app stores an encrypted session token, not the password. Tokens are bound to the configured service address and excluded from system backup and device transfer.
- Changing a password requires the current password, revokes previous sessions, and issues a replacement session for the current device.
- Account deletion requires a password and explicit confirmation. It permanently removes the account and its server-side saves. Separate guest saves on the device remain available.
- Signing out clears the local session. If the server cannot be reached, the app explains that the remote session will expire automatically.
- Email verification, password-reset email, SMS verification, third-party sign-in, and push notifications are not implemented.
- Tamil is the Indian language currently supported; Hindi is not included.

## Connect an event feed

Open **Me > Event data**, enter a compatible HTTPS JSON feed URL, and select **Connect & update events**.

The event-feed URL and account-service URL are separate settings. The feed format is documented in:

- `api/events.example.json`
- `api/feed.schema.json`

Supported region codes are `CN`, `MY`, and `SG`. Online events should also list the countries where they are available. The parser recognises the legacy Chinese region names for compatibility.

Real event records must include an official HTTPS source URL. Remote content retains the source's text; the app does not replace it with demo translations based on matching event IDs.

## Build and test

Android, on Windows:

```bat
gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

Android, on macOS or Linux:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Account service, from the project root using the intended Python environment:

```bash
python -m pip install -r server/requirements-dev.txt
python -m pytest server -q
```

The recorded validation on **20 October 2026** completed the debug build, 19 Android JVM tests, and 13 account-service tests. It did not include Android emulator or physical-device UI testing. See `VALIDATION.md` for the tested environment and limitations. These results describe that validation run, not later local changes.

## Development support

See `AI_ASSISTANCE.md` for the scope of AI-assisted debugging, reference support, implementation, localisation, testing, and documentation. This record does not replace the assessment's required Declaration of AI-Generated Material.

The project retains the package name `au.edu.jcu.assessment.utilityapp`. Configure the application ID and signing settings appropriately before publishing.
