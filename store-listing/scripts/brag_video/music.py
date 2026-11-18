"""Synthesises the soundtrack: 120 BPM, A minor, kick/hat/bass/pluck arp + a pop on every scene cut.
Everything sits on one beat grid counted from sample 0. Usage: music.py out.wav seconds"""
import sys
import wave

import numpy as np

SR = 44100
BPM = 120
BEAT = 60 / BPM
dur = float(sys.argv[2])
n = int(SR * dur)
mix = np.zeros(n)


def add(sig, at):
    i = int(at * SR)
    if i < n:
        sig = sig[: n - i]
        mix[i : i + len(sig)] += sig


def env(length, attack=0.003, decay=8.0):
    t = np.arange(int(length * SR)) / SR
    return np.minimum(1, t / attack) * np.exp(-decay * t), t


def kick():
    e, t = env(0.35, 0.002, 9)
    f = 50 + 110 * np.exp(-t * 28)
    return 0.9 * np.sin(2 * np.pi * np.cumsum(f) / SR) * e


def hat(seed):
    e, t = env(0.08, 0.001, 55)
    return 0.12 * np.random.default_rng(seed).standard_normal(len(t)) * e


def tone(freq, length, vol, decay=6.0, harm=(1, 0.5, 0.25)):
    e, t = env(length, 0.004, decay)
    s = sum(a * np.sin(2 * np.pi * freq * (k + 1) * t) for k, a in enumerate(harm))
    return vol * s * e / sum(harm)


def hz(semi_from_a4):
    return 440 * 2 ** (semi_from_a4 / 12)


beats = int(dur / BEAT)
bass_roots = [-24, -24, -19, -21]  # A1 A1 E2 F2, one per bar
arp = [0, 3, 7, 12, 7, 3, 0, 3]    # minor arpeggio, 8th notes
for b in range(beats):
    at = b * BEAT
    bar = b // 4
    root = bass_roots[bar % 4]
    if b >= 2:
        add(kick(), at)
        add(tone(hz(root), BEAT * 0.9, 0.35, 4, (1, 0.3)), at)
    add(hat(b), at + BEAT / 2)
    for k in range(2):
        step = (b * 2 + k) % 8
        if b >= 4:
            add(tone(hz(root + 24 + arp[step]), 0.3, 0.09, 9), at + k * BEAT / 2)
# scene-cut pops, on the beat
for cut in (3, 6, 11, 16):
    add(tone(hz(24), 0.25, 0.2, 10, (1, 0.6)), cut)
    add(tone(hz(36), 0.4, 0.1, 6, (1,)), cut)
mix = np.tanh(mix * 1.1)
fade = np.minimum(1, (n - np.arange(n)) / (SR * 0.8))
mix = mix * fade * 0.8
with wave.open(sys.argv[1], "wb") as w:
    w.setnchannels(1)
    w.setsampwidth(2)
    w.setframerate(SR)
    w.writeframes((mix * 32767).astype(np.int16).tobytes())
