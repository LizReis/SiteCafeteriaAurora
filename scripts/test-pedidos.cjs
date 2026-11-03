/* E2E Etapa 2: Java 21 + Playwright/Chromium. Somente bancos temporários;
 * LEGACY_H2_FILE opcional deve apontar para um arquivo fechado (será copiado).
 */
const {chromium}=require('playwright');
const {spawn,execFileSync}=require('node:child_process');
const fs=require('node:fs'),os=require('node:os'),path=require('node:path');
const assert=require('node:assert/strict');
const root=path.resolve(__dirname,'..'),dir=fs.mkdtempSync(path.join(os.tmpdir(),'aurora-pedidos-'));
const base='http://127.0.0.1:18082',password=require('node:crypto').randomBytes(32).toString('hex');
const auth='Basic '+Buffer.from('admin:'+password).toString('base64');
let server,browser;
async function api(url,method='GET',body){const r=await fetch(base+url,{method,headers:{Authorization:auth,'Content-Type':'application/json'},body:body?JSON.stringify(body):undefined});assert(r.ok,`${url} ${r.status}`);return r.status===204?null:r.json();}
async function start(db){
 const log=fs.openSync(path.join(dir,'server.log'),'a');
 server=spawn(process.env.JAVA_HOME+'/bin/java',['-jar','target/cafe-aurora-0.0.1-SNAPSHOT.jar','--spring.profiles.active=dev','--server.port=18082','--spring.datasource.url=jdbc:h2:file:'+db],{cwd:root,env:{...process.env,CAFE_ADMIN_PASSWORD:password},stdio:['ignore',log,log]});
 for(let i=0;i<120;i++){if(server.exitCode!==null)throw new Error('Falha ao iniciar. Log: '+dir);try{if((await api('/api/categorias')).length)return;}catch(_){} await new Promise(r=>setTimeout(r,250));}throw new Error('Timeout de inicialização');
}
async function stop(){if(server&&server.exitCode===null)await new Promise(r=>{server.once('exit',r);server.kill('SIGTERM');});}
async function fill(page,pickup){
 await page.locator('[name="nome"]').fill('Cliente de Teste');await page.locator('[name="telefone"]').fill('11999999999');await page.locator('[name="email"]').fill('cliente@example.com');
 if(pickup)await page.locator('#delivery-type').selectOption('retirada');
 else{for(const [name,value] of Object.entries({cep:'01310000',endereco:'Av Paulista',numero:'100',bairro:'Bela Vista'}))await page.locator(`[name="${name}"]`).fill(value);}
 await page.locator('[name="pagamento"][value="dinheiro"]').check();await page.waitForFunction(()=>!document.getElementById('confirm-order').disabled);
}
async function add(page,slug,name,quantity){await page.goto(base+'/cardapio-'+slug);const card=page.locator('[data-product-id]').filter({has:page.locator('h3',{hasText:name})}).first();await card.waitFor();for(let i=0;i<quantity;i++)await card.locator('button').last().click();assert.equal(await card.locator('output').textContent(),String(quantity));}
function registered(page,expectedStatus=201){
 // Capture the server response before delivering it to the page: a successful
 // checkout immediately navigates away, after which Chrome may discard its body.
 return new Promise((resolve,reject)=>{
  page.route('**/api/pedidos',async route=>{
   try {const response=await route.fetch();const data=await response.json();assert.equal(response.status(),expectedStatus);
    const request=route.request();await route.fulfill({response});resolve({json:async()=>data,request:()=>request});
   } catch(error){reject(error);await route.abort();}
  },{times:1}).catch(reject);
 });
}
function dbCheck(db,expected){
 const java=path.join(dir,'Check.java');fs.writeFileSync(java,`import java.sql.*;
class Check {public static void main(String[] args)throws Exception {try(var c=DriverManager.getConnection("jdbc:h2:file:"+args[0],"sa","");var s=c.createStatement()){
 try(var r=s.executeQuery("select count(*) from pedidos")){r.next();if(r.getInt(1)!=Integer.parseInt(args[1]))throw new AssertionError("Pedidos persistidos");}
 try(var r=s.executeQuery("select count(*) from itens_pedido")){r.next();if(r.getInt(1)<Integer.parseInt(args[1]))throw new AssertionError("Itens persistidos");}
 try(var r=s.executeQuery("select p.id from pedidos p left join (select pedido_id,sum(quantidade*preco_unitario) subtotal from itens_pedido group by pedido_id) i on p.id=i.pedido_id where p.subtotal<>i.subtotal or p.total<>p.subtotal+p.taxa_entrega or i.subtotal is null")){if(r.next())throw new AssertionError("Totais inconsistentes");}
 }}}`);
 const m2=process.env.MAVEN_USER_HOME||path.join(os.homedir(),'.m2');const versions=fs.readdirSync(path.join(m2,'repository/com/h2database/h2')).filter(v=>/^\d/.test(v));const v=versions.sort().at(-1);const jar=path.join(m2,'repository/com/h2database/h2',v,`h2-${v}.jar`);
 execFileSync(process.env.JAVA_HOME+'/bin/java',['--class-path',jar,java,db,String(expected)],{stdio:'pipe'});
}
function legacyFixture(db,check=false) {
 const java=path.join(dir,'Legacy.java');fs.writeFileSync(java,`import java.sql.*;
class Legacy {public static void main(String[] args)throws Exception {try(var c=DriverManager.getConnection("jdbc:h2:file:"+args[0],"sa","");var s=c.createStatement()){
 if(args[1].equals("seed")) {
 s.executeUpdate("insert into pedidos (nome_cliente,telefone_cliente,tipo_atendimento,status,forma_pagamento,status_pagamento,total,criado_em) values ('LEGADO_FIXTURE_STAGE2','11900000000','RETIRADA','RECEBIDO','DINHEIRO','PENDENTE',10.00,CURRENT_TIMESTAMP)",Statement.RETURN_GENERATED_KEYS);
 long id;try(var keys=s.getGeneratedKeys()){keys.next();id=keys.getLong(1);}
 s.executeUpdate("insert into itens_pedido (pedido_id,produto_id,nome_produto,quantidade,preco_unitario) values ("+id+",(select min(id) from produtos),'Item legado',1,10.00)");
 } else {
 try(var r=s.executeQuery("select total,subtotal,taxa_entrega,referencia_publica,email_cliente from pedidos where nome_cliente='LEGADO_FIXTURE_STAGE2'")){
 if(!r.next()||r.getBigDecimal(1).compareTo(new java.math.BigDecimal("10.00"))!=0||r.getObject(2)!=null||r.getObject(3)!=null||r.getObject(4)!=null||r.getObject(5)!=null)throw new AssertionError("Registro legado alterado");}
 try(var r=s.executeQuery("select count(*) from itens_pedido where nome_produto='Item legado' and preco_unitario=10.00")){r.next();if(r.getInt(1)!=1)throw new AssertionError("Item legado perdido");}
 }
 }}}`);
 const m2=process.env.MAVEN_USER_HOME||path.join(os.homedir(),'.m2');const v=fs.readdirSync(path.join(m2,'repository/com/h2database/h2')).filter(v=>/^\d/.test(v)).sort().at(-1);
 execFileSync(process.env.JAVA_HOME+'/bin/java',['--class-path',path.join(m2,'repository/com/h2database/h2',v,`h2-${v}.jar`),java,db,check?'check':'seed'],{stdio:'pipe'});
}
(async()=>{
 const db=path.join(dir,'pedidos');await start(db);
 browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH||'/usr/bin/chromium',headless:true,args:['--no-sandbox']});
 const context=await browser.newContext({viewport:{width:1366,height:900},reducedMotion:'reduce'}),page=await context.newPage();
 const errors=[];page.on('pageerror',e=>errors.push(e.message));
 await add(page,'cafe','Espresso Italiano',2);await add(page,'comidas','Croissant de Queijo',1);
 await page.goto(base+'/sacola-pedido');assert.equal(await page.locator('.cart-item').count(),2);assert.equal(await page.locator('.bag-count').textContent(),'3');
 await page.locator('#cart-checkout').click();await fill(page,false);
 const created=registered(page);await page.locator('#confirm-order').click();const response=await created,order=await response.json();
 const originalRequest=response.request().postDataJSON(),idem=response.request().headers()['idempotency-key'];
 assert.equal(order.total,42);assert.equal(order.subtotal,35);assert.equal(order.taxaEntrega,7);assert.equal(order.status,'RECEBIDO');assert.equal(order.statusPagamento,'PENDENTE');assert.equal(order.itens.length,2);
 await page.waitForURL('**/pedido-confirmado.html#pedido=*');await page.waitForFunction(()=>document.getElementById('current-status-label').textContent==='Pedido recebido');
 assert.equal(await page.locator('.bag-count').textContent(),'0');assert(await page.locator('#review-order-button').isDisabled());assert.equal(await page.locator('#demo-status-select').count(),0);
 await page.locator('#toggle-confirmed-details').click();assert((await page.locator('.confirmed-layout').getAttribute('class')).includes('is-details'));assert((await page.locator('#confirmed-total').textContent()).includes('42,00'));assert((await page.locator('#confirmed-address').textContent()).includes('Av Paulista'));
 await page.screenshot({path:path.join(dir,'pedido-entrega.png'),fullPage:true});const confirmation=page.url();
 await page.reload();await page.waitForFunction(()=>document.getElementById('current-status-label').textContent==='Pedido recebido');assert(await page.locator('#review-order-button').isDisabled());
 console.log('Entrega OK: menus → sacola → POST 201 → confirmação persistida, detalhes e reload; avaliação bloqueada');
 // Retry the exact successful request, then restart and retry again with a new CSRF session.
 async function retry(){await page.goto(base+'/finalizar-pedido');return page.evaluate(async({body,key})=>{const header=document.querySelector('meta[name="_csrf_header"]').content;const token=document.querySelector('meta[name="_csrf"]').content;const r=await fetch('/api/pedidos',{method:'POST',headers:{'Content-Type':'application/json',[header]:token,'Idempotency-Key':key},body:JSON.stringify(body)});return {status:r.status,body:await r.json()};},{body:originalRequest,key:idem});}
 assert.equal((await retry()).body.id,order.id);
 await add(page,'cafe','Espresso Italiano',1);await page.goto(base+'/finalizar-pedido');await fill(page,true);
 const pickupResponse=registered(page);await page.locator('#confirm-order').click();const pickup=await (await pickupResponse).json();assert.equal(pickup.taxaEntrega,0);assert.equal(pickup.total,10);await page.waitForURL('**/pedido-confirmado.html#pedido=*');
 console.log('Retirada OK: sem endereço e taxa zero');
 // Unavailable after quote: no incomplete order, cart remains and button can recover.
 await add(page,'cafe','Espresso Italiano',1);await page.goto(base+'/finalizar-pedido');await fill(page,true);
 let espresso=(await api('/api/admin/produtos')).find(p=>p.nome==='Espresso Italiano');
 const productBody={nome:espresso.nome,descricao:espresso.descricao,preco:espresso.preco,imagemUrl:espresso.imagemUrl,categoriaId:espresso.categoriaId,ativo:true,disponivel:false};
 await api('/api/admin/produtos/'+espresso.id,'PUT',productBody);
 const failed=registered(page,409);await page.locator('#confirm-order').click();await failed;await page.waitForFunction(()=>document.getElementById('checkout-feedback').textContent.includes('indisponível'));
 assert.equal(await page.evaluate(()=>window.CafeAuroraCart.getCount()),1);assert(page.url().includes('finalizar-pedido'));
 productBody.disponivel=true;await api('/api/admin/produtos/'+espresso.id,'PUT',productBody);await page.locator('#refresh-quote').click();await page.waitForFunction(()=>!document.getElementById('confirm-order').disabled);
 // Change price after quote: 409, refreshed official summary, deliberate second click.
 productBody.preco=12;await api('/api/admin/produtos/'+espresso.id,'PUT',productBody);
 const conflict=registered(page,409);await page.locator('#confirm-order').click();await conflict;await page.waitForFunction(()=>!document.getElementById('confirm-order').disabled);assert.equal(await page.evaluate(()=>window.CafeAuroraCart.getCount()),1);assert((await page.locator('#checkout-total').textContent()).includes('12,00'));
 // Server accepts, but the response is lost. Retry must reuse the persisted key.
 let accepted;
 await page.route('**/api/pedidos',async route=>{const r=await route.fetch();accepted=await r.json();assert.equal(r.status(),201);await route.abort('failed');});
 await page.locator('#confirm-order').click();await page.waitForFunction(()=>!document.getElementById('confirm-order').disabled);
 assert.equal(await page.evaluate(()=>window.CafeAuroraCart.getCount()),1);await page.unroute('**/api/pedidos');
 await page.reload();await fill(page,true);
 const retryResponse=registered(page);await page.locator('#confirm-order').click();const recovered=await (await retryResponse).json();assert.equal(recovered.id,accepted.id);assert.equal(recovered.total,12);await page.waitForURL('**/pedido-confirmado.html#pedido=*');
 console.log('Falhas OK: indisponibilidade e preço alterado preservam sacola; resposta perdida + retry não duplicam pedido');
 await stop();dbCheck(db,3);await start(db);
 const saved=await api('/api/pedidos/consulta/'+order.referencia);assert.deepEqual(saved,order);assert.equal((await retry()).body.id,order.id);
 await page.goto(confirmation);await page.waitForFunction(()=>document.getElementById('current-status-label').textContent==='Pedido recebido');assert(await page.locator('#review-order-button').isDisabled());
 await stop();dbCheck(db,3);
 console.log('H2 OK: 3 pedidos e itens conferidos por JDBC; totais, histórico e idempotência preservados após reiniciar');
 if(process.env.LEGACY_H2_FILE){const legacy=path.join(dir,'legado');fs.copyFileSync(process.env.LEGACY_H2_FILE,legacy+'.mv.db');legacyFixture(legacy);await start(legacy);const before=await api('/api/admin/produtos');assert(before.length>0);await stop();await start(legacy);assert.deepEqual(await api('/api/admin/produtos'),before);await stop();legacyFixture(legacy,true);console.log('Legado OK: pedidos/itens do esquema anterior e imagens preservados após duas inicializações');}
 assert.deepEqual(errors,[]);console.log('Evidências:',dir);
})().catch(e=>{console.error(e);process.exitCode=1;}).finally(async()=>{if(browser)await browser.close();await stop();});
