/**
 * Echter Test von Morni Angelkönig mit Test-Spielern (mineflayer) auf Paper 26.1.2 – gestartet von test/run.sh.
 *   node angelkoenig-test.cjs <host> <port> <rcon-host:port> <rcon-passwort-datei> <server-ordner>
 *
 * Spieler: Anna (deutsch, Party-Leiterin mit Carl), Bert (englisch), Carl (kommt später, landet bei seiner Party).
 * Sprache und Party kommen vom Test-Netzwerk (test/fake-network), das auch Statistik, Zustand und „zur Lobby“ in
 * calls.log mitschreibt. Geangelt wird echt (bot.fish()); welche Art anbeißt, legt der Test für manche Würfe per
 * /angeladmin naechster fest, damit sich Namen, Fischbuch, Aufgaben und Turnier sicher prüfen lassen.
 *
 * Verfasst mit Claude 🤖
 */
const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');
const mineflayer = require('mineflayer');
const { Vec3 } = require('vec3');
const { rcon } = require('./rcon.cjs');

const [HOST = '127.0.0.1', PORT = '25970', RCON = '127.0.0.1:26070', PASS_FILE, SERVER] = process.argv.slice(2);
const PASS = fs.readFileSync(PASS_FILE, 'utf8').trim();
const VERSION = process.env.MC_VERSION ?? '26.1';
const SPAWN = { x: 0.5, y: 64, z: -46.5 };
const PLACES = {
  bergsee: { at: { x: 0.5, y: 64, z: -7.5 }, cast: new Vec3(0.5, 62.9, -2.5) },
  bach: { at: { x: -99.5, y: 64, z: -7.5 }, cast: new Vec3(-99.5, 62.9, -2.5) },
  moor: { at: { x: 85.5, y: 64.5, z: 6.5 }, cast: new Vec3(90.5, 62.9, 6.5) },
  eis: { at: { x: 69.5, y: 83, z: 93.5 }, cast: new Vec3(69.5, 82.9, 96.0) },
};
const MENU_SLOT = { dorf: 10, bergsee: 12, bach: 13, moor: 14, eis: 15 };
const FISH_ITEMS = new Set(['cod', 'salmon', 'tropical_fish', 'pufferfish', 'armadillo_scute', 'leather_boots', 'stick',
  'glass_bottle']);

const results = [];
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
function check(name, ok, detail = '') {
  results.push({ name, ok: Boolean(ok) });
  console.log(`${ok ? '✓' : '✗'} ${name}${detail ? ` – ${detail}` : ''}`);
}

/** Text aus Chat-Komponenten (String, JSON oder NBT) zusammensetzen. */
function plain(v) {
  if (v == null) return '';
  if (typeof v === 'string') {
    if (v.startsWith('{') || v.startsWith('[')) {
      try {
        return plain(JSON.parse(v));
      } catch {
        return v;
      }
    }
    return v;
  }
  if (Array.isArray(v)) return v.map(plain).join('');
  if (typeof v !== 'object') return String(v);
  if (v.type === 'compound') return plain(v.value);
  if (v.type === 'list') return plain(v.value?.value ?? []);
  if (v.type === 'string') return v.value;
  let s = '';
  if ('text' in v) s += plain(v.text);
  if ('' in v) s += plain(v['']);
  if ('translate' in v) s += plain(v.translate);
  if ('extra' in v) s += plain(v.extra);
  return s;
}

function offlineUuid(name) {
  const h = crypto.createHash('md5').update(`OfflinePlayer:${name}`, 'utf8').digest();
  h[6] = (h[6] & 0x0f) | 0x30;
  h[8] = (h[8] & 0x3f) | 0x80;
  const x = h.toString('hex');
  return `${x.slice(0, 8)}-${x.slice(8, 12)}-${x.slice(12, 16)}-${x.slice(16, 20)}-${x.slice(20)}`;
}

const cmd = (...commands) => rcon(RCON, PASS, commands);
const playerFile = (name) => path.join(SERVER, 'plugins/MorniAngelkoenig/spieler', `${offlineUuid(name)}.yml`);
const readFile = (file) => (fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : '');
const calls = () =>
  readFile(path.join(SERVER, 'plugins/MoinMorni/calls.log'))
    .split('\n')
    .filter(Boolean)
    .map((l) => JSON.parse(l));
