(() => {
  'use strict';
  const CART = window.CafeAuroraCart;
  const items = document.getElementById('cart-items');
  if (!CART || !items) return;
  const deliveryFee = 700;
  const currency = CART.formatBRL;
  const text = (node, value) => { const element = document.getElementById(node); if (element) element.textContent = value; };

  function render() {
    const cart = CART.getItems();
    const count = CART.getCount();
    const subtotal = CART.getSubtotalCents();
    items.replaceChildren();
    document.getElementById('cart-empty').hidden = cart.length > 0;
    const checkout = document.getElementById('cart-checkout');
    checkout.setAttribute('aria-disabled', String(cart.length === 0));
    checkout.setAttribute('tabindex', cart.length ? '0' : '-1');

    cart.forEach(item => {
      const article = document.createElement('article');
      article.className = 'cart-item';
      const img = document.createElement('img');
      img.src = item.image; img.alt = item.name; img.loading = 'lazy';
      const description = document.createElement('div');
      description.className = 'cart-item-title';
      const title = document.createElement('h3'); title.textContent = item.name;
      const info = document.createElement('p'); info.textContent = item.description;
      const price = document.createElement('p'); price.className = 'cart-unit-price';
      price.textContent = currency(item.priceCents) + ' cada';
      description.append(title, info, price);

      const controls = document.createElement('div'); controls.className = 'cart-item-controls';
      const minus = document.createElement('button'); minus.type = 'button'; minus.textContent = '−';
      minus.setAttribute('aria-label', 'Diminuir ' + item.name);
      const quantity = document.createElement('output'); quantity.textContent = item.quantity;
      const plus = document.createElement('button'); plus.type = 'button'; plus.textContent = '+';
      plus.setAttribute('aria-label', 'Aumentar ' + item.name);
      const remove = document.createElement('button'); remove.type = 'button';
      remove.className = 'remove-item'; remove.innerHTML = '&#128465;';
      remove.setAttribute('aria-label', 'Remover ' + item.name);
      minus.addEventListener('click', () => CART.setQuantity(item, item.quantity - 1));
      plus.addEventListener('click', () => CART.setQuantity(item, item.quantity + 1));
      remove.addEventListener('click', () => CART.remove(item.id));
      controls.append(minus, quantity, plus, remove);
      article.append(img, description, controls);
      items.append(article);
    });
    text('cart-summary-count', count + (count === 1 ? ' item no carrinho' : ' itens no carrinho'));
    text('cart-subtotal', currency(subtotal));
    text('cart-delivery', currency(cart.length ? deliveryFee : 0));
    text('cart-total', currency(cart.length ? subtotal + deliveryFee : 0));
  }
  document.getElementById('cart-checkout').addEventListener('click', event => {
    if (CART.getCount() === 0) event.preventDefault();
  });
  document.addEventListener('cafeaurora:cart-change', render);
  render();
})();
