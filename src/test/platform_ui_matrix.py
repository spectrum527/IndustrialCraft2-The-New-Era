"""Open native authored IC2 screens against a dedicated NeoForge server."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import time


TARGET = '2 100 0'


def load_cases(spec):
    cases = json.loads(spec.read_text(encoding='utf-8'))
    if not isinstance(cases, list) or not cases:
        raise ValueError('Expected a nonempty list of screen cases')
    for case in cases:
        if not re.fullmatch(r'[a-z0-9_]+', case['id']):
            raise ValueError(f'Invalid screen ID: {case["id"]}')
    return cases


def check_layout(case, state):
    panel = state.get('panel')
    if panel is None:
        if case.get('nativeContainer', True):
            raise AssertionError(f'{case["id"]} did not open a native container: {state}')
        return
    if any(panel[key] <= 0 for key in ('width', 'height', 'scale', 'menuId')):
        raise AssertionError(f'{case["id"]} invalid native dimensions: {panel}')
    if 'size' in case and [panel['width'], panel['height']] != case['size']:
        raise AssertionError(f'{case["id"]} dimensions differ: {panel}')
    slots = panel['slots']
    points = [(slot['x'], slot['y']) for slot in slots]
    for i, slot in enumerate(slots):
        for other in slots[i+1:]:
            if (max(slot['x'], other['x']) < min(slot['x'] + slot['width'], other['x'] + other['width'])
                    and max(slot['y'], other['y']) < min(slot['y'] + slot['height'], other['y'] + other['height'])):
                raise AssertionError(f'{case["id"]} has overlapping native slots: {slot} and {other}')
    if any(slot['x'] < 0 or slot['y'] < 0 or
           slot['x'] + slot['width'] > panel['width'] or
           slot['y'] + slot['height'] > panel['height'] for slot in slots):
        raise AssertionError(f'{case["id"]} contains an off-panel slot: {panel}')
    if 'slots' in case and len(slots) != case['slots']:
        raise AssertionError(f'{case["id"]} expected {case["slots"]} slots, got {len(slots)}')
    for point in case.get('anchors', []):
        if tuple(point) not in points:
            raise AssertionError(f'{case["id"]} missing slot anchor {point}: {points}')


def run_cases(client, command, player, session, cases, capture, escape, clear):
    command('fill -3 99 -3 5 99 3 minecraft:stone')
    command(f'tp {player} 0.5 100 0.5 -90 12')
    client.check('prepared-ui-dock', lambda state: state.get('uiY') == 100)
    screenshots = []
    failures = []
    stamp_file = session / 'server-jar-start.json'
    if not stamp_file.is_file():
        stamp_file = session / 'jar-start.json'
    stamp = json.loads(stamp_file.read_text(encoding='utf-8'))

    def record(case, name, state):
        if state.get('panel'):
            client.act('move-ui:-8,-8')
        client.act(clear)
        screenshot = capture(name)
        screenshots.append({'id': name, 'screen': state['screen'], 'title': state['title'],
                            'panel': state.get('panel'), 'atlas': case.get('atlas'),
                            'atlasSha256': stamp['entries'].get('assets/ic2/textures/gui/' + str(case.get('atlas')) + '.png'),
                            'productionJarSha256': stamp['jarSha256'],
                            'measurements': {key: state[key] for key in ('machineEnergy', 'machineProgress',
                                             'fuelFraction', 'redstoneMode', 'meterMode', 'meterSamples') if key in state},
                            'image': screenshot.name,
                            'sha256': hashlib.sha256(screenshot.read_bytes()).hexdigest()})
        (session / 'authored-screens.json').write_text(json.dumps(screenshots, indent=2) + '\n', encoding='utf-8')

    def open_case(case):
        name = case['id']
        item = case.get('kind') == 'item'
        target = case.get('target', 'stone' if item else name)
        target_id = target if ':' in target else 'ic2:' + target
        if target == 'stone':
            target_id = 'minecraft:stone'
        command('fill 1 97 -3 8 103 3 minecraft:air')
        command('fill 1 99 -3 8 99 3 minecraft:stone')
        command(f'setblock {TARGET} {target_id}{case.get("state", "")}')
        held = 'ic2:' + name if item else case.get('held', 'minecraft:air')
        command(f'item replace entity {player} weapon.mainhand with {held}')
        for setup in case.get('setup', []):
            command(setup.replace('{target}', TARGET).replace('{player}', player))
        client.check('ready-' + name, lambda state:
                     state.get('uiTarget') == target_id and state.get('uiHeld') == held, timeout=12)
        action = 'ui-use-on' if item and 'target' in case else 'ui-item' if item else 'ui'
        action += ':' + (name if item else target_id.split(':', 1)[1])

        def open_screen(label):
            client.act(action)
            return client.check(label, lambda value:
                                value['screen'] == case['screen'] if 'screen' in case else bool(value['screen']), timeout=12)

        def match_item(value, expected):
            return all(value.get(key) == wanted for key, wanted in expected.items() if key != 'at')

        def check_slot(expected, label):
            return client.check(label, lambda value: any(
                [it['x'], it['y']] == expected['at'] and match_item(it, expected)
                for it in value.get('panel', {}).get('slots', [])), timeout=12)

        state = open_screen('open-' + name)
        if 'atLeast' in case:
            state = client.check('active-' + name, lambda value: all(value.get(key, 0) >= required
                                 for key, required in case['atLeast'].items()), timeout=12)
        for expected in case.get('items', []):
            state = check_slot(expected, f'{name}-slot-{expected["item"]}')
        record(case, name, state)
        check_layout(case, state)
        for click in case.get('clicks', []):
            x, y = click['at']
            client.act(f'click-ui:{x},{y}')
            if 'field' in click:
                expected = click['value']
                state = client.check(f'{name}-{click["field"]}-{expected}',
                                     lambda value: value.get(click['field']) == expected, timeout=12)
            if 'expectSlot' in click:
                slot = click['expectSlot']
                state = check_slot(slot, f'{name}-slot-{slot["item"]}')
            elif 'snapshot' in click:
                time.sleep(.3)
                state = client.act('state')
            if 'snapshot' in click:
                page = {'id': click['snapshot'], 'atlas': case.get('atlas')}
                page.update(click.get('layout', {}))
                check_layout(page, state)
                record(page, click['snapshot'], state)
        client.act(escape)
        client.check('close-' + name, lambda value: value['world'] and not value['screen'], timeout=12)
        if 'returnItem' in case:
            client.check('returned-' + name, lambda value: any(match_item(it, case['returnItem'])
                         for it in value.get('uiInventory', [])), timeout=12)
        if 'reopen' in case:
            open_screen('reopen-' + name)
            state = check_slot(case['reopen'], 'retained-' + name)
            record(case, name + '_reopened', state)
            client.act(escape)
            client.check('close-reopened-' + name, lambda value: not value['screen'], timeout=12)

    for case in cases:
        try:
            open_case(case)
        except (AssertionError, TimeoutError, RuntimeError, ValueError) as error:
            failures.append({'id': case['id'], 'error': str(error)})
            (session / 'authored-screen-failures.json').write_text(json.dumps(failures, indent=2) + '\n', encoding='utf-8')
            state = client.act('state')
            if state['screen']:
                client.act(escape)
                client.check('recover-' + case['id'], lambda value: not value['screen'], timeout=12)
    if failures:
        raise AssertionError(f'{len(failures)} authored screen cases failed; see authored-screen-failures.json')
    return screenshots


def main():
    from client_acceptance import launch
    from platform_play_check import Client, leave

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--spec', required=True, type=Path)
    parser.add_argument('--directory', required=True, type=Path)
    parser.add_argument('--prepared', action='store_true')
    args = parser.parse_args()
    cases = load_cases(args.spec)
    session = args.directory.resolve()
    with launch(session, mode='normal', prepared=args.prepared) as run:
        client = Client(run)
        client.check('graphical-title', lambda state: state['screen'] == 'TitleScreen' and not state['overlay'])
        client.enter('connect', 'dedicated-join')
        run.command('op P2Tester')

        def capture(name):
            client.act('snapshot-ui:' + name)
            source = client.directory / 'screenshots' / (name + '.png')
            for _ in range(100):
                if source.is_file() and source.stat().st_size > 0:
                    target = session / source.name
                    shutil.copy2(source, target)
                    return target
                time.sleep(.1)
            raise RuntimeError(f'No screenshot for {name}')

        images = run_cases(client, run.command, 'P2Tester', session, cases, capture, 'esc', 'hide-chat')
        leave(client, 'remote-exit')
        print(f'{len(images)} authored panels verified in one dedicated client session')


if __name__ == '__main__':
    main()