const inWorld = (c) => `execute in minecraft:angelkoenig run ${c}`;

async function waitFor(cond, ms = 5000, step = 100) {
  const start = Date.now();
  while (Date.now() - start < ms) {
    if (await cond()) return true;
    await sleep(step);
  }
  return Boolean(await cond());
}

let lastJoin = 0;
async function join(name) {
  const wait = lastJoin + 3500 - Date.now();
  if (wait > 0) await sleep(wait);
  lastJoin = Date.now();
  const bot = mineflayer.createBot({ host: HOST, port: Number(PORT), username: name, version: VERSION, auth: 'offline' });
  bot.chats = [];
  bot.bars = [];
  bot.titles = [];
  bot.on('messagestr', (m, position) => (position === 'game_info' ? bot.bars : bot.chats).push(m));
  bot._client.on('action_bar', (p) => bot.bars.push(plain(p.text)));
  bot._client.on('set_title_text', (p) => bot.titles.push(plain(p.text)));
  bot.on('kicked', (r) => {
    bot.kickReason = plain(r);
  });
  bot.on('error', (e) => console.log(`  (${name}: ${e.message})`));
  await new Promise((resolve, reject) => {
    bot.once('spawn', resolve);
    bot.once('kicked', (r) => reject(new Error(`${name} gekickt: ${plain(r)}`)));
    setTimeout(() => reject(new Error(`${name}: kein Spawn`)), 30000);
  });
  await sleep(2500);
  return bot;
}

const slot = (bot, i) => bot.inventory.slots[36 + i];
const near = (p, q, d) => Math.abs(p.x - q.x) < d && Math.abs(p.y - q.y) < d && Math.abs(p.z - q.z) < d;
const sees = (bot, re) => bot.chats.some((c) => re.test(c));
const clear = (...bots) =>
  bots.forEach((b) => {
    b.chats.length = 0;
    b.bars.length = 0;
    b.titles.length = 0;
  });
const isFish = (it) => it && FISH_ITEMS.has(it.name) && plain(it.customName) !== '';
const fishes = (bot) => bot.inventory.items().filter(isFish);
const lore = (it) => (it?.customLore ?? []).map(plain).join(' | ');

/** /angeladmin status als Werte: {turnier, zeit, wetter, koenig, t: {Anna: g}, players: {Anna: {taler, …, a: [...]}}} */
async function status() {
  const lines = (await cmd('angeladmin status'))[0].split('\n');
  const s = { t: {}, players: {} };
  for (const part of lines[0].split(' ')) {
    const [k, v] = part.split('=');
    if (k.startsWith('t:')) s.t[k.slice(2)] = Number(v);
    else s[k] = v;
  }
  for (const line of lines.slice(1)) {
    const [name, ...parts] = line.split(' ');
    const p = { a: [] };
    for (const part of parts) {
      const i = part.indexOf('=');
      const k = part.slice(0, i);
      const v = part.slice(i + 1);
      if (/^a\d$/.test(k)) {
        const [spec, progress, done] = v.split('|');
        p.a[Number(k.slice(1))] = { spec, progress: Number(progress), done: done === 'true' };
      } else p[k] = /^-?\d+$/.test(v) ? Number(v) : v;
    }
    s.players[name] = p;
  }
  return s;
}

async function openWithItem(bot, hotbar) {
  bot.setQuickBarSlot(hotbar);
  await sleep(300);
  const opened = new Promise((resolve) => {
    bot.once('windowOpen', resolve);
    setTimeout(() => resolve(null), 5000);
  });
  bot.activateItem();
  return opened;
}

async function click(bot, slotNo, expectNewWindow = false) {
  const opened = expectNewWindow
    ? new Promise((resolve) => {
        bot.once('windowOpen', resolve);
        setTimeout(() => resolve(null), 5000);
      })
    : null;
  await bot.clickWindow(slotNo, 0, 0);
  await sleep(700);
  return opened ? opened : bot.currentWindow;
}

async function closeWindow(bot) {
  if (bot.currentWindow) bot.closeWindow(bot.currentWindow);
  await sleep(300);
}

