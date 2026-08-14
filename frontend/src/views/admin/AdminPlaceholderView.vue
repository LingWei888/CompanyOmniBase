<script setup lang="ts">
defineProps<{
  title: string
  tip: string
}>()
</script>

<template>
  <div class="panel">
    <h2>{{ title }}</h2>
    <p>{{ tip }}</p>
  </div>
</template>

<style scoped>
.panel {
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 12px;
  padding: 20px;
}

h2 {
  margin: 0 0 8px;
}

p {
  margin: 0;
  color: #737373;
}
</style>
