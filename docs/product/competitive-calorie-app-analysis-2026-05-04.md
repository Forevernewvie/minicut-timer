# MiniCut Competitive Calorie-App Analysis (2026-05-04)

Scope: Play Store + App Store Health & Fitness top-free charts were reviewed through rank 1-50 for South Korea first, with United States charts used as a signal for global calorie-app UX trends. The goal is not to clone any app; it is to extract category expectations and then make MiniCut visually and structurally distinct.

## Sources checked

- AppBrain, App Store Health & Fitness Top Free, South Korea — last updated 2026-05-02: https://www.appbrain.com/stats/appstore-rankings/top_free/health_and_fitness/kr
- AppBrain, Google Play Health & Fitness Top Free, South Korea — last updated 2026-05-03: https://www.appbrain.com/stats/google-play-rankings/top_free/health_fitness/kr
- AppBrain, App Store Health & Fitness Top Free, United States — last updated 2026-04-28: https://www.appbrain.com/stats/appstore-rankings/top_free/health_and_fitness/us
- AppBrain, Google Play Health & Fitness Top Free, United States — last updated 2026-05-04: https://www.appbrain.com/stats/google-play-rankings/top_free/health_fitness/us
- Official feature references: MyFitnessPal App Store, Lose It! App Store, Cronometer, YAZIO, Lifesum, fatsecret, MyNetDiary, Cal AI.

## South Korea top-50 audit highlights

### App Store KR Health & Fitness 1-50

Diet/calorie/food/body-management apps inside the top 50:
- #6 필라이즈 — diet AI coach, meal, blood glucose, workout.
- #7 인아웃 — diet coach, meal/exercise/weight logging.
- #35 SimFast — fasting timer tracker.
- #42 Calorie Counter by fatsecret — global calorie counter.
- #45 Mealligram — diet journal.
- #49 루션 — diet/meal/weight/exercise logging.

Other top-50 signals by category:
- Running/walking/reward apps dominate (#1 Nike Run Club, #2 Charlie, #4 WalkOn, #8 Moneywalk, #9 RunDay, #39 WalkPay, #40 돈이돼지, #50 SuperWalk).
- Body device/measurement platforms appear strongly (#5 InBody, #24 OKOK, #25 Garmin, #34 어떠케어).
- Clinic/healthcare booking and public-health utility apps are common (#3 UNNI, #12 복지로, #13 바비톡, #20 모두닥, #30 건강e음).

### Google Play KR Health & Fitness 1-50

Diet/calorie/food/body-management apps inside the top 50:
- #13 파스타(PASTA) — Kakao Healthcare, glucose/health management signal.
- #24 인아웃 — diet coach, meal/exercise/weight logging.
- #30 필라이즈 — diet AI coach, meal, blood glucose, workout.

Nearby calorie/diet signals just outside top 50:
- #55 닥터다이어리 — diet/meal/weight/blood-glucose.
- #83 루션 — diet/meal/weight/exercise.
- #85 Calorie Counter by fatsecret.

Other top-50 signals by category:
- Walking rewards and pedometers dominate (#3 WalkON, #5 Moneywalk, #6 Step Counter, #7 CashWalk, #8 야핏무브, #21 Step Counter, #23 돈이돼지, #25-26 pedometer variants, #42 Step Counter).
- Public health, insurance, and device utilities occupy the very top (#1 건강보험25시, #2 모바일 건강보험증, #12 InBody, #16 Health Connect).
- Fitness-plan and running apps remain visible (#10 Nike Run Club, #14 adidas Running, #20 RunDay, #29 Planfit).

## United States top-50 calorie-app signal

The U.S. top-50 charts show the calorie category moving toward AI/photo logging and fast capture:
- App Store US top-50 calorie/food signals: #5 Cal AI, #7 MyFitnessPal, #8 MenuFit, #12 Cronometer, #14 Olive Food Scanner, #21 BitePal, #23 MyNetDiary, #24 Calorie Counter & Food Tracker, #32 Lose It!, #40 SuppCo, #44 Oasis, #48 Fig, #50 Simple.
- Google Play US top-50 calorie/food signals: #15 MyFitnessPal, #19 BitePal, #25 Cronometer, #30 Simple, #49 MenuFit, #50 AI Calorie Counter - Appediet.

Official feature pages reinforce common expectations:
- MyFitnessPal: broad calorie/macro/workout tracking, AI logging, large food database, water tracker, wearable/fitness integrations.
- Lose It!: personalized calorie budget, photo/voice meal logging, barcode scanner, fasting, meal targets, weekend calorie schedule.
- Cronometer: verified nutrition data, barcode scanning, macro/micronutrient depth, health data view.
- YAZIO/Lifesum: barcode/AI/voice/photo logging, food ratings, meal plans, recipes, water/habit trackers.
- fatsecret/MyNetDiary: food diary, verified database, barcode scanning, macro/weight tracking, reminders, charts/community.
- Cal AI and similar: photo-first meal capture with dark, minimal, high-contrast onboarding.

## Category patterns to use without copying

Common category expectations:
1. Fast logging path: users expect one-tap entry, history, favorite meals, photo/barcode/voice in larger competitors.
2. Daily budget visibility: consumed, remaining, over-target, and macro/protein status must be glanceable.
3. Coaching/guardrails: recommendations are moving from raw logs toward next-action guidance.
4. Streak/rhythm feedback: top Korean apps especially emphasize walking/reward consistency; calorie apps emphasize streaks and progress.
5. Trust and safety: verified data, health context, and warning copy are differentiators.

MiniCut-specific differentiation:
1. MiniCut is not a general forever-diet tracker; it is a 2-6 week cut sprint with an explicit ending.
2. The app should make the end date and safety guardrail feel more important than infinite food database depth.
3. The new visual language should be a dark sprint cockpit: gauge, shield/guardrail, mission stack, and rhythm calendar.
4. Do not copy competitor surfaces such as MyFitnessPal diary tables, Lose It! orange budget rings, YAZIO food rating palette, or Cal AI photo-first onboarding. MiniCut should present a “short campaign control room.”

## Implementation brief applied to this repo

- Preserve core concept: 2-6 week MiniCut plan, 1000-1500 kcal guide, daily food kcal logging, plan/calendar/coaching check-in, local/offline-first behavior.
- Redesign system: dark obsidian base, luminous mint/blue/amber accents, glass cockpit panels, segmented gauge motifs, floating nav.
- Home: make it a “Today Sprint Cockpit” with calorie gauge, mission rail, current plan status, quick logs, and coach signal.
- Plan: make it a “Guardrail Planner” with target/duration chips, maintenance estimate, deficit risk gate, and explicit suitability confirmations.
- Calendar: make it a “Rhythm Map” rather than a plain log table; use cells/dots/badges for adherence and check-in rhythm.
- Icon: generated via GPT image, then used as launcher/store icon. Symbol: calorie sprint gauge + protective guardrail/shield, no text, no competitor logo/palette clone.
