# phs-iptv-tv — project guide for Claude Code

## What this is
Android TV (Google TV) IPTV app for PHS-Lite hotels. Kotlin + Compose for TV.
Spec of record: PRD_PHS_Lite_IPTV_AndroidTV_v0.4.md (read before editing any UI/logic).

## Hard rules
- UI: Compose for TV only (androidx.tv:tv-material). Every D-pad-navigable element must have a gold focus ring (PRD §9.5).
- Brand: only use tokens from `ui/theme/Color.kt` + `Type.kt`. NO hardcoded hex in screen files (PRD §9).
- Thin client: no PMS logic embedded here. Backend communication via Retrofit API only (PRD §8); DTOs mirror PRD §5.3.
- Kiosk: app always in foreground; never navigate to system launcher/settings.
- Persist: DataStore only (provisioning config). Clear guest session on check-out event.
- i18n: all strings in res/values(-en)/strings.xml; language selected by guest.nationality.

## Provisioning model (F0 — v0.4)
1. First boot: generate UUID `deviceId` once, persist permanently in DataStore.
2. `POST /iptv/devices/register { deviceId }` → `{ displayCode, status }`.
3. Show `displayCode` on screen — staff goes to PHS console and assigns room to this device.
   (displayCode is a label to identify the machine; NOT a code to type on TV.)
4. Poll `GET /iptv/devices/me` every 5s → when `status=="assigned"` → save `branchId/roomId/roomNo` → transition to IdleScreen.
5. "Làm mới" button = re-calls register + restarts poll. `deviceId` never changes.
6. Debug bypass (debug build only): "Debug: Bỏ qua" button calls `onAssigned()` directly.

## State machine (PRD §12)
PROVISIONING → IDLE → (FCM guest.checked_in) WELCOME → INTRO_VIDEO → HOME → {FOLIO|VOICE|ORDER}
HOME → (FCM guest.checked_out) IDLE

## Architecture (the load-bearing wiring)
- `AppStateMachine` (Hilt @HiltViewModel, `domain/`) is the single source of truth for which screen shows.
  It holds `screen` + guest/stay/videoUrl StateFlows. Screens never navigate directly — they call its
  transition methods (`onProvisioned`, `onWelcomeDismissed`, `navigateTo`, …).
- `AppNavigation` (`ui/navigation/`) is a thin mirror: a LaunchedEffect maps `screen` → a NavHost route
  and always `popUpTo(0)` (no back stack — kiosk). Adding a screen = add to `AppScreen`, the `when` in
  AppNavigation, and a `composable` + Routes entry. Folio/Order currently fall through to Home.
- FCM → state flows through `GuestEventBus` (@Singleton SharedFlow): `PhsFcmService` emits a GuestEvent,
  AppStateMachine collects it in `init`. This bridges Service → ViewModel without a direct reference.
- Intro-video replay is gated per-stay: `onWelcomeDismissed`/`onIntroVideoEnded` check/mark `stayId` in
  ProvisioningDataStore so a re-check-in with the same stay skips the video (PRD §10.4).
- All endpoints (`data/remote/IptvApi.kt`) auth by `X-Device-Id` header (the first-boot UUID) — no token.
- No tests exist yet (no `:test`/`:androidTest` sources). D-pad/focus behaviour can only be checked on
  emulator/remote, never in @Preview.

## Build / run
- ./gradlew assembleDebug --no-daemon
- Emulator: AVD "Television (1080p)"
- Place google-services.json in app/ before enabling FCM (see §FCM below)

## Package / stack
- Package root: vn.phs.iptv
- DI: Hilt
- Networking: Retrofit + kotlinx.serialization
- Images: Coil
- Local config: DataStore (Preferences)
- Push: Firebase Cloud Messaging (skeleton until google-services.json added)

## Conventions
- One Composable per screen file under ui/<feature>/
- @Preview(device = Devices.TV_1080p) required on every screen
- Focus testing must be done on emulator/remote — @Preview does not reflect D-pad behaviour
- ViewModel per screen, injected via hiltViewModel()

## FCM setup (pending)
1. Create Firebase project → download google-services.json
2. Place in app/
3. In app/build.gradle.kts: uncomment `alias(libs.plugins.google.services)`
4. In AndroidManifest.xml: remove the `tools:node="remove"` block for FirebaseInitProvider
5. Rebuild

## Scaffold status
- [x] F0 ProvisioningScreen + IdleScreen (auto-register + console assign model)
- [x] F1 WelcomeScreen
- [x] F2 IntroVideoScreen + Media3
- [x] HomeScreen
- [x] F3 VoiceAssistantScreen
- [ ] F4 FolioScreen (route falls through to Home)
- [ ] F5 OrderScreen (route falls through to Home)

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **phs-iptv** (1581 symbols, 2912 relationships, 112 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> Index stale? Run `node .gitnexus/run.cjs analyze` from the project root — it auto-selects an available runner. No `.gitnexus/run.cjs` yet? `npx gitnexus analyze` (npm 11 crash → `npm i -g gitnexus`; #1939).

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows. For regression review, compare against the default branch: `detect_changes({scope: "compare", base_ref: "main"})`.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `query({search_query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `context({name: "symbolName"})`.
- For security review, `explain({target: "fileOrSymbol"})` lists taint findings (source→sink flows; needs `analyze --pdg`).

## Never Do

- NEVER edit a function, class, or method without first running `impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `rename` which understands the call graph.
- NEVER commit changes without running `detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/phs-iptv/context` | Codebase overview, check index freshness |
| `gitnexus://repo/phs-iptv/clusters` | All functional areas |
| `gitnexus://repo/phs-iptv/processes` | All execution flows |
| `gitnexus://repo/phs-iptv/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->
