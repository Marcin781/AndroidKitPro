# CyberAgent Android

Modern Android security agent — defensive, local-first architecture.

## Stage 1
- application inventory
- permission risk analysis
- APK SHA-256 calculation
- local risk engine: SAFE / REVIEW / HIGH
- background scanning with WorkManager (planned in Android module)
- read-only threat-feed synchronization from CyberAgent API
- event history and Cyber Coach (planned)

No destructive actions are performed automatically. Uncertain findings require user confirmation.

## Backend
`https://cyberagent-api.onrender.com`

Threat feed endpoint:
`/api/v1/threat-feed`
