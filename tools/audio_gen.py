#!/usr/bin/env python3
"""
Brain Blitz audio generator.

Synthesises two musical loops and a set of sound effects from scratch:
no samples, no stock libraries. Everything here is written as waveforms.

Output: brain-blitz/app/src/main/res/raw/*.wav
"""

import os
import wave
import numpy as np

SR = 44100
OUT = "brain-blitz/app/src/main/res/raw"
rng = np.random.default_rng(7)

os.makedirs(OUT, exist_ok=True)


# ---------------------------------------------------------------- utilities
def midi_to_hz(m):
    return 440.0 * (2.0 ** ((m - 69) / 12.0))


def t_axis(dur):
    return np.arange(int(dur * SR)) / SR


def adsr(dur, a=0.005, d=0.08, s=0.7, r=0.2):
    n = max(int(dur * SR), 8)
    na = max(int(a * SR), 1)
    nd = max(int(d * SR), 1)
    nr = max(int(r * SR), 1)
    ns = max(n - na - nd - nr, 1)
    env = np.concatenate([
        np.linspace(0.0, 1.0, na),
        np.linspace(1.0, s, nd),
        np.full(ns, s),
        np.linspace(s, 0.0, nr),
    ])
    if len(env) < n:
        env = np.pad(env, (0, n - len(env)))
    return env[:n]


def fft_filter(x, lo=None, hi=None, slope=2.0):
    """Gentle one-pole-ish filtering done in the frequency domain."""
    if len(x) < 8:
        return x
    spec = np.fft.rfft(x)
    freqs = np.fft.rfftfreq(len(x), 1.0 / SR)
    gain = np.ones_like(freqs)
    safe = np.maximum(freqs, 1e-6)
    if hi:
        gain = gain / (1.0 + (safe / hi) ** slope)
    if lo:
        gain = gain / (1.0 + (lo / safe) ** slope)
    return np.fft.irfft(spec * gain, len(x))


def make_ir(seconds=0.55, decay=2.6, predelay=0.012, seed=7):
    n = int(seconds * SR)
    local = np.random.default_rng(seed)
    ir = local.standard_normal(n) * np.exp(-np.linspace(0, decay, n))
    ir[: int(predelay * SR)] = 0.0
    ir = fft_filter(ir, lo=350, hi=6500)
    energy = np.sqrt(np.sum(ir ** 2)) or 1.0
    return ir / energy


IR = make_ir(seed=7)
IR_L = make_ir(seconds=0.62, decay=2.4, predelay=0.009, seed=11)
IR_R = make_ir(seconds=0.58, decay=2.8, predelay=0.016, seed=23)


def reverb(x, mix=0.22, ir=None):
    ir = IR if ir is None else ir
    n = len(x) + len(ir) - 1
    size = 1 << (n - 1).bit_length()
    wet = np.fft.irfft(np.fft.rfft(x, size) * np.fft.rfft(ir, size), size)[: len(x)]
    return x * (1 - mix) + wet * mix * 2.0


def soft_clip(x, drive=1.3):
    return np.tanh(x * drive) / np.tanh(drive)


def match_loudness(x, target_db, max_peak=0.95):
    """Bring every sound to a deliberate level instead of leaving it wherever it landed."""
    rms = float(np.sqrt(np.mean(np.square(x)))) or 1e-6
    target = 10.0 ** (target_db / 20.0)
    gain = target / rms
    gain = min(max(gain, 0.35), 6.0)
    x = x * gain
    peak = float(np.max(np.abs(x)))
    if peak > max_peak:
        x = x * (max_peak / peak)
    return x


def fade_edges(x, ms=4):
    n = int(SR * ms / 1000.0)
    if len(x) < n * 2:
        return x
    ramp = np.linspace(0, 1, n)
    x[:n] *= ramp
    x[-n:] *= ramp[::-1]
    return x


# ------------------------------------------------------------------ voices
def marimba(freq, dur, vel=1.0, bright=1.0):
    t = t_axis(dur)
    parts = [(1.0, 1.0, 5.0), (2.0, 0.45, 8.0), (3.98, 0.22 * bright, 13.0), (9.2, 0.09 * bright, 20.0)]
    sig = np.zeros_like(t)
    for ratio, amp, decay in parts:
        sig += amp * np.sin(2 * np.pi * freq * ratio * t) * np.exp(-t * decay)
    sig *= np.minimum(1.0, t * 400.0)
    return sig * vel * 0.42


