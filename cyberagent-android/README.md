# CyberAgent Android

Modern Android security agent — defensive, local-first architecture.

## Stage 1 — implemented
- installed application inventory
- sensitive-permission heuristic analysis
- APK SHA-256 calculation
- local risk engine: SAFE / REVIEW / HIGH
- manual scan with result list and details
- periodic read-only background scan with WorkManager (12 h)
- read-only CyberAgent threat-feed endpoint integration planned for the next stage
- event history and Cyber Coach planned

The scanner is heuristic: a sensitive permission is a signal, not proof of malware. SHA-256 identifies an APK and can later be matched against a trusted threat feed.

No destructive actions are performed automatically. Uncertain findings require user confirmation.

## Android notes
`QUERY_ALL_PACKAGES` is used by the prototype to build a broad installed-app inventory on Android 11+. Distribution through Google Play is subject to package-visibility policy and requires appropriate justification.

## Backend
`https://cyberagent-api.onrender.com`

Threat feed endpoint:
`/api/v1/threat-feed`

## Build
The repository contains a GitHub Actions workflow that builds the debug APK and uploads it as an artifact on changes under `cyberagent-android/`.
