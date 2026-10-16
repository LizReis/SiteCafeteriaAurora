
$(document).ready(function () {
    // Função para atualizar as classes dependendo da posição atual das cartas
    function atualizarCarrossel() {
        let cartas = $('.carousel-track .polaroid-card');

        // Removemos todas as classes de posição de todas as cartas
        cartas.removeClass('side-card far near center-card');

        // Reatribuimos as classes baseadas na nova ordem (0 a 4)
        cartas.eq(0).addClass('side-card far');
        cartas.eq(1).addClass('side-card near');
        cartas.eq(2).addClass('center-card');
        cartas.eq(3).addClass('side-card near');
        cartas.eq(4).addClass('side-card far');
    }

    // Clique no botão "Próximo" (Direita)
    $('.next-btn').click(function () {
        // Pega a primeira carta e joga pro final da fila
        $('.carousel-track').append($('.carousel-track .polaroid-card').first());
        atualizarCarrossel();
    });

    // Clique no botão "Anterior" (Esquerda)
    $('.prev-btn').click(function () {
        // Pega a última carta e joga pro começo da fila
        $('.carousel-track').prepend($('.carousel-track .polaroid-card').last());
        atualizarCarrossel();
    });
});


//Adicione o JavaScript "Olheiro"
//Agora vamos colocar o script que fica "olhando" a tela e dispara o gatilho quando a seção aparece.

$(document).ready(function() {
    // Cria o "olheiro" que vai vigiar quando os elementos entram na tela
    const observer = new IntersectionObserver((entries, observer) => {
        entries.forEach(entry => {
            // Se o elemento apareceu na tela (isIntersecting)
            if (entry.isIntersecting) {
                // Adiciona a classe que ativa o CSS
                $(entry.target).addClass('revelado');
                // Para de observar esse elemento para a animação acontecer só uma vez
                observer.unobserve(entry.target);
            }
        });
    }, {
        // Dispara a animação quando 15% do elemento estiver visível na tela
        threshold: 0.15 
    });

    // Pede para o olheiro vigiar todas as seções animadas, incluindo o Footer
    $('.about-title, .about-card, .reviews-title, .carousel-wrapper, .carousel-dots, .footer-col, .footer-bottom').each(function() {
        observer.observe(this);
    });
});