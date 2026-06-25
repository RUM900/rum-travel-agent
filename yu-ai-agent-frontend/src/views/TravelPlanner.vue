<template>
  <div class="page">
    <!-- 背景光晕 -->
    <div class="bg-orbs" aria-hidden="true">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
    </div>

    <!-- 顶部栏 -->
    <header class="bar">
      <button class="back-btn" @click="$router.push('/')" aria-label="返回">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m15 18-6-6 6-6"/></svg>
      </button>
      <div class="bar-mid">
        <span class="bar-logo">RUM</span>
        <span class="bar-sep">/</span>
        <span class="bar-label">旅行规划</span>
      </div>
      <button class="sidebar-toggle" @click="sidebarOpen = !sidebarOpen" aria-label="对话列表">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
          <line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/>
        </svg>
      </button>
    </header>

    <!-- 主体区域：侧边栏 + 聊天区 -->
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

// ── 加载对话列表 ──
const listError = ref(false)
const refreshList = async () => {
  listError.value = false
  try {
    const res = await listConversations()
    conversations.value = res.data.data || []
  } catch (e) {
    console.error('加载对话列表失败:', e)
    listError.value = true
  }
}

// ── 新建对话 ──
const newChat = () => {
  if (es) es.close()
  msgs.value = []
  cid.value = 't_' + Math.random().toString(36).slice(2, 8)
  conn.value = 'disconnected'
  sidebarOpen.value = false
  add('你好，我是你的旅行规划助手。想去哪里、计划几天、预算和偏好告诉我，我来为你定制行程。', false)
}

// ── 切换历史对话 ──
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
    console.error('加载对话失败:', e)
  }
}

// ── 删除对话 ──
const removeConversation = async (id) => {
  try {
    await deleteConversation(id)
    conversations.value = conversations.value.filter(c => c.conversationId !== id)
    // 如果删除的是当前对话，自动新建
    if (id === cid.value) {
      newChat()
    }
  } catch (e) {
    console.error('删除对话失败:', e)
  }
}

// ── 发送消息 ──
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
  // 先拉对话列表
  await refreshList()
  // 如果已有历史对话，默认打开最近的一条；否则新建
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
  background: var(--bg);
  position: relative;
  overflow: hidden;
}

/* ── 背景 ── */
.bg-orbs { position: fixed; inset: 0; pointer-events: none; z-index: 0; }
.orb { position: absolute; border-radius: 50%; filter: blur(100px); opacity: 0.35; }
.orb-1 {
  width: 500px; height: 500px;
  top: -20%; right: -15%;
  background: radial-gradient(circle, var(--blue-glow) 0%, transparent 70%);
}
.orb-2 {
  width: 400px; height: 400px;
  bottom: -15%; left: -10%;
  background: radial-gradient(circle, var(--accent-glow) 0%, transparent 70%);
}

/* ── 顶栏 ── */
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
  z-index: 11;
}

.back-btn {
  width: 40px; height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--r-sm);
  color: var(--t2);
  transition: all var(--fast) var(--ease);
  border: none;
  background: transparent;
  cursor: pointer;
  flex-shrink: 0;
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

.sidebar-toggle {
  width: 40px; height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--r-sm);
  color: var(--t2);
  transition: all var(--fast) var(--ease);
  border: none;
  background: transparent;
  cursor: pointer;
  flex-shrink: 0;
}
.sidebar-toggle:hover { color: var(--t1); background: rgba(255,255,255,0.05); }

/* ── 主区域 ── */
.main-area {
  flex: 1;
  display: flex;
  overflow: hidden;
  position: relative;
  z-index: 1;
}

.chat-zone { flex: 1; overflow: hidden; min-width: 0; }
</style>
