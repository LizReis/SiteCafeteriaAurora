(() => {
  'use strict';
  const CART = window.CafeAuroraCart;
  const form = document.getElementById('checkout-form');
  if (!CART || !form) return;
  const currency = CART.formatBRL;
  const feedback = document.getElementById('checkout-feedback');
  const delivery = document.getElementById('delivery-type');
  const paymentMessage = document.getElementById('payment-message');
  const paymentOptions = form.querySelectorAll('[name="pagamento"]');
  const text = (id, value) => { const el = document.getElementById(id); if (el) el.textContent = value; };

  function refresh() {
    const items = CART.getItems();
    const subtotal = CART.getSubtotalCents();
    const deliveryCost = items.length && delivery.value === 'entrega' ? 700 : 0;
    const list = document.getElementById('checkout-items');
    list.replaceChildren();
    for (const item of items) {
      const row = document.createElement('article'); row.className = 'checkout-item';
      const img = document.createElement('img'); img.src = item.image; img.alt = item.name; img.loading = 'lazy';
      const details = document.createElement('div');
      const title = document.createElement('h3'); title.textContent = item.name;
      const sub = document.createElement('p');
      sub.textContent = item.quantity + (item.quantity === 1 ? ' unidade' : ' unidades') + ' · ' + currency(item.priceCents * item.quantity);
      details.append(title, sub); row.append(img, details); list.append(row);
    }
    const count = CART.getCount();
    text('checkout-count', count + (count === 1 ? ' item no seu pedido' : ' itens no seu pedido'));
    text('checkout-subtotal', currency(subtotal));
    text('checkout-delivery', currency(deliveryCost));
    text('checkout-total', currency(subtotal + deliveryCost));
    document.getElementById('confirm-order').disabled = !items.length;
  }

  const updateDelivery = () => {
    const active = delivery.value === 'entrega';
    const fields = document.getElementById('delivery-fields');
    const notice = document.getElementById('pickup-notice');
    fields.hidden = !active; notice.hidden = active;
    fields.querySelectorAll('input').forEach(input => {
      input.disabled = !active;
      if (['cep','endereco','numero','bairro'].includes(input.name)) input.required = active;
    });
    refresh();
  };

  const updatePayment = () => {
    const selected = form.querySelector('[name="pagamento"]:checked')?.value;
    document.getElementById('card-fields').hidden = selected !== 'cartao';
    paymentMessage.textContent = selected === 'cartao'
      ? 'Pagamento com cartão indisponível neste protótipo. Não digite dados de cartão.'
      : selected === 'pix'
        ? 'PIX ilustrativo: este protótipo não gera cobrança nem QR Code.'
        : 'Dinheiro ilustrativo: ainda não há criação real de pedidos.';
  };

  delivery.addEventListener('change', updateDelivery);
  paymentOptions.forEach(option => option.addEventListener('change', updatePayment));
  document.addEventListener('cafeaurora:cart-change', refresh);

  form.addEventListener('submit', event => {
    event.preventDefault();
    if (!form.reportValidity() || !CART.getCount()) return;
    // Fluxo visual de confirmação: não envia dados pessoais nem processa pagamentos.
    const type = delivery.value;
    const address = type === 'retirada' ? ''
      : [
          form.elements.endereco.value.trim() + ', ' + form.elements.numero.value.trim(),
          form.elements.complemento.value.trim(),
          form.elements.bairro.value.trim(),
          'CEP ' + form.elements.cep.value.trim()
        ].filter(Boolean).join(' — ');
    const items = CART.getItems();
    const subtotalCents = CART.getSubtotalCents();
    const deliveryCents = type === 'entrega' ? 700 : 0;
    const preview = {
      items, type, address,
      subtotalCents, deliveryCents, totalCents: subtotalCents + deliveryCents,
      status: 'RECEBIDO', isPreview: true
    };
    try {
      sessionStorage.setItem('cafe-aurora.order-preview.v1', JSON.stringify(preview));
    } catch (_) {}
      window.location.assign('/pedido-confirmado.html');
  });

  updatePayment();
  updateDelivery();
})();
