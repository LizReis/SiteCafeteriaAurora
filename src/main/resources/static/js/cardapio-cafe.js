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


// ANIMACOES CAFE AURORA: inicialização resiliente e diagnóstico local.
// Interação de quantidades acima é preservada integralmente.
(() => {
  const ANIMATION_VERSION = '20261019-animacoes-2';
  const page = document.querySelector('.cafe-menu-page');
  if (!page) return;

  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
  let observer;

  function showAllCategories() {
    document.querySelectorAll('.cafe-menu-page .coffee-category')
      .forEach(category => category.classList.add('is-revealed'));
    page.classList.remove('coffee-reveal-enabled');
  }

  function initCategoryAnimations() {
    if (reducedMotion.matches || !('IntersectionObserver' in window)) {
      showAllCategories();
      return;
    }
    const categories = Array.from(document.querySelectorAll('.cafe-menu-page .coffee-category'));
    if (!categories.length) return;

    page.classList.add('coffee-reveal-enabled');

    const firstCategory = categories[0];
    // Os títulos e a caligrafia terminam antes da entrada dos primeiros produtos.
    const initialDelay = 3200;
    const pageLoadTime = performance.timeOrigin;
    observer = new IntersectionObserver((entries, instance) => {
      for (const entry of entries) {
        if (!entry.isIntersecting) continue;
        instance.unobserve(entry.target);
        const category = entry.target;
        const elapsed = Date.now() - pageLoadTime;
        const wait = category === firstCategory ? Math.max(0, initialDelay - elapsed) : 0;
        if (wait > 0) {
          window.setTimeout(() => category.classList.add('is-revealed'), wait);
        } else {
          category.classList.add('is-revealed');
        }
      }
    }, { threshold: 0.1, rootMargin: '0px 0px -25px 0px' });

    categories.forEach(category => observer.observe(category));
  }

  // Em scripts carregados depois do DOMContentLoaded, o callback também executa.
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initCategoryAnimations, { once: true });
  } else {
    initCategoryAnimations();
  }

  if (typeof reducedMotion.addEventListener === 'function') {
    reducedMotion.addEventListener('change', event => {
      if (event.matches) {
        if (observer) observer.disconnect();
        showAllCategories();
      }
    });
  }

  // Execute CafeAuroraAnimationDebug() no console do navegador.
  // O relatório identifica o estilo carregado, preferência de movimento e animação aplicada.
  window.CafeAuroraAnimationDebug = () => {
    const logo = document.querySelector('.coffee-menu-brand img');
    const css = [...document.styleSheets].map(s => s.href).filter(Boolean);
    return {
      versaoJS: ANIMATION_VERSION,
      url: window.location.href,
      prefereMovimentoReduzido: reducedMotion.matches,
      nomeAnimacaoLogo: logo ? getComputedStyle(logo).animationName : 'logo ausente',
      cssCardapioEncontrado: css.some(url => url.includes('cardapio-cafe.css')),
      arquivosCSS: css.filter(url => url.includes('cardapio-cafe')),
      categoriaInicializada: page.classList.contains('coffee-reveal-enabled')
    };
  };
})();
