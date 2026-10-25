// Store-Grafiken: App-Icon 512×512, Feature-Grafik 1024×500 und Screenshots 1080×1920 mit Überschrift.
// Die rohen Screenshots erzeugt der Test StoreScreenshots (./gradlew testGithubDebugUnitTest) in
// app/build/outputs/store-raw/. Aufruf aus dem Repository-Ordner:
//   node store/make-store-images.mjs            alles neu
//   node store/make-store-images.mjs 1-editor   nur diesen Screenshot
// Braucht Playwright (npm i -g playwright) und Chromium.
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { readFileSync } from 'node:fs';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const RAW = `${ROOT}/app/build/outputs/store-raw`;
const OUT = `${ROOT}/store`;
const only = process.argv[2];

// Kamera mit Tausch-Pfeilen (gleiche Form wie das App-Icon, viewBox 108)
const logo = (fg = '#fff', bg = '#1E6FD9') => `
<svg viewBox="21 21 66 66" xmlns="http://www.w3.org/2000/svg">
 <path fill="${fg}" d="M40,38 L46,32 L62,32 L68,38 L76,38 C78.2,38 80,39.8 80,42 L80,70 C80,72.2 78.2,74 76,74 L32,74 C29.8,74 28,72.2 28,70 L28,42 C28,39.8 29.8,38 32,38 Z"/>
 <path fill="${bg}" d="M54,44 A12,12 0 1,1 53.99,44 Z"/>
 <path fill="${fg}" d="M46,54 L50,50 L50,52.5 L58,52.5 L58,55.5 L50,55.5 L50,58 Z"/>
 <path fill="${fg}" d="M62,60 L58,64 L58,61.5 L50,61.5 L50,58.5 L58,58.5 L58,56 Z"/>
</svg>`;
const font = `font-family: 'Roboto', system-ui, sans-serif;`;
const browser = await chromium.launch();

async function render(html, w, h, file) {
  const page = await browser.newPage({ viewport: { width: w, height: h } });
  await page.setContent(`<!doctype html><html><head><style>*{margin:0;box-sizing:border-box} body{${font}}</style></head><body>${html}</body></html>`);
  await page.waitForTimeout(150);
  await page.screenshot({ path: file, type: 'png' });
  await page.close();
}

if (!only) {
  // App-Icon 512×512 (Google rundet die Ecken selbst ab)
  await render(`<div style="width:512px;height:512px;background:#1E6FD9;display:grid;place-items:center">
    <div style="width:400px;height:400px">${logo()}</div></div>`, 512, 512, `${OUT}/icon-512.png`);

  // Feature-Grafik 1024×500
  await render(`<div style="width:1024px;height:500px;background:linear-gradient(135deg,#0B4FA8 0%,#1E6FD9 55%,#4C9BFF 100%);position:relative;overflow:hidden;color:#fff">
    <div style="position:absolute;right:-60px;top:-60px;width:420px;height:420px;border-radius:50%;background:rgba(255,255,255,.08)"></div>
    <div style="position:absolute;right:120px;bottom:-140px;width:300px;height:300px;border-radius:50%;background:rgba(255,255,255,.07)"></div>
    <div style="position:absolute;left:70px;top:50%;transform:translateY(-50%);display:flex;align-items:center;gap:40px">
      <div style="width:190px;height:190px;border-radius:44px;background:#fff;display:grid;place-items:center;box-shadow:0 18px 40px rgba(0,0,0,.25)">
        <div style="width:150px;height:150px">${logo('#1E6FD9', '#fff')}</div></div>
      <div><div style="font-size:84px;font-weight:800;letter-spacing:-1px">GlobeSwap</div>
        <div style="font-size:34px;opacity:.95;margin-top:6px">Swap photos with strangers<br>around the world</div></div>
    </div></div>`, 1024, 500, `${OUT}/feature-graphic.png`);
}

// Screenshots 1080×1920 mit Überschrift
const shots = [
  ['1-editor.png', 'Swap a photo.', 'It goes to a stranger somewhere in the world – and you get theirs.'],
  ['2-chats.png', 'Photos from everywhere', 'Reply with photos and start a conversation made only of pictures.'],
  ['3-sticker.png', 'Make it yours', 'Add text, stickers, drawings and filters before you send.'],
  ['4-onboarding.png', 'No sign-up', 'Anonymous, no ads – with reporting and blocking built in.'],
];
for (const [file, title, sub] of shots) {
  if (only && file !== `${only}.png`) continue;
  await render(`<div style="width:1080px;height:1920px;background:linear-gradient(180deg,#1E6FD9 0%,#0B4FA8 100%);color:#fff;display:flex;flex-direction:column;align-items:center">
    <div style="padding:90px 70px 0;text-align:center">
      <div style="font-size:76px;font-weight:800;letter-spacing:-1px;line-height:1.1">${title}</div>
      <div style="font-size:38px;opacity:.92;margin-top:22px;line-height:1.35">${sub}</div></div>
    <div style="margin-top:62px;width:700px;border-radius:56px;overflow:hidden;border:14px solid #0A1B33;box-shadow:0 30px 70px rgba(0,0,0,.35);background:#000">
      <img src="data:image/png;base64,${readFileSync(`${RAW}/${file}`).toString('base64')}" style="width:100%;display:block"></div></div>`, 1080, 1920, `${OUT}/screenshots/${file}`);
}
await browser.close();
console.log('fertig');
