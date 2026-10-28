// Controles de quantidade (demonstração visual, ainda sem integração com a sacola).
(() => {
  const page = document.querySelector('.drink-page');
  if (!page) return;
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
  const categories = Array.from(page.querySelectorAll('.drink-category'));
  let observer;
  const timers = new Set();
  const visibleCategories = new Set();
  const scheduled = new Set();
  let previousRevealAt = 0;
  const VERSION = '20261028-drinks-animacoes-2';

  function initQuantity() {
    page.querySelectorAll('.drink-product').forEach(card => {
      const output = card.querySelector('output');
      const minus = card.querySelector('.drink-minus');
      const plus = card.querySelector('.drink-plus');
      if (!output || !minus || !plus) return;
      let quantity = 0;
      const render = () => {
        output.value = String(quantity);
        output.textContent = String(quantity);
        minus.disabled = quantity === 0;
      };
      minus.addEventListener('click', () => { quantity = Math.max(0, quantity - 1); render(); });
      plus.addEventListener('click', () => { quantity = Math.min(99, quantity + 1); render(); });
      render();
    });
  }

  function schedule(fn, delay) {
    const id = window.setTimeout(() => { timers.delete(id); fn(); }, delay);
    timers.add(id);
  }

  function reveal(category) {
    if (category.classList.contains('is-revealed')) return;
    category.classList.add('is-revealed');
    previousRevealAt = performance.now();
    processVisibleCategories();
  }

  function processVisibleCategories() {
    if (reducedMotion.matches) return;
    categories.forEach((category, index) => {
      if (!visibleCategories.has(category) || scheduled.has(category)) return;
      if (index > 0 && !categories[index - 1].classList.contains('is-revealed')) return;

      scheduled.add(category);
      const wait = index === 0
        ? Math.max(0, 3250 - performance.now())
        : Math.max(0, 1750 - (performance.now() - previousRevealAt));
      schedule(() => reveal(category), wait);
    });
  }

  function showAll() {
    observer?.disconnect();
    timers.forEach(id => window.clearTimeout(id));
    timers.clear();
    page.classList.remove('drink-reveal-enabled');
    categories.forEach(c => c.classList.add('is-revealed'));
  }

  function init() {
    initQuantity();
    if (reducedMotion.matches || !('IntersectionObserver' in window)) {
      showAll();
      return;
    }
    if (!categories.length) return;
    page.classList.add('drink-reveal-enabled');
    observer = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        if (entry.isIntersecting) visibleCategories.add(entry.target);
      });
      processVisibleCategories();
    }, { threshold: 0.05, rootMargin: '0px 0px -15px 0px' });
    categories.forEach(category => observer.observe(category));
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init, { once: true });
  else init();

  reducedMotion.addEventListener?.('change', event => {
    if (event.matches) showAll();
  });

  window.CafeAuroraDrinksAnimationDebug = () => ({
    versaoJS: VERSION,
    prefereMovimentoReduzido: reducedMotion.matches,
    animacaoLogo: getComputedStyle(page.querySelector('.drink-brand img')).animationName,
    categorias: categories.map(c => ({nome:c.querySelector('h2')?.textContent.trim(),animada:c.classList.contains('is-revealed')}))
  });
})();