def kalimba(freq, dur, vel=1.0):
    t = t_axis(dur)
    sig = (
        np.sin(2 * np.pi * freq * t) * np.exp(-t * 6.5)
        + 0.3 * np.sin(2 * np.pi * freq * 2.01 * t) * np.exp(-t * 11.0)
        + 0.12 * np.sin(2 * np.pi * freq * 5.4 * t) * np.exp(-t * 18.0)
    )
    sig *= np.minimum(1.0, t * 500.0)
    return sig * vel * 0.4


def saw_voice(freq, dur, detune=0.004, vel=1.0):
    t = t_axis(dur)
    sig = np.zeros_like(t)
    for d in (-detune, 0.0, detune):
        phase = 2 * np.pi * freq * (1 + d) * t
        sig += 2.0 * ((phase / (2 * np.pi)) % 1.0) - 1.0
    sig /= 3.0
    return sig * vel


def pad_chord(notes, dur, vel=0.5):
    out = np.zeros(int(dur * SR))
    for m in notes:
        v = saw_voice(midi_to_hz(m), dur, detune=0.006, vel=1.0)
        v = fft_filter(v, hi=1800)
        out[: len(v)] += v
    out *= adsr(dur, a=0.35, d=0.3, s=0.8, r=0.9)
    return out * vel * 0.14


def bass_note(midi, dur, vel=1.0):
    f = midi_to_hz(midi)
    t = t_axis(dur)
    body = np.sin(2 * np.pi * f * t) + 0.35 * np.sin(4 * np.pi * f * t)
    body *= np.exp(-t * 2.2)
    body = fft_filter(body, hi=900)
    return body * vel * 0.75


def kick(vel=1.0):
    t = t_axis(0.42)
    f = 44 + 95 * np.exp(-t * 26)
    phase = np.cumsum(2 * np.pi * f / SR)
    body = np.sin(phase) * np.exp(-t * 7.5)
    click = rng.standard_normal(len(t)) * np.exp(-t * 220) * 0.25
    return (body + click) * vel * 0.95


def rim(vel=1.0):
    t = t_axis(0.12)
    tone = np.sin(2 * np.pi * 320 * t) * np.exp(-t * 45) * 0.5
    noise = rng.standard_normal(len(t)) * np.exp(-t * 60)
    noise = fft_filter(noise, lo=900, hi=5200)
    return (tone + noise) * vel * 0.5


def hat(vel=1.0, open_=False):
    dur = 0.07 if not open_ else 0.22
    t = t_axis(dur)
    noise = rng.standard_normal(len(t)) * np.exp(-t * (70 if not open_ else 22))
    noise = fft_filter(noise, lo=7000)
    return noise * vel * 0.3


def shaker(vel=1.0):
    t = t_axis(0.09)
    noise = rng.standard_normal(len(t)) * np.exp(-t * 34)
    noise = fft_filter(noise, lo=3800, hi=11000)
    return noise * vel * 0.28


# --------------------------------------------------------------- song render
class Mix:
    def __init__(self, seconds):
        self.buf = np.zeros((2, int(seconds * SR) + SR))

    def add(self, sig, at, pan=0.0, gain=1.0):
        i = int(at * SR)
        n = min(len(sig), self.buf.shape[1] - i)
        if n <= 0:
            return
        left = gain * np.sqrt(0.5 * (1.0 - pan))
        right = gain * np.sqrt(0.5 * (1.0 + pan))
        self.buf[0, i:i + n] += sig[:n] * left
        self.buf[1, i:i + n] += sig[:n] * right

    def render(self, peak=0.89):
        out = self.buf.copy()
        out = soft_clip(out, 1.15)
        m = np.max(np.abs(out)) or 1.0
        return out * (peak / m)


