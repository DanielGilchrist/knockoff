# knockoff

A personal Android `NotificationListenerService`, sideloaded via adb. It holds notifications from chosen apps, accounts or chats according to rules, by snoozing them and silencing their channels, and releases them when no rule holds them.

## Working norms

- British English in prose, user-facing strings and identifiers.
- No comments unless one explains a surprising quirk the code can't make clear. No KDoc. History belongs in commit messages.
- Log through `Log.x(LOG_TAG, ...)`. Never log subtext, message text or account addresses. Notification keys and conversation channel IDs can contain chat IDs (often phone numbers); log them only where needed to act over adb, and never add a log of a bare chat ID.
- Be concise in code, commit messages and review feedback. Skip preambles.

## Type-driven design

Encode invariants in types so the compiler enforces them. Where that isn't possible, model the boundary explicitly.

### Make invalid states unrepresentable

Model a value with several states as a sealed type, not a combination of flags and nullables.

```kotlin
data class Download(val url: String, val complete: Boolean, val path: String?, val error: String?)
```

This allows `complete = true, path = null`, which makes no sense but compiles.

```kotlin
sealed interface Download {
    val url: String
    data class Pending(override val url: String) : Download
    data class Complete(override val url: String, val path: String) : Download
    data class Failed(override val url: String, val reason: String) : Download
}
```

Each state carries exactly the data it needs, and an exhaustive `when` stops compiling when a variant is added.

### Parse, don't validate

Validate untrusted input once, at the boundary, into a typed value; downstream code accepts only the typed value. A raw `StatusBarNotification` is parsed at the listener boundary into a domain type, and decision logic never sees the raw notification.

### Give meaningful primitives a type

A package name is a `PackageName`, an account address an `AccountAddress`, never a `String`. Use `@JvmInline value class` so the wrapper costs nothing at runtime.

### Errors as values

`fun foo(): Bar` is a lie if `foo` can throw. Every I/O boundary (files, the package manager, snooze calls, any system API) returns a sealed result. Catch specific exceptions at the boundary and convert them straight away. Each failure variant carries a diagnostic `detail` plus whatever its caller needs to decide. The real example, `app/src/main/kotlin/dev/danielgilchrist/knockoff/settings/SettingsLoad.kt`:

```kotlin
sealed interface SettingsLoad {
    sealed interface Usable : SettingsLoad {
        val settings: Settings
    }

    data class Loaded(
        override val settings: Settings,
    ) : Usable

    data object Empty : Usable {
        override val settings: Settings get() = Settings.DEFAULT
    }

    sealed interface Failure : SettingsLoad {
        val detail: String
    }

    data class ReadFailed(
        override val detail: String,
    ) : Failure

    data class Corrupt(
        val failure: SettingsDecode.Failure,
    ) : Failure {
        override val detail: String get() = failure.detail
    }
}
```

- **Same swallow, different syntax.** Replacing `runCatching { x }.getOrNull()` with `try { x } catch (_: E) { null }` isn't a fix. A change that keeps a nullable, `Unit` or `Boolean` return has renamed the violation, not removed it.
- **"Absent" isn't always "failed".** Absence on a normal path (first run, no settings yet) gets its own variant, as `Empty` does above. If the thing must always exist, its absence is a failure. Model what callers switch on, not every imaginable state.
- **Catch only what you can handle; let the rest crash.** Never catch `Exception` or `Throwable`. An unexpected `NullPointerException` or `IllegalStateException` has found a real bug.
- `runCatching` swallows `CancellationException` and `kotlin.Result` carries an untyped `Throwable`, so neither is used.
- **Handle failures where the result is consumed.** Domain functions never take or return platform failures, so core logic never sees an unhappy path it can't act on.
- **Throw only for a genuine bug types can't rule out**, and make it loud. Prefer a private constructor plus a `parse` returning a sealed result wherever the invariant can be expressed.

### No loose strings or nullable state markers

- A `String` parameter that picks behaviour, names a variant or describes the caller (`applyRules(load, "sweep")`) is a missing type. Use an enum or sealed type, or let each call site write its own message.
- A nullable `var` meaning "nothing yet" (`var before: RuleSet? = null`) is an unmodelled state. Name the states in a sealed type.
- `null` from a platform API is fine; convert it at the boundary.

### Use exhaustive `when`, never `as?`

`as?` makes the compiler forget what it knows: a variant added later quietly becomes `null`, and the `?` spreads downstream.

Drops `ChannelPattern` silently:

```kotlin
val name = target as? Target.ChatNamePattern ?: return
```

Compiler forces every variant (from `PatternPick.of`):

```kotlin
when (target) {
    is Target.ChannelPattern -> Channel(target.pattern)
    is Target.ChatNamePattern -> ChatName(target.pattern)
}
```

