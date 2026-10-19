// Interação de quantidades — demonstração visual, ainda não envia pedidos ao backend.
document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.coffee-product').forEach(card => {
    const display = card.querySelector('output');
    const decrease = card.querySelector('.quantity-minus');
    const increase = card.querySelector('.quantity-plus');
    if (!display || !decrease || !increase) return;
    let quantity = 0;
    function render() {
      display.value = String(quantity);
      display.textContent = String(quantity);
      decrease.disabled = quantity === 0;
    }
    decrease.addEventListener('click', () => { quantity = Math.max(0, quantity - 1); render(); });
    increase.addEventListener('click', () => { quantity = Math.min(99, quantity + 1); render(); });
    render();
  });
});


// ANIMACOES CAFE AURORA — categorias em sequência e entrada ao rolar.
(() => {
  const ANIMATION_VERSION = '20261019-animacoes-3';
  const page = document.querySelector('.cafe-menu-page');
  if (!page) return;

  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
  let observer;
  const timers = new Set();
  const categories = Array.from(page.querySelectorAll('.coffee-category'));
  const visibleCategories = new Set();
  let firstRevealAt = 0;

  function schedule(callback, delay) {
    const id = window.setTimeout(() => {
      timers.delete(id);
      callback();
    }, delay);
    timers.add(id);
  }

  function reveal(category) {
    if (category.classList.contains('is-revealed')) return;
    category.classList.add('is-revealed');
    if (category === categories[0]) firstRevealAt = performance.now();
  }

  function revealEligible() {
    if (reducedMotion.matches) return;
    categories.forEach((category, index) => {
      if (!visibleCategories.has(category) || category.dataset.revealQueued === 'true') return;
      category.dataset.revealQueued = 'true';

      if (index === 0) {
        const wait = Math.max(0, 3250 - performance.now());
        schedule(() => {
          reveal(category);
          revealEligible();
        }, wait);
        return;
      }

      // A segunda categoria repete o mesmo efeito, mas somente depois dos
      // títulos, linha e produtos de Espressos iniciarem a animação.
      // Se ela entrar no viewport mais tarde, animará naquele momento.
      const predecessor = categories[index - 1];
      if (!predecessor.classList.contains('is-revealed')) {
        category.dataset.revealQueued = 'false';
        return;
      }
      const remaining = Math.max(0, 1750 - (performance.now() - firstRevealAt));
      schedule(() => reveal(category), remaining);
    });
  }

  function showAllCategories() {
    if (observer) observer.disconnect();
    timers.forEach(id => window.clearTimeout(id));
    timers.clear();
    categories.forEach(category => category.classList.add('is-revealed'));
    page.classList.remove('coffee-reveal-enabled');
  }

  function init() {
    if (reducedMotion.matches || !('IntersectionObserver' in window)) {
      showAllCategories();
      return;
    }
    if (!categories.length) return;
    page.classList.add('coffee-reveal-enabled');
    observer = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        if (entry.isIntersecting) visibleCategories.add(entry.target);
      });
      revealEligible();
    }, { threshold: 0.05, rootMargin: '0px 0px -15px 0px' });
    categories.forEach(category => observer.observe(category));
  }

  if (document.readyState === 'loading')
    document.addEventListener('DOMContentLoaded', init, { once: true });
  else init();

  if (typeof reducedMotion.addEventListener === 'function') {
    reducedMotion.addEventListener('change', event => {
      if (event.matches) showAllCategories();
    });
  }

  window.CafeAuroraAnimationDebug = () => {
    const logo = document.querySelector('.coffee-menu-brand img');
    return {
      versaoJS: ANIMATION_VERSION,
      prefereMovimentoReduzido: reducedMotion.matches,
      nomeAnimacaoLogo: logo ? getComputedStyle(logo).animationName : 'logo ausente',
      categorias: categories.map(c => ({
        titulo: c.querySelector('h2')?.textContent.trim(),
        visivel: visibleCategories.has(c),
        animada: c.classList.contains('is-revealed')
      }))
    };
  };
})();
