# Knockoff

An app to silence notifications for particular apps under various conditions.

## Build

Requires JDK 25 and the Android SDK.

```sh
./gradlew :app:installDebug
```

## Check

```sh
./gradlew ktlintCheck detekt check :app:testDebugUnitTest :app:lintDebug
```

## Licence

GPL-3.0. See [LICENSE](LICENSE).
