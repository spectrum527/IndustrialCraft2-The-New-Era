"""Rebuild the original mono IC2 sound set from oscillators and filtered noise (requires ffmpeg)."""
import argparse
import array
import hashlib
import json
import math
import os
import random
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RATE = 22050
TAU = math.tau
SABER_VOICES = {
    # Duration, start frequency, end frequency, and relative amplitude.
    'idle': (.18, 225, 180, .65),
    'power_up': (.24, 300, 620, 1.0),
    'swing1': (.19, 505, 240, .88),
    'swing2': (.21, 465, 205, .88),
    'swing3': (.20, 540, 270, .88),
}


def saber_cue(voice, rng):
    seconds, start, end, gain = SABER_VOICES[voice]
    samples = array.array('h')
    air = 0.0
    for index in range(round(seconds * RATE)):
        t = index / RATE
        fraction = t / seconds
        phase = TAU * t * (start + (end - start) * fraction / 2)
        air += .11 * (rng.uniform(-1, 1) - air)
        attack = math.sin(math.pi / 2 * min(1, t / .024)) ** 2
        tail = (1 - fraction) ** 1.6
        tone = .080 * math.sin(phase) + .017 * math.sin(phase * 2)
        samples.append(round(32767 * gain * attack * tail * (tone + .065 * air)))
    return samples.tobytes()


def sound(event):
    seed = int.from_bytes(hashlib.sha256(event.encode()).digest()[:8], 'little')
    rng = random.Random(seed)
    if event.startswith('item.nanosaber.'):
        return saber_cue(event.rsplit('.', 1)[1], rng)
    running = event.endswith(('.loop', '.operate', '.idle', '.charge'))
    laser = '.laser.' in event or 'teleporter' in event
    air = any(name in event for name in ('wind', 'geothermal', 'water', 'furnace'))
    hammer = any(name in event for name in ('compressor', 'miner', 'macerator', 'recycler', 'chainsaw','drill'))
    impact = any(name in event for name in ('explode','overload','wrench','crowbar','cutter'))
    seconds = 2.4 if running else (1.5 if 'explode' in event else .34 + .3*rng.random())
    count = round(seconds * RATE)
    frequency = round((45 + seed % 49) * seconds) / seconds
    rotor = (3 + seed % 8) / seconds
    samples = array.array('h')
    filtered = 0.0
    flutter = 0.0
    for index in range(count):
        t = index / RATE
        fraction = index / count
        noise = rng.uniform(-1, 1)
        filtered += .063*(noise-filtered)
        flutter += .008*(filtered-flutter)
        phase = TAU*frequency*t
        if laser:
            glide = t * (580 + seed%350) * (1-.36*fraction if 'power_up' not in event else .65+.42*fraction)
            value = .35*math.sin(TAU*glide + 1.4*math.sin(phase)) + filtered*.35
        elif impact:
            resonance = .29*math.sin(phase)*math.exp(-5*t) + .18*math.sin(phase*2.5)*math.exp(-12*t)
            value = resonance + .95*filtered*math.exp(-3*t) + .41*noise*math.exp(-45*t)
        elif air:
            value = .7*filtered + .48*flutter + .1*math.sin(phase) + .06*math.sin(2*phase)
            value *= .78+.2*math.sin(TAU*t/seconds)
        else:
            gear = .22*math.sin(phase) + .10*math.sin(phase*2) + .07*math.sin(phase*4 + .5*math.sin(TAU*rotor*t))
            pad = math.exp(-10*((t*rotor)%1)) if hammer else .35
            value = gear*(.69+.30*math.cos(TAU*rotor*t)) + (.44+.55*pad)*filtered + .10*noise*pad
        if running:
            envelope = min(1, t/.017, (seconds-t)/.018)
        else:
            envelope = min(1, t/.009)*math.exp(-2.6*fraction)*min(1, (seconds-t)/.05)
        samples.append(round(32767*max(-.85,min(.85,value*envelope))))
    return samples.tobytes()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--saber-only', action='store_true',
                        help='Update the nano saber clips without touching other assets')
    args = parser.parse_args()
    definitions = {}
    for event in (ROOT/'spec/registry/sounds.txt').read_text().splitlines():
        saber = event.startswith('item.nanosaber.')
        if args.saber_only and not saber:
            continue
        key = event.replace('.', '/')
        dest = ROOT/'assets/ic2/sounds'/f'{key}.ogg'
        dest.parent.mkdir(parents=True,exist_ok=True)
        subprocess.run([os.environ.get('FFMPEG', 'ffmpeg'),'-nostdin','-hide_banner','-loglevel','error','-y','-f','s16le',
                        '-ar',str(RATE),'-ac','1','-i','pipe:0','-c:a','libvorbis','-q:a','3',
                        *(['-fflags', '+bitexact'] if saber else []), str(dest)],
                       input=sound(event),check=True)
        definitions[event]={'sounds':['ic2:'+key],'subtitle':'subtitles.ic2.'+event}
    if not args.saber_only:
        (ROOT/'assets/ic2/sounds.json').write_text(
            json.dumps(definitions, indent=2, sort_keys=True) + '\n')


if __name__=='__main__':
    main()
