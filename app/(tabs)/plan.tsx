import React, { useState, useEffect } from 'react';
import { View, Text, ScrollView, TouchableOpacity, TextInput, Alert } from 'react-native';
import { useMiniCutStore } from '../../src/store/useMiniCutStore';
import { MiniCutRules, toLocalDateString, addWeeks } from '../../src/domain/rules';
import { ActivityLevels, MiniCutGoalModes, MiniCutGoalModeType, ActivityLevelType } from '../../src/domain/models';
import { Settings, ShieldAlert, ShieldCheck, Trash2, Calendar, Flame, RefreshCw } from 'lucide-react-native';

export default function PlanScreen() {
  const { plan, savePlan, clearAllData } = useMiniCutStore();

  // Onboarding Form States
  const [startDate, setStartDate] = useState(toLocalDateString(new Date()));
  const [durationWeeks, setDurationWeeks] = useState(4);
  const [dailyTargetKcal, setDailyTargetKcal] = useState(1300);
  const [goalMode, setGoalMode] = useState<MiniCutGoalModeType>('MassReset');
  const [activityLevel, setActivityLevel] = useState<ActivityLevelType>('Moderate');
  const [weightKg, setWeightKg] = useState('');
  const [estimatedMaintenanceKcal, setEstimatedMaintenanceKcal] = useState(0);

  // Auto calculate maintenance & deficit guardrails when inputs change
  useEffect(() => {
    const wt = parseFloat(weightKg);
    if (!isNaN(wt) && wt > 0) {
      const maintenance = MiniCutRules.estimateMaintenanceCalories(wt, activityLevel) || 0;
      setEstimatedMaintenanceKcal(maintenance);
    } else {
      setEstimatedMaintenanceKcal(0);
    }
  }, [weightKg, activityLevel]);

  const guardrail = MiniCutRules.deficitGuardrail(dailyTargetKcal, estimatedMaintenanceKcal > 0 ? estimatedMaintenanceKcal : null);
  const reverseDiet = plan ? MiniCutRules.reverseDietPlan(plan.dailyTargetKcal, plan.goalMode) : null;

  const handleSavePlan = async () => {
    // Basic validations
    if (!startDate.match(/^\d{4}-\d{2}-\d{2}$/)) {
      Alert.alert('오류', '날짜 형식은 YYYY-MM-DD이어야 합니다.');
      return;
    }
    if (!MiniCutRules.isValidDuration(durationWeeks)) {
      Alert.alert('오류', '미니컷 기간은 2~6주만 설정 가능합니다.');
      return;
    }
    if (!MiniCutRules.isValidTarget(dailyTargetKcal)) {
      Alert.alert('오류', '하루 목표 칼로리는 1000~1500 kcal 범위 내여야 합니다.');
      return;
    }

    if (estimatedMaintenanceKcal > 0 && !guardrail.canSave) {
      Alert.alert('저장 불가', '현재 목표 칼로리가 유지 칼로리에 비해 과도하게 낮아 건강에 심각한 무리를 줍니다. 하루 목표 칼로리를 상향해주세요.');
      return;
    }

    try {
      await savePlan(
        startDate,
        durationWeeks,
        dailyTargetKcal,
        goalMode,
        activityLevel,
        estimatedMaintenanceKcal
      );
      Alert.alert('성공', '미니컷 플랜이 저장되었습니다.');
    } catch (err: any) {
      Alert.alert('오류', err.message || '저장하는 동안 문제가 발생했습니다.');
    }
  };

  const handleClearAllData = () => {
    Alert.alert(
      '데이터 초기화',
      '플랜을 포함한 모든 섭취량 기록, 체크인 피드백 데이터가 완전히 영구 삭제됩니다. 진행하시겠습니까?',
      [
        { text: '취소', style: 'cancel' },
        { text: '완전 삭제', style: 'destructive', onPress: () => clearAllData() }
      ]
    );
  };

  return (
    <ScrollView className="flex-1 bg-slate-950" contentContainerStyle={{ paddingBottom: 40 }}>
      {plan ? (
        // Active Plan Details View
        <View className="px-5 mt-6 space-y-6">
          <View className="bg-slate-900/60 border border-slate-800/80 rounded-3xl p-6 shadow-xl">
            <View className="flex-row items-center mb-4 pb-3 border-b border-slate-800">
              <View className="bg-emerald-500/10 p-2 rounded-xl mr-3">
                <Settings color="#10B981" size={20} />
              </View>
              <View>
                <Text className="text-slate-100 font-bold text-lg">활성화된 플랜 정보</Text>
                <Text className="text-slate-500 text-xs mt-0.5">미니컷 설정 요약</Text>
              </View>
            </View>

            <View className="space-y-4">
              <View className="flex-row justify-between">
                <Text className="text-slate-500 text-xs">기간 설정</Text>
                <Text className="text-slate-200 text-xs font-semibold">{plan.startDate} ~ {plan.endDate} ({plan.durationWeeks}주)</Text>
              </View>
              <View className="flex-row justify-between">
                <Text className="text-slate-500 text-xs">하루 목표 칼로리</Text>
                <Text className="text-slate-200 text-xs font-semibold">{plan.dailyTargetKcal} kcal</Text>
              </View>
              <View className="flex-row justify-between">
                <Text className="text-slate-500 text-xs">벌크업/목적</Text>
                <Text className="text-slate-200 text-xs font-semibold">{MiniCutGoalModes[plan.goalMode].displayName}</Text>
              </View>
              <View className="flex-row justify-between">
                <Text className="text-slate-500 text-xs">활동 수준</Text>
                <Text className="text-slate-200 text-xs font-semibold">{ActivityLevels[plan.activityLevel].displayName}</Text>
              </View>
              {plan.estimatedMaintenanceKcal > 0 && (
                <View className="flex-row justify-between">
                  <Text className="text-slate-500 text-xs">추정 유지 칼로리</Text>
                  <Text className="text-slate-200 text-xs font-semibold">{plan.estimatedMaintenanceKcal} kcal</Text>
                </View>
              )}
            </View>
          </View>

          {/* Reverse Diet Plan Guidelines */}
          {reverseDiet && (
            <View className="bg-slate-900/60 border border-slate-800/80 rounded-3xl p-6 shadow-xl">
              <View className="mb-3">
                <Text className="text-slate-200 font-bold text-base">{reverseDiet.title}</Text>
                <Text className="text-slate-500 text-xs mt-0.5">{reverseDiet.summary}</Text>
              </View>

              <View className="space-y-3 mt-3">
                {reverseDiet.steps.map((step: any, idx: number) => (
                  <View key={idx} className="bg-slate-800/30 border border-slate-700/10 p-3.5 rounded-2xl">
                    <View className="flex-row justify-between items-center mb-1">
                      <Text className="text-emerald-400 font-bold text-xs">{step.weekLabel}</Text>
                      <Text className="text-slate-300 font-bold text-xs">{step.targetCalories} kcal</Text>
                    </View>
                    <Text className="text-slate-400 text-xs leading-relaxed">{step.note}</Text>
                  </View>
                ))}
              </View>

              <View className="bg-slate-950 p-4 rounded-xl border border-slate-800 mt-4">
                <Text className="text-slate-500 text-[10px] font-bold text-rose-400 uppercase tracking-widest">주의사항</Text>
                <Text className="text-slate-400 text-xs leading-relaxed mt-1">{reverseDiet.caution}</Text>
              </View>
            </View>
          )}

          {/* Data Reset Button */}
          <TouchableOpacity
            onPress={handleClearAllData}
            className="w-full bg-rose-500/10 border border-rose-500/20 py-4 rounded-xl flex-row justify-center items-center active:bg-rose-500/20"
          >
            <View className="mr-2">
              <Trash2 color="#EF4444" size={18} />
            </View>
            <Text className="text-rose-400 font-bold text-sm">모든 데이터 초기화하기</Text>
          </TouchableOpacity>
        </View>
      ) : (
        // Plan Onboarding Creation Form
        <View className="px-5 mt-6 space-y-6">
          <View className="bg-slate-900/60 border border-slate-800/80 rounded-3xl p-6 shadow-xl">
            <Text className="text-slate-100 font-bold text-xl mb-1">새 미니컷 플랜 만들기</Text>
            <Text className="text-slate-500 text-xs mb-6">2~6주 최적의 감량 플랜을 설정해보세요.</Text>

            <View className="space-y-5">
              
              {/* Start Date input */}
              <View className="space-y-2">
                <Text className="text-slate-400 text-xs font-semibold">시작 날짜 (YYYY-MM-DD)</Text>
                <TextInput
                  value={startDate}
                  onChangeText={setStartDate}
                  placeholder="예: 2026-08-06"
                  placeholderTextColor="#475569"
                  className="w-full bg-slate-800 border border-slate-700/50 rounded-xl px-4 py-3 text-slate-100 font-medium"
                />
              </View>

              {/* Duration selection */}
              <View className="space-y-2">
                <Text className="text-slate-400 text-xs font-semibold">플랜 기간</Text>
                <View className="flex-row space-x-2">
                  {[2, 3, 4, 5, 6].map((weeks) => (
                    <TouchableOpacity
                      key={weeks}
                      onPress={() => setDurationWeeks(weeks)}
                      className={`flex-1 py-3 rounded-xl border items-center justify-center ${durationWeeks === weeks ? 'bg-emerald-500/10 border-emerald-500' : 'bg-slate-800 border-slate-700/50'}`}
                    >
                      <Text className={`text-sm font-bold ${durationWeeks === weeks ? 'text-emerald-400' : 'text-slate-400'}`}>{weeks}주</Text>
                    </TouchableOpacity>
                  ))}
                </View>
              </View>

              {/* Goal mode selection */}
              <View className="space-y-2">
                <Text className="text-slate-400 text-xs font-semibold">미니컷 주 목적</Text>
                <View className="flex-row space-x-3">
                  {(Object.keys(MiniCutGoalModes) as MiniCutGoalModeType[]).map((mode) => (
                    <TouchableOpacity
                      key={mode}
                      onPress={() => setGoalMode(mode)}
                      className={`flex-1 p-4 rounded-2xl border ${goalMode === mode ? 'bg-emerald-500/10 border-emerald-500' : 'bg-slate-800 border-slate-700/50'}`}
                    >
                      <Text className={`font-bold text-sm ${goalMode === mode ? 'text-emerald-400' : 'text-slate-200'}`}>
                        {MiniCutGoalModes[mode].displayName}
                      </Text>
                      <Text className="text-slate-500 text-[10px] mt-1">{MiniCutGoalModes[mode].shortDescription}</Text>
                    </TouchableOpacity>
                  ))}
                </View>
              </View>

              {/* Weight calculation helper for maintenance */}
              <View className="space-y-2">
                <Text className="text-slate-400 text-xs font-semibold">현재 체중 (kg) · 유지칼로리 계산용</Text>
                <TextInput
                  value={weightKg}
                  onChangeText={setWeightKg}
                  keyboardType="numeric"
                  placeholder="예: 78.5"
                  placeholderTextColor="#475569"
                  className="w-full bg-slate-800 border border-slate-700/50 rounded-xl px-4 py-3 text-slate-100 font-medium"
                />
              </View>

              {/* Activity level selection */}
              <View className="space-y-2">
                <Text className="text-slate-400 text-xs font-semibold">활동량 선택</Text>
                <View className="space-y-2">
                  {(Object.keys(ActivityLevels) as ActivityLevelType[]).map((level) => (
                    <TouchableOpacity
                      key={level}
                      onPress={() => setActivityLevel(level)}
                      className={`w-full p-4 rounded-2xl border flex-row justify-between items-center ${activityLevel === level ? 'bg-emerald-500/10 border-emerald-500' : 'bg-slate-800 border-slate-700/50'}`}
                    >
                      <Text className={`font-bold text-sm ${activityLevel === level ? 'text-emerald-400' : 'text-slate-200'}`}>
                        {ActivityLevels[level].displayName}
                      </Text>
                      <Text className="text-slate-500 text-xs">{ActivityLevels[level].kcalPerKgFactor} kcal/kg</Text>
                    </TouchableOpacity>
                  ))}
                </View>
              </View>

              {/* Target Calories Selection */}
              <View className="space-y-2">
                <Text className="text-slate-400 text-xs font-semibold">하루 목표 칼로리 설정 (kcal)</Text>
                <View className="flex-row flex-wrap gap-2">
                  {MiniCutRules.TARGET_OPTIONS_KCAL.map((kcal: number) => (
                    <TouchableOpacity
                      key={kcal}
                      onPress={() => setDailyTargetKcal(kcal)}
                      className={`px-4 py-3 rounded-xl border ${dailyTargetKcal === kcal ? 'bg-emerald-500/10 border-emerald-500' : 'bg-slate-800 border-slate-700/50'}`}
                    >
                      <Text className={`font-bold text-sm ${dailyTargetKcal === kcal ? 'text-emerald-400' : 'text-slate-300'}`}>{kcal}</Text>
                    </TouchableOpacity>
                  ))}
                </View>
                <Text className="text-slate-500 text-[10px] mt-1 leading-relaxed">
                  * 미니컷 권장 칼로리 범위는 1000kcal ~ 1500kcal입니다.
                </Text>
              </View>

            </View>
          </View>

          {/* Real-time Deficit Safety Guardrail */}
          {estimatedMaintenanceKcal > 0 && (
            <View className={`border rounded-3xl p-5 flex-row space-x-3.5 shadow-md ${guardrail.level === 'High' ? 'bg-rose-500/10 border-rose-500/30' : guardrail.level === 'Caution' ? 'bg-amber-500/10 border-amber-500/30' : 'bg-emerald-500/10 border-emerald-500/30'}`}>
              <View className="pt-0.5 mr-3">
                {guardrail.level === 'High' ? (
                  <ShieldAlert color="#F43F5E" size={24} />
                ) : guardrail.level === 'Caution' ? (
                  <ShieldAlert color="#F59E0B" size={24} />
                ) : (
                  <ShieldCheck color="#10B981" size={24} />
                )}
              </View>
              <View className="flex-1">
                <Text className={`font-bold text-base ${guardrail.level === 'High' ? 'text-rose-400' : guardrail.level === 'Caution' ? 'text-amber-400' : 'text-emerald-400'}`}>
                  {guardrail.title}
                </Text>
                <Text className="text-slate-400 text-xs leading-relaxed mt-1">
                  {guardrail.message}
                </Text>
              </View>
            </View>
          )}

          {/* Save Action Button */}
          <TouchableOpacity
            onPress={handleSavePlan}
            disabled={estimatedMaintenanceKcal > 0 && !guardrail.canSave}
            className={`w-full py-4 rounded-xl items-center shadow-lg ${estimatedMaintenanceKcal > 0 && !guardrail.canSave ? 'bg-slate-800 opacity-50' : 'bg-emerald-500 shadow-emerald-500/20 active:scale-95'}`}
          >
            <Text className="text-white font-bold text-base">플랜 완료하고 시작하기</Text>
          </TouchableOpacity>
        </View>
      )}
    </ScrollView>
  );
}
