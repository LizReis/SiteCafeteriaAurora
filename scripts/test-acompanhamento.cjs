/* Etapa 6: JAR, H2 e credenciais exclusivamente temporários. */
const {chromium}=require('playwright');
const {spawn}=require('node:child_process');
const fs=require('node:fs'),os=require('node:os'),path=require('node:path'),assert=require('node:assert/strict');
const dir=fs.mkdtempSync(path.join(os.tmpdir(),'aurora-acompanhamento-'));
const root=path.resolve(__dirname,'..'),base='http://127.0.0.1:18084';
const password=require('node:crypto').randomBytes(32).toString('hex');
const jar=path.join(dir,'app.jar');fs.copyFileSync(path.join(root,'target/cafe-aurora-0.0.1-SNAPSHOT.jar'),jar);
let server,browser,checks=0;
const check=(value,message)=>{assert(value,message);checks++;};
async function start(){
 const log=fs.openSync(path.join(dir,'server.log'),'a');
 server=spawn(process.env.JAVA_HOME+'/bin/java',['-jar',jar,'--server.address=127.0.0.1','--server.port=18084','--spring.profiles.active=dev','--spring.datasource.url=jdbc:h2:file:'+path.join(dir,'db')],{cwd:root,env:{...process.env,CAFE_ADMIN_PASSWORD:password},stdio:['ignore',log,log]});fs.closeSync(log);
 for(let i=0;i<160;i++){if(server.exitCode!==null)throw Error('Server exited');try{if((await fetch(base+'/api/produtos')).ok)return;}catch{}await new Promise(r=>setTimeout(r,250));}throw Error('Startup timeout');
}
async function stop(){if(server&&server.exitCode===null)await new Promise(r=>{server.once('exit',r);server.kill('SIGTERM');});}
(async()=>{
 await start();browser=await chromium.launch({headless:true,executablePath:process.env.CHROMIUM_PATH||'/usr/bin/chromium',args:['--no-sandbox']});
 const context=await browser.newContext({reducedMotion:'reduce'}),page=await context.newPage(),admin=await context.newPage();
 const errors=[];page.on('pageerror',e=>errors.push(e.message));
 await admin.goto(base+'/admin/login');await admin.locator('[name=username]').fill('admin');await admin.locator('[name=password]').fill(password);await admin.locator('button[type=submit]').click();await admin.waitForURL('**/admin');
 async function create(tipo){
  await page.goto(base+'/pedido-confirmado');
  const token=await page.locator('meta[name=_csrf]').getAttribute('content');
  const products=await (await fetch(base+'/api/produtos')).json();const itens=[{produtoId:products[0].id,quantidade:1}];
  const headers={'X-CSRF-TOKEN':token};
  const quote=await (await page.request.post(base+'/api/pedidos/cotacao',{headers,data:{tipoAtendimento:tipo,itens}})).json();
  const body={nomeCliente:'Teste etapa seis',telefoneCliente:'11999999999',emailCliente:'teste@example.com',tipoAtendimento:tipo,formaPagamento:'DINHEIRO',itens,cotacao:quote.cotacao};
  if(tipo==='ENTREGA')body.endereco={cep:'01310000',logradouro:'Rua teste',numero:'1',bairro:'Teste'};
  const r=await page.request.post(base+'/api/pedidos',{headers:{...headers,'Idempotency-Key':require('node:crypto').randomUUID()},data:body});check(r.status()===201,'Create '+r.status());return r.json();
 }
 async function status(p,s){await admin.goto(base+'/admin/pedidos/'+p.id);await admin.locator('select[name=status]').selectOption(s);await admin.locator('form[action$="/status"] button').click();await admin.waitForLoadState('domcontentloaded');check(await admin.locator('.admin-alert.success').count()===1,'Admin transition '+s);}
 async function tick(s){await page.clock.runFor(15000);await page.waitForFunction(s=>window.CafeAuroraPedido.getStatus()===s,s);}
 const delivery=await create('ENTREGA');await page.clock.install();let count=0;
 page.on('request',r=>{if(r.url().includes('/api/pedidos/consulta/'))count++;});
 await page.goto(base+'/pedido-confirmado?view='+Math.random()+'#pedido='+delivery.referencia);await page.waitForFunction(()=>window.CafeAuroraPedido.getStatus()==='RECEBIDO');check(count===1,'Immediate lookup');
 // No DOM mutations on unchanged responses, including the summary.
 await page.evaluate(()=>{window.mutations=0;new MutationObserver(m=>window.mutations+=m.length).observe(document.querySelector('.confirmed-layout'),{subtree:true,childList:true,attributes:true,characterData:true});});
 await page.clock.runFor(15000);await page.waitForFunction(()=>window.CafeAuroraPedido.getStatus()==='RECEBIDO');await page.waitForTimeout(200);
 check(await page.evaluate(()=>window.mutations)===0,'Unchanged response does not render');
 await page.locator('#toggle-confirmed-details').click();await page.clock.runFor(1000);const layout=await page.locator('.confirmed-layout').getAttribute('class');
 await status(delivery,'EM_PREPARO');await tick('EM_PREPARO');check(await page.locator('.confirmed-layout').getAttribute('class')===layout,'Summary stays open');
 await status(delivery,'PRONTO');await tick('PRONTO');check(!(await page.locator('[data-step="2"]').getAttribute('class')).includes('is-current'),'Motorcycle inactive PRONTO');check((await page.locator('#current-status-label').textContent()).includes('aguardando'),'Ready copy');
 await status(delivery,'EM_ENTREGA');await tick('EM_ENTREGA');check((await page.locator('[data-step="2"]').getAttribute('class')).includes('is-current'),'Motorcycle active');check(await page.locator('#review-order-button').isDisabled(),'Review blocked');
 // Close details with existing animation before reviewing later.
 await page.locator('#toggle-confirmed-details').click();await page.clock.runFor(1000);
 await status(delivery,'CONCLUIDO');await tick('CONCLUIDO');check(!(await page.locator('#review-order-button').isDisabled()),'Review enabled');
 let before=count;await page.clock.runFor(60000);check(count===before,'Terminal stops polling');
 await page.locator('#review-order-button').click();await page.clock.runFor(1000);
 await page.locator('#review-customer-name').fill('Autor etapa seis');await page.locator('#review-comment').fill('Pedido entregue corretamente');await page.locator('.review-stars-choice label').last().click();await page.locator('#review-consent').check();
 await page.locator('#review-photo').setInputFiles({name:'foto.png',mimeType:'image/png',buffer:Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAFklEQVR4nGMMqHBjYGBgYmBgYGBgAAAM3AEScAD53QAAAABJRU5ErkJggg==','base64')});
 await page.locator('#review-comment').focus();
 await page.evaluate(()=>window.CafeAuroraPedido.refresh());
 check(await page.locator('#review-photo').evaluate(e=>e.files.length)===1,'Refresh preserves selected photo');
 check(await page.locator('#review-comment').evaluate(e=>e===document.activeElement),'Refresh preserves focus');
 check(await page.locator('#review-comment').inputValue()==='Pedido entregue corretamente','Refresh preserves review fields');
 await page.locator('#review-submit').click();await page.waitForFunction(()=>document.querySelector('.confirmed-layout').classList.contains('is-thanks'));
 await page.reload();await page.waitForFunction(()=>window.CafeAuroraPedido.jaAvaliado());check(await page.locator('#review-order-button').isDisabled(),'Duplicate blocked after reload');
 await admin.goto(base+'/admin/pedidos/'+delivery.id);check(await admin.locator('.order-status-history li').count()===5,'5 persisted events');check((await admin.locator('.order-status-history').textContent()).includes('admin'),'Authenticated actor');
 await stop();await start();await admin.goto(base+'/admin/pedidos/'+delivery.id); // auth session was lost on restart
 await admin.goto(base+'/admin/login');await admin.locator('[name=username]').fill('admin');await admin.locator('[name=password]').fill(password);await admin.locator('button[type=submit]').click();await admin.waitForURL('**/admin');await admin.goto(base+'/admin/pedidos/'+delivery.id);
 check(await admin.locator('.order-status-history li').count()===5,'History survives restart');await page.reload();await page.waitForFunction(()=>window.CafeAuroraPedido.jaAvaliado());check(await page.evaluate(()=>window.CafeAuroraPedido.getStatus())==='CONCLUIDO','Status survives restart');
 const pickup=await create('RETIRADA');await page.goto(base+'/pedido-confirmado?view='+Math.random()+'#pedido='+pickup.referencia);await page.waitForFunction(()=>window.CafeAuroraPedido.getStatus()==='RECEBIDO');
 await status(pickup,'EM_PREPARO');await tick('EM_PREPARO');await status(pickup,'PRONTO');await tick('PRONTO');check((await page.locator('#current-status-label').textContent()).includes('retirada'),'Pickup ready');check((await page.locator('[data-step="2"] .sr-only').textContent()).includes('retirada'),'Pickup icon label');
 await admin.goto(base+'/admin/pedidos/'+pickup.id);check(await admin.locator('option[value=EM_ENTREGA]').count()===0,'No delivery action for pickup');
 const csrf=await admin.locator('input[name=_csrf]').first().getAttribute('value');
 const bad=await admin.request.post(base+'/admin/pedidos/'+pickup.id+'/status',{form:{_csrf:csrf,status:'EM_ENTREGA'}});check(bad.ok(),'Controlled rejection');check((await (await page.request.get(base+'/api/pedidos/consulta/'+pickup.referencia)).json()).status==='PRONTO','Manipulated pickup rejected');
 await status(pickup,'CONCLUIDO');await tick('CONCLUIDO');check((await page.locator('#current-status-label').textContent()).includes('Retirada'),'Pickup complete');check(!(await page.locator('#review-order-button').isDisabled()),'Pickup enables review');await admin.goto(base+'/admin/pedidos/'+pickup.id);check(await admin.locator('.order-status-history li').count()===4,'Pickup history');
 const cancel=await create('RETIRADA');await page.goto(base+'/pedido-confirmado?view='+Math.random()+'#pedido='+cancel.referencia);await page.waitForFunction(()=>window.CafeAuroraPedido.getStatus()==='RECEBIDO');await status(cancel,'CANCELADO');await tick('CANCELADO');before=count;await page.clock.runFor(60000);check(count===before,'Cancel stops polling');check(await page.locator('#review-order-button').isDisabled(),'Cancel blocks review');
 await admin.goto(base+'/admin/pedidos/'+cancel.id);check(await admin.locator('.order-status-history li').count()===2,'Cancel history');
 const cancelToken=await admin.locator('input[name=_csrf]').first().getAttribute('value');
 await admin.request.post(base+'/admin/pedidos/'+cancel.id+'/status',{form:{_csrf:cancelToken,status:'EM_PREPARO'}});
 check((await (await page.request.get(base+'/api/pedidos/consulta/'+cancel.referencia)).json()).status==='CANCELADO','Cannot reopen cancelled order');
 // Deterministic transport/lifecycle tests with the production 15-second clock.
 const active=await create('ENTREGA');await page.goto(base+'/pedido-confirmado?view='+Math.random()+'#pedido='+active.referencia);await page.waitForFunction(()=>window.CafeAuroraPedido.getStatus()==='RECEBIDO');
 await page.evaluate(()=>{Object.defineProperty(document,'visibilityState',{configurable:true,value:'hidden'});document.dispatchEvent(new Event('visibilitychange'));});before=count;await page.clock.runFor(60000);check(count===before,'Hidden pauses');
 await status(active,'EM_PREPARO');await page.evaluate(()=>{Object.defineProperty(document,'visibilityState',{configurable:true,value:'visible'});document.dispatchEvent(new Event('visibilitychange'));});await page.waitForFunction(()=>window.CafeAuroraPedido.getStatus()==='EM_PREPARO');check(count===before+1,'Visible refreshes immediately');
 for(const mode of ['network','http','json']){
  await page.route('**/api/pedidos/consulta/*',r=>mode==='network'?r.abort():r.fulfill({status:mode==='http'?503:200,contentType:'application/json',body:mode==='http'?'{}':'broken'}),{times:1});
  await page.evaluate(()=>window.CafeAuroraPedido.refresh());check(await page.evaluate(()=>window.CafeAuroraPedido.getStatus())==='EM_PREPARO','Retain status '+mode);check(await page.locator('#tracking-update-warning').isVisible(),'Warning '+mode);
  await page.evaluate(()=>window.CafeAuroraPedido.refresh());check(await page.locator('#tracking-update-warning').isHidden(),'Recover '+mode);
 }
 let release;const gate=new Promise(r=>release=r);let overlapping=0;
 await page.route('**/api/pedidos/consulta/*',async r=>{overlapping++;await gate;await r.continue();},{times:1});
 await page.evaluate(()=>{window.p1=window.CafeAuroraPedido.refresh();window.p2=window.CafeAuroraPedido.refresh();});await page.waitForTimeout(100);await page.evaluate(()=>{for(let i=0;i<3;i++)document.dispatchEvent(new Event('visibilitychange'));});
 check(overlapping===1,'No overlapping refresh');release();await page.evaluate(()=>window.p1);await page.unroute('**/api/pedidos/consulta/*');
 before=count;await page.clock.runFor(15000);await page.waitForTimeout(200);check(count===before+1,'Only one polling timer after repeated refreshes');
 for(const width of [1366,768,390]){await page.setViewportSize({width,height:900});check(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'Responsive '+width);await page.screenshot({path:path.join(dir,'tracking-'+width+'.png')});await admin.setViewportSize({width,height:900});await admin.goto(base+'/admin/pedidos/'+delivery.id);check(await admin.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'History responsive '+width);}
 await page.goto(base+'/');before=count;await page.clock.runFor(60000);check(count===before,'Navigation stops polling');
 await page.goto(base+'/pedido-confirmado?view=invalid#pedido=invalid');before=count;await page.clock.runFor(60000);check(count===before,'Invalid reference never fetched');
 await page.goto(base+'/pedido-confirmado?view='+Math.random()+'#pedido='+'x'.repeat(43));await page.locator('#tracking-update-warning').waitFor();before=count;await page.clock.runFor(60000);check(count===before,'404 stops retries');
 check(errors.length===0,errors.join('\n'));console.log('E2E Etapa 6 OK:',checks,'assertions. Evidence:',dir);
})().catch(e=>{console.error(e.stack);process.exitCode=1;}).finally(async()=>{if(browser)await browser.close();await stop();});
