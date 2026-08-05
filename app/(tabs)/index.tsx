import React, { useState } from 'react';
import { View, Text, ScrollView, TouchableOpacity, Modal, TextInput, Pressable, Alert } from 'react-native';
import { useRouter } from 'expo-router';
import { useMiniCutStore } from '../../src/store/useMiniCutStore';
import { MiniCutRules, toLocalDateString } from '../../src/domain/rules';
import { Plus, CheckCircle, Circle, Flame, Star, Trash2, Heart, Award, ArrowRight } from 'lucide-react-native';

export default function DashboardScreen() {
  const router = useRouter();
  
  // Zustand State
  const {
    plan,
    selectedEntries,
    selectedTotalCalories,
    recentPresets,
    favoritePresets,
    weeklyCoaching,
    todayMissions,
    addEntry,
    addEntryFromPreset,
    deleteEntry,
    toggleEntryFavorite
  } = useMiniCutStore();

  const todayStr = toLocalDateString(new Date());

  // Local UI State
  const [modalVisible, setModalVisible] = useState(false);
  const [foodName, setFoodName] = useState('');
  const [calories, setCalories] = useState('');
  const [note, setNote] = useState('');
  const [timeLabel, setTimeLabel] = useState('아침');

  const targetKcal = plan ? plan.dailyTargetKcal : MiniCutRules.DEFAULT_TARGET_KCAL;
  
  // Progress calculations
  const remaining = MiniCutRules.remainingCalories(targetKcal, selectedTotalCalories);
  const over = MiniCutRules.overCalories(targetKcal, selectedTotalCalories);
  const progress = targetKcal > 0 ? Math.min(1, selectedTotalCalories / targetKcal) : 0;
  
  // Snapshot calculations
  const progressSnapshot = plan ? MiniCutRules.planProgressSnapshot(plan, todayStr) : null;

  const handleAddFood = async () => {
    if (!foodName.trim()) {
      Alert.alert('오류', '음식 이름을 입력해주세요.');
      return;
    }
    const calVal = parseInt(calories);
    if (isNaN(calVal) || calVal <= 0) {
      Alert.alert('오류', '올바른 칼로리 값을 입력해주세요.');
      return;
    }

    await addEntry(calVal, foodName, note, timeLabel);
    
    // Reset Form
    setFoodName('');
    setCalories('');
    setNote('');
    setTimeLabel('아침');
    setModalVisible(false);
  };

  const handlePresetPress = async (preset: any) => {
    await addEntryFromPreset(preset);
  };

  if (!plan) {
    return (
      <View className="flex-1 bg-slate-950 justify-center items-center px-6">
        <View className="bg-slate-900/60 border border-slate-800 rounded-3xl p-8 items-center w-full max-w-md shadow-2xl backdrop-blur-md">
          <View className="w-16 h-16 bg-emerald-500/10 rounded-2xl items-center justify-center mb-6 border border-emerald-500/20">
            <Flame color="#10B981" size={32} />
          </View>
          <Text className="text-2xl font-bold text-slate-100 mb-2 text-center">미니컷 플랜이 없습니다</Text>
          <Text className="text-slate-400 text-center mb-8 leading-relaxed">
            체계적이고 효율적인 2~6주 감량 목표를 수립하고, 매일 섭취와 신체 신호를 확인해보세요.
          </Text>
          <TouchableOpacity
            onPress={() => router.push('/plan')}
            className="w-full bg-emerald-500 hover:bg-emerald-600 active:scale-95 py-4 rounded-xl flex-row justify-center items-center shadow-lg shadow-emerald-500/20"
          >
            <Text className="text-white font-semibold text-lg mr-2">첫 미니컷 설정하기</Text>
            <ArrowRight color="#ffffff" size={20} />
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  return (
    <ScrollView className="flex-1 bg-slate-950" contentContainerStyle={{ paddingBottom: 32 }}>
      {/* Plan Header Progress Snapshot */}
      {progressSnapshot && (
        <View className="bg-slate-900 border-b border-slate-800/80 px-6 pt-14 pb-5 flex-row justify-between items-center">
          <View className="flex-1 mr-4">
            <Text className="text-xs font-semibold text-emerald-400 uppercase tracking-widest">{progressSnapshot.headline}</Text>
            <Text className="text-lg font-bold text-slate-100 mt-1">{progressSnapshot.supportingText}</Text>
          </View>
          <View className="bg-emerald-500/10 border border-emerald-500/20 px-4 py-2 rounded-xl items-center">
            <Text className="text-xl font-black text-emerald-400">{progressSnapshot.dDayLabel}</Text>
          </View>
        </View>
      )}

      <View className="px-5 mt-6 space-y-6">
        
        {/* Calorie Tracker Widget (Circular Progress Feel) */}
        <View className="bg-slate-900/60 border border-slate-800/80 rounded-3xl p-6 shadow-xl relative overflow-hidden">
          <View className="flex-row justify-between items-center mb-4">
            <Text className="text-slate-400 text-sm font-semibold">오늘 칼로리 현황</Text>
            <View className="bg-slate-800/80 px-3 py-1 rounded-full border border-slate-700/50">
              <Text className="text-slate-300 text-xs font-medium">목표 {targetKcal} kcal</Text>
            </View>
          </View>

          <View className="flex-row items-center justify-between">
            <View className="flex-1">
              <Text className="text-5xl font-black text-slate-100 tracking-tight">{selectedTotalCalories} <Text className="text-lg font-normal text-slate-400">kcal</Text></Text>
              
              {over > 0 ? (
                <Text className="text-rose-400 text-sm font-medium mt-1">목표 대비 {over} kcal 초과</Text>
              ) : (
                <Text className="text-slate-400 text-sm font-medium mt-1">남은 칼로리: {remaining} kcal</Text>
              )}
            </View>

            {/* Glowing progress line visual indicator */}
            <View className="w-20 h-20 rounded-full border-4 border-slate-800 items-center justify-center relative">
              <View 
                className={`absolute inset-0 rounded-full border-4 ${over > 0 ? 'border-rose-500/30' : 'border-emerald-500/30'}`}
              />
              <Flame color={over > 0 ? '#F43F5E' : '#10B981'} size={28} />
            </View>
          </View>

          {/* Linear Progress Bar */}
          <View className="w-full bg-slate-800/60 h-3 rounded-full mt-5 overflow-hidden">
            <View 
              className={`h-full rounded-full ${over > 0 ? 'bg-rose-500' : 'bg-emerald-500'}`}
              style={{ width: `${progress * 100}%` }}
            />
          </View>
        </View>

        {/* Coach Coaching Snapshot Banner */}
        {weeklyCoaching && (
          <View className="bg-slate-900/40 border border-slate-800/80 rounded-2xl p-5 mt-4">
            <View className="flex-row items-center justify-between mb-2">
              <View className="flex-row items-center">
                <View className="bg-indigo-500/10 p-1.5 rounded-lg mr-2.5">
                  <Award color="#6366F1" size={16} />
                </View>
                <Text className="text-indigo-400 font-bold text-sm">AI 주간 코칭 가이드</Text>
              </View>
              <View className="bg-indigo-500/10 px-2 py-0.5 rounded border border-indigo-500/20">
                <Text className="text-indigo-300 text-xs font-medium">{weeklyCoaching.momentumLabel}</Text>
              </View>
            </View>
            <Text className="text-slate-100 font-bold text-base leading-snug">{weeklyCoaching.nextAction}</Text>
            <Text className="text-slate-400 text-xs mt-2">{weeklyCoaching.summary} · {weeklyCoaching.momentumMessage}</Text>
          </View>
        )}

        {/* Today's Missions */}
        <View className="bg-slate-900/60 border border-slate-800/80 rounded-3xl p-6 mt-4 shadow-xl">
          <Text className="text-slate-200 font-bold text-lg mb-4">오늘 수행 과제</Text>
          <View className="space-y-3.5">
            {todayMissions.map((mission: any, index: number) => (
              <TouchableOpacity
                key={index}
                onPress={() => {
                  if (mission.type === 'FoodLog') {
                    setModalVisible(true);
                  } else if (mission.type === 'CoachCheckIn') {
                    router.push('/checkin');
                  } else {
                    router.push('/calendar');
                  }
                }}
                className="flex-row items-center bg-slate-800/40 border border-slate-700/20 p-4 rounded-2xl active:bg-slate-800/60"
              >
                <View className="mr-3">
                  {mission.isComplete ? (
                    <CheckCircle color="#10B981" size={22} />
                  ) : (
                    <Circle color="#64748B" size={22} />
                  )}
                </View>
                <View className="flex-1 mr-2">
                  <Text className={`font-bold text-sm ${mission.isComplete ? 'text-slate-400 line-through' : 'text-slate-200'}`}>
                    {mission.title}
                  </Text>
                  <Text className="text-slate-500 text-xs mt-0.5">{mission.description}</Text>
                </View>
                <Text className="text-xs font-semibold text-emerald-400">{mission.actionLabel}</Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        {/* Quick Presets */}
        {(favoritePresets.length > 0 || recentPresets.length > 0) && (
          <View className="mt-4">
            <Text className="text-slate-400 font-bold text-sm mb-3">빠른 간편 기록</Text>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} className="flex-row space-x-3">
              {favoritePresets.map((preset: any, idx: number) => (
                <TouchableOpacity
                  key={`fav-${idx}`}
                  onPress={() => handlePresetPress(preset)}
                  className="bg-slate-900/60 border border-slate-800/80 px-4 py-3 rounded-2xl flex-row items-center mr-3 active:scale-95"
                >
                  <View className="mr-1.5">
                    <Star color="#10B981" size={14} />
                  </View>
                  <View>
                    <Text className="text-slate-200 font-bold text-xs">{preset.foodName}</Text>
                    <Text className="text-slate-500 text-[10px]">{preset.calories}kcal · {preset.timeLabel}</Text>
                  </View>
                </TouchableOpacity>
              ))}
              {recentPresets.map((preset: any, idx: number) => (
                <TouchableOpacity
                  key={`rec-${idx}`}
                  onPress={() => handlePresetPress(preset)}
                  className="bg-slate-900/60 border border-slate-800/80 px-4 py-3 rounded-2xl flex-row items-center mr-3 active:scale-95"
                >
                  <View className="mr-1.5">
                    <Plus color="#94A3B8" size={14} />
                  </View>
                  <View>
                    <Text className="text-slate-300 font-semibold text-xs">{preset.foodName}</Text>
                    <Text className="text-slate-500 text-[10px]">{preset.calories}kcal · {preset.timeLabel}</Text>
                  </View>
                </TouchableOpacity>
              ))}
            </ScrollView>
          </View>
        )}

        {/* Logged Meals List */}
        <View className="bg-slate-900/60 border border-slate-800/80 rounded-3xl p-6 mt-4 shadow-xl">
          <View className="flex-row justify-between items-center mb-4">
            <Text className="text-slate-200 font-bold text-lg">오늘 먹은 음식</Text>
            <TouchableOpacity
              onPress={() => setModalVisible(true)}
              className="bg-emerald-500/10 border border-emerald-500/20 px-3.5 py-1.5 rounded-xl flex-row items-center active:scale-95"
            >
              <View className="mr-1">
                <Plus color="#10B981" size={16} />
              </View>
              <Text className="text-emerald-400 font-bold text-xs">식사 추가</Text>
            </TouchableOpacity>
          </View>

          {selectedEntries.length === 0 ? (
            <View className="items-center py-8">
              <Text className="text-slate-500 text-sm">오늘 기록된 식사가 없습니다.</Text>
              <Text className="text-slate-600 text-xs mt-1">상단의 식사 추가를 통해 첫 음식을 남겨보세요.</Text>
            </View>
          ) : (
            <View className="space-y-3">
              {selectedEntries.map((entry: any) => (
                <View
                  key={entry.id}
                  className="flex-row items-center bg-slate-800/20 border border-slate-700/10 p-4 rounded-2xl justify-between"
                >
                  <View className="flex-row items-center flex-1 mr-4">
                    <TouchableOpacity
                      onPress={() => toggleEntryFavorite(entry.id, !entry.isFavorite)}
                      className="p-1 mr-2"
                    >
                      <Heart color={entry.isFavorite ? '#F43F5E' : '#64748B'} size={18} fill={entry.isFavorite ? '#F43F5E' : 'transparent'} />
                    </TouchableOpacity>
                    <View className="flex-1">
                      <View className="flex-row items-center">
                        <Text className="text-slate-200 font-bold text-sm">{entry.foodName}</Text>
                        <View className="bg-slate-800 px-1.5 py-0.5 rounded ml-2">
                          <Text className="text-slate-400 text-[10px]">{entry.timeLabel}</Text>
                        </View>
                      </View>
                      {entry.note.length > 0 && (
                        <Text className="text-slate-500 text-xs mt-0.5">{entry.note}</Text>
                      )}
                    </View>
                  </View>
                  <View className="flex-row items-center">
                    <Text className="text-slate-200 font-bold text-sm mr-4">{entry.calories} kcal</Text>
                    <TouchableOpacity
                      onPress={() => {
                        Alert.alert('삭제 확인', '이 음식을 식단 기록에서 삭제하시겠습니까?', [
                          { text: '취소', style: 'cancel' },
                          { text: '삭제', style: 'destructive', onPress: () => deleteEntry(entry.id) }
                        ]);
                      }}
                      className="p-1"
                    >
                      <Trash2 color="#EF4444" size={16} />
                    </TouchableOpacity>
                  </View>
                </View>
              ))}
            </View>
          )}
        </View>
      </View>

      {/* Add Food Modal */}
      <Modal
        animationType="slide"
        transparent={true}
        visible={modalVisible}
        onRequestClose={() => setModalVisible(false)}
      >
        <View className="flex-1 bg-slate-950/80 justify-end">
          <View className="bg-slate-900 border-t border-slate-800 rounded-t-3xl p-6 space-y-5">
            <View className="flex-row justify-between items-center pb-2">
              <Text className="text-slate-100 font-bold text-xl">음식 기록 추가</Text>
              <Pressable onPress={() => setModalVisible(false)}>
                <Text className="text-slate-400 font-semibold">닫기</Text>
              </Pressable>
            </View>

            <View className="space-y-2">
              <Text className="text-slate-400 text-xs font-semibold">음식 이름</Text>
              <TextInput
                value={foodName}
                onChangeText={setFoodName}
                placeholder="예: 닭가슴살 볶음밥"
                placeholderTextColor="#475569"
                className="w-full bg-slate-800 border border-slate-700/50 rounded-xl px-4 py-3 text-slate-100 font-medium"
              />
            </View>

            <View className="space-y-2">
              <Text className="text-slate-400 text-xs font-semibold">섭취 칼로리 (kcal)</Text>
              <TextInput
                value={calories}
                onChangeText={setCalories}
                keyboardType="numeric"
                placeholder="예: 350"
                placeholderTextColor="#475569"
                className="w-full bg-slate-800 border border-slate-700/50 rounded-xl px-4 py-3 text-slate-100 font-medium"
              />
            </View>

            <View className="space-y-2">
              <Text className="text-slate-400 text-xs font-semibold">시간대 구분</Text>
              <View className="flex-row space-x-2">
                {['아침', '점심', '저녁', '간식'].map((label) => (
                  <TouchableOpacity
                    key={label}
                    onPress={() => setTimeLabel(label)}
                    className={`flex-1 py-2.5 rounded-xl border items-center justify-center ${timeLabel === label ? 'bg-emerald-500/10 border-emerald-500 text-emerald-400' : 'bg-slate-800 border-slate-700/50'}`}
                  >
                    <Text className={`text-xs font-bold ${timeLabel === label ? 'text-emerald-400' : 'text-slate-400'}`}>{label}</Text>
                  </TouchableOpacity>
                ))}
              </View>
            </View>

            <View className="space-y-2">
              <Text className="text-slate-400 text-xs font-semibold">메모 (선택사항)</Text>
              <TextInput
                value={note}
                onChangeText={setNote}
                placeholder="예: 야채 추가, 쌈 싸서 섭취"
                placeholderTextColor="#475569"
                className="w-full bg-slate-800 border border-slate-700/50 rounded-xl px-4 py-3 text-slate-100 font-medium"
              />
            </View>

            <TouchableOpacity
              onPress={handleAddFood}
              className="w-full bg-emerald-500 py-4 rounded-xl items-center shadow-lg shadow-emerald-500/20 active:scale-95"
            >
              <Text className="text-white font-bold text-base">기록 저장하기</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    </ScrollView>
  );
}