/** Mit dem Wegweiser reisen. */
async function travel(bot, where) {
  const win = await openWithItem(bot, 4);
  if (!win) return false;
  await bot.clickWindow(MENU_SLOT[where], 0, 0);
  await sleep(1500);
  return true;
}

/** Einmal echt angeln: Angel in die Hand, aufs Wasser zielen, warten bis es beißt, Fang kommt ins Inventar. */
async function fishOnce(bot, where, tries = 4) {
  const target = PLACES[where].cast;
  for (let t = 0; t < tries; t++) {
    bot.setQuickBarSlot(0);
    await sleep(300);
    await bot.lookAt(target, true);
    await sleep(300);
    const before = fishes(bot).length;
    const beforeChats = bot.chats.length;
    let bitten = true;
    try {
      await Promise.race([bot.fish(), sleep(20000).then(() => Promise.reject(new Error('kein Biss')))]);
    } catch (e) {
      bitten = false;
      const hooks = Object.values(bot.entities).filter((x) => x.name === 'fishing_bobber')
        .map((x) => `${x.position.x.toFixed(1)}/${x.position.y.toFixed(1)}/${x.position.z.toFixed(1)}`);
      console.log(`  (${bot.username}: ${e.message}, Schwimmer bei ${hooks.join(' ') || '–'})`);
      bot.activateItem();
      await sleep(800);
    }
    if (bitten) {
      const got = await waitFor(() => fishes(bot).length > before || bot.chats.slice(beforeChats).some((c) =>
        /gefangen|caught|herausgezogen|pulled out|Korb ist voll|basket is full/.test(c)), 6000);
      if (got) {
        // der Fang landet erst ein paar Ticks später im Korb – bei viel Last auch mal später
        await waitFor(() => fishes(bot).length > before, 4000);
        await sleep(200);
        return true;
      }
    }
    await sleep(500);
  }
  return false;
}

const newest = (bot) => {
  const f = fishes(bot);
  return f[f.length - 1];
};

