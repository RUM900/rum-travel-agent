<template>
  <div class="home">
    <!-- Background gradient orbs — inspired by reference -->
    <div class="bg-orbs" aria-hidden="true">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
    </div>

    <header class="hero">
      <div class="hero-badge">PERSONAL TRAVEL PLANNER</div>
      <h1 class="hero-title">
        <span class="hero-letter">R</span>
        <span class="hero-letter">U</span>
        <span class="hero-letter">M</span>
      </h1>
      <p class="hero-desc">你的私人旅行规划师，<br>让每段旅程都有温度。</p>
    </header>

    <div class="grid">
      <article class="card" @click="$router.push('/travel-planner')">
        <div class="card-glow"></div>
        <div class="card-icon-wrap">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/><path d="m14.3 8.3 1.4-5.6-5.6 1.4"/><circle cx="12" cy="12" r="3"/><path d="m12 2v2"/><path d="m12 20v2"/><path d="m2 12h2"/><path d="m20 12h2"/>
          </svg>
        </div>
        <div class="card-body">
          <span class="card-num">01</span>
          <h2 class="card-title">旅行规划</h2>
          <p class="card-desc">行程定制 · 路线优化 · 美食推荐 · PDF 导出</p>
        </div>
        <div class="card-arrow">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m9 18 6-6-6-6"/></svg>
        </div>
      </article>

      <article class="card" @click="$router.push('/super-agent')">
        <div class="card-glow"></div>
        <div class="card-icon-wrap alt">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 3a6 6 0 0 0-6 6c0 4.5 6 12 6 12s6-7.5 6-12a6 6 0 0 0-6-6z"/><circle cx="12" cy="9" r="2.5"/>
          </svg>
        </div>
        <div class="card-body">
          <span class="card-num">02</span>
          <h2 class="card-title">通用助手</h2>
          <p class="card-desc">知识问答 · 信息检索 · 文件处理 · 随时在线</p>
        </div>
        <div class="card-arrow">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m9 18 6-6-6-6"/></svg>
        </div>
      </article>
    </div>

    <footer class="ft">RUM &copy; {{ y }}</footer>
  </div>
</template>

<script setup>
const y = new Date().getFullYear()
</script>

<style scoped>
.home {
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--bg);
  position: relative;
  overflow: hidden;
  padding: var(--s-12) var(--s-6);
}

/* ── Background orbs ── */
.bg-orbs {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}
.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);
  opacity: 0.5;
}
.orb-1 {
  width: 800px; height: 800px;
  top: -30%; left: -15%;
  background: radial-gradient(circle, var(--blue-glow) 0%, transparent 70%);
  animation: drift 20s ease-in-out infinite alternate;
}
.orb-2 {
  width: 600px; height: 600px;
  top: 40%; right: -10%;
  background: radial-gradient(circle, var(--orange-glow) 0%, transparent 70%);
  animation: drift 25s ease-in-out infinite alternate-reverse;
}
.orb-3 {
  width: 500px; height: 500px;
  bottom: -20%; left: 40%;
  background: radial-gradient(circle, var(--accent-glow) 0%, transparent 70%);
  animation: drift 18s ease-in-out infinite alternate;
}
@keyframes drift {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(40px, 30px) scale(1.1); }
}

/* ── Hero ── */
.hero {
  position: relative;
  z-index: 1;
  text-align: center;
  margin-bottom: var(--s-16);
}

.hero-badge {
  font-family: var(--font-display);
  font-size: 0.7rem;
  font-weight: 500;
  letter-spacing: 0.35em;
  color: var(--t2);
  margin-bottom: var(--s-6);
}

.hero-title {
  display: flex;
  justify-content: center;
  gap: 0.06em;
  margin-bottom: var(--s-6);
}

.hero-letter {
  font-family: var(--font-display);
  font-size: clamp(4rem, 9vw, 6.5rem);
  font-weight: 700;
  color: var(--t1);
  letter-spacing: -0.03em;
  line-height: 1;
  transition: color var(--mid) var(--ease);
}
.hero-letter:nth-child(1):hover { color: var(--blue); }
.hero-letter:nth-child(2):hover { color: var(--accent); }
.hero-letter:nth-child(3):hover { color: var(--orange); }

.hero-desc {
  font-size: 1.1rem;
  color: var(--t2);
  font-weight: 350;
  line-height: 1.7;
  letter-spacing: 0.02em;
}

/* ── Cards ── */
.grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--s-5);
  width: 100%;
  max-width: 760px;
  margin-bottom: var(--s-12);
  position: relative;
  z-index: 1;
}

.card {
  width: 360px;
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--s-5);
  padding: var(--s-6) var(--s-6);
  background: var(--bg-card);
  border-radius: var(--r-lg);
  border: 1px solid var(--bd);
  cursor: pointer;
  transition: all var(--mid) var(--ease);
  overflow: hidden;
}

.card:hover {
  background: var(--bg-card-hover);
  border-color: var(--bd-strong);
  transform: translateY(-2px);
  box-shadow: var(--sh-lg);
}

.card-glow {
  position: absolute;
  inset: 0;
  opacity: 0;
  transition: opacity var(--slow) var(--ease);
  pointer-events: none;
}
.card:hover .card-glow {
  opacity: 1;
  background: radial-gradient(ellipse at 80% 20%, var(--accent-soft) 0%, transparent 60%);
}

.card-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: var(--r-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--blue-soft);
  color: var(--blue);
}
.card-icon-wrap.alt {
  background: var(--orange-soft);
  color: var(--orange);
}

.card-body { flex: 1; min-width: 0; }

.card-num {
  font-family: var(--font-display);
  font-size: 0.7rem;
  font-weight: 500;
  letter-spacing: 0.15em;
  color: var(--t3);
}

.card-title {
  font-family: var(--font-display);
  font-size: 1.15rem;
  font-weight: 600;
  color: var(--t1);
  margin: var(--s-1) 0 var(--s-1);
  letter-spacing: -0.01em;
}

.card-desc {
  font-size: 0.85rem;
  color: var(--t2);
  line-height: 1.5;
  font-weight: 350;
}

.card-arrow {
  color: var(--t3);
  transition: all var(--fast) var(--ease);
  flex-shrink: 0;
}
.card:hover .card-arrow { color: var(--accent); transform: translateX(3px); }

/* ── Footer ── */
.ft {
  position: relative;
  z-index: 1;
  font-family: var(--font-display);
  font-size: 0.78rem;
  color: var(--t3);
  letter-spacing: 0.06em;
}

@media (max-width: 768px) {
  .home { padding: var(--s-8) var(--s-4); }
  .hero { margin-bottom: var(--s-12); }
  .hero-letter { font-size: 3.5rem; }
  .card { width: 100%; max-width: 420px; }
}
</style>
