# Build Structure Documentation pour ShieldCheck Android

## Project Structure

```
Shield-Check-/
├── app/
│   ├── build.gradle.kts              # Dependencies and compilation config
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml   # App manifest with permissions
│   │   │   ├── java/com/shieldcheck/app/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── service/
│   │   │   │   │   └── DeviceMonitorService.kt
│   │   │   │   ├── receiver/
│   │   │   │   │   └── DeviceAdminReceiver.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/
│   │   │   │   │   │   └── StolenObject.kt
│   │   │   │   │   └── repository/
│   │   │   │   │       └── StolenObjectRepository.kt
│   │   │   │   └── util/
│   │   │   │       └── DeviceIdentifier.kt
│   │   │   └── res/
│   │   │       ├── xml/
│   │   │       │   └── device_admin_policy.xml
│   │   │       └── values/
│   │   │           ├── strings.xml
│   │   │           └── themes.xml
│   │   ├── test/                     # Unit tests
│   │   └── androidTest/              # Instrumented tests
│   └── proguard-rules.pro            # ProGuard configuration
│
├── build.gradle.kts                  # Root build configuration
├── settings.gradle.kts               # Modules configuration
├── gradle.properties                 # Global Gradle properties
├── local.properties.example          # Local environment template
├── README.md                         # Project overview
└── BUILD_INSTRUCTIONS.md             # Detailed build guide
```

## Build Process Flow

1. **Clean**: `./gradlew clean`
   - Removes all previous build artifacts

2. **Compile**: `./gradlew build`
   - Compiles Kotlin sources
   - Processes resources
   - Resolves dependencies

3. **Package**:
   - Debug APK: `./gradlew assembleDebug`
   - Release APK: `./gradlew assembleRelease`

4. **Deploy**:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## Gradle Build System

- **Build Tool**: Gradle 8.1.0+
- **Android Gradle Plugin**: 8.1.0+
- **Kotlin Plugin**: 1.9.10+

## Configuration Files

### build.gradle.kts (Root)
- Plugin versions
- Common build configurations

### app/build.gradle.kts
- Application ID and versioning
- Dependency declarations
- Compilation options

### settings.gradle.kts
- Module inclusion
- Repository configuration

### gradle.properties
- JVM arguments for Gradle daemon
- AndroidX enablement
- Build feature flags

## Output Artifacts

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`
- Compiled classes: `app/build/intermediates/classes/`
- Resources: `app/build/intermediates/res/`

## Key Dependencies

- **Supabase**: PostgREST, Realtime, Ktor Client
- **Jetpack Compose**: UI framework
- **Kotlin Coroutines**: Async operations
- **AndroidX**: Core libraries

## Build Variants

- **Debug**: Development builds with debugging symbols
- **Release**: Optimized production builds with ProGuard obfuscation