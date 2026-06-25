<template>
  <div class="page">
    <div class="bg-orbs" aria-hidden="true">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
    </div>

    <header class="bar">
      <button class="back-btn" @click="$router.push('/')" aria-label="返回">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m15 18-6-6 6-6"/></svg>
      </button>
      <div class="bar-mid">
        <span class="bar-logo">RUM</span>
        <span class="bar-sep">/</span>
        <span class="bar-label">通用助手</span>
      </div>
      <div class="bar-spacer"></div>
    </header>

    <main class="chat-zone">
      <ChatRoom :messages="msgs" :connection-status="conn" ai-type="super" @send-message="send" />
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
    if (e.data && e.data !== '[DONE]' && idx < msgs.value.length) msgs.value[idx].content += e.data
    if (e.data === '[DONE]') { conn.value = 'disconnected'; es.close() }
  }
  es.onerror = () => { conn.value = 'error'; es.close() }
}

onMounted(() => add('你好，有什么可以帮你的？', false))
onBeforeUnmount(() => { if (es) es.close() })
</script>

<style scoped>
.page {
  height: 100dvh;
  display: flex;
  flex-direction: column;
  background: var(--bg);
  position: relative;
  overflow: hidden;
}

.bg-orbs { position: fixed; inset: 0; pointer-events: none; z-index: 0; }
.orb { position: absolute; border-radius: 50%; filter: blur(100px); opacity: 0.35; }
.orb-1 {
  width: 500px; height: 500px;
  top: -20%; right: -15%;
  background: radial-gradient(circle, var(--orange-glow) 0%, transparent 70%);
}
.orb-2 {
  width: 400px; height: 400px;
  bottom: -15%; left: -10%;
  background: radial-gradient(circle, var(--accent-glow) 0%, transparent 70%);
}

.bar {
  display: flex;
  align-items: center;
  padding: var(--s-3) var(--s-4);
  background: rgba(9,9,11,0.75);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--bd);
  flex-shrink: 0;
  min-height: 54px;
  position: relative;
  z-index: 2;
}

.back-btn {
  width: 40px; height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--r-sm);
  color: var(--t2);
  transition: all var(--fast) var(--ease);
}
.back-btn:hover { color: var(--t1); background: rgba(255,255,255,0.05); }

.bar-mid { flex: 1; display: flex; align-items: center; justify-content: center; gap: var(--s-2); }
.bar-logo {
  font-family: var(--font-display);
  font-size: 1rem;
  font-weight: 600;
  color: var(--t1);
  letter-spacing: 0.06em;
}
.bar-sep { color: var(--t3); font-weight: 300; }
.bar-label { font-size: 0.88rem; color: var(--t2); font-weight: 400; }
.bar-spacer { width: 40px; }

.chat-zone { flex: 1; overflow: hidden; padding: 0 var(--s-4); position: relative; z-index: 1; }
</style>
