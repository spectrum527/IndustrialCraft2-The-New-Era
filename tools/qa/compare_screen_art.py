"""Compare native Minecraft framebuffer captures to the supplied IC2 GUI sheets."""
import argparse
import hashlib
import json
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw


def unpack(source):
    entries = json.loads(source.read_text(encoding='utf-8'))
    if isinstance(entries, list):
        return entries
    return [dict(row, id=key) for key, row in entries.items()]


def common_frame(sheet, width, height):
    frame = Image.new('RGBA', (width, height))
    def strips(length):
        position = 0
        while position < length:
            if position == 0:
                yield position, 16, 8
                position += 8
            elif position == length - 8:
                yield position, 72, 8
                position += 8
            else:
                size = min(48, length - position - 8)
                yield position, 24, size
                position += size
    for y, v, h in strips(height):
        for x, u, w in strips(width):
            frame.paste(sheet.crop((u, v, u + w, v + h)), (x, y))
    return frame


def count(expected, observed, mask):
    red, green, blue = ImageChops.difference(expected.convert('RGB'), observed).split()
    differences = ImageChops.lighter(ImageChops.lighter(red, green), blue)
    buckets = differences.histogram(mask)
    total = sum(buckets)
    return {'exact': buckets[0], 'sampled': total, 'percent': round(100 * buckets[0] / total, 2) if total else None}


def audit(root, record, source):
    geometry = record.get('panel') or record.get('geometry')
    if not geometry:
        raise ValueError(f'{record["id"]}: missing native geometry')
    image = Path(record.get('png') or source.parent / record['image'])
    atlas = record.get('atlas') or geometry.get('atlas')
    if not atlas:
        raise ValueError(f'{image}: missing atlas')
    atlas = atlas.removeprefix('ic2:').removeprefix('textures/gui/').removesuffix('.png')
    texture = root / 'assets/ic2/textures/gui' / (atlas + '.png')
    sheet = Image.open(texture).convert('RGBA')
    screenshot = Image.open(image).convert('RGB')
    w, h = geometry['width'], geometry['height']
    scale = geometry.get('scale', geometry.get('guiScale'))
    x = geometry.get('x', geometry.get('left'))
    y = geometry.get('y', geometry.get('top'))
    if w <= 0 or h <= 0 or scale <= 0:
        raise ValueError(f'{image}: invalid GUI dimensions')
    bounds = tuple(round(k * scale) for k in (x, y, x + w, y + h))
    observed = screenshot.crop(bounds).resize((w, h), Image.Resampling.NEAREST)
    common = atlas == 'common'
    expected = common_frame(sheet, w, h) if common else sheet.crop((0, 0, w, h))
    if common:
        well = sheet.crop((103, 7, 121, 25))
        for slot in geometry['slots']:
            expected.alpha_composite(well, (slot['x'] - 1, slot['y'] - 1))
    # Count every opaque author pixel before masking text, native items or animated gauges.
    opaque = expected.getchannel('A').point(lambda n: 255 if n == 255 else 0)
    upper = opaque.copy()
    ImageDraw.Draw(upper).rectangle((0, max(20, h - 86), w, h), fill=0)
    border = Image.new('L', (w, h))
    ImageDraw.Draw(border).rectangle((0, 0, w-1, h-1), outline=255, width=4)
    border = ImageChops.multiply(opaque, border)
    # A mostly grey fill or the inventory strip alone must not pass as an authored upper panel.
    return {'id': record['id'], 'texture': atlas, 'textureSha256': hashlib.sha256(texture.read_bytes()).hexdigest(),
            'screenshotSha256': hashlib.sha256(image.read_bytes()).hexdigest(),
            'dimensions': [w,h], 'framebuffer': list(screenshot.size), 'scale': scale,
            'whole': count(expected, observed, opaque), 'upper': count(expected, observed, upper),
            'border': count(expected, observed, border)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('records', type=Path, nargs='+')
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument('--output', type=Path, required=True)
    opts = parser.parse_args()
    rows = {}
    for path in opts.records:
        for record in unpack(path):
            rows[record['id']] = audit(opts.root, record, path)
    opts.output.parent.mkdir(parents=True, exist_ok=True)
    opts.output.write_text(json.dumps({'method': 'Opaque RGB pixels, native-scale nearest samples. Upper section excludes the bottom 86 pixels. Counts include items, labels, states and controls.', 'screens':rows},indent=2)+'\n',encoding='utf-8')
    for row in sorted(rows.values(),key=lambda r:r['upper']['percent'] or 0)[:6]:
        print(row['id'],row['texture'],row['whole']['percent'],row['upper']['percent'])
    print(f'{len(rows)} real screens compared')


if __name__ == '__main__':
    main()
