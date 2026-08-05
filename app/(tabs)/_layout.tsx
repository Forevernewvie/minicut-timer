import React from 'react';
import { Tabs } from 'expo-router';
import { Home, Calendar, Settings } from 'lucide-react-native';

export default function TabLayout() {
  return (
    <Tabs
      screenOptions={{
        tabBarActiveTintColor: '#10B981', // Emerald-500
        tabBarInactiveTintColor: '#64748B', // Slate-500
        tabBarStyle: {
          backgroundColor: '#0F172A', // Slate-900
          borderTopColor: '#1E293B', // Slate-800
          borderTopWidth: 1,
          height: 60,
          paddingBottom: 8,
          paddingTop: 8,
        },
        headerStyle: {
          backgroundColor: '#0F172A', // Slate-900
          borderBottomColor: '#1E293B',
          borderBottomWidth: 1,
        },
        headerTintColor: '#F8FAFC',
        headerTitleStyle: {
          fontWeight: 'bold',
        },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: '대시보드',
          tabBarLabel: '대시보드',
          tabBarIcon: ({ color, size }) => <Home color={color as string} size={size} />,
        }}
      />
      <Tabs.Screen
        name="calendar"
        options={{
          title: '캘린더 리듬',
          tabBarLabel: '캘린더',
          tabBarIcon: ({ color, size }) => <Calendar color={color as string} size={size} />,
        }}
      />
      <Tabs.Screen
        name="plan"
        options={{
          title: '플랜 설정',
          tabBarLabel: '설정',
          tabBarIcon: ({ color, size }) => <Settings color={color as string} size={size} />,
        }}
      />
    </Tabs>
  );
}
