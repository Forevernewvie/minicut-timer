import React, { useState, useEffect } from 'react';
import { View, Text, ScrollView, TouchableOpacity, Alert } from 'react-native';
import { useMiniCutStore } from '../../src/store/useMiniCutStore';
import { MiniCutRules, toLocalDateString, addDays } from '../../src/domain/rules';
import { ChevronLeft, ChevronRight, Activity, Calendar as CalendarIcon, Heart, User, Check, Plus } from 'lucide-react-native';

export default function CalendarScreen() {
  const {
    plan,
    selectedDate,
    selectedEntries,
    selectedTotalCalories,
    selectedConditionCheck,
    calendarSummaries,
    calendarChecks,
    calendarRhythm,
    setSelectedDate,
  } = useMiniCutStore();

  const [currentMonth, setCurrentMonth] = useState(new Date());

  // Trigger data reload for the selected month window
  useEffect(() => {
    // When month changes, update selected date to first day of that month
    const year = currentMonth.getFullYear();
    const month = String(currentMonth.getMonth() + 1).padStart(2, '0');
    const firstDay = `${year}-${month}-01`;
    setSelectedDate(firstDay);
  }, [currentMonth]);

  const targetKcal = plan ? plan.dailyTargetKcal : MiniCutRules.DEFAULT_TARGET_KCAL;

  // Calendar logic helpers
  const getDaysInMonth = (date: Date) => {
    const year = date.getFullYear();
    const month = date.getMonth();
    return new Date(year, month + 1, 0).getDate();
  };

  const getFirstDayOfMonth = (date: Date) => {
    const year = date.getFullYear();
    const month = date.getMonth();
    return new Date(year, month, 1).getDay(); // 0 is Sunday, 6 is Saturday
  };

  const handlePrevMonth = () => {
    setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1, 1));
  };

  const handleNextMonth = () => {
    setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1, 1));
  };

  const daysInMonth = getDaysInMonth(currentMonth);
  const firstDayIndex = getFirstDayOfMonth(currentMonth);

  // Generate grid array
  const calendarCells = [];
  // Empty slots for padding
  for (let i = 0; i < firstDayIndex; i++) {
    calendarCells.push(null);
  }
  // Days of the month
  for (let day = 1; day <= daysInMonth; day++) {
    const year = currentMonth.getFullYear();
    const month = String(currentMonth.getMonth() + 1).padStart(2, '0');
    const dayStr = String(day).padStart(2, '0');
    calendarCells.push(`${year}-${month}-${dayStr}`);
  }

  // Get style details for a date
  const getDateStatus = (dateStr: string) => {
    const summary = calendarSummaries.find(s => s.date === dateStr);
    const check = calendarChecks.find((c: any) => c.date === dateStr);
    const hasCheck = check !== undefined;

    if (!summary || summary.totalCalories <= 0) {
      return { status: 'Empty', hasCheck };
    }

    const status = MiniCutRules.calendarRhythmStatus(summary.totalCalories, targetKcal);
    return { status, hasCheck };
  };

  return (
    <ScrollView className="flex-1 bg-slate-950" contentContainerStyle={{ paddingBottom: 32 }}>
      
      {/* Calendar Controller Header */}
      <View className="flex-row justify-between items-center px-6 pt-6 pb-4 bg-slate-900 border-b border-slate-800">
        <Text className="text-xl font-bold text-slate-100">
          {currentMonth.getFullYear()}년 {currentMonth.getMonth() + 1}월
        </Text>
        <View className="flex-row space-x-2">
          <TouchableOpacity onPress={handlePrevMonth} className="p-2 bg-slate-800 rounded-lg mr-2 active:bg-slate-700">
            <ChevronLeft color="#F8FAFC" size={20} />
          </TouchableOpacity>
          <TouchableOpacity onPress={handleNextMonth} className="p-2 bg-slate-800 rounded-lg active:bg-slate-700">
            <ChevronRight color="#F8FAFC" size={20} />
          </TouchableOpacity>
        </View>
      </View>

      <View className="px-5 mt-6">
        
        {/* Calendar Grid Card */}
        <View className="bg-slate-900/60 border border-slate-800/80 rounded-3xl p-5 shadow-xl">
          {/* Weekday Names */}
          <View className="flex-row mb-3">
            {['일', '월', '화', '수', '목', '금', '토'].map((d, index) => (
              <Text key={d} className={`flex-1 text-center text-xs font-bold ${index === 0 ? 'text-rose-500' : index === 6 ? 'text-blue-400' : 'text-slate-500'}`}>
                {d}
              </Text>
            ))}
          </View>

          {/* Calendar Cells Grid */}
          <View className="flex-row flex-wrap">
            {calendarCells.map((dateStr, index) => {
              if (dateStr === null) {
                return <View key={`empty-${index}`} className="w-[14.28%] aspect-square p-1" />;
              }

              const isSelected = selectedDate === dateStr;
              const { status, hasCheck } = getDateStatus(dateStr);
              const dayNum = dateStr.split('-')[2];

              // Styling configurations
              let cellBgClass = 'bg-slate-900/30 border-slate-800/40';
              let textClass = 'text-slate-400';
              
              if (status === 'WithinTarget') {
                cellBgClass = 'bg-emerald-950/40 border-emerald-500/30';
                textClass = 'text-emerald-400 font-bold';
              } else if (status === 'OverTarget') {
                cellBgClass = 'bg-rose-950/40 border-rose-500/30';
                textClass = 'text-rose-400 font-bold';
              }

              if (isSelected) {
                cellBgClass = `${status === 'WithinTarget' ? 'bg-emerald-900/60' : status === 'OverTarget' ? 'bg-rose-900/60' : 'bg-slate-800'} border-emerald-400 border-2`;
              }

              return (
                <TouchableOpacity
                  key={dateStr}
                  onPress={() => setSelectedDate(dateStr)}
                  className={`w-[14.28%] aspect-square p-1`}
                >
                  <View className={`w-full h-full rounded-xl border justify-center items-center relative ${cellBgClass}`}>
                    <Text className={`text-xs ${textClass}`}>{parseInt(dayNum)}</Text>
                    {/* Small dot indicating condition check exists */}
                    {hasCheck && (
                      <View className="absolute bottom-1.5 w-1 h-1 bg-indigo-400 rounded-full" />
                    )}
                  </View>
                </TouchableOpacity>
              );
            })}
          </View>
        </View>

        {/* Monthly Summary Indicators */}
        {calendarRhythm && (
          <View className="bg-slate-900/40 border border-slate-800/80 rounded-2xl p-5 mt-6">
            <Text className="text-slate-400 text-xs font-semibold mb-3">월간 캘린더 요약</Text>
            <View className="flex-row justify-between mb-4">
              <View className="items-center flex-1">
                <Text className="text-slate-500 text-xs">식사 기록</Text>
                <Text className="text-slate-200 font-black text-lg mt-1">{calendarRhythm.loggedDays}일</Text>
              </View>
              <View className="items-center flex-1 border-x border-slate-800/80">
                <Text className="text-emerald-400 text-xs font-semibold">목표 준수</Text>
                <Text className="text-emerald-400 font-black text-lg mt-1">{calendarRhythm.withinTargetDays}일</Text>
              </View>
              <View className="items-center flex-1">
                <Text className="text-indigo-400 text-xs font-semibold">코칭 체크인</Text>
                <Text className="text-indigo-400 font-black text-lg mt-1">{calendarRhythm.checkInDays}일</Text>
              </View>
            </View>
            <Text className="text-slate-300 text-xs font-medium leading-relaxed bg-slate-900/60 p-3.5 rounded-xl border border-slate-800/50">
              {calendarRhythm.message}
            </Text>
          </View>
        )}

        {/* Selected Date Details Panel */}
        <View className="bg-slate-900/60 border border-slate-800/80 rounded-3xl p-6 mt-6 shadow-xl">
          <View className="flex-row justify-between items-center mb-4 pb-2 border-b border-slate-800">
            <View>
              <Text className="text-slate-400 text-xs font-bold">선택한 날짜 기록</Text>
              <Text className="text-slate-100 font-black text-base mt-0.5">{selectedDate}</Text>
            </View>
            <View className="bg-slate-800 px-3 py-1.5 rounded-full border border-slate-700/40">
              <Text className="text-slate-300 font-semibold text-xs">{selectedTotalCalories} kcal</Text>
            </View>
          </View>

          {/* Meals on Selected Date */}
          <Text className="text-slate-400 font-bold text-xs mb-3">섭취 칼로리 목록</Text>
          {selectedEntries.length === 0 ? (
            <View className="bg-slate-800/20 border border-slate-700/10 p-4 rounded-2xl items-center py-6 mb-5">
              <Text className="text-slate-500 text-xs">식사 기록이 없습니다.</Text>
            </View>
          ) : (
            <View className="space-y-2 mb-5">
              {selectedEntries.map((entry: any) => (
                <View key={entry.id} className="flex-row justify-between items-center bg-slate-800/20 p-3.5 rounded-xl">
                  <View>
                    <Text className="text-slate-200 font-bold text-sm">{entry.foodName}</Text>
                    <Text className="text-slate-500 text-xs mt-0.5">{entry.timeLabel} {entry.note.length > 0 && `· ${entry.note}`}</Text>
                  </View>
                  <Text className="text-slate-300 font-bold text-sm">{entry.calories} kcal</Text>
                </View>
              ))}
            </View>
          )}

          {/* Condition Check on Selected Date */}
          <Text className="text-slate-400 font-bold text-xs mb-3">피드백 & 신호 체크인</Text>
          {!selectedConditionCheck ? (
            <View className="bg-slate-800/20 border border-slate-700/10 p-4 rounded-2xl items-center py-6 mb-2">
              <Text className="text-slate-500 text-xs">코칭 체크인 기록이 없습니다.</Text>
            </View>
          ) : (
            <View className="space-y-3 bg-slate-800/30 p-4 rounded-2xl border border-slate-800/40">
              <View className="flex-row flex-wrap gap-2.5">
                {selectedConditionCheck.bodyWeightKg !== null && (
                  <View className="bg-slate-800 px-3 py-1.5 rounded-xl border border-slate-700/30">
                    <Text className="text-slate-500 text-[10px]">체중</Text>
                    <Text className="text-slate-200 font-bold text-xs mt-0.5">{selectedConditionCheck.bodyWeightKg} kg</Text>
                  </View>
                )}
                {selectedConditionCheck.proteinGrams !== null && (
                  <View className="bg-slate-800 px-3 py-1.5 rounded-xl border border-slate-700/30">
                    <Text className="text-slate-500 text-[10px]">단백질</Text>
                    <Text className="text-slate-200 font-bold text-xs mt-0.5">{selectedConditionCheck.proteinGrams} g</Text>
                  </View>
                )}
                {selectedConditionCheck.resistanceSets !== null && (
                  <View className="bg-slate-800 px-3 py-1.5 rounded-xl border border-slate-700/30">
                    <Text className="text-slate-500 text-[10px]">저항 세트</Text>
                    <Text className="text-slate-200 font-bold text-xs mt-0.5">{selectedConditionCheck.resistanceSets} 세트</Text>
                  </View>
                )}
                {selectedConditionCheck.sleepHours !== null && (
                  <View className="bg-slate-800 px-3 py-1.5 rounded-xl border border-slate-700/30">
                    <Text className="text-slate-500 text-[10px]">수면</Text>
                    <Text className="text-slate-200 font-bold text-xs mt-0.5">{selectedConditionCheck.sleepHours} 시간</Text>
                  </View>
                )}
              </View>

              {/* Stress / Relapse trigger logs */}
              {selectedConditionCheck.relapseTrigger && (
                <View className="mt-2 pt-2 border-t border-slate-800">
                  <Text className="text-rose-400 font-bold text-[10px]">이탈 트리거</Text>
                  <Text className="text-slate-200 text-xs mt-0.5">{selectedConditionCheck.relapseTrigger}</Text>
                  {selectedConditionCheck.copingAction && (
                    <View className="mt-1.5 bg-rose-500/5 px-2.5 py-1.5 rounded border border-rose-500/10">
                      <Text className="text-rose-300 text-[10px] font-bold">대응 루틴</Text>
                      <Text className="text-slate-300 text-xs mt-0.5">{selectedConditionCheck.copingAction}</Text>
                    </View>
                  )}
                </View>
              )}
            </View>
          )}
        </View>
      </View>
    </ScrollView>
  );
}
