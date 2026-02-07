<template>
  <el-radio-group v-model="mode" size="small" @change="handleChange">
    <el-radio-button :label="UI_MODE_BEGINNER">新手模式</el-radio-button>
    <el-radio-button :label="UI_MODE_EXPERT">专家模式</el-radio-button>
  </el-radio-group>
</template>

<script setup>
  import { ref } from 'vue';
  import {
    readCleaningUiMode,
    UI_MODE_BEGINNER,
    UI_MODE_EXPERT,
    writeCleaningUiMode,
  } from '@/constants/cleaningLabelMaps';

  const emit = defineEmits(['change']);

  const mode = ref(readCleaningUiMode());

  const handleChange = value => {
    writeCleaningUiMode(value);
    emit('change', value);
  };
</script>
