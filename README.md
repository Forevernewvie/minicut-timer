# ⏳ MiniCut Timer (React Native & Expo)

**미니컷 타이머**는 2~6주 단기 미니컷 계획을 성공적으로 설계하고 수행할 수 있도록 돕는 감량 코칭 및 라이프 로그 애플리케이션입니다.  
기존의 Kotlin Android 전용 앱에서 **React Native(Expo SDK 57) 및 TypeScript 기반의 크로스 플랫폼 애플리케이션**으로 풀 마이그레이션이 완료되었습니다.

---

## 🏛️ Technology Stack & Architecture

현대적이고 일관된 크로스 플랫폼 설계를 위해 다음과 같은 스택을 준수하여 작성되었습니다.

*   **Core**: React Native & **Expo SDK 57 (Latest)**
*   **Navigation**: **Expo Router** (파일 기반 라우팅 규칙 준수)
*   **State Management**: **Zustand**를 활용해 비즈니스 로직과 글로벌 상태 관리
*   **Styling**: **NativeWind (Tailwind CSS v3)**를 사용해 다크 슬레이트 무드의 프리미엄 UI 설계
*   **Database**: **`expo-sqlite` (Promise-based API)**를 활용한 온디바이스 로컬 관계형 DB 영구 저장소 구축
*   **Iconography**: `lucide-react-native` 패키지의 타입 안정성 기반 아이콘 세트 적용
*   **Unit Verification**: TypeScript 컴파일러 정적 검사 및 22개 조건의 도메인 검증 테스트 파이프라인 탑재

---

## 🎨 Premium Dark UI/UX Design

