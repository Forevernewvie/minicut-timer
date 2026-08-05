import React, { useState, useEffect } from 'react';
import { View, Text, ScrollView, TouchableOpacity, TextInput, Alert } from 'react-native';
import { useRouter } from 'expo-router';
import { useMiniCutStore } from '../src/store/useMiniCutStore';
import { RelapsePreventionCatalog } from '../src/domain/rules';
import { Check, Shield, AlertTriangle, Smile, Moon, Eye, Award } from 'lucide-react-native';

export default function CheckInScreen() {
  const router = useRouter();
  
  const {
    selectedDate,
    selectedConditionCheck,
    upsertConditionCheck
  } = useMiniCutStore();

  // Inputs
  const [weight, setWeight] = useState('');
  const [protein, setProtein] = useState('');
  const [sets, setSets] = useState('');
  const [lift, setLift] = useState('');
  const [sleep, setSleep] = useState('');
  const [trigger, setTrigger] = useState<string | null>(null);
  const [action, setAction] = useState<string | null>(null);
  
  const [fatigue, setFatigue] = useState<number | null>(null);
  const [hunger, setHunger] = useState<number | null>(null);
  const [mood, setMood] = useState<number | null>(null);
  const [performance, setPerformance] = useState<number | null>(null);

  // Populate form if existing check-in data exists for selected date
  useEffect(() => {
    if (selectedConditionCheck) {
      setWeight(selectedConditionCheck.bodyWeightKg?.toString() || '');
      setProtein(selectedConditionCheck.proteinGrams?.toString() || '');
      setSets(selectedConditionCheck.resistanceSets?.toString() || '');
      setLift(selectedConditionCheck.mainLiftKg?.toString() || '');
      setSleep(selectedConditionCheck.sleepHours?.toString() || '');
      setTrigger(selectedConditionCheck.relapseTrigger);
      setAction(selectedConditionCheck.copingAction);
      setFatigue(selectedConditionCheck.fatigueScore);
      setHunger(selectedConditionCheck.hungerScore);
      setMood(selectedConditionCheck.moodScore);
      setPerformance(selectedConditionCheck.workoutPerformanceScore);
    }
  }, [selectedConditionCheck]);

  const handleSaveCheckIn = async () => {
    const weightVal = weight ? parseFloat(weight) : null;
    const proteinVal = protein ? parseInt(protein) : null;
    const setsVal = sets ? parseInt(sets) : null;
    const liftVal = lift ? parseFloat(lift) : null;
    const sleepVal = sleep ? parseFloat(sleep) : null;

    if (weight.length > 0 && isNaN(weightVal || 0)) {
      Alert.alert('오류', '체중은 올바른 숫자여야 합니다.');
      return;
    }
    if (protein.length > 0 && isNaN(proteinVal || 0)) {
      Alert.alert('오류', '단백질 섭취량은 올바른 정수여야 합니다.');
      return;
    }
    if (sets.length > 0 && isNaN(setsVal || 0)) {
      Alert.alert('오류', '저항 세트 수는 올바른 정수여야 합니다.');
      return;
    }

    await upsertConditionCheck({
      bodyWeightKg: weightVal,
      proteinGrams: proteinVal,
      resistanceSets: setsVal,
      mainLiftKg: liftVal,
      sleepHours: sleepVal,
      relapseTrigger: trigger,
      copingAction: action,
      fatigueScore: fatigue,
      hungerScore: hunger,
      moodScore: mood,
      workoutPerformanceScore: performance
    });

    Alert.alert('완료', '오늘의 체크인이 저장되었습니다.', [
      { text: '확인', onPress: () => router.back() }
    ]);
  };

  const handleSelectTrigger = (selTrigger: string) => {
    if (trigger === selTrigger) {
      setTrigger(null);
      setAction(null);
    } else {
      setTrigger(selTrigger);
      // Auto recommend coping action
      const recAction = RelapsePreventionCatalog.recommendedActionFor(selTrigger);
      setAction(recAction);
    }
  };

  const renderScoreSelector = (
    label: string,
    currentValue: number | null,
    setValue: (val: number) => void
  ) => {
    return (
      <View className="space-y-2">
        <Text className="text-slate-400 text-xs font-semibold">{label}</Text>
        <View className="flex-row space-x-2">
          {[1, 2, 3, 4, 5].map((val) => (
            <TouchableOpacity
              key={val}
              onPress={() => setValue(val)}
              className={`flex-1 py-2.5 rounded-xl border items-center justify-center ${currentValue === val ? 'bg-indigo-500/10 border-indigo-500' : 'bg-slate-800 border-slate-700/50'}`}
            >
              <Text className={`text-sm font-bold ${currentValue === val ? 'text-indigo-400' : 'text-slate-400'}`}>{val}</Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>
    );
  };

  return (
    <ScrollView className="flex-1 bg-slate-950" contentContainerStyle={{ paddingBottom: 40, paddingHorizontal: 20 }}>
      
      {/* Target date marker */}
      <View className="bg-slate-900 border border-slate-800 rounded-2xl p-4 my-6 flex-row justify-between items-center">
        <View>
          <Text className="text-slate-400 text-xs">체크인 대상 날짜</Text>
          <Text className="text-slate-100 font-bold text-base mt-0.5">{selectedDate}</Text>
        </View>
        <View className="bg-indigo-500/10 px-3 py-1.5 rounded-xl border border-indigo-500/20">
          <Text className="text-indigo-400 font-semibold text-xs">기록 동기화 중</Text>
        </View>
      </View>

      <View className="space-y-6">
        
        {/* Core Bio-markers Block */}
        <View className="bg-slate-900/60 border border-slate-800 rounded-3xl p-5 space-y-4">
          <Text className="text-slate-200 font-bold text-base mb-2">신체 바이오마커 기록</Text>
          
          <View className="space-y-2">
            <Text className="text-slate-400 text-xs font-semibold">아침 공복 체중 (kg)</Text>
            <TextInput
              value={weight}
              onChangeText={setWeight}
              keyboardType="numeric"
              placeholder="예: 76.5"
              placeholderTextColor="#475569"
              className="w-full bg-slate-800 border border-slate-700/50 rounded-xl px-4 py-3 text-slate-100 font-medium"
            />
          </View>

          <View className="space-y-2">
            <Text className="text-slate-400 text-xs font-semibold">단백질 섭취량 (g)</Text>
            <TextInput
              value={protein}
              onChangeText={setProtein}
              keyboardType="numeric"
              placeholder="예: 150"
              placeholderTextColor="#475569"
              className="w-full bg-slate-800 border border-slate-700/50 rounded-xl px-4 py-3 text-slate-100 font-medium"
            />
          </View>

          <View className="space-y-2">
            <Text className="text-slate-400 text-xs font-semibold">저항 운동 세트 수 (주요 부위 총합)</Text>
            <TextInput
              value={sets}
              onChangeText={setSets}
              keyboardType="numeric"
              placeholder="예: 12"
              placeholderTextColor="#475569"
              className="w-full bg-slate-800 border border-slate-700/50 rounded-xl px-4 py-3 text-slate-100 font-medium"
            />
          </View>

          <View className="space-y-2">
            <Text className="text-slate-400 text-xs font-semibold">핵심 리프트 중량 (kg · 3대 운동/주요 종목)</Text>
            <TextInput
              value={lift}
              onChangeText={setLift}
              keyboardType="numeric"
              placeholder="예: 100"
              placeholderTextColor="#475569"
              className="w-full bg-slate-800 border border-slate-700/50 rounded-xl px-4 py-3 text-slate-100 font-medium"
            />
          </View>
        </View>

        {/* Habits & Scores Block */}
        <View className="bg-slate-900/60 border border-slate-800 rounded-3xl p-5 space-y-4">
          <Text className="text-slate-200 font-bold text-base mb-2">컨디션 & 회복 지표 (1~5)</Text>

          <View className="space-y-2">
            <Text className="text-slate-400 text-xs font-semibold">수면 시간 (시간)</Text>
            <TextInput
              value={sleep}
              onChangeText={setSleep}
              keyboardType="numeric"
              placeholder="예: 7.5"
              placeholderTextColor="#475569"
              className="w-full bg-slate-800 border border-slate-700/50 rounded-xl px-4 py-3 text-slate-100 font-medium"
            />
          </View>

          {renderScoreSelector('피로 강도 (낮을수록 좋음)', fatigue, setFatigue)}
          {renderScoreSelector('공복감/식욕 제어 (낮을수록 좋음)', hunger, setHunger)}
          {renderScoreSelector('스트레스/기분 수준 (높을수록 좋음)', mood, setMood)}
          {renderScoreSelector('운동 수행력/집중력 (높을수록 좋음)', performance, setPerformance)}
        </View>

        {/* Relapse Prevention Block */}
        <View className="bg-slate-900/60 border border-slate-800 rounded-3xl p-5 space-y-4">
          <Text className="text-slate-200 font-bold text-base">식단 이탈 트리거 & 예방 행동</Text>
          <Text className="text-slate-500 text-xs">오늘 충동이나 이탈 위기를 겪으셨다면 트리거를 입력해보세요.</Text>

          <View className="flex-row flex-wrap gap-2.5">
            {RelapsePreventionCatalog.triggerOptions.map((trigOption) => {
              const isSelected = trigger === trigOption;
              return (
                <TouchableOpacity
                  key={trigOption}
                  onPress={() => handleSelectTrigger(trigOption)}
                  className={`px-3 py-2 rounded-xl border flex-row items-center ${isSelected ? 'bg-rose-500/10 border-rose-500' : 'bg-slate-800 border-slate-700/50'}`}
                >
                  <Text className={`text-xs font-semibold mr-1.5 ${isSelected ? 'text-rose-400' : 'text-slate-300'}`}>{trigOption}</Text>
                  {isSelected && <Check color="#F43F5E" size={14} />}
                </TouchableOpacity>
              );
            })}
          </View>

          {/* AI Coping Advice Tip */}
          {trigger && action && (
            <View className="bg-indigo-500/5 border border-indigo-500/20 p-4 rounded-2xl space-y-2 mt-2">
              <View className="flex-row items-center space-x-2">
                <Shield color="#6366F1" size={18} />
                <Text className="text-indigo-400 font-bold text-xs ml-1.5">AI 추천 행동 대응 지침</Text>
              </View>
              <Text className="text-slate-200 text-xs leading-relaxed">{action}</Text>
            </View>
          )}

          {/* Custom Coping Note */}
          {trigger && (
            <View className="space-y-2 mt-2">
              <Text className="text-slate-400 text-xs font-semibold">커스텀 실행 행동 기록 (선택사항)</Text>
              <TextInput
                value={action || ''}
                onChangeText={setAction}
                placeholder="예: 물 한 잔 마시고 바로 양치를 했습니다."
                placeholderTextColor="#475569"
                className="w-full bg-slate-800 border border-slate-700/50 rounded-xl px-4 py-3 text-slate-100 font-medium"
              />
            </View>
          )}
        </View>

        {/* Action Button */}
        <TouchableOpacity
          onPress={handleSaveCheckIn}
          className="w-full bg-indigo-500 py-4 rounded-xl items-center shadow-lg shadow-indigo-500/20 active:scale-95 mt-4"
        >
          <Text className="text-white font-bold text-base">체크인 제출하기</Text>
        </TouchableOpacity>
      </View>
    </ScrollView>
  );
}
