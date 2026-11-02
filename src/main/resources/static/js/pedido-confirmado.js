/*
 * Tela demonstrativa de confirmação. O pedido não é salvo no H2.
 * Status correspondem a br.com.cafeaurora.enums.StatusPedido.
 * Futuramente, após autenticação/autorização, conectar à consulta de status do backend.
 */
(() => {
  'use strict';
  const root = document.querySelector('.order-confirmed');
  if (!root) return;
  const STORAGE_KEY = 'cafe-aurora.order-preview.v1';
  const STATUS = Object.freeze({
    RECEBIDO: {step:0,title:'Pedido recebido',description:'Recebemos o seu pedido e vamos começar o preparo.'},
    EM_PREPARO: {step:1,title:'Em preparo',description:'Nossa equipe já está preparando o seu pedido.'},
    PRONTO: {step:2,title:'Pronto para entrega',description:'Seu pedido está pronto e aguarda a entrega.'},
    CONCLUIDO: {step:3,title:'Pedido entregue',description:'Seu pedido foi concluído. Aproveite e conte como foi!'},
    CANCELADO: {step:-1,title:'Pedido cancelado',description:'Este pedido foi cancelado.'}
  });
  const ICONS = Object.freeze({
    RECEBIDO:'<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2.5"><path d="m5 12 5 5L20 7"/></svg>',
    EM_PREPARO:'<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.35"><path d="M6 17v-4a5 5 0 0 1-1-9 5 5 0 0 1 6 1 5 5 0 0 1 6-1 5 5 0 0 1 1 9v4"/><path d="M6 16h12v4H6z"/></svg>',
    PRONTO:'<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.4"><circle cx="5" cy="17" r="3"/><circle cx="19" cy="17" r="3"/><path d="m5 17 5-8 4 8h5l-3.5-8H12M9 7h3M13 4h3l1.5 3H21"/></svg>',
    CONCLUIDO:'<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.5"><path d="m2.5 11.5 9.5-8 9.5 8V21h-7v-7H9v7H2.5Z"/></svg>',
    CANCELADO:'<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 5l14 14M19 5L5 19"/></svg>'
  });
  const byId=id=>document.getElementById(id);
  const money=cents=>(cents/100).toLocaleString('pt-BR',{style:'currency',currency:'BRL'});
  const cap=(text,length)=>String(text||'').slice(0,length);
  function loadPreview() {
    try {
      const raw=sessionStorage.getItem(STORAGE_KEY);
      if (raw) {
        const data=JSON.parse(raw);
        if(data && Array.isArray(data.items) && data.items.length)return data;
      }
    } catch (_) {}
    return {status:'EM_PREPARO',type:'entrega',address:'Endereço demonstrativo — nenhum pedido real foi registrado.',
      items:[{name:'Espresso Italiano',quantity:1,priceCents:1000,image:'/img/42953.jpg'},
             {name:'Latte Macchiato',quantity:1,priceCents:1600,image:'/img/1050.jpg'},
             {name:'Cappuccino',quantity:1,priceCents:1400,image:'/img/42953.jpg'}],
      deliveryCents:700};
  }
  const order=loadPreview(),layout=root.querySelector('.confirmed-layout');
  const button=byId('toggle-confirmed-details'),summary=byId('confirmed-summary');
  const review=byId('review-order-button'),previewSelector=byId('demo-status-select');

  const cup=root.querySelector('.confirmed-cup');
  cup?.addEventListener('error',()=>{
    // Foto temporária se a arte fornecida ainda não tiver sido enviada a static/img.
    if(!cup.src.endsWith('/img/cupPrincipal.jpg'))cup.src='/img/cupPrincipal.jpg';
  });

  function renderSummary() {
    const container=byId('confirmed-summary-items');
    const validItems=(order.items||[]).filter(item=>item && Number.isInteger(item.quantity) &&
      item.quantity>0 && item.quantity<=99 && Number.isSafeInteger(item.priceCents) && item.priceCents>=0);
    container.replaceChildren();
    for(const item of validItems) {
      const row=document.createElement('article');row.className='confirmed-summary-item';
      const img=document.createElement('img');
      img.src=/^\/img\/[\w-]+\.(?:jpg|jpeg|png|webp)$/i.test(item.image||'')?item.image:'/img/42953.jpg';
      img.alt=cap(item.name,80);img.loading='lazy';
      const info=document.createElement('div'),name=document.createElement('h3'),units=document.createElement('p');
      name.textContent=cap(item.name,90);
      units.textContent=item.quantity+(item.quantity===1?' unidade':' unidades');
      info.append(name,units);row.append(img,info);container.append(row);
    }
    const count=validItems.reduce((sum,item)=>sum+item.quantity,0);
    byId('confirmed-item-count').textContent=count+(count===1?' item no seu pedido':' itens no seu pedido');
    const subtotal=validItems.reduce((sum,item)=>sum+item.quantity*item.priceCents,0);
    const delivery=Number.isSafeInteger(order.deliveryCents)&&order.deliveryCents>=0?order.deliveryCents:0;
    byId('confirmed-subtotal').textContent=money(subtotal);
    byId('confirmed-delivery').textContent=money(delivery);
    byId('confirmed-total').textContent=money(subtotal+delivery);
    const pickup=order.type==='retirada';
    byId('confirmed-address-heading').textContent=pickup?'Retirada no local':'Endereço de entrega';
    byId('confirmed-address').textContent=pickup
      ? 'Retirada ilustrativa — endereço real ainda não cadastrado.'
      : cap(order.address,350)||'Endereço não informado.';
  }
  function applyStatus(next,persist=true) {
    const status=STATUS[next];if(!status)return;
    order.status=next;
    const pickup=order.type==='retirada';
    byId('current-status-label').textContent=pickup&&next==='CONCLUIDO'?'Pedido retirado':status.title;
    byId('current-status-description').textContent=pickup&&next==='CONCLUIDO'
      ?'Retirada concluída. Obrigado por escolher o Café Aurora!'
      :pickup&&next==='PRONTO'?'Seu pedido está pronto para retirada.':status.description;
    byId('current-status-icon').innerHTML=ICONS[next];
    root.querySelectorAll('.tracking-step').forEach(el=>{
      const n=Number(el.dataset.step);
      el.classList.toggle('is-done',status.step>=0&&n<status.step);
      el.classList.toggle('is-current',n===status.step);
      if(n===status.step)el.setAttribute('aria-current','step');
      else el.removeAttribute('aria-current');
    });
    review.disabled=next!=='CONCLUIDO';
    document.dispatchEvent(new CustomEvent('cafeaurora:review-status',{detail:{status:next}}));
    previewSelector.value=next;
    if(persist){try{sessionStorage.setItem(STORAGE_KEY,JSON.stringify(order));}catch(_){}}
  }
  button.addEventListener('click',()=>{
    document.dispatchEvent(new CustomEvent('cafeaurora:toggle-details'));
  });
  previewSelector.addEventListener('change',event=>applyStatus(event.target.value));
  review.addEventListener('click',()=>{
    if (review.disabled) return;
    document.dispatchEvent(new CustomEvent('cafeaurora:open-review'));
  });
  document.addEventListener('cafeaurora:order-status-changed',event=>{
    const status=event.detail?.status;
    if(typeof status==='string')applyStatus(status);
  });
  renderSummary();applyStatus(STATUS[order.status]?order.status:'RECEBIDO',false);
  window.CafeAuroraPedidoDemo=Object.freeze({
    setStatus:status=>{if(STATUS[status])applyStatus(status);},
    getStatus:()=>order.status
  });
})();
