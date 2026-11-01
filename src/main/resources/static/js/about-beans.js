/* Grãos reais do pacote enviado: sprite-sheet WebP transparente, sem imagem estática de base.
 * Física e lançamento individual inspirados na demonstração enviada.
 * Mantido exclusivamente na seção Sobre Nós.
 */
(() => {
  'use strict';
  const section = document.getElementById('sobre');
  const canvas = document.getElementById('about-beans-canvas');
  if (!section || !canvas) return;
  const ctx = canvas.getContext('2d', {alpha:true});
  if (!ctx) return;

  const sprite = new Image();
  sprite.src = '/img/graos-aurora-sprites.webp';
  const regions = [[3,6,46,39],[57,3,42,46]];
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
  const particles = [];
  const random = (a,b) => a+Math.random()*(b-a);
  let width = 0, height = 0, density = 1, visible = false;
  let last = 0, accumulated = 0;

  function resize() {
    const rect = section.getBoundingClientRect();
    width = Math.max(1,rect.width); height = Math.max(1,rect.height);
    const dpr = Math.min(devicePixelRatio || 1,2);
    canvas.width = Math.round(width*dpr);
    canvas.height = Math.round(height*dpr);
    ctx.setTransform(dpr,0,0,dpr,0,0);
    density = Math.min(1,Math.max(.55,width/1200));
  }

  function emit() {
    if (particles.length >= 27) return;
    const index = Math.floor(Math.random()*regions.length);
    particles.push({
      index,
      x: random(-34, 48),
      y: height + random(-25,85),
      vx: random(115,245) * density,
      vy: -random(370,570)*Math.sqrt(Math.min(1.3,height/700)),
      gravity: random(145,215),
      angle: random(-3,3),
      spin: random(-2.6,2.6),
      age: 0,
      duration: random(3.0,4.25),
      size: random(26,68),
      alpha: random(.53,.88),
      wobble: random(3,12),
      phase: random(0,Math.PI*2)
    });
  }

  function frame(time) {
    requestAnimationFrame(frame);
    const dt = Math.min(.05, last ? (time-last)/1000 : 0);
    last = time;
    if (!visible || document.hidden || reducedMotion.matches) return;
    ctx.clearRect(0,0,width,height);
    if (!sprite.complete || !sprite.naturalWidth) return;
    accumulated += dt*4.2*density;
    while (accumulated>=1) {emit(); accumulated-=1;}
    for (let i=particles.length-1;i>=0;i--) {
      const p=particles[i];
      p.age+=dt;
      if(p.age>=p.duration){particles.splice(i,1);continue;}
      const t=p.age;
      const x=p.x+p.vx*t+Math.sin(t*2+p.phase)*p.wobble;
      const y=p.y+p.vy*t+.5*p.gravity*t*t;
      const progress=t/p.duration;
      const fade=Math.min(1,t/.33,(1-progress)/.28);
      const region=regions[p.index];
      const ratio=region[2]/region[3];
      ctx.save();
      ctx.globalAlpha=Math.max(0,p.alpha*fade);
      ctx.translate(x,y);
      ctx.rotate(p.angle+p.spin*t);
      ctx.drawImage(sprite,...region,-p.size*ratio/2,-p.size/2,p.size*ratio,p.size);
      ctx.restore();
    }
  }
  resize();
  if ('ResizeObserver' in window) new ResizeObserver(resize).observe(section);
  else window.addEventListener('resize',resize);
  if ('IntersectionObserver' in window) new IntersectionObserver(entries=>{
    visible=entries.some(x=>x.isIntersecting);
  },{threshold:0}).observe(section);
  else visible=true;
  requestAnimationFrame(frame);
})();