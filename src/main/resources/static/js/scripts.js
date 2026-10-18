$(document).ready(function () {
    const $track = $('.carousel-track');
    const $cartas = $track.children('.polaroid-card');
    const quantidade = $cartas.length;

    // A classe center-card controla automaticamente qual comentário fica visível.
    function atualizarCarrossel() {
        if (!quantidade) return;

        const $cartasAtuais = $track.children('.polaroid-card');
        $cartasAtuais.removeClass('side-card far near center-card');

        $cartasAtuais.eq(0).addClass('side-card far');
        $cartasAtuais.eq(1).addClass('side-card near');
        $cartasAtuais.eq(2).addClass('center-card');
        $cartasAtuais.eq(3).addClass('side-card near');
        $cartasAtuais.eq(4).addClass('side-card far');

        const indiceAtivo = Number($cartasAtuais.eq(2).attr('data-review-index'));
        $('.carousel-dots .dot')
            .removeClass('active')
            .removeAttr('aria-current')
            .filter('[data-review-index="' + indiceAtivo + '"]')
            .addClass('active')
            .attr('aria-current', 'true');
    }

    if (quantidade === 5) {
        $('.next-btn').on('click', function () {
            $track.append($track.children('.polaroid-card').first());
            atualizarCarrossel();
        });

        $('.prev-btn').on('click', function () {
            $track.prepend($track.children('.polaroid-card').last());
            atualizarCarrossel();
        });

        $('.carousel-dots .dot').on('click', function () {
            const destino = Number($(this).attr('data-review-index'));
            for (let tentativa = 0; tentativa < quantidade; tentativa++) {
                const centro = Number($track.children('.polaroid-card').eq(2).attr('data-review-index'));
                if (centro === destino) break;
                $track.append($track.children('.polaroid-card').first());
            }
            atualizarCarrossel();
        });

        atualizarCarrossel();
    }

    // Animações de entrada das seções e do rodapé.
    if ('IntersectionObserver' in window) {
        const observer = new IntersectionObserver((entries, observador) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    $(entry.target).addClass('revelado');
                    observador.unobserve(entry.target);
                }
            });
        }, { threshold: 0.15 });

        $('.about-title, .about-card, .reviews-title, .carousel-wrapper, .carousel-dots, .footer-col, .footer-bottom')
            .each(function () { observer.observe(this); });
    } else {
        $('.about-title, .about-card, .reviews-title, .carousel-wrapper, .carousel-dots, .footer-col, .footer-bottom')
            .addClass('revelado');
    }
});
