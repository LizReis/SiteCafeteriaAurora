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
