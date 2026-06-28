<template>
  <div class="home">
    <!-- Animated background orbs -->
    <div class="bg-ambient" aria-hidden="true">
      <div class="orb orb--primary"></div>
      <div class="orb orb--accent"></div>
      <div class="orb orb--teal"></div>
      <div class="orb orb--subtle"></div>
    </div>

    <!-- Grid overlay texture -->
    <div class="bg-grid" aria-hidden="true"></div>

    <!-- Hero -->
    <section class="hero">
      <div class="hero-badge">
        <span class="badge-dot"></span>
        AI-POWERED TRAVEL AGENT
      </div>

      <h1 class="hero-title">
        <span class="hero-word">
          <span class="hero-char" v-for="c in '旅途智策'" :key="c" :style="{ animationDelay: (Math.random() * 0.3).toFixed(2) + 's' }">{{ c }}</span>
        </span>
      </h1>

      <p class="hero-subtitle">
        你的私人 AI 旅行规划师
      </p>
      <p class="hero-desc">
        智能行程定制 · 路线优化 · 美食推荐 · 知识问答
        <br>让每一次出发都充满期待
      </p>

      <!-- Quick stats -->
      <div class="hero-stats">
        <div class="stat">
          <span class="stat-value">AI</span>
          <span class="stat-label">智能规划</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat">
          <span class="stat-value">PDF</span>
          <span class="stat-label">行程导出</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat">
          <span class="stat-value">24/7</span>
          <span class="stat-label">随时在线</span>
        </div>
      </div>
    </section>

    <!-- Agent cards -->
    <section class="cards-section">
      <div class="cards-grid">
        <!-- Travel Planner Card -->
        <article
          class="agent-card card--travel"
          @click="$router.push('/travel-planner')"
        >
          <div class="card-shine"></div>
          <div class="card-header">
            <div class="card-icon icon--travel">
              <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <path d="m14.3 8.3 1.4-5.6-5.6 1.4"/>
                <circle cx="12" cy="12" r="3"/>
                <path d="m12 2v2"/><path d="m12 20v2"/><path d="m2 12h2"/><path d="m20 12h2"/>
              </svg>
            </div>
            <span class="card-tag">TRAVEL</span>
          </div>

          <div class="card-body">
            <h2 class="card-title">旅行规划</h2>
            <p class="card-desc">
              智能生成个性化旅行方案，涵盖路线、住宿、美食与景点推荐
            </p>
            <ul class="card-features">
              <li>多日行程自动编排</li>
              <li>实时天气与路况参考</li>
              <li>精美 PDF 行程导出</li>
            </ul>
          </div>

          <div class="card-footer">
            <span class="card-cta">
              开始规划
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="m9 18 6-6-6-6"/>
              </svg>
            </span>
          </div>
        </article>

        <!-- Super Agent Card -->
        <article
          class="agent-card card--super"
          @click="$router.push('/super-agent')"
        >
          <div class="card-shine"></div>
          <div class="card-header">
            <div class="card-icon icon--super">
              <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 3a6 6 0 0 0-6 6c0 4.5 6 12 6 12s6-7.5 6-12a6 6 0 0 0-6-6z"/>
                <circle cx="12" cy="9" r="2.5"/>
              </svg>
            </div>
            <span class="card-tag">AI AGENT</span>
          </div>

          <div class="card-body">
            <h2 class="card-title">通用助手</h2>
            <p class="card-desc">
              全能 AI 智能体，回答各类问题、处理文档、检索信息
            </p>
            <ul class="card-features">
              <li>知识问答与深度推理</li>
              <li>文件读取与信息提取</li>
              <li>多轮对话上下文记忆</li>
            </ul>
          </div>

          <div class="card-footer">
            <span class="card-cta">
              开始对话
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="m9 18 6-6-6-6"/>
              </svg>
            </span>
          </div>
        </article>
      </div>
    </section>

    <!-- Footer -->
    <footer class="footer">
      <p>旅途智策 &copy; {{ year }} — Built with AI, Designed for You</p>
    </footer>
  </div>
</template>

<script setup>
const year = new Date().getFullYear()
</script>

<style scoped>
/* ══════════════════════════════════════
   HOME — Premium AI Landing
   ══════════════════════════════════════ */

.home {
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--bg-base);
  position: relative;
  overflow: hidden;
  padding: var(--space-12) var(--space-6);
  gap: var(--space-16);
}

/* ── Ambient Background ── */
.bg-ambient {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(140px);
  will-change: transform;
}

.orb--primary {
  width: 700px; height: 700px;
  top: -25%; left: -10%;
  background: radial-gradient(circle, var(--primary-glow) 0%, transparent 70%);
  opacity: 0.45;
  animation: drift 22s ease-in-out infinite alternate;
}

