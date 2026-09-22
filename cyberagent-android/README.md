# CyberAgent Android

CyberAgent to defensywny agent bezpieczeństwa Android — lokalny, read-only i nastawiony na wykrywanie oraz informowanie użytkownika.

## Wersja 0.2.0

- inwentaryzacja zainstalowanych aplikacji
- analiza wrażliwych uprawnień: SAFE / REVIEW / HIGH
- obliczanie SHA-256 pliku APK
- dopasowanie SHA-256 do threat feedu CyberAgent
- ręczne skanowanie oraz historia wyników
- cykliczny skan WorkManager co 12 godzin
- lokalny Cyber Coach z zaleceniami
- bezpieczna telemetria stanu sieci przez ConnectivityManager
- historia zmian stanu sieci, lokalnie i ograniczona do 100 zdarzeń
- lokalna historia alertów IOC z deduplikacją 10 minut
- dokładne dopasowanie IOC przygotowane dla IP / domen / URL / SHA-256
- powiadomienia o wykryciach wysokiego ryzyka

## Ważne ograniczenie monitoringu sieci

Aktualna wersja nie przechwytuje, nie odszyfrowuje, nie przekierowuje i nie blokuje pakietów. Telemetria sieci pokazuje stan połączenia (transport, walidacja, taryfikowanie, VPN i liczbę aktywnych sieci). Silnik IOC jest przygotowany do korelacji danych, ale obecna wersja nie pobiera arbitralnych adresów docelowych z ruchu aplikacji.

## Bezpieczeństwo i prywatność

Dane historii są przechowywane lokalnie w ograniczonych buforach. Agent nie wykonuje automatycznie destrukcyjnych działań ani nie usuwa aplikacji. Ocena uprawnień jest heurystyczna — samo uprawnienie nie jest dowodem złośliwego oprogramowania.

## Android / Google Play

Prototyp używa `QUERY_ALL_PACKAGES`, aby zbudować szeroki lokalny wykaz aplikacji na Androidzie 11+. Publikacja w Google Play podlega zasadom widoczności pakietów i wymaga odpowiedniego uzasadnienia.

## Backend

https://cyberagent-api.onrender.com

Threat feed: `/api/v1/threat-feed`

## Build

Repozytorium zawiera workflow GitHub Actions budujący debug APK i zapisujący go jako artifact przy zmianach w `cyberagent-android/`.

Debug APK służy do testów instalacyjnych; wydanie produkcyjne wymaga osobnego podpisania kluczem release.
