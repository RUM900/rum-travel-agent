<template>
  <div class="av" :class="type" aria-hidden="true">
    <svg
      v-if="type === 'travel'"
      :width="size" :height="size"
      viewBox="0 0 24 24"
      fill="none" stroke="currentColor"
      stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"
    >
      <circle cx="12" cy="12" r="10"/>
      <path d="m14.3 8.3 1.4-5.6-5.6 1.4"/>
      <circle cx="12" cy="12" r="3"/>
      <path d="m12 2v2"/><path d="m12 20v2"/><path d="m2 12h2"/><path d="m20 12h2"/>
    </svg>
    <svg
      v-else
      :width="size" :height="size"
      viewBox="0 0 24 24"
      fill="none" stroke="currentColor"
      stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"
    >
      <path d="M12 3a6 6 0 0 0-6 6c0 4.5 6 12 6 12s6-7.5 6-12a6 6 0 0 0-6-6z"/>
      <circle cx="12" cy="9" r="2.5"/>
    </svg>
  </div>
</template>

<script setup>
defineProps({
  type: { type: String, default: 'travel' },
  size: { type: [String, Number], default: 16 }
})
</script>

<style scoped>
.av {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--teal);
  transition: color var(--duration-fast) var(--ease-out);
}

.super {
  color: var(--primary);
}
</style>
