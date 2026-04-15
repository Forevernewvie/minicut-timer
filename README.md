# MiniCut Timer

안드로이드 전용 미니컷 타이머 앱입니다. 2~6주 미니컷 계획을 설정하고, 날짜별 총 섭취 칼로리를 기록하며, 달력에서 기록 상태를 한눈에 확인할 수 있습니다.

## 프로젝트 운영
- Git Flow 브랜치 전략 사용 (`main`, `develop`, `feature/*`, `release/*`, `hotfix/*`)
- GitHub Actions CI: 테스트/린트/빌드 자동 검증

## 개인정보 처리방침
- 페이지 파일: `docs/privacy-policy/index.html`
- GitHub Pages 공개 URL: `https://forevernewvie.github.io/minicut-timer/privacy-policy/`
- 배포 방식: `main` 브랜치 반영 시 GitHub Actions(`Privacy Policy Pages`)로 자동 배포
- 최초 1회 저장소 Settings → Pages에서 Source를 **GitHub Actions**로 설정해야 합니다.

## 주요 기능
- 2~6주 미니컷 계획 생성
- 하루 목표 칼로리 설정
- 오늘 먹은 음식 + 섭취 칼로리 빠른 기록
- 오늘 섭취 / 남음 / 초과 칼로리 확인
- 달력에서 날짜별 칼로리 및 상태 확인
- 1000~1500kcal 권장 가이드 노출
- Room 기반 로컬 저장
- 오프라인 사용

## 기술 스택
- Kotlin
- Jetpack Compose + Material 3
- Room
- MVVM + Repository

## 실행 방법
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"
./gradlew test
./gradlew assembleDebug
```
