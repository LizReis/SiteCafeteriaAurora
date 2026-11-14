/* Consulta somente de leitura de um pedido registrado no H2. */
(() => {
  'use strict';
  const root = document.querySelector('.order-confirmed');
  if (!root) return;
  const STATUS = Object.freeze({
    RECEBIDO: {step:0,title:'Pedido recebido',description:'Recebemos o seu pedido e vamos começar o preparo.'},
    EM_PREPARO: {step:1,title:'Em preparo',description:'Nossa equipe já está preparando o seu pedido.'},
    PRONTO: {step:1,title:'Pronto, aguardando saída para entrega',description:'Seu pedido está pronto e aguarda a saída do entregador.'},
    EM_ENTREGA: {step:2,title:'Em entrega',description:'Seu pedido saiu para entrega e está a caminho!'},
    CONCLUIDO: {step:3,title:'Pedido entregue',description:'Seu pedido foi entregue! Obrigado por escolher o Café Aurora.'},
    CANCELADO: {step:-1,title:'Pedido cancelado',description:'Este pedido foi cancelado.'}
  });
  const ICONS = Object.freeze({
    RECEBIDO:'<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2.5"><path d="m5 12 5 5L20 7"/></svg>',
    EM_PREPARO:'<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.35"><path d="M6 17v-4a5 5 0 0 1-1-9 5 5 0 0 1 6 1 5 5 0 0 1 6-1 5 5 0 0 1 1 9v4"/><path d="M6 16h12v4H6z"/></svg>',
    EM_ENTREGA:'<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.4"><circle cx="5" cy="17" r="3"/><circle cx="19" cy="17" r="3"/><path d="m5 17 5-8 4 8h5l-3.5-8H12M9 7h3M13 4h3l1.5 3H21"/></svg>',
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
    const step=pickup&&next==='PRONTO'?2:status.step;
    byId('current-status-label').textContent=pickup&&next==='CONCLUIDO'?'Retirada concluída':pickup&&next==='PRONTO'?'Pronto para retirada':status.title;
    byId('current-status-description').textContent=pickup&&next==='CONCLUIDO'
      ?'Retirada concluída. Obrigado por escolher o Café Aurora!'
      :pickup&&next==='PRONTO'?'Seu pedido está pronto para retirada.':status.description;
    byId('current-status-icon').innerHTML=ICONS[next==='PRONTO'?'EM_PREPARO':next];
    const steps=root.querySelectorAll('.tracking-step');
    if(steps[2]) {
      steps[2].querySelector('svg').outerHTML=pickup?ICONS.RECEBIDO:ICONS.EM_ENTREGA;
      steps[2].querySelector('.sr-only').textContent=pickup?'Pronto para retirada':'Em entrega';
    }
    if(steps[3])steps[3].querySelector('.sr-only').textContent=pickup?'Retirado':'Entregue';
    // As 3 conexões ocupam exatamente um terço da linha cada.
    // Entrega: PRONTO permanece no preparo; retirada: PRONTO ocupa a posição 2.
    root.querySelector('.tracking-steps').style.setProperty(
      '--tracking-progress',String(Math.max(0,step)/3)
    );
    root.querySelectorAll('.tracking-step').forEach(el=>{
      const n=Number(el.dataset.step);
      el.classList.toggle('is-done',step>=0&&n<step);
      el.classList.toggle('is-current',n===step);
      if(n===step)el.setAttribute('aria-current','step');
      else el.removeAttribute('aria-current');
    });
    review.disabled=next!=='CONCLUIDO'||order.jaAvaliado;
    byId('review-feedback').hidden=!order.jaAvaliado;
    byId('review-feedback').textContent=order.jaAvaliado?'A avaliação deste pedido já foi registrada.':'';
    document.dispatchEvent(new CustomEvent('cafeaurora:review-status',{detail:{status:next,jaAvaliado:order.jaAvaliado}}));

  }
  button.addEventListener('click',()=>{
    document.dispatchEvent(new CustomEvent('cafeaurora:toggle-details'));
  });

  review.addEventListener('click',()=>{
    if (review.disabled) return;
    document.dispatchEvent(new CustomEvent('cafeaurora:open-review'));
  });
  const warning=document.createElement('p');
  warning.id='tracking-update-warning';warning.setAttribute('role','status');warning.hidden=true;
  byId('current-status-description').after(warning);
  const token=new URLSearchParams(location.hash.slice(1)).get('pedido');
  const valid=token&&/^[A-Za-z0-9_-]{43}$/.test(token);
  let timer=null,inflight=null,controller=null,disposed=false,permanent=false,failures=0;
  const terminal=()=>order&&['CONCLUIDO','CANCELADO'].includes(order.status);
  function clearTimer(){clearTimeout(timer);timer=null;}
  function schedule(){
    clearTimer();
    if(!disposed&&!permanent&&!terminal()&&document.visibilityState==='visible')
      timer=setTimeout(load,Math.min(15000*2**Math.min(failures,2),60000));
  }
  function load(){
    clearTimer();
    if(disposed||permanent)return Promise.resolve(order);
    if(inflight)return inflight;
    if(!valid){
      permanent=true;
      byId('order-reference').textContent='Referência do pedido ausente ou inválida. Nenhum pedido foi carregado.';
      byId('current-status-label').textContent='Pedido não carregado';
      return Promise.resolve(null);
    }
    controller=new AbortController();
    const timeout=setTimeout(()=>controller?.abort(),10000);
    inflight=(async()=>{
      try {
        const response=await fetch('/api/pedidos/consulta/'+encodeURIComponent(token),{cache:'no-store',signal:controller.signal});
        if([400,404,410].includes(response.status)){permanent=true;throw new Error('Pedido não encontrado.');}
        if(!response.ok)throw new Error('Falha na consulta');
        const data=await response.json();
        if(!STATUS[data.status]||!['ENTREGA','RETIRADA'].includes(data.tipoAtendimento)||!Array.isArray(data.itens)||typeof data.jaAvaliado!=='boolean')
          throw new Error('Resposta inválida');
        if(disposed)return order;
        const previous=order;
        // Uma avaliação confirmada não pode regredir por uma resposta antiga.
        data.jaAvaliado=Boolean(data.jaAvaliado||previous?.jaAvaliado);
        const changed=!previous||previous.status!==data.status||previous.tipoAtendimento!==data.tipoAtendimento||previous.jaAvaliado!==data.jaAvaliado;
        order=data;
        if(!previous){byId('order-heading-title').textContent='Pedido registrado';renderSummary();button.disabled=false;}
        if(changed)applyStatus(order.status);
        failures=0;if(!warning.hidden)warning.hidden=true;
        return order;
      }catch(error){
        if(disposed)return order;
        failures++;
        warning.textContent=permanent?'Pedido não encontrado.':'Não foi possível atualizar o acompanhamento agora. Mostrando a última situação confirmada.';
        warning.hidden=false;
        if(!order){byId('current-status-label').textContent='Consulta indisponível';}
        return order;
      }finally{clearTimeout(timeout);controller=null;inflight=null;schedule();}
    })();
    return inflight;
  }
  document.addEventListener('visibilitychange',()=>{
    clearTimer();
    if(document.visibilityState==='visible'&&!terminal())load();
  });
  window.addEventListener('pagehide',()=>{disposed=true;clearTimer();controller?.abort();});
  window.addEventListener('pageshow',event=>{if(event.persisted){disposed=false;if(!terminal())load();}});
  window.CafeAuroraPedido=Object.freeze({getStatus:()=>order?.status || null,jaAvaliado:()=>Boolean(order?.jaAvaliado),refresh:load});
  load();
})();