def pentatonic_phrase(scale, bars, beat, swing=0.06):
    """A short singable motif, varied per bar so it never sounds looped to death."""
    events = []
    motif = [0, 2, 1, 3, 2, 4, 3, 1]
    for bar in range(bars):
        beats = [0.0, 0.75, 1.5, 2.25, 3.0, 3.5]
        for k, b in enumerate(beats):
            if (bar + k) % 5 == 4:
                continue
            idx = motif[(bar * 3 + k) % len(motif)]
            dur = 0.5 if k % 2 == 0 else 0.32
            events.append((bar * 4 * beat + b * beat + (swing * beat if b % 1 else 0), scale[idx % len(scale)], dur))
    return events


def render_menu():
    """Warm lofi-afro loop: 92 bpm, 16 bars of A minor."""
    bpm = 92
    beat = 60.0 / bpm
    bars = 16
    total = bars * 4 * beat
    mix = Mix(total)

    chords = [
        [57, 60, 64, 67],   # Am7
        [53, 57, 60, 64],   # Fmaj7
        [60, 64, 67, 71],   # Cmaj7
        [55, 59, 62, 64],   # G6
    ]
    roots = [45, 41, 48, 43]  # A2 F2 C3 G2

    for bar in range(bars):
        bar_time = bar * 4 * beat
        chord = chords[bar % 4]

        # pad, held across the bar
        mix.add(pad_chord(chord, 4 * beat * 1.02), bar_time, pan=0.0, gain=0.85)

        # kalimba arpeggio answering the pad
        for k, note in enumerate([chord[0] + 12, chord[2] + 12, chord[1] + 12, chord[3] + 12]):
            at = bar_time + (k * 0.75) * beat
            mix.add(kalimba(midi_to_hz(note), 0.9, vel=0.55 + 0.1 * (k % 2)), at, pan=-0.62 + 0.42 * (k % 2), gain=0.5)

        # bass: syncopated, sits behind the beat
        root = roots[bar % 4]
        for offset, octave, vel in [(0.0, 0, 1.0), (1.25, 0, 0.75), (2.5, 12, 0.65), (3.25, 0, 0.8)]:
            mix.add(bass_note(root + octave, 0.6, vel), bar_time + offset * beat, pan=0.0, gain=0.9)

        # drums
        mix.add(kick(), bar_time, gain=0.9)
        mix.add(kick(0.8), bar_time + 2.75 * beat, gain=0.7)
        mix.add(rim(0.9), bar_time + 2 * beat, pan=0.32, gain=0.55)
        mix.add(rim(0.7), bar_time + 3.5 * beat, pan=0.15, gain=0.4)
        for step in range(16):
            at = bar_time + step * beat / 4.0
            if step % 4 == 2:
                mix.add(hat(0.7, open_=True), at, pan=0.62, gain=0.32)
            elif step % 2 == 0:
                mix.add(hat(0.6), at, pan=0.58, gain=0.22)
            if step % 2 == 1:
                mix.add(shaker(0.6 + 0.25 * (step % 4 == 3)), at, pan=-0.72, gain=0.24)

    # the singable part
    scale = [69, 72, 74, 76, 79, 81]  # A minor pentatonic, up high
    for at, note, dur in pentatonic_phrase(scale, bars, beat):
        mix.add(marimba(midi_to_hz(note), dur + 0.5, vel=0.5), at, pan=-0.14, gain=0.42)

    out = mix.render(0.82)

    # vinyl warmth
    crackle = (rng.standard_normal(out.shape[1]) * 0.004)
    crackle = fft_filter(crackle, lo=900, hi=7000)
    out[0] += crackle
    out[1] += np.roll(crackle, 37)

    out = np.stack([reverb(out[0], 0.24, IR_L), reverb(out[1], 0.24, IR_R)])
    out = out[:, : int(bars * 4 * beat * SR)]
    out = np.stack([match_loudness(out[0], -15.0), match_loudness(out[1], -15.0)])
    return np.stack([fade_edges(out[0], 14), fade_edges(out[1], 14)])


