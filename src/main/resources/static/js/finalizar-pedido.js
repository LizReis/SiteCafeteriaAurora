(() => {
  'use strict';
  const CART = window.CafeAuroraCart, form = document.getElementById('checkout-form');
  if (!CART || !form) return;
  const feedback = document.getElementById('checkout-feedback');
  const delivery = document.getElementById('delivery-type');
  const button = document.getElementById('confirm-order');
  const currency = CART.formatBRL;
  const text = (id,value) => { document.getElementById(id).textContent=value; };
  let quote=null, busy=false, sequence=0;
  const lines = () => CART.getItems().map(p=>({produtoId:Number(p.id),quantidade:p.quantity}));
  const type = () => delivery.value==='entrega'?'ENTREGA':'RETIRADA';
  const signature = () => JSON.stringify({tipoAtendimento:type(),itens:lines()});
  const csrfHeaders = () => ({'Content-Type':'application/json',
    [document.querySelector('meta[name="_csrf_header"]').content]:document.querySelector('meta[name="_csrf"]').content});
  function message(value) { feedback.textContent=value; feedback.hidden=!value; }
  function render() {
    const items=quote?.itens || [];
    const list=document.getElementById('checkout-items'); list.replaceChildren();
    for(const item of items) {
      const row=document.createElement('article');row.className='checkout-item';
      const img=document.createElement('img');
      img.src=item.imagemUrl || '/img/produto-sem-imagem.svg';
      img.alt=item.nomeProduto;img.loading='lazy';
      img.onerror=()=>{img.onerror=null;img.src='/img/produto-sem-imagem.svg';};
      const details=document.createElement('div'), title=document.createElement('h3'), sub=document.createElement('p');
      title.textContent=item.nomeProduto;
      sub.textContent=item.quantidade+' unidade(s) · '+currency(Math.round(item.subtotal*100));
      details.append(title,sub);row.append(img,details);list.append(row);
    }
    text('checkout-count',items.reduce((n,p)=>n+p.quantidade,0)+' itens no seu pedido');
    for(const [id,field] of [['subtotal','subtotal'],['delivery','taxaEntrega'],['total','total']])
      text('checkout-'+id,currency(Math.round((quote?.[field]||0)*100)));
    button.disabled=busy || !quote || !CART.getCount();
    button.firstChild.textContent=busy?'Processando pedido… ':'Confirmar pedido ';
  }
  async function refreshQuote(notice='Revise os valores oficiais antes de confirmar.') {
    const current=++sequence, captured=signature();quote=null;render();
    if(!CART.getCount()) { message('Sua sacola está vazia. Volte ao cardápio.');return; }
    if(pending?.quote?.signature===captured) {quote=pending.quote;message('Tentativa anterior recuperada. Confira os dados e tente novamente com segurança.');render();return;}
    try {
      const response=await fetch('/api/pedidos/cotacao',{method:'POST',headers:csrfHeaders(),body:captured});
      if(response.status===403) throw new Error('Sessão expirada. Atualize a página e tente novamente. Sua sacola foi preservada.');
      const data=await response.json();
      if(current!==sequence || captured!==signature()) return;
      if(!response.ok) throw new Error(data.mensagem || 'Não foi possível atualizar o resumo.');
      quote={...data,signature:captured}; message(notice);
    } catch(error) { if(current===sequence) message(error instanceof TypeError ? 'Falha de conexão. Atualize o resumo e tente novamente.' : (error.message || 'Não foi possível atualizar o resumo.')); }
    if(current===sequence) render();
  }
  function updateDelivery() {
    const active=type()==='ENTREGA';
    const fields=document.getElementById('delivery-fields');fields.hidden=!active;
    document.getElementById('pickup-notice').hidden=active;
    fields.querySelectorAll('input').forEach(input=>{
      input.disabled=!active;
      if(['cep','endereco','numero','bairro'].includes(input.name))input.required=active;
    });
    refreshQuote();
  }
  function updatePayment() {
    document.getElementById('card-fields').hidden=true;
    document.getElementById('payment-message').textContent='A forma escolhida é uma intenção de pagamento. Pagamento pendente; não há cobrança online, QR Code ou coleta de dados de cartão.';
  }
  delivery.addEventListener('change',updateDelivery);
  form.querySelectorAll('[name="pagamento"]').forEach(p=>p.addEventListener('change',updatePayment));
  document.getElementById('refresh-quote').addEventListener('click',()=>{if(!busy)refreshQuote();});
  document.addEventListener('cafeaurora:cart-change',()=>{if(!busy)refreshQuote();});
  // Keep the same key for an identical payload after a network failure/reload.
  // Only a fingerprint is retained here, never customer/contact/address data.
  let pending=null;
  try { pending=JSON.parse(sessionStorage.getItem('cafe-aurora.pending-order') || 'null'); } catch (_) {}
  form.addEventListener('submit',async event=>{
    event.preventDefault();
    if(busy || !form.reportValidity() || !quote) return;
    if(quote.signature!==signature()) {await refreshQuote('Sacola alterada. Revise o novo resumo.');return;}
    busy=true;render();message('Registrando seu pedido…');
    const value=name=>form.elements[name].value.trim();
    const payments={cartao:'CARTAO_CREDITO',pix:'PIX',dinheiro:'DINHEIRO'};
    const request={nomeCliente:value('nome'),telefoneCliente:value('telefone').replace(/\D/g,''),emailCliente:value('email'),
      tipoAtendimento:type(),formaPagamento:payments[form.querySelector('[name="pagamento"]:checked').value],
      endereco:type()==='ENTREGA'?{cep:value('cep').replace(/\D/g,''),logradouro:value('endereco'),numero:value('numero'),
        complemento:value('complemento'),bairro:value('bairro')}:null,
      observacoes:value('observacoes'),itens:lines(),cotacao:quote.cotacao};
    try {
      const fingerprint=Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(JSON.stringify(request))))).map(b=>b.toString(16).padStart(2,'0')).join('');
      if(pending?.fingerprint!==fingerprint) pending={fingerprint,key:crypto.randomUUID(),quote};
      try {sessionStorage.setItem('cafe-aurora.pending-order',JSON.stringify(pending));}catch(_){}
      const response=await fetch('/api/pedidos',{method:'POST',headers:{...csrfHeaders(),'Idempotency-Key':pending.key},body:JSON.stringify(request)});
      if(response.status===403) throw new Error('Sessão expirada. Atualize a página e tente novamente. Sua sacola foi preservada.');
      const data=await response.json();
      if(!response.ok) {
        if(response.status===409) {busy=false;pending=null;await refreshQuote((data.mensagem || 'Condições alteradas.')+' Revise e confirme novamente.');return;}
        throw new Error([data.mensagem,...(data.detalhes||[])].filter(Boolean).join(' — ') || 'Não foi possível registrar o pedido.');
      }
      if(response.status!==201 || !/^[A-Za-z0-9_-]{43}$/.test(data.referencia)) throw new Error('Resposta inesperada. Tente novamente com os mesmos dados.');
      // The secure reference in the fragment survives refresh and is not sent
      // in page requests or Referer headers. No demo order data is used.
      const destination='/pedido-confirmado.html#pedido='+data.referencia;
      CART.clear();
      try {sessionStorage.removeItem('cafe-aurora.pending-order');sessionStorage.removeItem('cafe-aurora.order-preview.v1');}catch(_){}
      window.location.assign(destination);
    } catch(error) { message(error instanceof TypeError ? 'Falha de conexão. Sua sacola foi preservada; tente novamente.' : (error.message || 'Não foi possível registrar o pedido.')); }
    finally {busy=false;render();}
  });
  updatePayment();updateDelivery();
})();
