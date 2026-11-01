/*
 * Sacola local do Café Aurora.
 * Estado temporário no navegador, sem integração com pedidos/pagamentos no servidor.
 * Store pattern: estado e cálculos ficam isolados das páginas.
 */
(() => {
  'use strict';
  const KEY = 'cafe-aurora.cart.v1';
  const MAX_QTY = 99;
  const FALLBACK_IMAGE = '/img/42953.jpg';

  const safeImage = value => typeof value === 'string' && /^\/img\/[\w-]+\.(?:jpg|jpeg|png|webp)$/i.test(value)
    ? value : FALLBACK_IMAGE;
  const formatBRL = cents => (Math.max(0, cents) / 100).toLocaleString('pt-BR', {
    style: 'currency', currency: 'BRL'
  });

  const cleanItem = item => {
    if (!item || typeof item !== 'object') return null;
    const id = typeof item.id === 'string' ? item.id.slice(0, 100) : '';
    const name = typeof item.name === 'string' ? item.name.slice(0, 120).trim() : '';
    const cents = Number(item.priceCents);
    const qty = Number(item.quantity);
    if (!id || !name || !Number.isSafeInteger(cents) || cents <= 0 || cents > 10000000 ||
        !Number.isInteger(qty) || qty < 1 || qty > MAX_QTY) return null;
    return { id, name, description: String(item.description || '').slice(0, 250),
      priceCents: cents, image: safeImage(item.image), quantity: qty };
  };

  let state = [];
  try {
    const parsed = JSON.parse(localStorage.getItem(KEY) || '[]');
    if (Array.isArray(parsed)) state = parsed.map(cleanItem).filter(Boolean).slice(0, 100);
  } catch (_) { /* localStorage indisponível: permanece em memória */ }

  function getItems() { return state.map(item => ({ ...item })); }
  function getSubtotalCents() { return state.reduce((sum, item) => sum + item.priceCents * item.quantity, 0); }
  function getCount() { return state.reduce((sum, item) => sum + item.quantity, 0); }
  function updateBadge() {
    document.querySelectorAll('.bag-count').forEach(element => {
      const count = getCount();
      element.textContent = String(count);
      element.setAttribute('aria-label', count + ' itens no carrinho');
    });
  }
  function notify() {
    try { localStorage.setItem(KEY, JSON.stringify(state)); } catch (_) {}
    updateBadge();
    document.dispatchEvent(new Event('cafeaurora:cart-change'));
  }
  function setQuantity(item, quantity) {
    const id = String(item?.id || '').slice(0, 100);
    if (!id) return;
    const count = Math.min(MAX_QTY, Math.max(0, Math.floor(Number(quantity) || 0)));
    state = state.filter(current => current.id !== id);
    if (count > 0) {
      const prepared = cleanItem({ ...item, id, quantity: count });
      if (prepared) state.push(prepared);
    }
    notify();
  }
  function remove(id) { state = state.filter(item => item.id !== id); notify(); }
  function clear() { state = []; notify(); }
  function get(id) { return state.find(item => item.id === id) || null; }

  window.CafeAuroraCart = Object.freeze({
    getItems, getSubtotalCents, getCount, setQuantity, remove, clear, get, formatBRL
  });

  function getMenuCardInfo(card) {
    const title = card.querySelector('h3')?.textContent?.trim();
    const priceText = card.querySelector('strong')?.textContent || '';
    const priceMatch = priceText.match(/R\$\s*([\d.]+),([\d]{2})/);
    const id = card.dataset.productId || (card.dataset.drinkIndex === undefined
      ? '' : 'drink-' + card.dataset.drinkIndex);
    if (!title || !priceMatch || !id) return null;
    const whole = Number(priceMatch[1].replaceAll('.', ''));
    const cents = whole * 100 + Number(priceMatch[2]);
    return {
      id, name: title, priceCents: cents,
      description: card.querySelector('p')?.textContent?.trim() || '',
      image: card.querySelector('img')?.getAttribute('src') || FALLBACK_IMAGE
    };
  }

  function menuCards() {
    return document.querySelectorAll('.coffee-product, .drink-product, .food-product');
  }

  // As páginas do menu já têm os próprios handlers dos steppers.
  // Escutamos o click na fase de bolha, APÓS o contador visual ser atualizado.
  document.addEventListener('click', event => {
    const button = event.target.closest(
      '.quantity-plus, .quantity-minus, .drink-plus, .drink-minus, .food-plus, .food-minus'
    );
    if (!button) return;
    const card = button.closest('.coffee-product, .drink-product, .food-product');
    if (!card) return;
    const item = getMenuCardInfo(card);
    const amount = Number(card.querySelector('output')?.textContent || 0);
    if (item && Number.isInteger(amount)) setQuantity(item, amount);
  });

  document.addEventListener('DOMContentLoaded', () => {
    updateBadge();
    // Aguarda os scripts das páginas terminarem de inicializar o valor do output.
    requestAnimationFrame(() => {
      menuCards().forEach(card => {
        const info = getMenuCardInfo(card);
        const saved = info ? get(info.id) : null;
        const output = card.querySelector('output');
        if (output) {
          const count = saved?.quantity || 0;
          output.value = String(count);
          output.textContent = String(count);
          const minus = card.querySelector('.quantity-minus, .drink-minus, .food-minus');
          if (minus) minus.disabled = count === 0;
        }
      });
    });
  });
})();