### Be suspicious of `isX` / `canX` / `hasX` Boolean parameters

Before adding one, check in order:

1. **Is it derived from a sealed type the caller already has?** Pass the type and use `when`.
2. **Is it derived from another parameter you already pass?** Derive it inside.
3. **Does it gate an action the user wants?** Remove the dependency that makes it conditional, rather than gating it in the UI.

Otherwise, as with a local `expanded` toggle, the Boolean is fine. That's rare.

### Rich domain models, pure core

Schedule maths and rule matching are pure functions on domain types, such as `schedule.coverageAt(now)` and `ruleSet.nextBoundary(now)`. No service objects, no `Util` classes. The listener is a thin shell: it parses each notification, asks the domain for a decision, and carries it out.

### Idiomatic Kotlin

- `data class` for values, `sealed interface` for sum types, `enum class` for closed sets with no per-variant data.
- Expose observable state as `StateFlow`; only the owner mutates its `MutableStateFlow`, everyone else gets `asStateFlow()`.
- Coroutines, not threads; let structured concurrency handle cancellation.

### Pragmatism

These rules pay off for values with several states or failure modes, that cross layers, or that come from outside. Plain types are fine for local, self-contained code. When in doubt, lean towards the typed version.

## Layout

- One top-level type per file, named after it, directories mirroring packages. A sealed type's variants are nested inside it (`SettingsLoad`); apart from companion objects, don't nest anything else.
- Class body order: companion object, `init`, properties, public functions, private functions. A sealed type puts its abstract members first, then its variants, then its companion (`PatternPick`). Never interleave public and private members. Existing files that differ are fixed when touched, not in bulk. No tool checks this, so review does.
- A blank line follows any multi-line statement before the next logical chunk.

## Build gate

- Needs JDK 25 (`jvmToolchain(25)`, `JavaVersion.VERSION_25`). Gradle doesn't provision toolchains here, so `JAVA_HOME` must point at a JDK 25 (asdf: `java temurin-25.0.4+101.0.LTS`).
- Before every commit: `./gradlew ktlintFormat check`. `check` runs detekt, ktlint, Android lint, every unit test, the `:lint` tests and `:domain:verifyMutants`.
- While iterating: `./gradlew :domain:test :app:testDebugUnitTest`. Tests time out after 10 s by default.
- Detekt results are never cached and the daemon can keep stale `:lint` classes: after changing `:lint`, run `./gradlew --stop` and check a planted violation is reported.

## Lints