.orb--accent {
  width: 550px; height: 550px;
  top: 45%; right: -12%;
  background: radial-gradient(circle, var(--accent-glow) 0%, transparent 70%);
  opacity: 0.35;
  animation: drift 26s ease-in-out infinite alternate-reverse;
}

.orb--teal {
  width: 400px; height: 400px;
  bottom: -20%; left: 35%;
  background: radial-gradient(circle, var(--teal-glow) 0%, transparent 70%);
  opacity: 0.3;
  animation: drift 20s ease-in-out infinite alternate;
}

.orb--subtle {
  width: 300px; height: 300px;
  top: 10%; left: 50%;
  background: radial-gradient(circle, rgba(167, 139, 250, 0.15) 0%, transparent 70%);
  opacity: 0.3;
  animation: drift 18s ease-in-out infinite alternate-reverse;
}

@keyframes drift {
  0%   { transform: translate(0, 0) scale(1); }
  33%  { transform: translate(30px, -20px) scale(1.05); }
  66%  { transform: translate(-20px, 15px) scale(0.97); }
  100% { transform: translate(15px, 25px) scale(1.03); }
}

/* ── Grid Texture ── */
.bg-grid {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  opacity: 0.03;
  background-image:
    linear-gradient(rgba(255,255,255,0.1) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.1) 1px, transparent 1px);
  background-size: 64px 64px;
  mask-image: radial-gradient(ellipse at 50% 50%, black 30%, transparent 70%);
}

/* ══════════════════════════════════════
   HERO
   ══════════════════════════════════════ */

.hero {
  position: relative;
  z-index: 1;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-5);
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-1) var(--space-4);
  border-radius: var(--radius-full);
  background: var(--bg-surface-2);
  border: 1px solid var(--border-default);
  font-family: var(--font-display);
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  letter-spacing: 0.25em;
  color: var(--text-secondary);
  text-transform: uppercase;
}

.badge-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: var(--accent);
  box-shadow: 0 0 8px var(--accent-glow);
  animation: pulse-glow 2s ease-in-out infinite;
}

.hero-title {
  font-size: clamp(4.5rem, 10vw, 7rem);
  font-weight: var(--weight-extrabold);
  letter-spacing: -0.04em;
  line-height: 1;
}

.hero-word {
  display: inline-flex;
  gap: 0.02em;
}

.hero-char {
  display: inline-block;
  background: linear-gradient(
    135deg,
    var(--text-primary) 0%,
    #e2e8f0 30%,
    var(--primary) 60%,
    #a78bfa 100%
  );
  background-size: 200% 200%;
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: shimmerText 4s ease-in-out infinite;
  transition: transform var(--duration-mid) var(--ease-spring);
}

.hero-char:hover {
  transform: translateY(-4px) scale(1.1);
  -webkit-text-fill-color: var(--accent);
}

@keyframes shimmerText {
  0%, 100% { background-position: 0% 50%; }
  50%      { background-position: 100% 50%; }
}

.hero-subtitle {
  font-family: var(--font-display);
  font-size: var(--text-xl);
  font-weight: var(--weight-medium);
  color: var(--text-primary);
  letter-spacing: -0.01em;
  margin-top: var(--space-2);
}

.hero-desc {
  font-size: var(--text-base);
  color: var(--text-secondary);
  font-weight: var(--weight-light);
  line-height: var(--leading-relaxed);
  max-width: 480px;
}

/* ── Hero Stats ── */
.hero-stats {
  display: flex;
  align-items: center;
  gap: var(--space-6);
  margin-top: var(--space-4);
  padding: var(--space-3) var(--space-6);
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
}

.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat-value {
  font-family: var(--font-display);
  font-size: var(--text-lg);
  font-weight: var(--weight-bold);
  letter-spacing: -0.02em;
  background: var(--primary-gradient);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.stat-label {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  font-weight: var(--weight-regular);
}

.stat-divider {
  width: 1px;
  height: 28px;
  background: var(--border-default);
}

/* ══════════════════════════════════════
   AGENT CARDS
   ══════════════════════════════════════ */

.cards-section {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 820px;
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
  gap: var(--space-5);
}

/* ── Card Base ── */
.agent-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
  padding: var(--space-6);
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  cursor: pointer;
  overflow: hidden;
  transition:
    transform var(--duration-mid) var(--ease-out),
    border-color var(--duration-mid) var(--ease-out),
    box-shadow var(--duration-mid) var(--ease-out);
}