*   **Aesthetics**: 딥 다크 슬레이트 백그라운드(`#0B0F19`)와 미세한 경계 테두리를 사용한 반투명 다크 카드로 글래스모피즘 분위기를 연출하였습니다.
*   **Vibrant Accents**: 
    *   `Emerald-500` (#10B981): 감량 목표 진행률 및 미션 성공을 상징
    *   `Indigo-500` (#6366F1): AI 코칭 추천 행동 및 체크인 유도
    *   `Rose-500` (#F43F5E): 목표 초과량 지표 및 건강 경고(Guardrail)
*   **Micro-Interactions**: 모든 버튼 터치 영역(`Pressable`)에 부드러운 스케일 피드백 및 호버링 활성화를 설계하여 프리미엄 질감을 부여하였습니다.

---

## 💾 Local SQLite Database Schema

애플리케이션 기동 시 `src/database/db.ts` 내에서 자동으로 Write-Ahead Logging (WAL)을 기동하고 다음의 3개 관계형 테이블을 생성합니다.

### 1. `mini_cut_plan` (단일 활성 플랜 테이블)
*   `id`: INTEGER PRIMARY KEY CHECK (id = 1) -> 항상 단 하나의 액티브 플랜만 허용
*   `startDate`: TEXT (YYYY-MM-DD)
*   `durationWeeks`: INTEGER (2 ~ 6주)
*   `endDate`: TEXT (YYYY-MM-DD)
*   `dailyTargetKcal`: INTEGER (1000 ~ 1500 kcal)
*   `goalMode`: TEXT ('MassReset' | 'EventReady')
*   `activityLevel`: TEXT ('Low' | 'Moderate' | 'High')
*   `estimatedMaintenanceKcal`: INTEGER
*   `isActive`: INTEGER (0 또는 1)

### 2. `calorie_entries` (오늘 섭취 칼로리 내역 테이블)
*   `id`: INTEGER PRIMARY KEY AUTOINCREMENT
*   `date`: TEXT (YYYY-MM-DD, 인덱싱)
*   `calories`: INTEGER
*   `foodName`: TEXT
*   `note`: TEXT
*   `timeLabel`: TEXT ('아침' | '점심' | '저녁' | '간식')
*   `isFavorite`: INTEGER (0 또는 1)
*   `createdAt`: TEXT (ISO Timestamp)

### 3. `daily_condition_checks` (3분 코칭 컨디션 체크인 테이블)
*   `date`: TEXT PRIMARY KEY (YYYY-MM-DD)
*   `bodyWeightKg`: REAL (몸무게)
*   `proteinGrams`: INTEGER (단백질 섭취량)
*   `resistanceSets`: INTEGER (저항 운동 세트 수)
*   `mainLiftKg`: REAL (핵심 훈련 중량)
*   `relapseTrigger`: TEXT (식단 이탈 트리거)
*   `copingAction`: TEXT (예방/대응 실행 행동)
*   `sleepHours`: REAL (수면 시간)
*   `fatigueScore`: INTEGER (피로도 1~5)
*   `hungerScore`: INTEGER (공복도 1~5)
*   `moodScore`: INTEGER (기분 1~5)
*   `workoutPerformanceScore`: INTEGER (수행력 1~5)
*   `updatedAt`: TEXT (ISO Timestamp)

---

## 🛡️ Deficit Safety Guardrail (안전 가이드라인)

미니컷 플랜 설계 시, 사용자의 몸무게와 활동 수준에 따른 추정 유지 칼로리 대비 설정한 감량 목표 칼로리의 **결핍 격차(Calorie Deficit Intensity)**를 실시간 모니터링합니다.
*   **Safe**: 결핍률 25% 미만. 감량에 안전하며 근손실 저항이 보장되는 상태.
*   **Caution**: 결핍률 25%~35%. 높은 피로도가 예상되므로 더 잦은 수면/컨디션 체크인을 권고.
*   **High**: 결핍률 35% 이상 혹은 하루 결핍량이 900kcal를 초과하는 위험 지점. **"플랜 저장 완료"를 강제로 차단**하여 사용자의 생리적 안전을 도모합니다.

---

## ⚡ 3분 코칭 & AI 복기 엔진

1.  **오늘 수행 미션**: 식사 1회 기록, 코칭 체크인, 주간 복기를 완료할 수 있도록 동적인 체크리스트 제공.
2.  **이탈 예방 루틴**: 야식, 스트레스, 회식, 수면부족 등 식단 이탈 트리거를 체크하면 카탈로그 기반의 맞춤 행동 전략(예: '양치질 + 단백질 간식')이 즉시 화면에 가이드됩니다.
3.  **다이어트 브레이크 권장**: 누적 피로도가 임계치에 도달하거나 속도가 위험할 정도로 과속되면 자동으로 3~5일간 유지 칼로리로 상향 조정하는 다이어트 브레이크 가이드가 활성화됩니다.
4.  **리버스 다이어트 연동**: 미니컷이 끝나면 다음 벌크업 효율성을 높이고 요요 현상을 방지하도록 3주간 점진적으로 목표량을 올리는 리버스 다이어트 스케줄러가 노출됩니다.

---

## 🚀 실행 및 테스트 방법

### 1. 의존성 설치
```bash
npm install --legacy-peer-deps
```

### 2. 로컬 개발 서버 구동
```bash
npx expo start
```
*   `a`를 눌러 Android 시뮬레이터를 켜거나, `i`를 눌러 iOS 시뮬레이터를 켭니다.
*   실기기 테스트 시 카메라로 QR 코드를 스캔하여 **Expo Go** 앱에서 실행할 수 있습니다.

### 3. 도메인 로직 유닛 테스트 실행
```bash
npx tsc src/domain/rules.test.ts --target es2022 --module commonjs --moduleResolution node --esModuleInterop --ignoreConfig --ignoreDeprecations 6.0 && node src/domain/rules.test.js
```

---

## 🤖 CI/CD Workflow (`React Native CI`)

리포지토리에 커밋이 반영되거나 풀 리퀘스트(PR)가 발송될 때마다 GitHub Actions 가 구동되어 빌드 품질을 실시간 검증합니다.
*   **정적 컴파일 검사**: `npx tsc --noEmit`을 통해 타입 에러나 참조 오류가 없는지 검증합니다.
*   **테스트 검증**: 마이그레이션된 22개 유닛 테스트를 Node 환경에서 빌드 및 수행하여 도메인 논리가 온전히 보존되었는지 검사합니다.
