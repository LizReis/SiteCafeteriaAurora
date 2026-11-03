/* Functional scenarios A–E. Requires a built JAR, Java 21, Playwright and Chromium.
 * Uses only a temporary H2 database. No credentials are printed or stored.
 */
const { chromium } = require('playwright');
const { spawn } = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '..');
const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'aurora-functional-'));
const password = require('node:crypto').randomBytes(32).toString('hex');
const base = 'http://127.0.0.1:18081';
const auth = 'Basic ' + Buffer.from('admin:' + password).toString('base64');
let server, browser;
async function request(url, method = 'GET', data) {
  const response = await fetch(base + url, { method, headers: {Authorization: auth, 'Content-Type':'application/json'}, body:data ? JSON.stringify(data) : undefined });
  assert(response.ok, `${method} ${url}: ${response.status}`);
  return response.status === 204 ? null : response.json();
}
async function start(db) {
  const log = fs.openSync(path.join(dir,'server.log'),'a');
  server = spawn(process.env.JAVA_HOME + '/bin/java', ['-jar', 'target/cafe-aurora-0.0.1-SNAPSHOT.jar', '--spring.profiles.active=dev', '--server.port=18081', '--spring.datasource.url=jdbc:h2:file:' + db],
    {cwd:root, env:{...process.env, CAFE_ADMIN_PASSWORD:password}, stdio:['ignore',log,log]});
  for(let i=0;i<120;i++) {
    if(server.exitCode !== null) throw new Error('Server startup failed: ' + path.join(dir,'server.log'));
    try { const c = await request('/api/categorias'); if(c.length) return; } catch (_) {}
    await new Promise(r=>setTimeout(r,250));
  }
  throw new Error('Startup timeout');
}
async function stop() {
  if (!server || server.exitCode !== null) return;
  await new Promise(resolve=>{server.once('exit',resolve);server.kill('SIGTERM');});
}
(async()=>{
  const db = path.join(dir,'fresh'); await start(db);
  const cats = await request('/api/admin/categorias'); assert.equal(cats.length,6);
  assert.equal((await request('/api/produtos')).length,9);
  browser = await chromium.launch({headless:true,executablePath:process.env.CHROMIUM_PATH || '/usr/bin/chromium',args:['--no-sandbox']});
  const page = await browser.newPage({viewport:{width:1366,height:900}});
  const errors = []; page.on('pageerror',e=>errors.push(e.message));
  if (process.env.VISUAL_ONLY) {
    for (const slug of ['cafe','drinks','comidas']) {
      await page.setViewportSize({width:390,height:844});
      await page.goto(base+'/cardapio-'+slug); await page.waitForTimeout(7000);
      for (const button of await page.locator('[data-product-id] button').all()) {
        const bounds=await button.boundingBox(); assert(bounds.x>=0 && bounds.x+bounds.width<=390);
      }
      await page.screenshot({path:path.join(dir,slug+'-mobile.png'),fullPage:true});
    }
    console.log('Layout móvel OK: controles dentro do viewport nos três cardápios. Evidências:',dir);
    return;
  }
  await page.goto(base+'/cardapio-cafe');
  await page.evaluate(()=>{localStorage.setItem('cafe-aurora.cart.v1',JSON.stringify([{id:'espresso-italiano',quantity:4}]));});
  // Different categories coexist by their database IDs; quantity restoration
  // must never overwrite another product when navigating between menus.
  const selected=[];
  for (const slug of ['cafe','drinks','comidas']) {
    await page.goto(base+'/cardapio-'+slug); await page.waitForTimeout(5500);
    const card=page.locator('[data-product-id]').first();
    selected.push(await card.getAttribute('data-product-id'));
    await card.locator('button').last().click();
    assert.equal(await card.locator('output').textContent(),'1');
  }
  assert.equal(new Set(selected).size,3);
  assert.equal(await page.evaluate(()=>window.CafeAuroraCart.getCount()),3);
  await page.goto(base+'/cardapio-cafe'); await page.waitForTimeout(5500);
  assert.equal(await page.locator(`[data-product-id="${selected[0]}"] output`).textContent(),'1');
  assert.equal(await page.evaluate(()=>localStorage.getItem('cafe-aurora.cart.v1')),null);
  await page.goto(base+'/sacola-pedido');
  await page.locator('.cart-item-controls').first().locator('button').nth(1).click();
  const updated=await page.evaluate(()=>window.CafeAuroraCart.getItems().find(p=>p.quantity===2)?.id);
  await page.goBack(); await page.waitForTimeout(5500);
  if (updated===selected[0]) assert.equal(await page.locator(`[data-product-id="${selected[0]}"] output`).textContent(),'2');
  assert.equal(await page.evaluate(()=>window.CafeAuroraCart.getCount()),4);
  await page.evaluate(()=>window.CafeAuroraCart.clear());
  console.log('Sacola OK: IDs entre categorias, retorno, restauração e descarte de v1');
  const made=[];
  for(const [tipo,slug] of [['CAFE','cafe'],['DRINKS','drinks'],['COMIDAS','comidas']]) {
    const c=cats.find(c=>c.tipoCardapio===tipo);
    let body={nome:'Teste '+tipo,descricao:'Descrição funcional '+tipo,preco:19.35,imagemUrl:'/img/1050.jpg',categoriaId:c.id,ativo:true,disponivel:true};
    let p=await request('/api/admin/produtos','POST',body); made.push(p);
    const card=page.locator(`[data-product-id="${p.id}"]`);
    await page.goto(base+'/cardapio-'+slug); await page.waitForTimeout(5500);
    assert((await card.textContent()).includes(body.nome)); assert((await card.textContent()).includes('19,35'));
    assert.equal(await card.locator('img').getAttribute('src'),body.imagemUrl);
    assert(await card.locator('img').evaluate(img=>img.complete && img.naturalWidth>0));
    const grid=page.locator('.coffee-products-grid, .drink-grid, .food-grid').first();
    assert.equal(await grid.evaluate(g=>getComputedStyle(g).gridTemplateColumns.split(' ').length),2);
    await page.screenshot({path:path.join(dir,slug+'-desktop.png'),fullPage:true});
    await card.locator('button').last().click(); assert.equal(await card.locator('output').textContent(),'1');
    await card.locator('button').last().click(); assert.equal(await card.locator('output').textContent(),'2');
    await page.reload(); await page.waitForTimeout(5500);
    assert.equal(await card.locator('output').textContent(),'2');
    await card.locator('button').first().click(); assert.equal(await card.locator('output').textContent(),'1');
    body={...body,preco:31.90,nome:'Editado '+tipo,descricao:'Descrição editada',imagemUrl:'/img/1320.jpg'};
    await request('/api/admin/produtos/'+p.id,'PUT',body);
    await page.reload(); await page.waitForTimeout(5500);
    assert((await card.textContent()).includes('31,90'));
    assert.equal(await page.evaluate(id=>window.CafeAuroraCart.get(String(id)).priceCents,p.id),3190);
    body.disponivel=false; await request('/api/admin/produtos/'+p.id,'PUT',body);
    await page.reload(); await page.waitForTimeout(5500);
    assert((await card.textContent()).includes('Indisponível'));
    assert(await card.locator('button').last().isDisabled());
    assert.equal(await page.evaluate(id=>window.CafeAuroraCart.get(String(id)),p.id),null);
    await request('/api/admin/produtos/'+p.id,'DELETE');
    await page.reload(); assert.equal(await card.count(),0);
    await request('/api/admin/categorias/'+c.id,'PUT',{...c,nome:'Renomeada '+tipo});
    await page.reload(); assert((await page.content()).includes('Renomeada '+tipo));
    await request('/api/admin/categorias/'+c.id,'DELETE');
    await page.reload(); assert(!(await page.content()).includes('Renomeada '+tipo));
    console.log('A–D OK:',tipo);
  }
  await page.setViewportSize({width:390,height:844});
  await page.goto(base+'/cardapio-comidas'); await page.waitForTimeout(5500);
  await page.screenshot({path:path.join(dir,'mobile.png'),fullPage:true});
  assert.equal(errors.length,0,errors.join('\n'));
  await stop(); await start(db);
  assert.equal((await request('/api/admin/categorias')).length,6);
  assert.equal((await request('/api/admin/produtos')).length,12);
  for (const p of made) {const saved=await request('/api/admin/produtos/'+p.id);assert.equal(saved.preco,31.90);assert.equal(saved.ativo,false);}
  console.log('E OK: reinício, edições persistidas, sem duplicação ou reativação');
  await stop();
  // Optional closed legacy file copy: never open or migrate the source database.
  if(process.env.LEGACY_H2_FILE) {
    const legacy=path.join(dir,'legacy');fs.copyFileSync(process.env.LEGACY_H2_FILE,legacy+'.mv.db');
    await start(legacy);
    const before=await request('/api/admin/produtos');assert.equal(before.length,9);
    assert.equal((await request('/api/admin/categorias')).length,3);
    for (const slug of ['cafe','drinks','comidas']) {const r=await fetch(base+'/cardapio-'+slug);assert.equal(r.status,200);assert((await r.text()).includes('data-product-id='));}
    await stop(); await start(legacy);
    assert.deepEqual(await request('/api/admin/produtos'),before);
    console.log('H2 legado OK: esquema antigo atualizado na cópia, IDs e produtos preservados em dois inícios');
  }
  console.log('Evidências:',dir);
})().catch(e=>{console.error(e);process.exitCode=1;}).finally(async()=>{if(browser) await browser.close();await stop();});
