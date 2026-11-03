/*
 * Sacola local do Café Aurora.
 * Estado temporário no navegador, sem integração com pedidos/pagamentos no servidor.
 * Store pattern: estado e cálculos ficam isolados das páginas.
 */
(() => {
  'use strict';
  const KEY = 'cafe-aurora.cart.v2';
  const MAX_QTY = 99;
  const FALLBACK_IMAGE = '/img/produto-sem-imagem.svg';

  const safeImage = value => typeof value === 'string' && /^(?:\/(?!\/)|https:\/\/)/i.test(value)
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
    if (!/^[1-9]\d*$/.test(id) || !name || !Number.isSafeInteger(cents) || cents <= 0 || cents > 9999999999 ||
        !Number.isInteger(qty) || qty < 1 || qty > MAX_QTY) return null;
    return { id, name, description: String(item.description || '').slice(0, 250),
      priceCents: cents, image: safeImage(item.image), quantity: qty };
  };

  let state = [];
  try { localStorage.removeItem('cafe-aurora.cart.v1'); } catch (_) {}
  try {
    const parsed = JSON.parse(localStorage.getItem(KEY) || '[]');
    if (Array.isArray(parsed)) state = [...new Map(parsed.map(cleanItem).filter(Boolean).map(item => [item.id, item])).values()].slice(0, 100);
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
    if (!/^[1-9]\d*$/.test(id)) return;
    const card = Array.from(menuCards()).find(c => c.dataset.productId === id);
    if (card && card.dataset.available !== 'true') { remove(id); return; }
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
    return { id: card.dataset.productId, name: card.querySelector('h3')?.textContent.trim(),
      priceCents: Number(card.dataset.priceCents),
      description: card.querySelector('p')?.textContent.trim() || '',
      image: card.querySelector('img')?.getAttribute('src') || FALLBACK_IMAGE };
  }
  function menuCards() {
    return document.querySelectorAll('.coffee-product, .drink-product, .food-product');
  }
  function renderCounters() {
    menuCards().forEach(card => {
      const available = card.dataset.available === 'true';
      const quantity = available ? (get(card.dataset.productId)?.quantity || 0) : 0;
      const output = card.querySelector('output');
      if (output) { output.value = String(quantity); output.textContent = String(quantity); }
      card.querySelectorAll('button').forEach(button => {
        const minus = button.matches('.quantity-minus, .drink-minus, .food-minus');
        button.disabled = !available || (minus ? quantity === 0 : quantity >= MAX_QTY);
      });
    });
  }
  document.addEventListener('click', event => {
    const button = event.target.closest('.quantity-plus, .quantity-minus, .drink-plus, .drink-minus, .food-plus, .food-minus');
    if (!button || button.disabled) return;
    const card = button.closest('[data-product-id]');
    if (!card || card.dataset.available !== 'true') return;
    const item = getMenuCardInfo(card);
    const minus = button.matches('.quantity-minus, .drink-minus, .food-minus');
    setQuantity(item, (get(item.id)?.quantity || 0) + (minus ? -1 : 1));
  });
  document.addEventListener('cafeaurora:cart-change', renderCounters);
  // Reconcile saved metadata and visibility against the public catalogue, including
  // products removed while another menu was open. Never trust this at checkout.
  async function reconcile() {
    if (document.getElementById('checkout-form')) { renderCounters(); updateBadge(); return; }
    if (!state.length) { renderCounters(); updateBadge(); return; }
    try {
      const response = await fetch('/api/produtos');
      if (!response.ok) throw new Error('Catálogo indisponível');
      const products = await response.json();
      state = state.flatMap(saved => {
        const p = products.find(p => String(p.id) === saved.id && p.disponivel);
        if (!p) return [];
        const item = cleanItem({ ...saved, name: p.nome, description: p.descricao,
          priceCents: p.precoCentavos, image: p.imagem });
        return item ? [item] : [];
      });
      notify();
    } catch (_) { renderCounters(); updateBadge(); }
  }
  document.addEventListener('DOMContentLoaded', () => {
    menuCards().forEach(card => {
      const item = getMenuCardInfo(card);
      const saved = get(item.id);
      if (saved) {
        state = state.filter(p => p.id !== item.id);
        if (card.dataset.available === 'true') {
          const prepared = cleanItem({ ...item, quantity: saved.quantity });
          if (prepared) state.push(prepared);
        }
      }
    });
    notify();
    reconcile();
  });
  function restoreStoredState() {
    try {
      const parsed = JSON.parse(localStorage.getItem(KEY) || '[]');
      if (Array.isArray(parsed)) state = [...new Map(parsed.map(cleanItem).filter(Boolean)
        .map(item => [item.id, item])).values()].slice(0, 100);
    } catch (_) { /* Mantém estado em memória se o armazenamento estiver indisponível. */ }
  }
  window.addEventListener('pageshow', () => {
    // Voltar pelo histórico pode restaurar uma página congelada (bfcache).
    // Releia a store antes de reconciliar para não sobrescrever quantidades novas.
    restoreStoredState(); renderCounters(); updateBadge(); reconcile();
  });
  window.addEventListener('storage', event => {
    if (event.key === KEY || event.key === null) {
      restoreStoredState(); renderCounters(); updateBadge();
    }
  });
})();
