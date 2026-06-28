<template>
  <div class="page">
    <!-- Ambient background -->
    <div class="bg-ambient" aria-hidden="true">
      <div class="orb orb--primary"></div>
      <div class="orb orb--subtle"></div>
    </div>

    <!-- Top bar -->
    <header class="bar">
      <button class="bar-btn" @click="$router.push('/')" aria-label="返回首页">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="m15 18-6-6 6-6"/>
        </svg>
      </button>

      <div class="bar-center">
        <span class="bar-brand">RUM</span>
        <span class="bar-divider">/</span>
        <span class="bar-page">通用助手</span>
      </div>

      <div class="bar-spacer"></div>
    </header>

    <main class="chat-zone">
      <ChatRoom
        :messages="msgs"
        :connection-status="conn"
        ai-type="super"
        @send-message="send"
      />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import ChatRoom from '../components/ChatRoom.vue'
import { chatWithManus } from '../api'

const msgs = ref([])
const conn = ref('disconnected')
let es = null

const add = (c, u) => msgs.value.push({ content: c, isUser: u, time: Date.now() })

const send = (m) => {
  add(m, true)
  if (es) es.close()
  const idx = msgs.value.length
  add('', false)
  conn.value = 'connecting'
  es = chatWithManus(m)
  es.onmessage = (e) => {
    if (e.data && e.data !== '[DONE]' && idx < msgs.value.length) {
      msgs.value[idx].content += e.data
    }
    if (e.data === '[DONE]') {
      conn.value = 'disconnected'
      es.close()
    }
  }
  es.onerror = () => { conn.value = 'error'; es.close() }
}

onMounted(() => add('你好，我是你的通用 AI 助手。有什么需要帮忙的？', false))
onBeforeUnmount(() => { if (es) es.close() })
</script>

<style scoped>
.page {
  height: 100dvh;
  display: flex;
  flex-direction: column;
  background: var(--bg-base);
  position: relative;
  overflow: hidden;
}

/* ── Ambient background ── */
.bg-ambient {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.3;
}

.orb--primary {
  width: 500px; height: 500px;
  top: -15%; right: -12%;
  background: radial-gradient(circle, var(--primary-glow) 0%, transparent 70%);
  animation: float 18s ease-in-out infinite alternate;
}

.orb--subtle {
  width: 350px; height: 350px;
  bottom: -10%; left: -8%;
  background: radial-gradient(circle, var(--accent-glow) 0%, transparent 70%);
  opacity: 0.2;
  animation: float 22s ease-in-out infinite alternate-reverse;
}

@keyframes float {
  0%   { transform: translate(0, 0) scale(1); }
  100% { transform: translate(20px, 15px) scale(1.05); }
}

/* ── Top bar ── */
.bar {
  display: flex;
  align-items: center;
  padding: var(--space-2) var(--space-3);
  background: var(--glass-bg);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border-bottom: 1px solid var(--glass-border);
  flex-shrink: 0;
  min-height: 52px;
  position: relative;
  z-index: 2;
}

.bar-btn {
  width: 38px; height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  transition:
    color var(--duration-fast) var(--ease-out),
    background var(--duration-fast) var(--ease-out);
  border: none;
  background: transparent;
  cursor: pointer;
  flex-shrink: 0;
}

.bar-btn:hover {
  color: var(--text-primary);
  background: var(--bg-hover);
}

.bar-center {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
}

.bar-brand {
  font-family: var(--font-display);
  font-size: var(--text-base);
  font-weight: var(--weight-bold);
  color: var(--text-primary);
  letter-spacing: 0.04em;
}

.bar-divider {
  color: var(--text-tertiary);
  font-weight: var(--weight-light);
}

.bar-page {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  font-weight: var(--weight-regular);
}

.bar-spacer {
  width: 38px;
  flex-shrink: 0;
}

/* ── Main area ── */
.chat-zone {
  flex: 1;
  overflow: hidden;
  padding: 0 var(--space-2);
  position: relative;
  z-index: 1;
}
</style>
