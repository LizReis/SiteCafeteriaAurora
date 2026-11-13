/* Navegação móvel do Café Aurora. Não interfere nas animações do site. */
(() => {
    "use strict";

    const header = document.querySelector(".site-header");
    if (!header) return;

    const button = header.querySelector(".mobile-menu-toggle");
    const navigation = header.querySelector("#site-primary-nav");
    if (!button || !navigation) return;

    const mobileScreen = window.matchMedia("(max-width: 560px)");
    header.classList.add("has-mobile-menu");

    function setMenuOpen(open) {
        const expanded = Boolean(open && mobileScreen.matches);
        button.setAttribute("aria-expanded", String(expanded));
        button.setAttribute("aria-label", expanded
            ? "Fechar menu de navegação"
            : "Abrir menu de navegação");
        navigation.classList.toggle("mobile-nav-open", expanded);
    }

    button.addEventListener("click", () => {
        setMenuOpen(button.getAttribute("aria-expanded") !== "true");
    });

    /* O usuário escolheu uma página: recolhe o menu antes da navegação. */
    navigation.addEventListener("click", (event) => {
        if (event.target.closest("a")) setMenuOpen(false);
    });

    /* Clique fora do cabeçalho também recolhe o menu. */
    document.addEventListener("click", (event) => {
        if (mobileScreen.matches && !header.contains(event.target)) setMenuOpen(false);
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && button.getAttribute("aria-expanded") === "true") {
            setMenuOpen(false);
            button.focus();
        }
    });

    /* Ao voltar para tablet/desktop a navegação horizontal retorna normalmente. */
    if (typeof mobileScreen.addEventListener === "function") {
        mobileScreen.addEventListener("change", () => setMenuOpen(false));
    } else {
        mobileScreen.addListener(() => setMenuOpen(false));
    }
})();
