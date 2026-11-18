// Renders graphics/icon-512.png and graphics/feature-graphic-1024x500.png with headless Chromium.
// Usage (from repo root):  node store-listing/scripts/store_assets.js
// Needs Playwright (Chromium). The screenshots must exist (run `./gradlew :app:testDebugUnitTest --tests '*StoreScreenshotsTest*'`
// and copy app/build/screenshots/*.png into store-listing/screenshots/phone/, see screenshots/README.md).
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const b64 = (f) => fs.readFileSync(path.join(root, 'screenshots/phone', f)).toString('base64');

// The launcher glyph from app/src/main/res/drawable/ic_launcher_foreground.xml, path for path.
const glyph = `
<defs><linearGradient id="g" x1="32" y1="26" x2="76" y2="83" gradientUnits="userSpaceOnUse">
<stop offset="0" stop-color="#8A7BFF"/><stop offset="1" stop-color="#4FD8C4"/></linearGradient></defs>
<path d="M54,26 L76,34 L76,54 C76,68 66,78 54,83 C42,78 32,68 32,54 L32,34 Z" fill="url(#g)"/>
<path d="M38,56 L47,56 L51,46 L57,66 L61,52 L70,52" fill="none" stroke="#fff" stroke-width="4.5" stroke-linecap="round" stroke-linejoin="round"/>`;

const iconHtml = `<body style="margin:0"><svg width="512" height="512" viewBox="18 18 72 72" xmlns="http://www.w3.org/2000/svg">
<rect x="0" y="0" width="108" height="108" fill="#140A4A"/>${glyph}</svg></body>`;

const phone = (file, rotate, x) => `
<div style="position:absolute;left:${x}px;top:${rotate ? 70 : 45}px;width:230px;height:460px;border-radius:30px;border:6px solid #0b0a14;
 background:#000 url(data:image/png;base64,${b64(file)}) center top/cover;box-shadow:0 20px 50px rgba(0,0,0,.45);transform:rotate(${rotate}deg)"></div>`;

const featureHtml = `<body style="margin:0;width:1024px;height:500px;position:relative;overflow:hidden;font-family:Roboto,Helvetica,Arial,sans-serif;
 background:radial-gradient(900px 500px at 15% 10%,#3A2DB2 0%,#261A80 40%,#140A4A 100%);color:#fff">
<div style="position:absolute;left:60px;top:90px;width:110px;height:110px;border-radius:26px;overflow:hidden">
<svg width="110" height="110" viewBox="18 18 72 72"><rect x="0" y="0" width="108" height="108" fill="#140A4A"/>${glyph}</svg></div>
<div style="position:absolute;left:60px;top:220px;font-size:54px;font-weight:700;letter-spacing:-1px">Perfsentient</div>
<div style="position:absolute;left:60px;top:292px;width:470px;font-size:27px;line-height:1.3;color:#C6BFFF">Find the apps that slow your phone, show ads or drain your battery.</div>
<div style="position:absolute;left:60px;top:410px;font-size:20px;color:#4FD8C4">100% on-device · no internet permission</div>
${phone('01-report.png', -6, 600)}${phone('03-apps.png', 6, 810)}</body>`;

(async () => {
  const browser = await chromium.launch();
  const shot = async (html, w, h, out) => {
    const page = await browser.newPage({ viewport: { width: w, height: h }, deviceScaleFactor: 1 });
    await page.setContent(html);
    await page.screenshot({ path: path.join(root, out), omitBackground: false });
    await page.close();
  };
  fs.mkdirSync(path.join(root, 'graphics'), { recursive: true });
  await shot(iconHtml, 512, 512, 'graphics/icon-512.png');
  await shot(featureHtml, 1024, 500, 'graphics/feature-graphic-1024x500.png');
  await browser.close();
})();
