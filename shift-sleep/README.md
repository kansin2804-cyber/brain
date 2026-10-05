# 교대수면 (ShiftSleep)

교대근무에 맞춘 **취침·기상·카페인 컷오프** Android 앱 MVP.

> 근무표 앱(마이듀티 등)을 대체하지 않습니다. **옆에 붙는 수면 OS**입니다.  
> 의료·진단·치료 앱이 아닙니다.

## 스택

| 모듈 | 역할 |
|---|---|
| `:plan_engine` | 순수 Kotlin JVM — 교대 → 수면 플랜 룰 (iOS 재사용 대비) |
| `:app` | Kotlin + Jetpack Compose + Room + 로컬 알림 |

## 빌드

```bash
export ANDROID_HOME=~/android-sdk   # 또는 local.properties sdk.dir
./gradlew :plan_engine:test :app:testDebugUnitTest
./gradlew :app:assembleDebug
./scripts/smoke.sh   # 기기 연결 시
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## MVP 범위 (W1–W3)

- [x] 온보딩 (병원 3교대 / 12h 2교대 / 커스텀)
- [x] 근무 CRUD + 이번 주 시드
- [x] 오늘 플랜 카드 (취침 / 기상 / 카페인) + 이유 한 줄
- [x] 7일 근무·수면 요약
- [x] 로컬 알림 (취침·기상 기본, 카페인·wind-down 선택)
- [x] 면책 문구
- [x] Play Billing + 로컬 7일 체험 (soft free)
- [x] Analytics/Crashlytics 스캐폴딩 (google-services.json 선택)
- [ ] Play Console 상품 + Firebase 프로젝트 (사용자)

## 패키지

`com.shiftsleep.app` — JU Housing 브랜드와 분리.

## Play 구독 상품 ID

- `shiftsleep_pro_monthly`
- `shiftsleep_pro_yearly`

Play Console에 동일 ID로 등록해야 구매 버튼이 활성화됩니다.  
Analytics: `app/google-services.json.example` 참고 · Agent Store `shift-sleep-analytics-v04.md` / `shift-sleep-billing-v03.md`.

## 문서

기획·로드맵은 Cursor Agent Store의 `shift-sleep-*.md` 참조.