async function main() {
  await sleep(4000);
  check('Netzwerk: Zustand „open“', await waitFor(() => calls().some((c) => c.t === 'state' && c.mode === 'angelkoenig'
    && c.state === 'open'), 5000));
  const boards = String(await cmd(inWorld('execute positioned -13.6 66.6 -45.5 if entity @e[type=text_display,distance=..6]')));
  check('Bestenlisten stehen an der Tafelwand (3 Tafeln × 2 Sprachen)', /: 6\b/.test(boards), boards.trim());
  const boats = String(await cmd(inWorld('execute positioned 0 63 -22 if entity @e[tag=ak_boat,distance=..12]')));
  check('Ruderboote liegen am Steg', /: [4-9]\b/.test(boats), boats.trim());

  // ------------------------------------------------------------------ Ankunft
  const anna = await join('Anna');
  const bert = await join('Bert');
  check('Ankunft im Dorf am See', near(anna.entity.position, SPAWN, 2.5), JSON.stringify(anna.entity.position));
  check('Abenteuermodus', anna.game.gameMode === 'adventure', anna.game.gameMode);
  check('Schnellleiste: Angel, Köderdose, Wegweiser, Fischbuch, Aufgaben, Kleiderschrank, Lobby',
    slot(anna, 0)?.name === 'fishing_rod' && slot(anna, 1)?.name === 'bowl' && slot(anna, 4)?.name === 'compass'
      && slot(anna, 5)?.name === 'book' && slot(anna, 6)?.name === 'paper' && slot(anna, 7)?.name === 'armor_stand'
      && slot(anna, 8)?.name === 'red_bed', [0, 1, 4, 5, 6, 7, 8].map((i) => slot(anna, i)?.name).join(','));
  check('Deutsch: „Haselrute“', plain(slot(anna, 0)?.customName).includes('Haselrute'), plain(slot(anna, 0)?.customName));
  check('Englisch (Sprache vom Netzwerk): „Hazel rod“', plain(slot(bert, 0)?.customName).includes('Hazel rod'),
    plain(slot(bert, 0)?.customName));
  check('Erklärung beim ersten Besuch', sees(anna, /Willkommen im Tal/) && sees(anna, /Fischhändler Xaver/)
    && sees(bert, /Welcome to the valley/), anna.chats.slice(0, 3).join(' | '));
  check('Leiste rechts (Scoreboard) vorhanden', await waitFor(() => Object.keys(anna.scoreboards ?? {}).length > 0, 3000));
  let s = await status();
  check('Startgeld 25 Taler, Haselrute, drei Aufgaben', s.players.Anna?.taler === 25 && s.players.Anna.angel === 'hasel'
    && s.players.Anna.a.length === 3, JSON.stringify(s.players.Anna));
  const merchant = Object.values(anna.entities).find((e) => e.name === 'villager' && Math.abs(e.position.x - 11.5) < 1
    && Math.abs(e.position.z + 33.5) < 1);
  check('Fischhändler Xaver steht an seinem Stand', Boolean(merchant));

  // ------------------------------------------------------------------ Wegweiser + Bergsee
  let win = await openWithItem(anna, 4);
  check('Wegweiser öffnet die Fangplätze', win && plain(win.title).includes('Wegweiser'), win && plain(win.title));
  check('Fangplätze mit „Jetzt beißen gern“', win && plain(win.slots[12]?.customName).includes('Bergsee')
    && lore(win.slots[12]).includes('beißen gern') && plain(win.slots[15]?.customName).includes('Eisloch'),
    win && lore(win.slots[12]));
  await anna.clickWindow(MENU_SLOT.bergsee, 0, 0);
  check('Reise zum Steg am Bergsee', await waitFor(() => near(anna.entity.position, PLACES.bergsee.at, 2), 4000),
    JSON.stringify(anna.entity.position));
  await cmd('angeladmin aufgabe Anna 0 spot:bergsee:2:40');
  await cmd('angeladmin naechster Anna barsch');
  let ok = await fishOnce(anna, 'bergsee');
  let f = newest(anna);
  check('Echter Fang am Bergsee', ok && f, f && plain(f.customName));
  check('Fang mit Name, Länge, Gewicht, Ort und Wert', f && /Länge: [\d,]+ cm · Gewicht/.test(lore(f)) && /Bergsee/.test(lore(f))
    && /Wert: \d+ Taler/.test(lore(f)), f && lore(f));
  check('Fang-Nachricht im Chat', sees(anna, /» .+ (gefangen!|herausgezogen\.) [\d,]+ (cm|g|kg)/), anna.chats.slice(-3).join(' | '));
  check('Biss wird angesagt (Aktionsleiste)', anna.bars.some((b) => /Biss!/.test(b)), anna.bars.slice(-4).join(' | '));
  await cmd('angeladmin naechster Anna seesaibling 40');
  clear(anna);
  ok = await fishOnce(anna, 'bergsee');
  f = fishes(anna).find((i) => plain(i.customName) === 'Seesaibling');
  check('Seesaibling mit 40,0 cm (Fang festgelegt)', ok && f && lore(f).includes('40,0 cm'), f && lore(f));
  check('Seltener Fang: Titel', await waitFor(() => anna.titles.some((t) => t.includes('Seesaibling')), 3000),
    anna.titles.join(','));
  check('Neu im Fischbuch mit Bonus (+30 Taler für einen seltenen Fisch)', sees(anna, /Neu im Fischbuch! \+30 Taler/), anna.chats.filter((c) => /Fischbuch/.test(c)).join(' | '));
  s = await status();
  check('Aufgabe „2 Fische am Bergsee“ erledigt: 25 + 5 + 30 (Fischbuch) + 40 = 100 Taler', s.players.Anna.a[0].done && s.players.Anna.taler === 100
    && sees(anna, /Aufgabe erledigt/), JSON.stringify(s.players.Anna));

  // ------------------------------------------------------------------ Bert am Gebirgsbach (englisch)
  await travel(bert, 'bach');
  check('Bert reist zum Gebirgsbach', near(bert.entity.position, PLACES.bach.at, 2), JSON.stringify(bert.entity.position));
  await cmd('angeladmin naechster Bert bachforelle 33');
  ok = await fishOnce(bert, 'bach');
  f = fishes(bert).find((i) => plain(i.customName) === 'Brown trout');
  check('Englischer Fang am Bach: „Brown trout“, Length, Mountain stream', ok && f && /Length: 33.0 cm/.test(lore(f))
    && /Mountain stream/.test(lore(f)), f && lore(f));
  clear(anna, bert);
  await cmd('angeladmin naechster Bert goldforelle');
  ok = await fishOnce(bert, 'bach');
  f = fishes(bert).find((i) => plain(i.customName) === 'Golden trout');
  check('Legende: Goldforelle', ok && f, f && lore(f));
  check('Alle erfahren von der Legende', await waitFor(() => sees(anna, /Bert hat Goldforelle gefangen/), 3000),
    anna.chats.slice(-2).join(' | '));

  // ------------------------------------------------------------------ Fischhändler
  await travel(anna, 'dorf');
  check('Wegweiser: zurück ins Dorf', near(anna.entity.position, SPAWN, 2.5), JSON.stringify(anna.entity.position));
  await cmd(inWorld('tp Anna 11.5 64 -35.8 180 10'));
  await sleep(1500);
  const before = (await status()).players.Anna.taler;
  const fishBefore = fishes(anna).length;
  const villager = Object.values(anna.entities).find((e) => e.name === 'villager' && Math.abs(e.position.x - 11.5) < 1
    && Math.abs(e.position.z + 33.5) < 1);
  const opened = new Promise((r) => {
    anna.once('windowOpen', r);
    setTimeout(() => r(null), 5000);
  });
  if (villager) await anna.activateEntity(villager);
  win = await opened;
  check('Klick auf Xaver öffnet den Fischhändler', win && plain(win.title).includes('Fischhändler Xaver'), win && plain(win.title));
  check('Xaver grüßt', sees(anna, /Xaver:/));
  check('Eigene Fänge im Menü zum Verkaufen', win && isFish(win.slots[18]) && lore(win.slots[18]).includes('verkaufen'),
    win && lore(win.slots[18]));
  win = await click(anna, 18);
  check('Einen Fang verkauft', sees(anna, /Verkauft: .+ für \d+ Taler/) && fishes(anna).length === fishBefore - 1,
    anna.chats.slice(-1)[0]);
  win = await click(anna, 8);
  s = await status();
  check('Alles verkauft: Taler gestiegen, Korb leer', fishes(anna).length === 0 && s.players.Anna.taler > before
    && sees(anna, /Verkauft: \d+ Fänge/), `${before} → ${s.players.Anna.taler}`);
  // Angel kaufen
  await cmd('angeladmin taler Anna 300');
  win = await click(anna, 2, true);
  check('Reiter „Angeln“', win && plain(win.title).includes('Angeln'), win && plain(win.title));
  check('Eschenrute kostet 250 Taler', win && lore(win.slots[12]).includes('250 Taler'), win && lore(win.slots[12]));
  await click(anna, 12);
  s = await status();
  check('Eschenrute gekauft und in der Hand', s.players.Anna.angel === 'esche' && s.players.Anna.taler === 50
    && plain(slot(anna, 0)?.customName).includes('Eschenrute'), `${s.players.Anna.angel} ${s.players.Anna.taler}`);
  // Köder kaufen
  win = await click(anna, 18, true);
  win = await click(anna, 3, true);
  check('Reiter „Köder“', win && plain(win.title).includes('Köder'), win && plain(win.title));
  await click(anna, 10);
  s = await status();
  check('Regenwurm gekauft (10 Stück) und eingehängt', s.players.Anna.koeder === 'wurm:10' && s.players.Anna.taler === 30,
    `${s.players.Anna.koeder} ${s.players.Anna.taler}`);
  await cmd('angeladmin taler Anna 20');
  win = await click(anna, 18, true);
  win = await click(anna, 3, true);
  clear(anna);
  await click(anna, 14);
  check('Zu teuer: Hinweis, nichts gekauft', sees(anna, /fehlen dir noch 40 Taler/), anna.chats.slice(-1)[0]);
  await closeWindow(anna);

  // ------------------------------------------------------------------ Sitzen am Lagerfeuer
  await cmd(inWorld('tp Anna -26.5 64 -51.6 180 40'));
  await sleep(1200);
  anna.setQuickBarSlot(2);
  await sleep(300);
  const bench = anna.blockAt(new Vec3(-27, 64, -50));
  check('Bank am Lagerfeuer', bench && bench.name === 'spruce_stairs', bench && bench.name);
  if (bench) await anna.activateBlock(bench);
  check('Hinsetzen per Rechtsklick (leere Hand)', await waitFor(() => Boolean(anna.vehicle), 3000));
  anna.setControlState('sneak', true);
  await sleep(1200);
  anna.setControlState('sneak', false);
  await sleep(500);
  if (anna.vehicle) {
    anna.dismount();
    await sleep(1000);
  }
  const seats = String(await cmd(inWorld('execute if entity @e[tag=ak_seat]')));
  check('Aufstehen: Sitz ist wieder weg', /: 0\b|failed/i.test(seats), seats.trim());

  // ------------------------------------------------------------------ Köder benutzen, Moor, Party
  await travel(anna, 'moor');
  check('Reise zum Moorweiher', near(anna.entity.position, PLACES.moor.at, 2), JSON.stringify(anna.entity.position));
  clear(anna);
  ok = await fishOnce(anna, 'moor');
  s = await status();
  f = newest(anna);
  check('Fang am Moorweiher mit Köder (einer verbraucht)', ok && s.players.Anna.koeder === 'wurm:9'
    && /Moorweiher/.test(lore(f)), `${s.players.Anna.koeder} ${lore(f)}`);
  check('Köderdose in der Schnellleiste zählt mit (Regenwurm ×9)', await waitFor(() => /Regenwurm ×9/.test(plain(slot(anna, 1)?.customName)), 3000),
    plain(slot(anna, 1)?.customName));
  check('Kein Köder-Hinweis in der Aktionsleiste, solange die Dose voll genug ist', !anna.bars.some((b) => /Köder: Regenwurm/.test(b)),
    anna.bars.slice(-2).join(' | '));
  const carl = await join('Carl');
  check('Carl landet bei seiner Party (Anna am Moor)', await waitFor(() => near(carl.entity.position, anna.entity.position, 3),
    5000) && sees(carl, /Deine Party angelt schon hier/), JSON.stringify(carl.entity.position));
  await cmd('angeladmin naechster Carl karausche 25');
  ok = await fishOnce(carl, 'moor');
  f = fishes(carl).find((i) => plain(i.customName) === 'Karausche');
  check('Carl fängt eine Karausche im Moor', ok && f && /Moorweiher/.test(lore(f)), f && lore(f));

  // ------------------------------------------------------------------ Eisloch
  await travel(bert, 'eis');
  check('Bert reist zum Eisloch', near(bert.entity.position, PLACES.eis.at, 2), JSON.stringify(bert.entity.position));
  const ice = bert.blockAt(new Vec3(69, 82, 96));
  const iceAround = bert.blockAt(new Vec3(69, 82, 93));
  check('Loch im Eis, ringsum Eis', ice?.name === 'water' && ['ice', 'packed_ice'].includes(iceAround?.name),
    `${ice?.name} ${iceAround?.name}`);
  await cmd('angeladmin naechster Bert rutte 50');
  ok = await fishOnce(bert, 'eis');
  f = fishes(bert).find((i) => plain(i.customName) === 'Burbot');
  check('Rutte durchs Eisloch (englisch „Burbot“, „Ice hole“)', ok && f && /Ice hole/.test(lore(f)), f && lore(f));

  // ------------------------------------------------------------------ Fischbuch + Aufgaben
  win = await openWithItem(anna, 5);
  check('Fischbuch öffnet sich', win && plain(win.title).includes('Fischbuch'), win && plain(win.title));
  check('Fischbuch zählt Arten', win && /Arten: [2-9]\/20/.test(lore(win.slots[4])), win && lore(win.slots[4]));
  const known = win?.slots.find((it) => plain(it?.customName) === 'Seesaibling');
  const unknown = win?.slots.filter((it) => plain(it?.customName) === '???').length ?? 0;
  check('Bekannte Art mit größtem Fang, fehlende als „???“', known && /Größter: 40,0 cm/.test(lore(known)) && unknown > 10,
    `${known && lore(known)} / ${unknown}× ???`);
  await closeWindow(anna);
  win = await openWithItem(anna, 6);
  check('Aufgaben des Tages: drei Aufgaben', win && plain(win.title).includes('Aufgaben') && win.slots[11] && win.slots[13]
    && win.slots[15], win && plain(win.title));
  check('Erledigte Aufgabe ist abgehakt', win && /Erledigt/.test(lore(win.slots[11])), win && lore(win.slots[11]));
  await closeWindow(anna);

  // ------------------------------------------------------------------ Turnier
  await travel(anna, 'bergsee');
  await travel(bert, 'bergsee');
  await cmd(inWorld('tp Bert 3.5 64 -7.5 0 20'));
  clear(anna, bert);
  await cmd('angeladmin turnier start');
  check('Turnier beginnt: Ansage auf Deutsch und Englisch', await waitFor(() => sees(anna, /Das Angel-Turnier läuft/)
    && sees(bert, /fishing contest is on/), 3000), anna.chats.slice(-1)[0]);
  check('Turnier-Titel', await waitFor(() => anna.titles.some((t) => /Angel-Turnier/.test(t)), 3000),
    anna.titles.join(','));
  check('Netzwerk: Zustand „tournament“', await waitFor(() => calls().some((c) => c.t === 'state' && c.state === 'tournament'), 3000));
  await cmd('angeladmin naechster Anna hecht 100');
  await cmd('angeladmin naechster Bert rotauge 30');
  const a1 = await fishOnce(anna, 'bergsee');
  const b1 = await fishOnce(bert, 'bergsee');
  s = await status();
  check('Beide Fänge zählen im Turnier', a1 && b1 && s.t.Anna > 5000 && s.t.Bert > 0, JSON.stringify(s.t));
  check('Führung wird angesagt', sees(bert, /Anna now leads with Pike/) || sees(anna, /Anna führt jetzt/), bert.chats.slice(-3).join(' | '));
  clear(anna, bert);
  const talerBefore = s.players.Anna.taler;
  await cmd('angeladmin turnier ende');
  check('Ergebnis und neuer Angelkönig', await waitFor(() => sees(anna, /Ergebnis des Angel-Turniers/) && sees(anna, /Anna ist der neue/)
    && sees(bert, /Anna is the new/), 3000), anna.chats.join(' | '));
  s = await status();
  check('Sieg: Anna ist Angelkönig, +250 Taler, 1 Turniersieg', s.koenig === 'Anna' && s.players.Anna.siege === 1
    && s.players.Anna.taler === talerBefore + 250, `${s.koenig} ${s.players.Anna.siege} ${talerBefore}→${s.players.Anna.taler}`);
  win = await openWithItem(anna, 7);
  const crown = win?.slots.findIndex((it) => it?.name === 'golden_helmet') ?? -1;
  check('Königskrone im Kleiderschrank', crown >= 0, win && plain(win.title));
  if (crown >= 0) await click(anna, crown);
  await closeWindow(anna);
  check('Krone aufgesetzt', await waitFor(() => anna.inventory.slots[5]?.name === 'golden_helmet', 3000),
    anna.inventory.slots[5]?.name);

  // ------------------------------------------------------------------ Bestenliste + Statistik
  clear(anna);
  anna.chat('/top faenge');
  await sleep(800);
  check('/top: meiste Fänge mit Anna', sees(anna, /Meiste Fänge/) && sees(anna, /1\. (Anna|Bert)/), anna.chats.join(' | '));
  anna.chat('/top rekorde');
  await sleep(800);
  check('/top rekorde: Hecht von Anna', sees(anna, /Hecht: [\d,]+ kg \(Anna\)/), anna.chats.slice(-6).join(' | '));
  bert.chat('/stats Anna');
  await sleep(800);
  check('/stats auf Englisch', sees(bert, /Catches \d+ · species \d+\/20/), bert.chats.slice(-2).join(' | '));
  await cmd('angeladmin melden');
  await sleep(1000);
  const st = calls().filter((c) => c.t === 'stats' && c.mode === 'angelkoenig');
  const annaStats = st.filter((c) => c.name === 'Anna');
  const sum = (list, key) => list.reduce((n, c) => n + (c.add[key] ?? 0), 0);
  check('Netzwerk-Statistik: catches, species, heaviest_g', sum(annaStats, 'catches') >= 3
    && annaStats.some((c) => c.best.species >= 2 && c.best.heaviest_g > 5000), JSON.stringify(annaStats.slice(-1)));
  check('Netzwerk-Statistik: tournaments_won', sum(annaStats, 'tournaments_won') === 1);
  check('Netzwerk-Statistik: legendary (Bert)', sum(st.filter((c) => c.name === 'Bert'), 'legendary') === 1);
  const board = readFile(path.join(SERVER, 'plugins/MorniAngelkoenig/bestenliste.yml'));
  check('Bestenliste gespeichert (Rekord, König)', /hecht:/.test(board) && /name: Anna/.test(board), board.slice(0, 120));

  // ------------------------------------------------------------------ Zeit + Wetter
  clear(anna);
  await cmd('angeladmin zeit nacht', 'angeladmin wetter gewitter');
  await sleep(800);
  s = await status();
  check('Nacht und Gewitter', s.zeit === 'nacht' && s.wetter === 'gewitter', `${s.zeit} ${s.wetter}`);
  check('Wetter wird angesagt', sees(anna, /Gewitter zieht auf/), anna.chats.slice(-1)[0]);
  await cmd('angeladmin wetter klar', 'angeladmin zeit morgen');

  // ------------------------------------------------------------------ Wiederkommen
  const keep = (await status()).players.Anna;
  const keptFish = fishes(anna).length;
  anna.quit();
  await sleep(3000);
  check('Spielerdatei gespeichert', /taler: \d+/.test(readFile(playerFile('Anna'))));
  const anna2 = await join('Anna');
  s = await status();
  check('Wiederkommen: Taler, Angel, Fischbuch und Siege bleiben', s.players.Anna.taler === keep.taler
    && s.players.Anna.angel === 'esche' && s.players.Anna.arten === keep.arten && s.players.Anna.siege === 1,
    JSON.stringify(s.players.Anna));
  check('Wiederkommen: Fänge im Korb bleiben', fishes(anna2).length === keptFish, `${keptFish} → ${fishes(anna2).length}`);
  check('Begrüßung beim Wiederkommen', sees(anna2, /Schön, dass du wieder da bist/) && !sees(anna2, /Willkommen im Tal/));
  check('Krone wieder auf, Eschenrute in der Hand', anna2.inventory.slots[5]?.name === 'golden_helmet'
    && plain(slot(anna2, 0)?.customName).includes('Eschenrute'));

  // ------------------------------------------------------------------ Verwaltung: Fänge und Fischbuch (für Bilder)
  await cmd('angeladmin fisch Carl goldforelle 45');
  check('/angeladmin fisch: Goldforelle mit 45,0 cm im Korb', await waitFor(() => fishes(carl).some((i) =>
    plain(i.customName) === 'Goldforelle' && lore(i).includes('45,0 cm')), 3000), fishes(carl).map((i) => plain(i.customName)).join(','));
  await cmd('angeladmin fischbuch Carl alle');
  s = await status();
  check('/angeladmin fischbuch alle: alle 20 Arten', s.players.Carl?.arten === 20, JSON.stringify(s.players.Carl));
  await cmd('angeladmin fischbuch Carl leer');
  s = await status();
  check('/angeladmin fischbuch leer: keine Art mehr', s.players.Carl?.arten === 0 && s.players.Carl?.faenge === 0,
    JSON.stringify(s.players.Carl));

  // ------------------------------------------------------------------ Lobby
  await openWithItem(bert, 8);
  check('Lobby-Knopf schickt zur Lobby (Netzwerk)', await waitFor(() => calls().some((c) => c.t === 'to-lobby' && c.name === 'Bert'), 3000));

  for (const b of [anna2, carl]) b.quit();
  await sleep(1500);
}

main()
  .catch((e) => {
    console.error(e);
    check('Test lief ohne Absturz durch', false, e.message);
  })
  .finally(() => {
    const ok = results.filter((r) => r.ok).length;
    console.log(`\n${ok}/${results.length} Prüfungen bestanden`);
    process.exit(ok === results.length ? 0 : 1);
  });
