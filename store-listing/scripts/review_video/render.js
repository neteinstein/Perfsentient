// Composes the Play Console review walkthrough from the frames recorded by ReviewVideoFramesTest.
// Usage:  node store-listing/scripts/review_video/render.js <frames_dir> <out.mp4>
// Needs Playwright (Chromium) and ffmpeg. See store-listing/video/review/README.md.
const { chromium } = require('playwright');
const { execFileSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const framesIn = path.resolve(process.argv[2]);
const outFile = path.resolve(process.argv[3] || 'review-walkthrough.mp4');
const work = fs.mkdtempSync(path.join(path.dirname(outFile), '.review-work-'));
const FPS = 30;

fs.symlinkSync(framesIn, path.join(work, 'frames'));
fs.copyFileSync(path.join(__dirname, 'scene.html'), path.join(work, 'scene.html'));
const timeline = JSON.parse(fs.readFileSync(path.join(framesIn, 'timeline.json'), 'utf8'));
fs.mkdirSync(path.join(work, 'out'));

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 }, deviceScaleFactor: 1 });
  await page.goto('file://' + path.join(work, 'scene.html'));
  const duration = await page.evaluate((tl) => { window.init(tl); return window.DURATION; }, timeline);
  const total = Math.round(duration * FPS);
  for (let i = 0; i < total; i++) {
    await page.evaluate((t) => window.renderAt(t), i / FPS);
    await page.screenshot({ path: path.join(work, 'out', `v${String(i).padStart(5, '0')}.png`) });
  }
  await browser.close();
  // A silent stereo track: some upload pipelines reject video-only files.
  execFileSync('ffmpeg', ['-y', '-loglevel', 'error', '-framerate', String(FPS), '-i', path.join(work, 'out', 'v%05d.png'),
    '-f', 'lavfi', '-i', 'anullsrc=channel_layout=stereo:sample_rate=48000',
    '-c:v', 'libx264', '-pix_fmt', 'yuv420p', '-crf', '20', '-preset', 'slow', '-c:a', 'aac', '-shortest',
    '-movflags', '+faststart', outFile], { stdio: 'inherit' });
  fs.rmSync(work, { recursive: true, force: true });
  console.log(`${outFile}: ${duration.toFixed(1)} s`);
})();
