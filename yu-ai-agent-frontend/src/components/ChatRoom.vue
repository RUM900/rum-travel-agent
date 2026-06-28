<template>
  <div class="chat">
    <!-- Message list -->
    <div class="msg-list" ref="box">
      <!-- Welcome message when empty -->
      <div v-if="messages.length === 0" class="welcome">
        <div class="welcome-icon" :class="aiType">
          <AiAvatarFallback :type="aiType" :size="32" />
        </div>
        <p class="welcome-text">
          {{ aiType === 'travel' ? '告诉我你的旅行计划，我来帮你定制完美行程' : '有什么问题想问？我随时在线帮助你' }}
        </p>
      </div>

      <div
        v-for="(m, i) in messages"
        :key="i"
        class="row"
        :class="{ me: m.isUser }"
      >
        <!-- AI Avatar -->
        <div v-if="!m.isUser" class="av">
          <AiAvatarFallback :type="aiType" :size="16" />
        </div>

        <!-- Bubble -->
        <div class="bubble" :class="{ them: !m.isUser, mine: m.isUser }">
          <div class="bubble-inner">
            <div class="text">
              {{ m.content }}
              <span
                v-if="!m.isUser && connectionStatus === 'connecting' && i === messages.length - 1 && m.content"
                class="caret"
                aria-hidden="true"
              ></span>
            </div>
            <!-- Typing indicator -->
            <div
              v-if="!m.isUser && connectionStatus === 'connecting' && i === messages.length - 1 && !m.content"
              class="typing"
            >
              <span></span><span></span><span></span>
            </div>
          </div>
          <time class="time" :datetime="new Date(m.time).toISOString()">{{ fmt(m.time) }}</time>
        </div>

        <!-- User Avatar -->
        <div v-if="m.isUser" class="av me-av" aria-hidden="true">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
            <circle cx="12" cy="7" r="4"/>
          </svg>
        </div>
      </div>

      <!-- Connection status -->
      <div v-if="connectionStatus === 'error'" class="status-bar error">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        连接失败，请重试
      </div>
    </div>

    <!-- Input area -->
    <div class="input-area">
      <div class="input-row">
        <input
          ref="inputEl"
          v-model="text"
          @keydown.enter="send"
          :placeholder="connectionStatus === 'connecting' ? 'AI 正在思考…' : '输入你的问题…'"
          :disabled="connectionStatus === 'connecting'"
          aria-label="消息输入框"
          autocomplete="off"
        />
        <button
          class="send-btn"
          @click="send"
          :disabled="connectionStatus === 'connecting' || !text.trim()"
          aria-label="发送消息"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="22" y1="2" x2="11" y2="13"/>
            <polygon points="22 2 15 22 11 13 2 9 22 2"/>
          </svg>
        </button>
      </div>
      <p class="input-hint">Enter 发送 · 支持中文</p>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, watch, onMounted } from 'vue'
import AiAvatarFallback from './AiAvatarFallback.vue'

const props = defineProps({
  messages: { type: Array, default: () => [] },
  connectionStatus: { type: String, default: 'disconnected' },
  aiType: { type: String, default: 'travel' }
})

const emit = defineEmits(['send-message'])
const text = ref('')
const box = ref(null)
const inputEl = ref(null)

const send = () => {
  if (!text.value.trim() || props.connectionStatus === 'connecting') return
  emit('send-message', text.value.trim())
  text.value = ''
}

const fmt = ts => {
  const d = new Date(ts)
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const scroll = async () => {
  await nextTick()
  if (box.value) {
    box.value.scrollTo({ top: box.value.scrollHeight, behavior: 'smooth' })
  }
}

watch(() => props.messages.length, scroll)
watch(
  () => props.messages.map(m => m.content).join(''),
  () => { nextTick(scroll) }
)

onMounted(() => {
  scroll()
  inputEl.value?.focus()
})
</script>

<style scoped>
/* ══════════════════════════════════════
   CHAT ROOM — Premium Messaging UI
   ══════════════════════════════════════ */

.chat {
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* ── Message List ── */
.msg-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-4) var(--space-2);
  scroll-behavior: smooth;
  overscroll-behavior: contain;
}

/* ── Welcome ── */
.welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60%;
  gap: var(--space-5);
  padding: var(--space-12) var(--space-6);
  text-align: center;
}

.welcome-icon {
  width: 72px; height: 72px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-surface-2);
  border: 1px solid var(--border-default);
}

.welcome-icon.travel {
  background: var(--teal-soft);
  border-color: rgba(45, 212, 191, 0.2);
}

.welcome-icon.super {
  background: var(--primary-soft);
  border-color: rgba(129, 140, 248, 0.2);
}