def render_arcade():
    """Driving game loop: 118 bpm, 16 bars of E minor."""
    bpm = 118
    beat = 60.0 / bpm
    bars = 16
    total = bars * 4 * beat
    mix = Mix(total)

    chords = [
        [52, 55, 59, 62],  # Em7
        [48, 52, 55, 59],  # Cmaj7
        [55, 59, 62, 64],  # G6
        [50, 54, 57, 62],  # D
    ]
    roots = [40, 36, 43, 38]

    for bar in range(bars):
        bar_time = bar * 4 * beat
        chord = chords[bar % 4]
        mix.add(pad_chord([n + 12 for n in chord], 4 * beat, vel=0.32), bar_time, gain=0.7)

        # driving eighth-note bass
        for step in range(8):
            octave = 12 if step % 4 == 3 else 0
            vel = 0.9 if step % 2 == 0 else 0.6
            mix.add(bass_note(roots[bar % 4] + octave, 0.28, vel), bar_time + step * beat / 2, gain=0.95)

        # four on the floor with a syncopated ghost
        for b in range(4):
            mix.add(kick(1.0 if b in (0, 2) else 0.72), bar_time + b * beat, gain=0.9)
        mix.add(kick(0.55), bar_time + 3.5 * beat, gain=0.5)

        for step in range(16):
            at = bar_time + step * beat / 4.0
            if step % 4 == 2:
                mix.add(hat(0.75, open_=True), at, pan=0.65, gain=0.3)
            else:
                mix.add(hat(0.55), at, pan=0.6, gain=0.2)
            if step in (3, 11):
                mix.add(rim(0.8), at, pan=-0.45, gain=0.35)

        # bright arpeggio lead
        arp = [chord[0] + 24, chord[1] + 24, chord[2] + 24, chord[3] + 24, chord[2] + 24, chord[1] + 24]
        for k, note in enumerate(arp):
            at = bar_time + k * beat / 2.0
            mix.add(marimba(midi_to_hz(note), 0.45, vel=0.42, bright=1.4), at, pan=-0.55 if k % 2 else 0.55, gain=0.5)
            # answering echo across the field, a quarter beat later
            mix.add(
                marimba(midi_to_hz(note), 0.35, vel=0.26, bright=0.9),
                at + beat / 4.0,
                pan=0.6 if k % 2 else -0.6,
                gain=0.32,
            )

    out = mix.render(0.86)
    out = np.stack([reverb(out[0], 0.18, IR_L), reverb(out[1], 0.18, IR_R)])
    out = out[:, : int(bars * 4 * beat * SR)]
    out = np.stack([match_loudness(out[0], -15.0), match_loudness(out[1], -15.0)])
    return np.stack([fade_edges(out[0], 14), fade_edges(out[1], 14)])


# --------------------------------------------------------------------- sfx
def sfx_tap():
    sig = kalimba(midi_to_hz(84), 0.16, 0.9)
    return fade_edges(reverb(sig, 0.12))


def sfx_correct():
    notes = [72, 76, 79, 84]
    out = np.zeros(int(0.9 * SR))
    for i, m in enumerate(notes):
        sig = marimba(midi_to_hz(m), 0.7, 0.85, bright=1.3)
        at = int(i * 0.065 * SR)
        out[at:at + len(sig)] += sig[: len(out) - at]
    out = fade_edges(reverb(out, 0.3))
    return out * 0.8


def sfx_wrong():
    t = t_axis(0.5)
    low = saw_voice(midi_to_hz(52), 0.5, detune=0.012)
    low = fft_filter(low, hi=700)
    low *= np.exp(-t * 9)
    thump = np.sin(2 * np.pi * 90 * t) * np.exp(-t * 16) * 0.5
    sig = low * 0.6 + thump
    return fade_edges(reverb(sig, 0.16)) * 0.75


def sfx_streak():
    notes = [74, 76, 79, 81, 84, 88]
    out = np.zeros(int(0.75 * SR))
    for i, m in enumerate(notes):
        sig = marimba(midi_to_hz(m), 0.4, 0.7 + 0.05 * i, bright=1.5)
        at = int(i * 0.055 * SR)
        out[at:at + len(sig)] += sig[: len(out) - at]
    return fade_edges(reverb(out, 0.34)) * 0.8


def bell(freq, dur, index=2.4, ratio=3.5):
    t = t_axis(dur)
    mod = np.sin(2 * np.pi * freq * ratio * t) * index * np.exp(-t * 5)
    sig = np.sin(2 * np.pi * freq * t + mod) * np.exp(-t * 3.6)
    return sig * np.minimum(1.0, t * 600.0)


