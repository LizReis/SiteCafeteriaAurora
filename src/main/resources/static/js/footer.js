// Revelação suave do rodapé em todas as páginas, sem dependência de jQuery.
// Mantém o conteúdo visível quando o usuário prefere movimento reduzido.
(() => {
    const start = () => {
        const footer = document.querySelector('.site-footer');
        if (!footer) return;
        const parts = footer.querySelectorAll('.footer-col, .footer-bottom');
        const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

        if (reducedMotion || !('IntersectionObserver' in window)) {
            parts.forEach(element => element.classList.add('revelado'));
            return;
        }

        const observer = new IntersectionObserver((entries, obs) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.classList.add('revelado');
                    obs.unobserve(entry.target);
                }
            });
        }, { threshold: 0.12 });
        parts.forEach(element => observer.observe(element));
    };

    if (document.readyState === 'loading')
        document.addEventListener('DOMContentLoaded', start, { once: true });
    else start();
})();
