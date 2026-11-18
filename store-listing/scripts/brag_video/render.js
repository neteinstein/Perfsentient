// Renders scene.html to PNG frames (every frame is a pure function of time), then encodes with ffmpeg.
// Usage:  node store-listing/scripts/brag_video/render.js <out_dir>     (needs Playwright + ffmpeg + python3/numpy)
const { chromium } = require('playwright');
const { execFileSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const here = __dirname;
const out = path.resolve(process.argv[2] || 'brag-output');
const work = path.join(out, 'work');
const framesDir = path.join(work, 'frames');
fs.rmSync(work, { recursive: true, force: true });
fs.mkdirSync(framesDir, { recursive: true });
fs.mkdirSync(path.join(work, 'shots'), { recursive: true });
for (const f of fs.readdirSync(path.join(here, '../../screenshots/phone'))) {
  fs.copyFileSync(path.join(here, '../../screenshots/phone', f), path.join(work, 'shots', f));
}
fs.copyFileSync(path.join(here, 'scene.html'), path.join(work, 'scene.html'));

const FPS = 30;
(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 }, deviceScaleFactor: 1 });
  await page.goto('file://' + path.join(work, 'scene.html'));
  const duration = await page.evaluate(() => window.DURATION);
  const total = duration * FPS;
  for (let i = 0; i < total; i++) {
    await page.evaluate((t) => window.renderAt(t), i / FPS);
    await page.screenshot({ path: path.join(framesDir, `f${String(i).padStart(4, '0')}.png`) });
  }
  await browser.close();

  // Poster: the strongest settled frame (outro, text fully in) becomes brag.jpg and replaces frame 0,
  // so every platform's thumbnail shows it without changing the duration or the audio sync.
  const posterFrame = path.join(framesDir, `f${String(Math.round(18.5 * FPS)).padStart(4, '0')}.png`);
  execFileSync('ffmpeg', ['-y', '-loglevel', 'error', '-i', posterFrame, '-q:v', '2', path.join(out, 'brag.jpg')]);
  fs.copyFileSync(posterFrame, path.join(framesDir, 'f0000.png'));

  const wav = path.join(work, 'music.wav');
  execFileSync('python3', ['-I', path.join(here, 'music.py'), wav, String(duration)], { stdio: 'inherit' });
  execFileSync('ffmpeg', ['-y', '-loglevel', 'error', '-framerate', String(FPS), '-i', path.join(framesDir, 'f%04d.png'), '-i', wav,
    '-c:v', 'libx264', '-pix_fmt', 'yuv420p', '-crf', '18', '-preset', 'slow', '-c:a', 'aac', '-b:a', '192k', '-shortest',
    '-movflags', '+faststart', path.join(out, 'brag.mp4')], { stdio: 'inherit' });
})();

// README preview: autoplaying, looping GIF (GitHub does not autoplay <video>). Built by gif.sh.
