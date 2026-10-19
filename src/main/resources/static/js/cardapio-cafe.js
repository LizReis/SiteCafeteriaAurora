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


// ANIMACOES CAFE AURORA: as categorias entram quando aparecem no viewport.
// O conteúdo não fica oculto se JS estiver indisponível ou se movimento reduzido estiver ativo.
(() => {
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
  if (reducedMotion.matches || !('IntersectionObserver' in window)) return;

  const page = document.querySelector('.cafe-menu-page');
  if (!page) return;

  // Este script é deferido; o corpo já existe, antes do DOMContentLoaded.
  page.classList.add('coffee-reveal-enabled');

  document.addEventListener('DOMContentLoaded', () => {
    const categories = Array.from(document.querySelectorAll('.cafe-menu-page .coffee-category'));
    if (!categories.length) return;

    // Evita que a primeira categoria se antecipe ao título e à linha manuscrita.
    const firstCategory = categories[0];
    const startedAt = performance.now();
    const introFinishMs = 3250;

    const observer = new IntersectionObserver((entries, currentObserver) => {
      entries.forEach(entry => {
        if (!entry.isIntersecting) return;

        const category = entry.target;
        currentObserver.unobserve(category);

        const delay = category === firstCategory
          ? Math.max(0, introFinishMs - (performance.now() - startedAt))
          : 0;

        if (delay) {
          window.setTimeout(() => category.classList.add('is-revealed'), delay);
        } else {
          category.classList.add('is-revealed');
        }
      });
    }, { threshold: 0.12, rootMargin: '0px 0px -35px 0px' });

    categories.forEach(category => observer.observe(category));
  }, { once: true });
})();
