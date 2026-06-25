<template>
  <div class="chat">
    <div class="msg-list" ref="box">
      <div v-for="(m, i) in messages" :key="i" class="row" :class="{ me: m.isUser }">
        <div v-if="!m.isUser" class="av">
          <AiAvatarFallback :type="aiType" />
        </div>

        <div class="bubble" :class="{ them: !m.isUser, mine: m.isUser }">
          <div class="text">
            {{ m.content }}
            <span v-if="!m.isUser && status === 'connecting' && i === messages.length - 1" class="caret" aria-hidden="true"></span>
          </div>
          <time class="time" :datetime="new Date(m.time).toISOString()">{{ fmt(m.time) }}</time>
        </div>

        <div v-if="m.isUser" class="av me-av" aria-hidden="true">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
        </div>
      </div>
    </div>

    <div class="input-row">
      <input
        ref="inputEl"
        v-model="text"
        @keydown.enter="send"
        placeholder="输入消息…"
        :disabled="status === 'connecting'"
        aria-label="消息输入框"
        autocomplete="off"
      />
      <button @click="send" :disabled="status === 'connecting' || !text.trim()" aria-label="发送">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, watch, onMounted } from 'vue'
import AiAvatarFallback from './AiAvatarFallback.vue'

const p = defineProps({
  messages: { type: Array, default: () => [] },
  connectionStatus: { type: String, default: 'disconnected' },
  aiType: { type: String, default: 'travel' }
})

const emit = defineEmits(['send-message'])
const text = ref('')
const box = ref(null)
const inputEl = ref(null)
const status = p.connectionStatus

const send = () => {
  if (!text.value.trim()) return
  emit('send-message', text.value)
  text.value = ''
}

const fmt = ts => new Date(ts).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })

const scroll = async () => {
  await nextTick()
  if (box.value) box.value.scrollTop = box.value.scrollHeight
}

watch(() => p.messages.length, scroll)
watch(() => p.messages.map(m => m.content).join(''), scroll)
onMounted(() => { scroll(); inputEl.value?.focus() })
</script>

<style scoped>
.chat {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: var(--s-4) 0;
}

.msg-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--s-2) 0 var(--s-4);
  scroll-behavior: smooth;
}

.row {
  display: flex;
  align-items: flex-end;
  gap: var(--s-3);
  margin-bottom: var(--s-5);
  max-width: 78%;
}
.me { margin-left: auto; flex-direction: row-reverse; }

/* Avatar */
.av {
  width: 34px; height: 34px;
  border-radius: 50%;
  flex-shrink: 0;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255,255,255,0.06);
}
.me-av {
  background: rgba(59,130,246,0.2);
  color: var(--blue);
}

/* Bubble */
.bubble {
  padding: var(--s-3) var(--s-4);
  border-radius: var(--r-md);
  font-size: 0.93rem;
  line-height: 1.65;
}

.them {
  background: var(--bg-card);
  color: var(--t1);
  border-bottom-left-radius: var(--r-sm);
  border: 1px solid var(--bd);
}

.mine {
  background: rgba(59,130,246,0.15);
  color: #e2e8f0;
  border-bottom-right-radius: var(--r-sm);
  border: 1px solid rgba(59,130,246,0.2);
}

.text { white-space: pre-wrap; word-break: break-word; }

.time {
  display: block;
  font-size: 0.62rem;
  opacity: 0.35;
  margin-top: var(--s-1);
  text-align: right;
}

/* Caret */
.caret {
  display: inline-block;
  width: 2px; height: 1em;
  background: var(--accent);
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: pulse 0.9s var(--ease) infinite;
}
@keyframes pulse { 0%,100%{opacity:0} 50%{opacity:1} }

/* Input */
.input-row {
  display: flex;
  gap: var(--s-2);
  padding: var(--s-3) 0 0;
  border-top: 1px solid var(--bd);
  flex-shrink: 0;
}

.input-row input {
  flex: 1;
  border: 1px solid var(--bd-strong);
  border-radius: var(--r-sm);
  padding: var(--s-3) var(--s-4);
  font-size: 0.93rem;
  font-family: var(--font-body);
  outline: none;
  background: var(--bg-input);
  color: var(--t1);
  transition: border-color var(--fast) var(--ease),
              box-shadow var(--fast) var(--ease);
  min-height: 46px;
}
.input-row input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-soft);
}
.input-row input::placeholder { color: var(--t3); }
.input-row input:disabled { opacity: 0.3; }

.input-row button {
  width: 46px; height: 46px;
  border-radius: var(--r-sm);
  background: var(--accent);
  color: var(--t-inv);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all var(--fast) var(--ease);
}
.input-row button:hover:not(:disabled) { transform: scale(1.04); box-shadow: var(--sh-glow); }
.input-row button:active:not(:disabled) { transform: scale(0.97); }
.input-row button:disabled { opacity: 0.25; transform: none; }

@media (max-width: 700px) {
  .row { max-width: 86%; }
}
</style>