.agent-card::before {
  content: '';
  position: absolute;
  inset: 0;
  opacity: 0;
  transition: opacity var(--duration-slow) var(--ease-out);
  pointer-events: none;
  z-index: 0;
}

.card--travel::before {
  background: radial-gradient(ellipse at 70% 20%, rgba(45, 212, 191, 0.08) 0%, transparent 60%);
}

.card--super::before {
  background: radial-gradient(ellipse at 70% 20%, rgba(167, 139, 250, 0.08) 0%, transparent 60%);
}

.agent-card:hover {
  transform: translateY(-4px);
  border-color: var(--border-strong);
  box-shadow: var(--shadow-lg);
}

.agent-card:hover::before {
  opacity: 1;
}

.card--travel:hover {
  border-color: rgba(45, 212, 191, 0.3);
  box-shadow: var(--shadow-lg), 0 0 40px rgba(45, 212, 191, 0.08);
}

.card--super:hover {
  border-color: rgba(167, 139, 250, 0.3);
  box-shadow: var(--shadow-lg), 0 0 40px rgba(129, 140, 248, 0.08);
}

/* Shine effect on hover */
.card-shine {
  position: absolute;
  inset: 0;
  opacity: 0;
  transition: opacity var(--duration-slow) var(--ease-out);
  pointer-events: none;
  z-index: 1;
  background: linear-gradient(
    135deg,
    transparent 30%,
    rgba(255, 255, 255, 0.02) 50%,
    transparent 70%
  );
}

.agent-card:hover .card-shine {
  opacity: 1;
}

/* ── Card Header ── */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  z-index: 2;
}

.card-icon {
  width: 48px; height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform var(--duration-mid) var(--ease-spring);
}

.agent-card:hover .card-icon {
  transform: scale(1.08);
}

.icon--travel {
  background: var(--teal-soft);
  color: var(--teal);
}

.icon--super {
  background: var(--primary-soft);
  color: var(--primary);
}

.card-tag {
  font-family: var(--font-display);
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  letter-spacing: 0.2em;
  color: var(--text-tertiary);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-xs);
  background: var(--bg-surface-2);
}

/* ── Card Body ── */
.card-body {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.card-title {
  font-family: var(--font-display);
  font-size: var(--text-xl);
  font-weight: var(--weight-bold);
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.card-desc {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  line-height: var(--leading-relaxed);
  font-weight: var(--weight-light);
}

.card-features {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin-top: var(--space-2);
}

.card-features li {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  padding-left: var(--space-5);
  position: relative;
}

.card-features li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 5px; height: 5px;
  border-radius: 50%;
}

.card--travel .card-features li::before {
  background: var(--teal);
}

.card--super .card-features li::before {
  background: var(--primary);
}

/* ── Card Footer ── */
.card-footer {
  position: relative;
  z-index: 2;
  padding-top: var(--space-4);
  border-top: 1px solid var(--border-subtle);
}

.card-cta {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  font-family: var(--font-display);
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  letter-spacing: 0.02em;
  transition: gap var(--duration-fast) var(--ease-out), color var(--duration-fast) var(--ease-out);
}

.card--travel .card-cta { color: var(--teal); }
.card--super .card-cta { color: var(--primary); }

.agent-card:hover .card-cta {
  gap: var(--space-3);
}

.card-cta svg {
  transition: transform var(--duration-fast) var(--ease-out);
}

.agent-card:hover .card-cta svg {
  transform: translateX(2px);
}

/* ══════════════════════════════════════
   FOOTER
   ══════════════════════════════════════ */

.footer {
  position: relative;
  z-index: 1;
  text-align: center;
}

.footer p {
  font-family: var(--font-display);
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  letter-spacing: 0.04em;
  font-weight: var(--weight-light);
}

/* ══════════════════════════════════════
   RESPONSIVE
   ══════════════════════════════════════ */

@media (max-width: 768px) {
  .home {
    padding: var(--space-8) var(--space-4);
    gap: var(--space-10);
  }

  .hero-title {
    font-size: 3.5rem;
  }

  .hero-subtitle {
    font-size: var(--text-lg);
  }

  .hero-desc {
    font-size: var(--text-sm);
  }

  .hero-stats {
    gap: var(--space-4);
    padding: var(--space-2) var(--space-4);
  }

  .cards-grid {
    grid-template-columns: 1fr;
  }

  .agent-card {
    padding: var(--space-5);
  }
}

@media (max-width: 380px) {
  .hero-title {
    font-size: 2.8rem;
  }

  .hero-stats {
    flex-direction: column;
    gap: var(--space-2);
    padding: var(--space-3) var(--space-5);
  }

  .stat-divider {
    width: 32px;
    height: 1px;
  }
}
</style>
