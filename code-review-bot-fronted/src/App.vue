<template>
  <router-view />
</template>

<script setup lang="ts">
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

:root {
  --bg-page: #f0f2f5;
  --text-primary: #1d1d1f;
  --text-secondary: #6e6e73;
  --border-color: #e4e7ed;
  --card-bg: #ffffff;
  --brand-color: #3451b2;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
    'Helvetica Neue', Arial, sans-serif;
  background: var(--bg-page);
  color: var(--text-primary);
  -webkit-font-smoothing: antialiased;
}
</style>
