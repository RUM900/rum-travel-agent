<template>
  <div class="page">
    <!-- Ambient background -->
    <div class="bg-ambient" aria-hidden="true">
      <div class="orb orb--teal"></div>
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
        <span class="bar-page">旅行规划</span>
      </div>

      <button class="bar-btn" @click="sidebarOpen = !sidebarOpen" aria-label="对话列表">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
          <line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/>
        </svg>
      </button>
    </header>

    <!-- Main area: sidebar + chat -->
    <div class="main-area">
      <ConversationSidebar
        :conversations="conversations"
        :active-id="cid"
        :open="sidebarOpen"
        :error="listError"
        @new-conversation="newChat"
        @select-conversation="loadConversation"
        @delete-conversation="removeConversation"
        @toggle="sidebarOpen = !sidebarOpen"
        @retry="refreshList"
      />

      <main class="chat-zone">
        <ChatRoom
          :messages="msgs"
          :connection-status="conn"
          ai-type="travel"
          @send-message="send"
        />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import ChatRoom from '../components/ChatRoom.vue'
import ConversationSidebar from '../components/ConversationSidebar.vue'
import { chatWithTravelApp, listConversations, getConversation, deleteConversation } from '../api'

const msgs = ref([])
const cid = ref('')
const conn = ref('disconnected')
const conversations = ref([])
const sidebarOpen = ref(false)
let es = null

const add = (c, u) => msgs.value.push({ content: c, isUser: u, time: Date.now() })

// ── Conversation list ──
const listError = ref(false)
const refreshList = async () => {
  listError.value = false
  try {
    const res = await listConversations()
    conversations.value = res.data.data || []
  } catch (e) {
    console.error('Failed to load conversation list:', e)
    listError.value = true
  }
}

// ── New conversation ──
const newChat = () => {
  if (es) es.close()
  msgs.value = []
  cid.value = 't_' + Math.random().toString(36).slice(2, 8)
  conn.value = 'disconnected'
  sidebarOpen.value = false
  add('你好，我是你的旅行规划助手。想去哪里、计划几天、预算和偏好告诉我，我来为你定制行程。', false)
}

// ── Load conversation ──
const loadConversation = async (id) => {
  if (id === cid.value) { sidebarOpen.value = false; return }
  if (es) es.close()
  try {
    const res = await getConversation(id)
    const rawMessages = res.data.data || []
    msgs.value = rawMessages.map(m => ({
      content: m.content,
      isUser: m.role === 'USER',
      time: Date.now()
    }))
    cid.value = id
    conn.value = 'disconnected'
    sidebarOpen.value = false
  } catch (e) {
    console.error('Failed to load conversation:', e)
  }
}

// ── Delete conversation ──
const removeConversation = async (id) => {
  try {
    await deleteConversation(id)
    conversations.value = conversations.value.filter(c => c.conversationId !== id)
    if (id === cid.value) newChat()
  } catch (e) {
    console.error('Failed to delete conversation:', e)
  }
}

// ── Send message ──
const send = (m) => {
  add(m, true)
  if (es) es.close()
  const idx = msgs.value.length
  add('', false)
  conn.value = 'connecting'
  es = chatWithTravelApp(m, cid.value)
  es.onmessage = (e) => {
    if (e.data && e.data !== '[DONE]' && idx < msgs.value.length) {
      msgs.value[idx].content += e.data
    }
    if (e.data === '[DONE]') {
      conn.value = 'disconnected'
      es.close()
      refreshList()
    }
  }
  es.onerror = () => { conn.value = 'error'; es.close() }
}

onMounted(async () => {
  await refreshList()
  if (conversations.value.length > 0) {
    const latestId = conversations.value[0].conversationId
    await loadConversation(latestId)
  } else {
    newChat()
  }
})

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

.orb--teal {
  width: 500px; height: 500px;
  top: -15%; right: -12%;
  background: radial-gradient(circle, var(--teal-glow) 0%, transparent 70%);
  animation: float 18s ease-in-out infinite alternate;
}

.orb--subtle {
  width: 350px; height: 350px;
  bottom: -10%; left: -8%;
  background: radial-gradient(circle, var(--primary-glow) 0%, transparent 70%);
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
  z-index: 11;
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

/* ── Main area ── */
.main-area {
  flex: 1;
  display: flex;
  overflow: hidden;
  position: relative;
  z-index: 1;
}

.chat-zone {
  flex: 1;
  overflow: hidden;
  min-width: 0;
  background: var(--bg-base);
}
</style>