- Every module compiles with `allWarningsAsErrors = true`; a new module must too.
- Don't suppress lints; refactor instead (group a long parameter list into a data class, split a long method, model the missing sum type).
- `:lint` holds project detekt rules, applied to `:domain` and `:app` through `config/detekt/{domain,app}.yml`. They match names without type resolution, because type-resolved rules don't run in plain `detekt`:
  - `UnapprovedCatch`: only exception types under `allowedExceptions` may be caught; catch operators such as `Flow.catch` are banned.
  - `UnapprovedThrow`: `throw`, `!!`, `require`, `requireNotNull`, `check`, `checkNotNull`, `error` and `TODO` only in declarations under `allowedDeclarations`, listed with parameter types (`dev.danielgilchrist.knockoff.domain.schedule.Schedule.coverageAt(ZonedDateTime)`) so an overload isn't approved by accident.
  - `UntypedResult`, `NoImportAlias`, `NoTypeFiltering` (`as?`, `filterIsInstance`) and `UnusedImport` (operator and delegate imports such as `getValue`, `plus`, `componentN` count as used; detekt's own needs type resolution).
- `SuppressionsTest` fails on any `@Suppress`, `@SuppressWarnings` or `@SuppressLint` in `:domain`, `:app` or `:lint`. It's a test because detekt drops findings inside a matching suppression, including one on the suppression itself. kotlin-lsp false positives are ignored, not suppressed.
- Every allowlist entry carries a justification comment, and `AllowlistsTest` fails on an entry that no longer matches code. Adding one is a design decision: push for a type or boundary change first.
- Known gap: stdlib calls that throw on bad input (`first {}`, `single()`, `getValue`, `LocalTime.parse`, `lateinit`) can't be listed without types. Review covers them.
- A rule that can't be a type should become a `:lint` rule rather than rely on review.

## Tests

- `:domain` has no Android dependencies, so the compiler keeps it pure. Its tests live in `domain/src/test/kotlin`; Android boundaries and stores in `app/src/test/kotlin`; `:lint` rules in `lint/src/test/kotlin` (detekt-test's `lint`).
- Don't add kotest; assert with the JUnit-backed helpers in `domain/src/test/kotlin/dev/danielgilchrist/knockoff/domain/Assertions.kt`.
- **What gets tested:**
  - Parsing a notification (`NotificationSnapshot.parse`): each `Conversation`, `ConversationTitle` and `OriginGroup` variant, group summaries and unsnoozable notifications.
  - Matching and covering for every `Target` variant, including pattern expansion.
  - Schedule maths: before, during and after hours, exact boundaries, overnight windows, breaks, both DST transitions, fixed-zone and device-zone rules.
  - Every persisted document round-trips and upgrades from every older version. Each has a `CURRENT_VERSION` and a frozen `StoredXVn` reader per older version that upgrades to the next version, never straight to the current one; saving writes the current version. Changing a stored shape means a new version, a frozen copy of the old reader, and those upgrade tests.
  - Every store against a real file, including missing, corrupt and unreadable files.
- **What doesn't:** Compose layout and real listener behaviour. The phone is the source of truth for those.
- Every behaviour change ships with tests covering its full input matrix in the same commit.

### Mutation testing

- PIT mutates `:domain`. `./gradlew :domain:verifyMutants` fails on any surviving or uncovered mutant not listed in `config/pitest/accepted-survivors.txt`.
- Accepted survivors are keyed by class, method, call-site line and PIT's mutant description, matched by count, with a justification comment per group, so moved code forces a fresh look.
- Mutants in inlined stdlib bytecode map to their call-site line through the Kotlin SMAP. Mutants in inlined lambda classes (no SMAP) can't be accepted and must be killed.
- `TIMED_OUT` counts as detected; `RUN_ERROR` and `MEMORY_ERROR` fail the gate. A `RUN_ERROR` after code moved can be a stale report: rerun `./gradlew :domain:pitest --rerun` before investigating.
- PIT hot-swaps mutants without rerunning static initialisers, so a value computed once in a companion `val` hides mutants in everything it calls. Expose such values as getters (`val DEFAULT get() = ...`).
- `:domain` compiles with `jvmDefault = NO_COMPATIBILITY` so interface methods with bodies don't produce `DefaultImpls` bridges PIT reports as uncovered; `*$$serializer` classes are excluded; `pitest` is never restored from the build cache.
- `VOID_METHOD_CALLS` is excluded; revisit if the domain ever calls a `Unit` function for its effect.
- Kill a survivor with a test or simplify the code. Accept it only when it's genuinely equivalent.

## Review

- Before every commit, two fresh subagents that didn't write the code review in parallel, given the full diff and this file, not a summary. Both must return `VERDICT: APPROVE`.
  - **Principles reviewer:** correctness and every rule in this file. Read each added line for the Type-driven smells and for helpers whose arguments only feed a log line. Challenge every new allowlist entry, and treat fixes made after review as new code.
  - **Redundancy reviewer:** dead code, unused declarations and dependencies, speculative generality, duplicate tests, and anything deletable without losing behaviour or a principle.
- Fix every finding and re-review until both approve. If round three still requests changes, round four adds an arbiter that discards findings that aren't real problems.

## Device loop

```
./gradlew :app:installDebug
adb shell am start -n dev.danielgilchrist.knockoff/.MainActivity
adb logcat -s Knockoff:V
```

- After every `installDebug`, launch the app without asking. Before asking for a repro, start `adb logcat` in the background and read it yourself.
- I do the on-device interaction; you handle setup and observation.
- A green build isn't a working feature: a behaviour change isn't done until it's confirmed on the phone.
- Grant listener access: `adb shell cmd notification allow_listener dev.danielgilchrist.knockoff/dev.danielgilchrist.knockoff.listener.KnockoffListener`. `disallow_listener` then `allow_listener` forces a rebind, which sweeps.
- Post a test notification: `adb shell cmd notification post -t Title <tag> Body`. Use a fresh tag: re-posting a snoozed key replaces it silently without calling listeners.
- Inspect snoozes: `adb shell dumpsys notification | grep -A3 "Pending snoozed"`.
- Release a snooze by hand: `adb shell "cmd notification snooze --for 1 '<key>'"` (quote the key; it contains `|`). Never use `cmd notification unsnooze`: it fails with `Permission Denial: getIntentSender()` and loses the notification.
- Link without the UI: `adb shell cmd companiondevice associate 0 dev.danielgilchrist.knockoff <mac>` (any address works); `disassociate` removes it; `adb shell cmd companiondevice list 0` lists.
- Saved originals: `adb shell run-as dev.danielgilchrist.knockoff cat files/silenced-channels.json`. Channel importances: `adb shell dumpsys notification | grep "mId='<channel>'"`.

## Git

- Only commit when asked. Don't commit a behaviour change until it's confirmed on-device; pure refactors can be committed on a green gate.
- Correct an unpushed commit with `git commit --amend`, not a new or fixup commit.
- Ask before including markdown files other than AGENTS.md in a commit.
