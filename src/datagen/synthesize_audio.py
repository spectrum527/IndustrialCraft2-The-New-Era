"""Rebuild the original mono IC2 sound set from oscillators and filtered noise (requires ffmpeg)."""
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


def sound(event):
    seed = int.from_bytes(hashlib.sha256(event.encode()).digest()[:8], 'little')
    rng = random.Random(seed)
    running = event.endswith(('.loop', '.operate', '.idle', '.charge'))
    laser = '.laser.' in event or 'nanosaber' in event or 'teleporter' in event
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
    definitions = {}
    for event in (ROOT/'spec/registry/sounds.txt').read_text().splitlines():
        key = event.replace('.', '/')
        dest = ROOT/'assets/ic2/sounds'/f'{key}.ogg'
        dest.parent.mkdir(parents=True,exist_ok=True)
        subprocess.run([os.environ.get('FFMPEG', 'ffmpeg'),'-nostdin','-hide_banner','-loglevel','error','-y','-f','s16le',
                        '-ar',str(RATE),'-ac','1','-i','pipe:0','-c:a','libvorbis','-q:a','3',str(dest)],
                       input=sound(event),check=True)
        definitions[event]={'sounds':['ic2:'+key],'subtitle':'subtitles.ic2.'+event}
    (ROOT/'assets/ic2/sounds.json').write_text(json.dumps(definitions,indent=2,sort_keys=True)+'\n')


if __name__=='__main__':
    main()
