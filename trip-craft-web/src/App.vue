<template>
  <MapContainer />
</template>

<script setup lang="ts">
import MapContainer from './components/MapContainer.vue'
</script>

<style>
/* 去除默认边距，实现全屏铺满 */
html,
body,
#app {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
}
</style>
