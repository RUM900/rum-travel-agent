<template>
  <aside class="sidebar" :class="{ open }">
    <!-- Header -->
    <div class="sidebar-hd">
      <h2 class="sidebar-title">对话历史</h2>
      <button class="new-btn" @click="$emit('new-conversation')" aria-label="新建对话">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
        <span>新对话</span>
      </button>
    </div>

    <!-- List -->
    <div class="sidebar-list">
      <div
        v-for="c in conversations"
        :key="c.conversationId"
        class="conv-item"
        :class="{ active: c.conversationId === activeId }"
        @click="$emit('select-conversation', c.conversationId)"
        role="button"
        :aria-label="`对话: ${c.preview}`"
        tabindex="0"
        @keydown.enter="$emit('select-conversation', c.conversationId)"
      >
        <div class="conv-icon">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
        </div>
        <div class="conv-body">
          <p class="conv-preview">{{ c.preview || '新对话' }}</p>
          <span class="conv-meta">{{ c.messageCount || 0 }} 条消息</span>
        </div>
        <button
          class="conv-del"
          @click.stop="$emit('delete-conversation', c.conversationId)"
          :aria-label="`删除对话: ${c.preview}`"
          title="删除对话"
        >
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="3 6 5 6 21 6"/>
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
          </svg>
        </button>
      </div>

      <!-- Error state -->
      <div v-if="error" class="empty err">
        <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="var(--error)" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round" opacity="0.6">
          <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        <p>无法加载对话列表</p>
        <span>请确认后端服务已启动</span>
        <button class="retry-btn" @click="$emit('retry')">重试</button>
      </div>

      <!-- Empty state -->
      <div v-else-if="conversations.length === 0" class="empty">
        <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round" opacity="0.25">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
        </svg>
        <p>暂无对话记录</p>
        <span>开始你的第一次规划吧</span>
      </div>
    </div>

    <!-- Footer -->
    <div class="sidebar-ft">
      <span>{{ conversations.length }} 个对话</span>
    </div>
  </aside>

  <!-- Mobile overlay -->
  <div v-if="open" class="overlay" @click="$emit('toggle')" aria-hidden="true"></div>
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
/* ══════════════════════════════════════
   SIDEBAR — Conversation History
   ══════════════════════════════════════ */

.sidebar {
  width: 290px;
  flex-shrink: 0;
  background: var(--bg-surface-0);
  border-right: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  transition: transform var(--duration-mid) var(--ease-out);
}

/* ── Header ── */
.sidebar-hd {
  padding: var(--space-4) var(--space-3);
  border-bottom: 1px solid var(--border-subtle);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.sidebar-title {
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  letter-spacing: 0.12em;
  color: var(--text-tertiary);
  text-transform: uppercase;
}

.new-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-sm);
  border: 1px dashed var(--border-default);
  color: var(--text-secondary);
  font-family: var(--font-body);
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  transition:
    border-color var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out),
    background var(--duration-fast) var(--ease-out);
  background: transparent;
  cursor: pointer;
}

.new-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--primary-soft);
}

/* ── List ── */
.sidebar-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-2);
  overscroll-behavior: contain;
}

.conv-item {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
  padding: var(--space-3);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition:
    background var(--duration-fast) var(--ease-out);
  margin-bottom: 1px;
  position: relative;
}

.conv-item:hover {
  background: var(--bg-hover);
}

.conv-item.active {
  background: var(--bg-selected);
}

.conv-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 12px;
  bottom: 12px;
  width: 2px;
  border-radius: 0 2px 2px 0;
  background: var(--primary);
}

.conv-icon {
  width: 30px; height: 30px;
  border-radius: var(--radius-sm);
  background: var(--bg-surface-2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--text-tertiary);
  transition: color var(--duration-fast) var(--ease-out);
}

.conv-item.active .conv-icon {
  color: var(--primary);
  background: var(--primary-soft);
}

.conv-body {
  flex: 1;
  min-width: 0;
}

.conv-preview {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  line-height: 1.4;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conv-item.active .conv-preview {
  color: var(--text-primary);
  font-weight: var(--weight-medium);
}

.conv-meta {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin-top: 1px;
  display: block;
}

.conv-del {
  width: 28px; height: 28px;
  border-radius: var(--radius-xs);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--text-tertiary);
  opacity: 0;
  transition:
    opacity var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out),
    background var(--duration-fast) var(--ease-out);
  background: none;
  border: none;
  cursor: pointer;
}

.conv-item:hover .conv-del {
  opacity: 1;
}

.conv-del:hover {
  color: var(--error);
  background: var(--error-soft);
}

/* ── Empty / Error ── */
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-12) var(--space-4);
  color: var(--text-tertiary);
  text-align: center;
  gap: var(--space-1);
}

.empty p {
  font-size: var(--text-sm);
  margin: var(--space-2) 0 0;
  color: var(--text-tertiary);
}

.empty span {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  opacity: 0.6;
}

.err p { color: var(--error) !important; }

.retry-btn {
  margin-top: var(--space-4);
  padding: var(--space-1) var(--space-4);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-default);
  background: transparent;
  color: var(--text-secondary);
  font-size: var(--text-xs);
  cursor: pointer;
  transition:
    border-color var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out);
}

.retry-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
}

/* ── Footer ── */
.sidebar-ft {
  padding: var(--space-2) var(--space-3);
  border-top: 1px solid var(--border-subtle);
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  text-align: center;
  flex-shrink: 0;
}

/* ── Mobile Overlay ── */
.overlay {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  z-index: 9;
}

/* ══════════════════════════════════════
   RESPONSIVE
   ══════════════════════════════════════ */

@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0; top: 0; bottom: 0;
    z-index: 10;
    transform: translateX(-100%);
    box-shadow: var(--shadow-xl);
  }

  .sidebar.open {
    transform: translateX(0);
  }

  .overlay {
    display: block;
  }
}
</style>
