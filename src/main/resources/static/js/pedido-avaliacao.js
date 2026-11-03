/* Avaliação do pedido — protótipo visual, sem envio nem persistência de foto. */
(() => {
  'use strict';
  const layout=document.querySelector('.confirmed-layout');
  const form=document.getElementById('order-review-form');
  if(!layout||!form)return;
  const review=document.getElementById('confirmed-review');
  const thanks=document.getElementById('confirmed-review-thanks');
  const summary=document.getElementById('confirmed-summary');
  const detailsButton=document.getElementById('toggle-confirmed-details');
  const photo=document.getElementById('review-photo');
  const photoFeedback=document.getElementById('review-photo-feedback');
  const photoPreview=document.getElementById('review-photo-preview');
  const thumbnail=document.getElementById('review-photo-thumbnail');
  const photoName=document.getElementById('review-photo-name');
  const error=document.getElementById('review-form-error');
  const radios=[...form.querySelectorAll('input[name="nota"]')];
  let selectedPhoto=null;
  let objectURL=null;
  let submitted=false;
  let status=window.CafeAuroraPedido?.getStatus?.()||'RECEBIDO';

  function accessible(panel,enabled) {
    panel.inert=!enabled;
    panel.setAttribute('aria-hidden',String(!enabled));
  }
  function view(state) {
    layout.classList.toggle('is-details',state==='details');
    layout.classList.toggle('is-review',state==='review');
    layout.classList.toggle('is-thanks',state==='thanks');
    accessible(summary,state==='details');
    accessible(review,state==='review');
    accessible(thanks,state==='thanks');
    detailsButton.setAttribute('aria-expanded',String(state==='details'));
    detailsButton.querySelector('span').textContent=state==='details'?'Ocultar detalhes':'Ver detalhes';
    if (matchMedia('(max-width:800px)').matches && (state==='review'||state==='thanks')) {
      const panel=state==='review'?review:thanks;
      requestAnimationFrame(()=>panel.scrollIntoView({behavior:matchMedia('(prefers-reduced-motion: reduce)').matches?'auto':'smooth',block:'nearest'}));
    }
  }
  function clearPhoto() {
    selectedPhoto=null;
    photo.value='';
    if(objectURL)URL.revokeObjectURL(objectURL);
    objectURL=null;
    thumbnail.removeAttribute('src');
    photoName.textContent='';
    photoPreview.hidden=true;
    photoFeedback.textContent='';
  }
  function starPaint() {
    const value=Number(form.querySelector('input[name="nota"]:checked')?.value||0);
    radios.forEach(radio=>{
      radio.parentElement.classList.toggle('is-filled',Number(radio.value)<=value);
      radio.nextElementSibling.textContent=Number(radio.value)<=value?'★':'☆';
    });
  }
  radios.forEach(radio=>radio.addEventListener('change',starPaint));
  starPaint();

  photo.addEventListener('change',()=>{
    clearPhotoIfNeeded();
    const list=photo.files;
    if(!list?.length)return;
    if(list.length!==1){photoFeedback.textContent='Selecione somente uma foto.';photo.value='';return;}
    const file=list[0];
    if(!['image/jpeg','image/png','image/webp'].includes(file.type)) {
      photoFeedback.textContent='Formato inválido. Use JPG, PNG ou WebP.';photo.value='';return;
    }
    if(file.size>5*1024*1024){
      photoFeedback.textContent='A foto deve ter no máximo 5 MB.';photo.value='';return;
    }
    selectedPhoto=file;
    objectURL=URL.createObjectURL(file);
    thumbnail.src=objectURL;
    photoName.textContent=file.name;
    photoPreview.hidden=false;
    photoFeedback.textContent='Foto selecionada. Apenas uma imagem pode ser enviada.';
  });
  function clearPhotoIfNeeded(){
    if(objectURL)URL.revokeObjectURL(objectURL);
    objectURL=null;selectedPhoto=null;photoPreview.hidden=true;thumbnail.removeAttribute('src');
    photoFeedback.textContent='';
  }
  document.getElementById('review-photo-remove').addEventListener('click',clearPhoto);
  document.getElementById('review-cancel').addEventListener('click',()=>view('cup'));

  document.addEventListener('cafeaurora:toggle-details',()=>{
    const state=layout.classList.contains('is-details')?'cup':'details';
    view(state);
  });
  document.addEventListener('cafeaurora:open-review',()=>{
    status=window.CafeAuroraPedido?.getStatus?.()||status;
    if(status!=='CONCLUIDO'||submitted)return;
    error.hidden=true;
    view('review');
    setTimeout(()=>document.getElementById('review-customer-name').focus({preventScroll:true}),100);
  });
  document.addEventListener('cafeaurora:review-status',event=>{
    status=event.detail?.status||status;
    if(status!=='CONCLUIDO'&&layout.classList.contains('is-review'))view('cup');
  });

  form.addEventListener('submit',event=>{
    event.preventDefault();
    error.hidden=true;
    if(status!=='CONCLUIDO'){error.textContent='A avaliação só é permitida após a entrega.';error.hidden=false;return;}
    if(submitted)return;
    const name=document.getElementById('review-customer-name');
    const comment=document.getElementById('review-comment');
    const stars=form.querySelector('input[name="nota"]:checked');
    const nameValue=name.value.trim(),commentValue=comment.value.trim();
    if(nameValue.length<2){error.textContent='Informe um nome com pelo menos 2 caracteres.';error.hidden=false;name.focus();return;}
    if(!stars){error.textContent='Selecione de 1 a 5 estrelas.';error.hidden=false;radios[0].focus();return;}
    if(commentValue.length<5){error.textContent='Escreva um comentário com pelo menos 5 caracteres.';error.hidden=false;comment.focus();return;}
    if(photo.files.length>1||(photo.files.length>0&&selectedPhoto!==photo.files[0])){
      error.textContent='Selecione apenas uma foto válida de até 5 MB.';error.hidden=false;return;
    }
    // Não enviar nem armazenar dados pessoais ou arquivo até existir API autenticada.
    submitted=true;
    document.getElementById('review-order-button').disabled=true;
    clearPhoto();
    form.reset();
    starPaint();
    view('thanks');
  });
})();
