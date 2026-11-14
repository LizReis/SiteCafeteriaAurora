/* Etapa 5: somente H2 temporário, credenciais aleatórias e navegador real. */
const {chromium}=require('playwright');
const {spawn}=require('node:child_process');
const fs=require('node:fs'),os=require('node:os'),path=require('node:path'),assert=require('node:assert/strict');
const dir=fs.mkdtempSync(path.join(os.tmpdir(),'aurora-avaliacoes-'));
const password=require('node:crypto').randomBytes(32).toString('hex');
const base='http://127.0.0.1:18083',root=path.resolve(__dirname,'..');
const auth='Basic '+Buffer.from('admin:'+password).toString('base64');
let server,browser;
// O build pode substituir o JAR original; cada execução usa uma cópia imutável.
const appJar=path.join(dir,'app.jar');
fs.copyFileSync(path.join(root,'target/cafe-aurora-0.0.1-SNAPSHOT.jar'),appJar);
async function start(){
 const log=fs.openSync(path.join(dir,'server.log'),'a');
 server=spawn(process.env.JAVA_HOME+'/bin/java',['-jar',appJar,'--server.port=18083','--server.address=127.0.0.1','--spring.profiles.active=dev','--spring.datasource.url=jdbc:h2:file:'+path.join(dir,'avaliacoes')],{cwd:root,env:{...process.env,CAFE_ADMIN_PASSWORD:password},stdio:['ignore',log,log]});
 for(let i=0;i<160;i++){
  if(server.exitCode!==null)throw new Error('Falha ao iniciar: '+dir);
  try{if((await fetch(base+'/api/produtos')).ok)return;}catch(_){}
  await new Promise(r=>setTimeout(r,250));
 }throw new Error('Timeout: '+dir);
}
async function stop(){if(server&&server.exitCode===null)await new Promise(r=>{server.once('exit',r);server.kill('SIGTERM');});}
(async()=>{
 await start();browser=await chromium.launch({headless:true,executablePath:process.env.CHROMIUM_PATH||'/usr/bin/chromium',args:['--no-sandbox']});
 const context=await browser.newContext({reducedMotion:'reduce'}),page=await context.newPage();
 const adminContext=await browser.newContext({reducedMotion:'reduce'}),admin=await adminContext.newPage();
 const errors=[];page.on('pageerror',e=>errors.push(e.message));admin.on('pageerror',e=>errors.push(e.message));
 async function publicPost(url,body){
  const token=await page.locator('meta[name="_csrf"]').getAttribute('content');
  const r=await page.request.post(base+url,{headers:{'X-CSRF-TOKEN':token},data:body});
  assert(r.ok(),url+' '+r.status());return r.json();
 }
 async function create(){
  await page.goto(base+'/pedido-confirmado');
  const products=await (await fetch(base+'/api/produtos')).json(),itens=[{produtoId:products[0].id,quantidade:1}];
  const quote=await publicPost('/api/pedidos/cotacao',{tipoAtendimento:'RETIRADA',itens});
  const token=await page.locator('meta[name="_csrf"]').getAttribute('content');
  const r=await page.request.post(base+'/api/pedidos',{headers:{'X-CSRF-TOKEN':token,'Idempotency-Key':require('node:crypto').randomUUID()},data:{nomeCliente:'Comprador privado',telefoneCliente:'11999999999',emailCliente:'privado@example.com',tipoAtendimento:'RETIRADA',formaPagamento:'DINHEIRO',itens,cotacao:quote.cotacao}});
  assert.equal(r.status(),201);return r.json();
 }
 async function adminLogin(){await admin.goto(base+'/admin/login');await admin.locator('[name="username"]').fill('admin');await admin.locator('[name="password"]').fill(password);await admin.locator('button[type="submit"]').click();await admin.waitForURL('**/admin');}
 await adminLogin();
 async function concluir(order){
  await admin.goto(base+'/admin/pedidos/'+order.id);
  for(const status of ['EM_PREPARO','PRONTO','CONCLUIDO']){
   await admin.locator('select[name="status"]').selectOption(status);
   await admin.locator('form[action$="/status"] button').click();await admin.waitForLoadState('domcontentloaded');
  }
 }
 async function moderar(nome,acao){
  await admin.goto(base+'/admin/avaliacoes');const row=admin.locator('tbody tr').filter({hasText:nome});
  await row.locator('form[action$="/'+acao+'"] button').click();await admin.waitForLoadState('domcontentloaded');
 }
 async function carousel(n){
  await page.goto(base+'/');await page.locator('#avaliacoes').scrollIntoViewIfNeeded();
  assert.equal(await page.locator('.polaroid-card').count(),n);assert.equal(await page.locator('.carousel-dots .dot').count(),n>1?n:0);
  if(n){assert.equal(await page.locator('.center-card').count(),1);assert.equal(new Set(await page.locator('.polaroid-card h4').allTextContents()).size,n);}
  if(n>1){
   const before=await page.locator('.center-card').getAttribute('data-review-index');await page.locator('.next-btn').click();
   assert.notEqual(await page.locator('.center-card').getAttribute('data-review-index'),before);
   await page.locator('.prev-btn').click();assert.equal(await page.locator('.center-card').getAttribute('data-review-index'),before);
   for(const dot of await page.locator('.carousel-dots .dot').all()){
    await dot.click();assert.equal(await dot.getAttribute('data-review-index'),await page.locator('.center-card').getAttribute('data-review-index'));
   }
  } else assert.equal(await page.locator('.carousel-btn').count(),0);
  await page.setViewportSize({width:390,height:844});
  assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
  if(n)assert(await page.locator('.center-card').isVisible());
  await page.screenshot({path:path.join(dir,'carousel-'+n+'-mobile.png'),fullPage:true});
  await page.setViewportSize({width:1366,height:900});
  console.log('Carrossel OK:',n,'avaliações reais, navegação e mobile');
 }
 await carousel(0);
 const orders=[];
 for(let i=0;i<7;i++){
  const order=await create();orders.push(order);
  await page.goto(base+'/pedido-confirmado#pedido='+order.referencia);await page.reload();
  await page.waitForFunction(()=>window.CafeAuroraPedido?.getStatus()==='RECEBIDO');assert(await page.locator('#review-order-button').isDisabled());
  await concluir(order);await page.reload();await page.waitForFunction(()=>window.CafeAuroraPedido?.getStatus()==='CONCLUIDO');
  await page.locator('#review-order-button').click();
  // Aguarde o foco programado pelo painel antes de preencher: ele não deve interromper o fill do comentário.
  await page.waitForFunction(()=>document.activeElement===document.getElementById('review-customer-name'));
  await page.locator('#review-customer-name').fill('Autor fictício '+i);await page.locator('#review-comment').fill('Uma experiência excelente, comentário '+i);
  assert.equal(await page.locator('#review-customer-name').inputValue(),'Autor fictício '+i);
  assert.equal(await page.locator('#review-comment').inputValue(),'Uma experiência excelente, comentário '+i);
  await page.locator('.review-stars-choice label').last().click();await page.locator('#review-consent').check();
  if(i===1)await page.locator('#review-photo').setInputFiles({name:'../../cliente.png',mimeType:'image/png',buffer:Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAFklEQVR4nGMMqHBjYGBgYmBgYGBgAAAM3AEScAD53QAAAABJRU5ErkJggg==','base64')});
  if(i<2){await page.setViewportSize({width:i===1?390:1366,height:i===1?844:900});assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));await page.screenshot({path:path.join(dir,'formulario-'+i+'.png'),fullPage:true});}
  if(i===0){
   await page.route('**/api/avaliacoes',r=>r.fulfill({status:500,contentType:'application/json',body:'{"mensagem":"Falha simulada"}'}),{times:1});
   await page.locator('#review-submit').click();await page.locator('#review-form-error').waitFor({state:'visible'});
   assert.equal(await page.locator('#review-comment').inputValue(),'Uma experiência excelente, comentário '+i);
   assert(!(await page.locator('.confirmed-layout').getAttribute('class')).includes('is-thanks'));
  }
  if(i===2)await page.route('**/api/avaliacoes',async route=>{const r=await route.fetch();assert.equal(r.status(),201);await route.abort();},{times:1});
  await page.locator('#review-submit').click();await page.waitForFunction(()=>document.querySelector('.confirmed-layout').classList.contains('is-thanks'));
  await page.reload();await page.waitForFunction(()=>window.CafeAuroraPedido?.jaAvaliado());assert(await page.locator('#review-order-button').isDisabled());
  await page.setViewportSize({width:1366,height:900});await page.goto(base+'/');assert(!(await page.content()).includes('Autor fictício '+i));
  await moderar('Autor fictício '+i,'aprovar');await carousel(Math.min(i+1,5));
 }
 await admin.setViewportSize({width:390,height:844});await admin.goto(base+'/admin/avaliacoes');assert(await admin.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));await admin.screenshot({path:path.join(dir,'moderacao-mobile.png'),fullPage:true});await admin.setViewportSize({width:1366,height:900});
 await moderar('Autor fictício 6','rejeitar');await page.goto(base+'/');assert(!(await page.content()).includes('Autor fictício 6'));
 // Teste real do limite HTTP multipart (o MockMvc não executa o parser do Tomcat).
 await page.goto(base+'/pedido-confirmado');const csrf=await page.locator('meta[name="_csrf"]').getAttribute('content');
 const oversized=await page.request.post(base+'/api/avaliacoes',{headers:{'X-CSRF-TOKEN':csrf},multipart:{referencia:orders[0].referencia,nomeAutor:'Autor',nota:'5',comentario:'Comentário teste',consentimentoPublicacao:'true',foto:{name:'grande.png',mimeType:'image/png',buffer:Buffer.alloc(5*1024*1024+1)}}});
 assert.equal(oversized.status(),413);assert((await oversized.json()).mensagem.includes('5 MB'));
 await stop();await start();
 const state=await (await fetch(base+'/api/pedidos/consulta/'+orders[0].referencia)).json();assert.equal(state.jaAvaliado,true);
 await page.goto(base+'/pedido-confirmado');const renewed=await page.locator('meta[name="_csrf"]').getAttribute('content');
 const duplicate=await page.request.post(base+'/api/avaliacoes',{headers:{'X-CSRF-TOKEN':renewed},multipart:{referencia:orders[0].referencia,nomeAutor:'Outro autor',nota:'4',comentario:'Novo comentário',consentimentoPublicacao:'true'}});assert.equal(duplicate.status(),409);
 await page.goto(base+'/');assert.equal(await page.locator('.polaroid-card').count(),5);
 const photoUrl=await page.locator('.polaroid-card .main-photo[src^="/api/avaliacoes/"]').getAttribute('src');
 const stored=await page.request.get(base+photoUrl);assert.equal(stored.status(),200);assert(stored.headers()['content-type'].includes('image/jpeg'));assert((await stored.body()).length>0);
 await adminLogin();await moderar('Autor fictício 1','rejeitar');assert.equal((await page.request.get(base+photoUrl)).status(),404);
 assert.equal(errors.length,0,errors.join('\n'));
 console.log('E2E OK: antes/depois da conclusão, foto e sem foto, falha preserva formulário, resposta perdida, moderação, limite HTTP e reinício. Evidências:',dir);
})().catch(e=>{console.error(String(e).split('\n')[0].replace(/[A-Za-z0-9_-]{43}/g,'[redigido]'));process.exitCode=1;}).finally(async()=>{if(browser)await browser.close();await stop();});