def sfx_word():
    sig = bell(midi_to_hz(88), 0.9, index=3.0)
    sparkle = np.zeros(int(0.9 * SR))
    for i, m in enumerate([96, 100, 103]):
        s = bell(midi_to_hz(m), 0.5, index=1.6)
        at = int((0.06 + i * 0.05) * SR)
        sparkle[at:at + len(s)] += s[: len(sparkle) - at] * 0.35
    return fade_edges(reverb(sig * 0.7 + sparkle, 0.4)) * 0.85


def bubble(start=320, end=900, dur=0.11):
    t = t_axis(dur)
    f = np.linspace(start, end, len(t))
    phase = np.cumsum(2 * np.pi * f / SR)
    return np.sin(phase) * np.exp(-t * 26) * np.minimum(1.0, t * 900)


def sfx_pop():
    return fade_edges(reverb(bubble(300, 820) * 1.0, 0.12)) * 0.85


def sfx_cascade():
    out = np.zeros(int(0.6 * SR))
    for i, (a, b) in enumerate([(320, 700), (420, 900), (520, 1120), (620, 1320)]):
        s = bubble(a, b, 0.1)
        at = int(i * 0.07 * SR)
        out[at:at + len(s)] += s[: len(out) - at]
    return fade_edges(reverb(out, 0.2)) * 0.85


def sfx_swap():
    t = t_axis(0.12)
    click = rng.standard_normal(len(t)) * np.exp(-t * 120)
    click = fft_filter(click, lo=1200, hi=7000)
    tone = np.sin(2 * np.pi * 660 * t) * np.exp(-t * 55) * 0.35
    return fade_edges((click * 0.5 + tone)) * 0.7


def sfx_snake_eat():
    out = np.zeros(int(0.35 * SR))
    s1 = bubble(520, 1040, 0.09)
    s2 = marimba(midi_to_hz(91), 0.28, 0.6)
    out[: len(s1)] += s1
    at = int(0.03 * SR)
    out[at:at + len(s2)] += s2[: len(out) - at]
    return fade_edges(reverb(out, 0.18)) * 0.8


def sfx_snake_die():
    t = t_axis(0.9)
    f = np.linspace(520, 80, len(t))
    phase = np.cumsum(2 * np.pi * f / SR)
    sig = np.sin(phase) * np.exp(-t * 3.4)
    sig += fft_filter(rng.standard_normal(len(t)), lo=300, hi=2400) * np.exp(-t * 6) * 0.25
    return fade_edges(reverb(sig, 0.25)) * 0.8


def sfx_line_clear():
    out = np.zeros(int(1.1 * SR))
    # riser
    t = t_axis(0.35)
    riser = rng.standard_normal(len(t)) * np.linspace(0.05, 0.6, len(t))
    riser = fft_filter(riser, lo=800, hi=9000)
    out[: len(riser)] += riser
    # chord stab
    stab = np.zeros(int(0.75 * SR))
    for m in [72, 76, 79, 84]:
        s = marimba(midi_to_hz(m), 0.7, 0.6, bright=1.6)
        stab[: len(s)] += s
    at = int(0.3 * SR)
    out[at:at + len(stab)] += stab[: len(out) - at]
    return fade_edges(reverb(out, 0.3)) * 0.85


def sfx_rotate():
    t = t_axis(0.09)
    sig = np.sin(2 * np.pi * np.linspace(900, 1400, len(t)) * t) * np.exp(-t * 45)
    return fade_edges(sig) * 0.55


def sfx_win():
    out = np.zeros(int(1.8 * SR))
    chords = [[72, 76, 79], [74, 77, 81], [76, 79, 83], [79, 83, 88]]
    for i, ch in enumerate(chords):
        seg = np.zeros(int(0.7 * SR))
        for m in ch:
            s = marimba(midi_to_hz(m), 0.6, 0.7, bright=1.4)
            seg[: len(s)] += s * 0.6
        at = int(i * 0.16 * SR)
        out[at:at + len(seg)] += seg[: len(out) - at]
    return fade_edges(reverb(out, 0.34)) * 0.85


def sfx_gameover():
    out = np.zeros(int(1.6 * SR))
    for i, m in enumerate([76, 72, 69, 64]):
        s = marimba(midi_to_hz(m), 0.9, 0.75, bright=0.9)
        at = int(i * 0.19 * SR)
        out[at:at + len(s)] += s[: len(out) - at]
    pad = pad_chord([52, 55, 59], 1.2, 0.4)
    out[: len(pad)] += pad
    return fade_edges(reverb(out, 0.32)) * 0.85


