document.addEventListener('DOMContentLoaded', () => {
    const track = document.querySelector('.carousel-track');
    const quantidade = track?.children.length || 0;
    const centro = Math.floor(quantidade / 2);
    const cards = () => [...track.querySelectorAll('.polaroid-card')];
    const dots = [...document.querySelectorAll('.carousel-dots .dot')];
    function atualizarCarrossel() {
        if (!quantidade) return;
        const atuais = cards();
        atuais.forEach((card, i) => {
            card.classList.remove('side-card', 'far', 'near', 'center-card');
            if (i === centro) card.classList.add('center-card');
            else card.classList.add('side-card', Math.abs(i-centro) > 1 ? 'far' : 'near');
        });
        const indiceAtivo = atuais[centro].dataset.reviewIndex;
        dots.forEach(dot => {
            const ativo = dot.dataset.reviewIndex === indiceAtivo;
            dot.classList.toggle('active', ativo);
            if (ativo) dot.setAttribute('aria-current', 'true');
            else dot.removeAttribute('aria-current');
        });
    }
    if (quantidade > 1) {
        document.querySelector('.next-btn')?.addEventListener('click', () => {
            track.append(track.firstElementChild); atualizarCarrossel();
        });
        document.querySelector('.prev-btn')?.addEventListener('click', () => {
            track.prepend(track.lastElementChild); atualizarCarrossel();
        });
        dots.forEach(dot => dot.addEventListener('click', () => {
            for (let i=0; i<quantidade && cards()[centro].dataset.reviewIndex !== dot.dataset.reviewIndex; i++) {
                track.append(track.firstElementChild);
            }
            atualizarCarrossel();
        }));
    }
    atualizarCarrossel();
    // Mesmas classes e animações de entrada das seções e do rodapé.
    const elementos = document.querySelectorAll('.about-title, .about-card, .reviews-title, .carousel-wrapper, .carousel-dots, .footer-col, .footer-bottom');
    if ('IntersectionObserver' in window) {
        const observer = new IntersectionObserver((entries, observador) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.classList.add('revelado'); observador.unobserve(entry.target);
                }
            });
        }, {threshold: 0.15});
        elementos.forEach(el => observer.observe(el));
    } else elementos.forEach(el => el.classList.add('revelado'));
});
