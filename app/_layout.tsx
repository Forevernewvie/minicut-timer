import React, { useEffect } from 'react';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useMiniCutStore } from '../src/store/useMiniCutStore';
// @ts-ignore
import '../global.css';

export default function RootLayout() {
  const loadInitialData = useMiniCutStore((state) => state.loadInitialData);

  useEffect(() => {
    loadInitialData();
  }, [loadInitialData]);

  return (
    <>
      <StatusBar style="light" />
      <Stack
        screenOptions={{
          headerStyle: {
            backgroundColor: '#0B0F19', // Slate-950
          },
          headerTintColor: '#F8FAFC', // Slate-50
          headerTitleStyle: {
            fontWeight: 'bold',
          },
          contentStyle: {
            backgroundColor: '#0B0F19',
          },
        }}
      >
        <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        <Stack.Screen
          name="checkin"
          options={{
            presentation: 'modal',
            title: '코칭 체크인',
            headerStyle: { backgroundColor: '#111827' },
            headerTintColor: '#F3F4F6'
          }}
        />
      </Stack>
    </>
  );
}
