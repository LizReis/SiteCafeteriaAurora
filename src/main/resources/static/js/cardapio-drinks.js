// Animações e botões de quantidade do cardápio de drinks (demonstração visual).
(() => {
  const page=document.querySelector('.drink-page');
  if(!page)return;
  function initialize(){
    page.querySelectorAll('.drink-product').forEach(card=>{
      const output=card.querySelector('output'),minus=card.querySelector('.drink-minus'),plus=card.querySelector('.drink-plus');
      let number=0;
      const update=()=>{output.textContent=String(number);output.value=String(number);minus.disabled=number===0};
      minus.addEventListener('click',()=>{number=Math.max(0,number-1);update()});
      plus.addEventListener('click',()=>{number=Math.min(99,number+1);update()});
      update();
    });
    const media=window.matchMedia('(prefers-reduced-motion: reduce)');
    const categories=[...page.querySelectorAll('.drink-category')];
    if(media.matches||!('IntersectionObserver' in window))return;
    page.classList.add('drink-reveal-enabled');
    let previousReveal=0;
    const observer=new IntersectionObserver(entries=>{
      entries.forEach(entry=>{
        if(!entry.isIntersecting)return;
        observer.unobserve(entry.target);
        const i=categories.indexOf(entry.target);
        const delay=i===0?Math.max(0,3300-performance.now()):Math.max(0,previousReveal+1650-performance.now());
        const at=performance.now()+delay;previousReveal=at;
        setTimeout(()=>entry.target.classList.add('is-revealed'),delay);
      });
    },{threshold:.05,rootMargin:'0px 0px -15px 0px'});
    categories.forEach(c=>observer.observe(c));
    media.addEventListener?.('change',e=>{if(e.matches){observer.disconnect();page.classList.remove('drink-reveal-enabled')}});
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',initialize,{once:true});
  else initialize();
})();
