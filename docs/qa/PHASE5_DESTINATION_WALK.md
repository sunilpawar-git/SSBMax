# Phase 5 destination walk — `SSBMaxRoot` gate

KMP-convergence plan, Phase 5 ("The cutover") gate: walk every destination
registered in `SSBMaxNavHost` on both platforms after `MainActivity` swapped
onto `SSBMaxRoot()`. With no automated iOS UI suite, this list **is** the iOS
regression suite — keep it current as destinations are added or removed, and
re-walk it before any release, not just once.

55 destinations, one row per `composable<SSBMaxDestinations.X>()` registration
across `shared/navigation/*Graph.kt` (grouped by the graph file that registers
them). Check both columns when manually verified on a real device/simulator.

**Status as of Phase 5 close (2026-08-01): not walked.** No Android
emulator or iOS simulator was available in this environment — the same
constraint recorded in every prior phase's verification section. All code,
build, lint, detekt, and Koin-graph gates below are green; only the
device-level walk is outstanding. Treat every row as unverified until
someone with a device/simulator checks it off.

## Auth (`AuthGraph.kt`)
- [ ] Android [ ] iOS — Splash
- [ ] Android [ ] iOS — Login
- [ ] Android [ ] iOS — RoleSelection

## Home (`HomeGraph.kt`)
- [ ] Android [ ] iOS — StudentHome
- [ ] Android [ ] iOS — InstructorHome

## Psych tests (`PsychTestsGraph.kt`)
- [ ] Android [ ] iOS — OIRTest
- [ ] Android [ ] iOS — OIRTestResult
- [ ] Android [ ] iOS — PPDTTest
- [ ] Android [ ] iOS — PPDTSubmissionResult
- [ ] Android [ ] iOS — TATTest
- [ ] Android [ ] iOS — TATSubmissionResult
- [ ] Android [ ] iOS — WATTest
- [ ] Android [ ] iOS — WATSubmissionResult

## Written tests (`WrittenTestsGraph.kt`)
- [ ] Android [ ] iOS — SRTTest
- [ ] Android [ ] iOS — SRTSubmissionResult
- [ ] Android [ ] iOS — SDTest
- [ ] Android [ ] iOS — SDSubmissionResult
- [ ] Android [ ] iOS — PIQTest
- [ ] Android [ ] iOS — PIQSubmissionResult

## GTO (`GTOGraph.kt`)
- [ ] Android [ ] iOS — GTOGDTest
- [ ] Android [ ] iOS — GTOGDResult
- [ ] Android [ ] iOS — GTOLecturetteTest
- [ ] Android [ ] iOS — GTOLecturetteResult
- [ ] Android [ ] iOS — GTOGPETest
- [ ] Android [ ] iOS — GTOGPEResult

## Interview (`InterviewGraph.kt`)
- [ ] Android [ ] iOS — StartInterview
- [ ] Android [ ] iOS — VoiceInterviewSession
- [ ] Android [ ] iOS — InterviewResult (incl. real notification deep link, cold + background)

## Instructor vertical (`InstructorVerticalGraph.kt`)
- [ ] Android [ ] iOS — InstructorStudents
- [ ] Android [ ] iOS — InstructorGrading
- [ ] Android [ ] iOS — InstructorAnalytics
- [ ] Android [ ] iOS — CreateBatch
- [ ] Android [ ] iOS — BatchDetail
- [ ] Android [ ] iOS — StudentDetail
- [ ] Android [ ] iOS — InstructorGradingDetail

## Profile & settings (`ProfileSettingsGraph.kt`)
- [ ] Android [ ] iOS — UserProfile (incl. onboarding `BackHandler`)
- [ ] Android [ ] iOS — StudentProfile
- [ ] Android [ ] iOS — Settings (incl. live theme toggle, both platforms)
- [ ] Android [ ] iOS — SubscriptionManagement
- [ ] Android [ ] iOS — UpgradeScreen

## Submissions & results (`SubmissionsResultsGraph.kt`)
- [ ] Android [ ] iOS — JoinBatch (placeholder: `NotYetPorted`, no real screen either platform)
- [ ] Android [ ] iOS — Marketplace
- [ ] Android [ ] iOS — HistoricResults
- [ ] Android [ ] iOS — StudentSubmissions
- [ ] Android [ ] iOS — StudentTests
- [ ] Android [ ] iOS — SubmissionDetail
- [ ] Android [ ] iOS — Analytics

## Study content (`StudyContentGraph.kt`)
- [ ] Android [ ] iOS — StudyMaterialsList
- [ ] Android [ ] iOS — StudentStudy
- [ ] Android [ ] iOS — StudyMaterialDetail
- [ ] Android [ ] iOS — TopicScreen (incl. `?selectedTab=` arg, the Phase 3a #2 fix)
- [ ] Android [ ] iOS — NotificationCenter
- [ ] Android [ ] iOS — Phase1Detail
- [ ] Android [ ] iOS — Phase2Detail
- [ ] Android [ ] iOS — SSBOverview
- [ ] Android [ ] iOS — NotYetPorted (placeholder target itself — confirm it renders, doesn't crash)

## Not registered (confirmed dead in the Android original, correctly not ported)
`FAQScreen`, `GradingDetailScreen`, `MemoryLeakTestScreen`, `MockPaymentScreen`,
`PaymentSuccessScreen`, `app/ui/upgrade/UpgradeScreen` — see `SSBMaxNavHost.kt`'s
class doc for the reachability audit that confirmed these have zero live callers.

## Cross-cutting checks (not per-destination)
- [ ] Drawer + bottom-nav chrome renders on both platforms, `onOpenDrawer` opens the real drawer
- [ ] Auth-screen chrome suppression (Splash/Login/RoleSelection show no scaffold)
- [ ] Theme toggle in Settings re-themes the running app live, both platforms
- [ ] Cold-start notification tap resolves via `DeepLinkGateway`, both platforms
- [ ] Background notification tap resolves via `DeepLinkGateway`, both platforms
