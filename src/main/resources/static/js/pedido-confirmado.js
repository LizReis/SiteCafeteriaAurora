/* Consulta somente de leitura de um pedido registrado no H2. */
(() => {
  'use strict';
  const root = document.querySelector('.order-confirmed');
  if (!root) return;
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
  let order=null;
  const layout=root.querySelector('.confirmed-layout');
  const button=byId('toggle-confirmed-details'),summary=byId('confirmed-summary');
  const review=byId('review-order-button');
  button.disabled=true;
  const cup=root.querySelector('.confirmed-cup');
  cup?.addEventListener('error',()=>{
    // Foto temporária se a arte fornecida ainda não tiver sido enviada a static/img.
    if(!cup.src.endsWith('/img/cupPrincipal.jpg'))cup.src='/img/cupPrincipal.jpg';
  });

  function renderSummary() {
    const container=byId('confirmed-summary-items');container.replaceChildren();
    for(const item of order.itens) {
      const row=document.createElement('article');row.className='confirmed-summary-item';
      const img=document.createElement('img');img.src=item.imagemUrl || '/img/produto-sem-imagem.svg';img.alt=item.nomeProduto;
      img.onerror=()=>{img.onerror=null;img.src='/img/produto-sem-imagem.svg';};
      const info=document.createElement('div'),name=document.createElement('h3'),units=document.createElement('p');
      name.textContent=item.nomeProduto;
      units.textContent=item.quantidade+' unidade(s) × '+money(Math.round(item.precoUnitario*100))+' = '+money(Math.round(item.subtotal*100));
      info.append(name,units);row.append(img,info);container.append(row);
    }
    byId('confirmed-item-count').textContent=order.itens.reduce((n,p)=>n+p.quantidade,0)+' itens no seu pedido';
    byId('confirmed-subtotal').textContent=money(Math.round(order.subtotal*100));
    byId('confirmed-delivery').textContent=money(Math.round(order.taxaEntrega*100));
    byId('confirmed-total').textContent=money(Math.round(order.total*100));
    const pickup=order.tipoAtendimento==='RETIRADA';
    byId('confirmed-address-heading').textContent=pickup?'Retirada no local':'Endereço de entrega';
    byId('confirmed-address').textContent=pickup?'Retirada no local — aguarde o pedido ficar pronto.':order.enderecoEntrega;
    byId('order-reference').textContent='Pedido #'+order.id+' registrado · '+new Date(order.criadoEm).toLocaleString('pt-BR');
    byId('order-payment').textContent='Pagamento '+order.statusPagamento+' · '+order.formaPagamento.replaceAll('_',' ')+'. Nenhum pagamento online foi processado.';
  }
  function applyStatus(next) {
    const status=STATUS[next];if(!status)return;
    order.status=next;
    const pickup=order.tipoAtendimento==='RETIRADA';
    byId('current-status-label').textContent=pickup&&next==='CONCLUIDO'?'Pedido retirado':status.title;
    byId('current-status-description').textContent=pickup&&next==='CONCLUIDO'
      ?'Retirada concluída. Obrigado por escolher o Café Aurora!'
      :pickup&&next==='PRONTO'?'Seu pedido está pronto para retirada.':status.description;
    byId('current-status-icon').innerHTML=ICONS[next];
    // As 3 conexões ocupam exatamente um terço da linha cada.
    // Recebido: 0/3 | Em preparo: 1/3 | Pronto: 2/3 | Entregue: 3/3.
    root.querySelector('.tracking-steps').style.setProperty(
      '--tracking-progress',String(Math.max(0,status.step)/3)
    );
    root.querySelectorAll('.tracking-step').forEach(el=>{
      const n=Number(el.dataset.step);
      el.classList.toggle('is-done',status.step>=0&&n<status.step);
      el.classList.toggle('is-current',n===status.step);
      if(n===status.step)el.setAttribute('aria-current','step');
      else el.removeAttribute('aria-current');
    });
    review.disabled=next!=='CONCLUIDO';
    document.dispatchEvent(new CustomEvent('cafeaurora:review-status',{detail:{status:next}}));

  }
  button.addEventListener('click',()=>{
    document.dispatchEvent(new CustomEvent('cafeaurora:toggle-details'));
  });

  review.addEventListener('click',()=>{
    if (review.disabled) return;
    document.dispatchEvent(new CustomEvent('cafeaurora:open-review'));
  });
  async function load() {
    const token=new URLSearchParams(location.hash.slice(1)).get('pedido');
    if(!token || !/^[A-Za-z0-9_-]{43}$/.test(token)) {
      byId('order-reference').textContent='Referência do pedido ausente ou inválida. Nenhum pedido foi carregado.';
      byId('current-status-label').textContent='Pedido não carregado';return;
    }
    try {
      const response=await fetch('/api/pedidos/consulta/'+encodeURIComponent(token),{cache:'no-store'});
      if(!response.ok) throw new Error(response.status===404?'Pedido não encontrado.':'Não foi possível consultar o pedido. Atualize para tentar novamente.');
      order=await response.json();byId('order-heading-title').textContent='Pedido registrado';renderSummary();applyStatus(order.status);button.disabled=false;
    } catch(error) {byId('order-reference').textContent=error.message;byId('current-status-label').textContent='Consulta indisponível';}
  }
  window.CafeAuroraPedido=Object.freeze({getStatus:()=>order?.status || null});
  load();
})();