.welcome-text {
  font-size: var(--text-base);
  color: var(--text-secondary);
  font-weight: var(--weight-light);
  max-width: 320px;
  line-height: var(--leading-relaxed);
}

/* ── Message Row ── */
.row {
  display: flex;
  align-items: flex-end;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
  max-width: 80%;
  animation: msgIn var(--duration-mid) var(--ease-out);
}

.me {
  margin-left: auto;
  flex-direction: row-reverse;
}

@keyframes msgIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ── Avatar ── */
.av {
  width: 32px; height: 32px;
  border-radius: 50%;
  flex-shrink: 0;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-surface-2);
  border: 1px solid var(--border-subtle);
}

.me-av {
  background: rgba(129, 140, 248, 0.15);
  color: var(--primary);
  border-color: rgba(129, 140, 248, 0.2);
}

/* ── Bubble ── */
.bubble {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.bubble-inner {
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-lg);
  font-size: var(--text-base);
  line-height: var(--leading-relaxed);
  word-break: break-word;
}

.them .bubble-inner {
  background: var(--bubble-ai);
  border: 1px solid var(--bubble-ai-border);
  border-bottom-left-radius: var(--radius-sm);
  color: var(--text-primary);
}

.mine .bubble-inner {
  background: var(--bubble-user);
  border: 1px solid var(--bubble-user-border);
  border-bottom-right-radius: var(--radius-sm);
  color: #e8eaf6;
}

.text {
  white-space: pre-wrap;
  overflow-wrap: break-word;
}

/* ── Time ── */
.time {
  display: block;
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin-top: 3px;
  padding: 0 2px;
}

.me .time { text-align: right; }

/* ── Typing indicator ── */
.typing {
  display: flex;
  gap: 4px;
  padding: 2px 0;
}

.typing span {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: var(--text-tertiary);
  animation: typeBounce 1.4s ease-in-out infinite;
}

.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typeBounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30%           { transform: translateY(-6px); opacity: 1; }
}

/* ── Caret ── */
.caret {
  display: inline-block;
  width: 2px; height: 1.1em;
  background: var(--primary);
  margin-left: 1px;
  vertical-align: text-bottom;
  border-radius: 1px;
  animation: blink 1s step-end infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50%      { opacity: 0; }
}

/* ── Status Bar ── */
.status-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  margin: var(--space-3) auto;
  border-radius: var(--radius-full);
  font-size: var(--text-xs);
  font-weight: var(--weight-medium);
  max-width: fit-content;
}

.status-bar.error {
  background: var(--error-soft);
  color: var(--error);
  border: 1px solid rgba(248, 113, 113, 0.2);
}

/* ── Input Area ── */
.input-area {
  flex-shrink: 0;
  padding: var(--space-3) var(--space-2) var(--space-3);
}

.input-row {
  display: flex;
  gap: var(--space-2);
  align-items: center;
}

.input-row input {
  flex: 1;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  padding: var(--space-3) var(--space-4);
  font-size: var(--text-base);
  font-family: var(--font-body);
  font-weight: var(--weight-regular);
  outline: none;
  background: var(--bg-input);
  color: var(--text-primary);
  min-height: 48px;
  transition:
    border-color var(--duration-fast) var(--ease-out),
    box-shadow var(--duration-fast) var(--ease-out);
}

.input-row input:focus {
  border-color: var(--border-focus);
  box-shadow: 0 0 0 3px var(--primary-soft);
}

.input-row input::placeholder {
  color: var(--text-tertiary);
  font-weight: var(--weight-light);
}

.input-row input:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* ── Send Button ── */
.send-btn {
  width: 48px; height: 48px;
  border-radius: var(--radius-md);
  background: var(--primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition:
    transform var(--duration-fast) var(--ease-spring),
    box-shadow var(--duration-fast) var(--ease-out),
    opacity var(--duration-fast) var(--ease-out);
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.06);
  box-shadow: 0 0 20px var(--primary-glow);
}

.send-btn:active:not(:disabled) {
  transform: scale(0.95);
}

.send-btn:disabled {
  opacity: 0.25;
  transform: none;
  cursor: not-allowed;
}

/* ── Input Hint ── */
.input-hint {
  text-align: center;
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin-top: var(--space-2);
  font-weight: var(--weight-light);
}

/* ══════════════════════════════════════
   RESPONSIVE
   ══════════════════════════════════════ */

@media (max-width: 700px) {
  .row {
    max-width: 88%;
  }

  .bubble-inner {
    padding: var(--space-2) var(--space-3);
    font-size: var(--text-sm);
  }

  .welcome-icon {
    width: 60px; height: 60px;
  }
}
</style>
