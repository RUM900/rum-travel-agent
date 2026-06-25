<template>
  <router-view v-slot="{ Component, route }">
    <transition :name="route.meta.transition || 'fade'" mode="out-in">
      <component :is="Component" :key="route.path" />
    </transition>
  </router-view>
</template>

<style>
:root {
  --bg: #09090b;
  --bg-card: #131315;
  --bg-card-hover: #18181b;
  --bg-input: #18181b;
  --surface: #1a1a1e;

  --accent: #f59e0b;
  --accent-soft: rgba(245, 158, 11, 0.15);
  --accent-glow: rgba(245, 158, 11, 0.3);
  --blue: #3b82f6;
  --blue-soft: rgba(59, 130, 246, 0.12);
  --blue-glow: rgba(59, 130, 246, 0.25);
  --orange: #f97316;
  --orange-soft: rgba(249, 115, 22, 0.1);
  --orange-glow: rgba(249, 115, 22, 0.2);
  --emerald: #10b981;
  --emerald-soft: rgba(16, 185, 129, 0.12);

  --t1: #fafafa;
  --t2: #a1a1aa;
  --t3: #71717a;
  --t-inv: #09090b;

  --bd: rgba(255, 255, 255, 0.06);
  --bd-strong: rgba(255, 255, 255, 0.1);
  --bd-focus: rgba(255, 255, 255, 0.15);

  --sh-sm: 0 1px 2px rgba(0, 0, 0, 0.3);
  --sh-md: 0 4px 12px rgba(0, 0, 0, 0.4);
  --sh-lg: 0 16px 40px rgba(0, 0, 0, 0.5);
  --sh-glow: 0 0 30px var(--accent-glow);

  --r-sm: 8px;
  --r-md: 14px;
  --r-lg: 20px;
  --r-xl: 28px;

  --s-1: 4px;
  --s-2: 8px;
  --s-3: 12px;
  --s-4: 16px;
  --s-5: 20px;
  --s-6: 24px;
  --s-8: 32px;
  --s-10: 40px;
  --s-12: 48px;
  --s-16: 64px;
  --s-20: 80px;

  --font-display: 'Space Grotesk', 'Noto Sans SC', system-ui, sans-serif;
  --font-body: 'DM Sans', 'Noto Sans SC', system-ui, sans-serif;
  --font-serif: 'Noto Serif SC', serif;

  --ease: cubic-bezier(0.22, 1, 0.36, 1);
  --fast: 160ms;
  --mid: 280ms;
  --slow: 400ms;
}

*, *::before, *::after { margin: 0; padding: 0; box-sizing: border-box; }

html, body {
  height: 100%;
  font-family: var(--font-body);
  font-size: 16px;
  font-weight: 400;
  line-height: 1.6;
  color: var(--t1);
  background: var(--bg);
  -webkit-font-smoothing: antialiased;
}

#app { min-height: 100dvh; }

a { text-decoration: none; color: inherit; }
button { cursor: pointer; border: none; font-family: inherit; font-size: inherit; background: none; }

:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 3px;
  border-radius: 2px;
}

img { max-width: 100%; display: block; }

::-webkit-scrollbar { width: 4px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.08); border-radius: 10px; }
::-webkit-scrollbar-thumb:hover { background: rgba(255,255,255,0.15); }

.fade-enter-active, .fade-leave-active { transition: opacity var(--mid) var(--ease); }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.slide-enter-active, .slide-leave-active {
  transition: opacity var(--fast) var(--ease), transform var(--fast) var(--ease);
}
.slide-enter-from { opacity: 0; transform: translateY(8px); }
.slide-leave-to { opacity: 0; transform: translateY(-4px); }

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { animation-duration: 0s !important; transition-duration: 0s !important; }
}
</style>
