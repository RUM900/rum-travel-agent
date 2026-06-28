<template>
  <router-view v-slot="{ Component, route }">
    <transition :name="route.meta.transition || 'fade'" mode="out-in">
      <component :is="Component" :key="route.path" />
    </transition>
  </router-view>
</template>

<style>
/* ══════════════════════════════════════════════════
   NEBULA DESIGN SYSTEM — RUM AI Agent
   Premium dark AI interface with warm accents
   ══════════════════════════════════════════════════ */

:root {
  /* ── Surface Hierarchy (dark → elevated) ── */
  --bg-base:        #08080f;
  --bg-surface-0:   #0c0c16;
  --bg-surface-1:   #11111d;
  --bg-surface-2:   #161625;
  --bg-surface-3:   #1c1c30;
  --bg-surface-4:   #24243b;

  /* ── Interactive surfaces ── */
  --bg-hover:       rgba(255, 255, 255, 0.04);
  --bg-pressed:     rgba(255, 255, 255, 0.06);
  --bg-selected:    rgba(129, 140, 248, 0.08);
  --bg-input:       #12121f;

  /* ── Primary: Indigo-Violet (AI intelligence) ── */
  --primary:           #818cf8;
  --primary-soft:      rgba(129, 140, 248, 0.12);
  --primary-glow:      rgba(129, 140, 248, 0.25);
  --primary-gradient:  linear-gradient(135deg, #818cf8 0%, #a78bfa 50%, #c4b5fd 100%);

  /* ── Accent: Warm Amber (travel warmth) ── */
  --accent:            #f59e0b;
  --accent-light:      #fbbf24;
  --accent-soft:       rgba(245, 158, 11, 0.12);
  --accent-glow:       rgba(245, 158, 11, 0.3);
  --accent-gradient:   linear-gradient(135deg, #f59e0b 0%, #f97316 50%, #ef4444 100%);

  /* ── Secondary: Teal (travel/nature) ── */
  --teal:            #2dd4bf;
  --teal-soft:       rgba(45, 212, 191, 0.12);
  --teal-glow:       rgba(45, 212, 191, 0.2);

  /* ── Semantic colors ── */
  --success:         #34d399;
  --success-soft:    rgba(52, 211, 153, 0.12);
  --warning:         #fbbf24;
  --warning-soft:    rgba(251, 191, 36, 0.12);
  --error:           #f87171;
  --error-soft:      rgba(248, 113, 113, 0.12);
  --info:            #60a5fa;
  --info-soft:       rgba(96, 165, 250, 0.12);

  /* ── Text hierarchy ── */
  --text-primary:      #f1f1f4;
  --text-secondary:    #9898a8;
  --text-tertiary:     #606078;
  --text-disabled:     #404055;
  --text-inverse:      #0c0c16;
  --text-accent:       #fbbf24;

  /* ── Border system ── */
  --border-subtle:     rgba(255, 255, 255, 0.05);
  --border-default:    rgba(255, 255, 255, 0.08);
  --border-strong:     rgba(255, 255, 255, 0.12);
  --border-focus:      rgba(129, 140, 248, 0.4);
  --border-accent:     rgba(245, 158, 11, 0.3);

  /* ── Shadow system (multi-layer for depth) ── */
  --shadow-xs:     0 1px 2px rgba(0, 0, 0, 0.4);
  --shadow-sm:     0 1px 3px rgba(0, 0, 0, 0.4), 0 1px 2px rgba(0, 0, 0, 0.3);
  --shadow-md:     0 4px 12px rgba(0, 0, 0, 0.5), 0 1px 3px rgba(0, 0, 0, 0.3);
  --shadow-lg:     0 12px 40px rgba(0, 0, 0, 0.6), 0 4px 12px rgba(0, 0, 0, 0.4);
  --shadow-xl:     0 24px 64px rgba(0, 0, 0, 0.7), 0 8px 24px rgba(0, 0, 0, 0.5);
  --shadow-glow-primary: 0 0 40px var(--primary-glow);
  --shadow-glow-accent:  0 0 40px var(--accent-glow);

  /* ── Border radius ── */
  --radius-xs:      4px;
  --radius-sm:      8px;
  --radius-md:      12px;
  --radius-lg:      16px;
  --radius-xl:      24px;
  --radius-2xl:     32px;
  --radius-full:    9999px;

  /* ── Spacing (4px grid) ── */
  --space-1:        4px;
  --space-2:        8px;
  --space-3:        12px;
  --space-4:        16px;
  --space-5:        20px;
  --space-6:        24px;
  --space-8:        32px;
  --space-10:       40px;
  --space-12:       48px;
  --space-16:       64px;
  --space-20:       80px;
  --space-24:       96px;

  /* ── Typography ── */
  --font-display:   'Space Grotesk', 'Noto Sans SC', system-ui, sans-serif;
  --font-ui:        'Inter', 'Noto Sans SC', system-ui, -apple-system, sans-serif;
  --font-body:      'Inter', 'Noto Sans SC', system-ui, -apple-system, sans-serif;
  --font-mono:      'JetBrains Mono', 'SF Mono', 'Fira Code', monospace;
  --font-serif:     'Noto Serif SC', 'Georgia', serif;

  /* ── Font sizes ── */
  --text-xs:        0.6875rem;   /* 11px */
  --text-sm:        0.8125rem;   /* 13px */
  --text-base:      0.9375rem;   /* 15px */
  --text-md:        1.0625rem;   /* 17px */
  --text-lg:        1.25rem;     /* 20px */
  --text-xl:        1.5rem;      /* 24px */
  --text-2xl:       2rem;        /* 32px */
  --text-3xl:       2.5rem;      /* 40px */
  --text-4xl:       3.5rem;      /* 56px */

  /* ── Font weights ── */
  --weight-light:   300;
  --weight-regular: 400;
  --weight-medium:  500;
  --weight-semibold:600;
  --weight-bold:    700;
  --weight-extrabold:800;

  /* ── Line heights ── */
  --leading-tight:  1.25;
  --leading-normal: 1.5;
  --leading-relaxed:1.65;
  --leading-loose:  1.8;

  /* ── Animation ── */
  --ease-out:       cubic-bezier(0.16, 1, 0.3, 1);
  --ease-in-out:    cubic-bezier(0.65, 0, 0.35, 1);
  --ease-spring:    cubic-bezier(0.34, 1.56, 0.64, 1);
  --duration-fast:  150ms;
  --duration-mid:   250ms;
  --duration-slow:  400ms;
  --duration-xslow: 600ms;

  /* ── Glass effects ── */
  --glass-bg:       rgba(12, 12, 22, 0.7);
  --glass-border:   rgba(255, 255, 255, 0.06);
  --glass-blur:     20px;

  /* ── Chat specific ── */
  --bubble-user:    rgba(129, 140, 248, 0.15);
  --bubble-user-border: rgba(129, 140, 248, 0.25);
  --bubble-ai:      var(--bg-surface-2);
  --bubble-ai-border: var(--border-subtle);
}

/* ══════════════════════════════════════
   RESET & BASE
   ══════════════════════════════════════ */

*, *::before, *::after {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  height: 100%;
  font-family: var(--font-body);
  font-size: 16px;
  font-weight: var(--weight-regular);
  line-height: var(--leading-normal);
  color: var(--text-primary);
  background: var(--bg-base);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: optimizeLegibility;
}

#app {
  min-height: 100dvh;
}

/* ── Typography defaults ── */
h1, h2, h3, h4, h5, h6 {
  font-family: var(--font-display);
  font-weight: var(--weight-semibold);
  letter-spacing: -0.02em;
  line-height: var(--leading-tight);
}

a {
  text-decoration: none;
  color: inherit;
}

button {
  cursor: pointer;
  border: none;
  font-family: inherit;
  font-size: inherit;
  background: none;
  -webkit-tap-highlight-color: transparent;
}

img, svg {
  max-width: 100%;
  display: block;
}

/* ── Focus states (accessible) ── */
:focus-visible {
  outline: 2px solid var(--primary);
  outline-offset: 2px;
  border-radius: var(--radius-xs);
}

:focus:not(:focus-visible) {
  outline: none;
}

/* ── Selection ── */
::selection {
  background: var(--primary-soft);
  color: var(--text-primary);
}

/* ── Scrollbar (thin, subtle) ── */
::-webkit-scrollbar {
  width: 5px;
  height: 5px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.08);
  border-radius: 10px;
}
::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.16);
}

/* ══════════════════════════════════════
   TRANSITIONS
   ══════════════════════════════════════ */

.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--duration-mid) var(--ease-out);
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-enter-active {
  transition: opacity var(--duration-mid) var(--ease-out),
              transform var(--duration-mid) var(--ease-out);
}
.slide-leave-active {
  transition: opacity var(--duration-fast) var(--ease-out),
              transform var(--duration-fast) var(--ease-out);
}
.slide-enter-from {
  opacity: 0;
  transform: translateY(12px);
}
.slide-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* ══════════════════════════════════════
   REDUCED MOTION
   ══════════════════════════════════════ */

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

/* ══════════════════════════════════════
   UTILITY KEYFRAMES
   ══════════════════════════════════════ */

@keyframes shimmer {
  0%   { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

@keyframes pulse-glow {
  0%, 100% { opacity: 0.4; }
  50%      { opacity: 0.7; }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50%      { transform: translateY(-8px); }
}
</style>
