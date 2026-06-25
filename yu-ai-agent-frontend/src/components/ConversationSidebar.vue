<template>
  <aside class="sidebar" :class="{ open }">
    <!-- 头部 -->
    <div class="sidebar-hd">
      <button class="new-btn" @click="$emit('new-conversation')" title="新建对话">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        <span>新对话</span>
      </button>
    </div>

    <!-- 列表 -->
    <div class="sidebar-list">
      <div
        v-for="c in conversations"
        :key="c.conversationId"
        class="conv-item"
        :class="{ active: c.conversationId === activeId }"
        @click="$emit('select-conversation', c.conversationId)"
      >
        <div class="conv-icon">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
        </div>
        <div class="conv-body">
          <p class="conv-preview">{{ c.preview }}</p>
          <span class="conv-meta">{{ c.messageCount }} 条消息</span>
        </div>
        <button class="conv-del" @click.stop="$emit('delete-conversation', c.conversationId)" title="删除">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
        </button>
      </div>

      <!-- 错误状态 -->
      <div v-if="error" class="empty err">
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round" opacity="0.6">
          <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        <p>无法加载对话列表</p>
        <span>请确认后端服务已启动</span>
        <button class="retry-btn" @click="$emit('retry')">重试</button>
      </div>

      <!-- 空状态 -->
      <div v-else-if="conversations.length === 0" class="empty">
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round" opacity="0.3">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
        </svg>
        <p>暂无对话记录</p>
        <span>新建一个对话开始规划行程吧</span>
      </div>
    </div>

    <!-- 底部信息 -->
    <div class="sidebar-ft">
      <span>{{ conversations.length }} 个对话</span>
    </div>
  </aside>

  <!-- 移动端遮罩 -->
  <div v-if="open" class="overlay" @click="$emit('toggle')"></div>
</template>

<script setup>
defineProps({
  conversations: { type: Array, default: () => [] },
  activeId: { type: String, default: '' },
  open: { type: Boolean, default: false },
  error: { type: Boolean, default: false }
})

defineEmits(['new-conversation', 'select-conversation', 'delete-conversation', 'toggle', 'retry'])
</script>

<style scoped>
.sidebar {
  width: 280px;
  flex-shrink: 0;
  background: var(--bg-card);
  border-right: 1px solid var(--bd);
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  transition: transform var(--mid) var(--ease);
}

/* ── 头部 ── */
.sidebar-hd {
  padding: var(--s-3) var(--s-3);
  border-bottom: 1px solid var(--bd);
  flex-shrink: 0;
}

.new-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--s-2);
  padding: var(--s-2) var(--s-3);
  border-radius: var(--r-sm);
  border: 1px dashed var(--bd-strong);
  color: var(--t2);
  font-family: var(--font-body);
  font-size: 0.85rem;
  transition: all var(--fast) var(--ease);
  background: transparent;
  cursor: pointer;
}
.new-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-soft);
}

/* ── 列表 ── */
.sidebar-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--s-2);
}

.conv-item {
  display: flex;
  align-items: flex-start;
  gap: var(--s-2);
  padding: var(--s-3);
  border-radius: var(--r-sm);
  cursor: pointer;
  transition: background var(--fast) var(--ease);
  margin-bottom: 2px;
}
.conv-item:hover { background: rgba(255,255,255,0.04); }
.conv-item.active { background: rgba(255,255,255,0.08); }

.conv-icon {
  width: 32px; height: 32px;
  border-radius: var(--r-sm);
  background: rgba(255,255,255,0.05);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--t3);
}
.conv-item.active .conv-icon { color: var(--accent); }

.conv-body { flex: 1; min-width: 0; }
.conv-preview {
  font-size: 0.8rem;
  color: var(--t2);
  line-height: 1.4;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.conv-item.active .conv-preview { color: var(--t1); }

.conv-meta {
  font-size: 0.65rem;
  color: var(--t3);
  margin-top: 2px;
  display: block;
}

.conv-del {
  width: 26px; height: 26px;
  border-radius: var(--r-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--t3);
  opacity: 0;
  transition: all var(--fast) var(--ease);
  background: none;
  border: none;
  cursor: pointer;
}
.conv-item:hover .conv-del { opacity: 1; }
.conv-del:hover { color: #ef4444; background: rgba(239,68,68,0.1); }

/* ── 空状态 ── */
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--s-12) var(--s-4);
  color: var(--t3);
  text-align: center;
  gap: var(--s-1);
}
.empty p {
  font-size: 0.85rem;
  margin: var(--s-2) 0 0;
  color: var(--t3);
}
.empty span {
  font-size: 0.72rem;
  color: var(--t3);
  opacity: 0.6;
}

.err p { color: #ef4444 !important; }

.retry-btn {
  margin-top: var(--s-4);
  padding: var(--s-1) var(--s-4);
  border-radius: var(--r-sm);
  border: 1px solid var(--bd-strong);
  background: transparent;
  color: var(--t2);
  font-size: 0.78rem;
  cursor: pointer;
  transition: all var(--fast) var(--ease);
}
.retry-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}

/* ── 底部 ── */
.sidebar-ft {
  padding: var(--s-2) var(--s-3);
  border-top: 1px solid var(--bd);
  font-size: 0.68rem;
  color: var(--t3);
  text-align: center;
  flex-shrink: 0;
}

/* ── 遮罩（移动端） ── */
.overlay {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  z-index: 9;
}

@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0; top: 0; bottom: 0;
    z-index: 10;
    transform: translateX(-100%);
    box-shadow: var(--sh-lg);
  }
  .sidebar.open { transform: translateX(0); }
  .overlay { display: block; }
}
</style>