def sfx_splash():
    out = np.zeros(int(1.5 * SR))
    t = t_axis(1.0)
    riser = rng.standard_normal(len(t)) * np.linspace(0.0, 0.55, len(t)) ** 1.6
    riser = fft_filter(riser, lo=500, hi=11000)
    out[: len(riser)] += riser
    bloom = np.zeros(int(0.9 * SR))
    for m in [64, 71, 76, 83]:
        s = marimba(midi_to_hz(m), 0.8, 0.55, bright=1.7)
        bloom[: len(s)] += s * 0.5
    at = int(0.72 * SR)
    out[at:at + len(bloom)] += bloom[: len(out) - at]
    return fade_edges(reverb(out, 0.35)) * 0.85


def sfx_timewarn():
    out = np.zeros(int(0.5 * SR))
    for i in range(2):
        s = kalimba(midi_to_hz(76), 0.18, 0.7)
        at = int(i * 0.16 * SR)
        out[at:at + len(s)] += s[: len(out) - at]
    return fade_edges(reverb(out, 0.15)) * 0.65


def sfx_levelup():
    out = np.zeros(int(0.9 * SR))
    for i, m in enumerate([72, 76, 79, 84, 88]):
        s = marimba(midi_to_hz(m), 0.5, 0.65, bright=1.5)
        at = int(i * 0.07 * SR)
        out[at:at + len(s)] += s[: len(out) - at]
    return fade_edges(reverb(out, 0.28)) * 0.8


# ------------------------------------------------------------------- output
def write_wav(name, data, stereo=True):
    path = os.path.join(OUT, name + ".wav")
    if data.ndim == 1:
        data = np.stack([data, data]) if stereo else data
    seconds = data.shape[-1] / SR
    data = np.clip(data, -1.0, 1.0)
    pcm = (data.T * 32767).astype("<i2")
    channels = 1 if data.ndim == 1 else data.shape[0]
    with wave.open(path, "wb") as w:
        w.setnchannels(channels)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    size = os.path.getsize(path)
    print(f"  {name + '.wav':22s} {size/1024:8.1f} KB  {seconds:5.1f}s  {channels}ch")
    return size


print("rendering music")
total = 0
total += write_wav("music_menu", render_menu())
total += write_wav("music_arcade", render_arcade())

print("rendering sfx")
mono_sfx = {
    "sfx_tap": sfx_tap,
    "sfx_correct": sfx_correct,
    "sfx_wrong": sfx_wrong,
    "sfx_streak": sfx_streak,
    "sfx_word": sfx_word,
    "sfx_pop": sfx_pop,
    "sfx_cascade": sfx_cascade,
    "sfx_swap": sfx_swap,
    "sfx_snake_eat": sfx_snake_eat,
    "sfx_snake_die": sfx_snake_die,
    "sfx_line_clear": sfx_line_clear,
    "sfx_rotate": sfx_rotate,
    "sfx_win": sfx_win,
    "sfx_gameover": sfx_gameover,
    "sfx_splash": sfx_splash,
    "sfx_timewarn": sfx_timewarn,
    "sfx_levelup": sfx_levelup,
}
LOUDNESS = {
    "sfx_tap": -20.0,
    "sfx_correct": -16.0,
    "sfx_wrong": -19.0,
    "sfx_streak": -15.0,
    "sfx_word": -14.5,
    "sfx_pop": -17.5,
    "sfx_cascade": -16.0,
    "sfx_swap": -21.0,
    "sfx_snake_eat": -18.0,
    "sfx_snake_die": -15.0,
    "sfx_line_clear": -14.5,
    "sfx_rotate": -21.0,
    "sfx_win": -14.5,
    "sfx_gameover": -16.0,
    "sfx_splash": -16.5,
    "sfx_timewarn": -21.0,
    "sfx_levelup": -15.5,
}

for name, fn in mono_sfx.items():
    total += write_wav(name, match_loudness(fn(), LOUDNESS.get(name, -18.0)), stereo=False)

print(f"\ntotal raw audio: {total/1024/1024:.2f} MB")
