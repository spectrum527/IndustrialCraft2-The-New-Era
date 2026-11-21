#!/usr/bin/env python3
"""Validate the shipped native advancement data and bilingual contract for each target."""

import json
import sys
from collections import Counter
from pathlib import Path
from zipfile import ZipFile


REPO = Path(__file__).resolve().parents[2]
NODES = json.loads((REPO / 'spec/registry/gameplay-advancements.json').read_text(encoding='utf-8'))['advancements']


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def verify(target, path):
    forge = target == '1.19.2'
    base = 'data/ic2/' + ('advancements/' if forge else 'advancement/')
    expected_paths = {base + node['id'].split(':', 1)[1] + '.json' for node in NODES}
    with ZipFile(path) as jar:
        actual = {entry for entry in jar.namelist() if entry.startswith(base + 'industry/') and entry.endswith('.json')}
        require(actual == expected_paths, f'{target}: missing/extra advancement files: {actual ^ expected_paths}')
        languages = {language: json.loads(jar.read(f'assets/ic2/lang/{language}.json'))
                     for language in ('en_us', 'zh_cn')}
        for node in NODES:
            aid = node['id']
            data = json.loads(jar.read(base + aid.split(':', 1)[1] + '.json'))
            display, trigger = data['display'], node['trigger']
            require(data.get('parent') == node['parent'], f'{target}: {aid} parent')
            require(display['frame'] == node['frame'], f'{target}: {aid} frame')
            require(display['icon'].get('item' if forge else 'id') == node['icon'], f'{target}: {aid} icon')
            require(data.get('rewards', {}).get('experience', 0) == node['xp'], f'{target}: {aid} XP')
            for part in ('title', 'description'):
                key = 'advancements.ic2.' + aid.split(':', 1)[1].replace('/', '.') + '.' + part
                require(display[part] == {'translate': key}, f'{target}: {aid} {part} translation key')
                for language in languages:
                    require(languages[language].get(key) == node[part][language],
                            f'{target}: {aid} {language} {part}')
            criteria = data['criteria']
            require(data['requirements'] == [list(criteria)], f'{target}: {aid} requirements')
            kind = trigger['kind']
            expected_trigger = {'inventory': 'minecraft:inventory_changed', 'placed': 'minecraft:placed_block',
                                'equipped': 'minecraft:tick', 'action': 'minecraft:impossible'}[kind]
            require(all(c['trigger'] == expected_trigger for c in criteria.values()), f'{target}: {aid} trigger')
            if kind == 'inventory':
                require(set(criteria) == {'acquired'}, f'{target}: {aid} inventory criteria')
                predicates = criteria['acquired']['conditions']['items']
                actual_items = [p['items'] for p in predicates]
                wanted = ([trigger['items']] if trigger['require'] == 'any' else
                          [[item] if forge else item for item in trigger['items']])
                require(actual_items == wanted, f'{target}: {aid} inventory items')
            elif kind == 'placed':
                wanted = {'place_' + block.split(':', 1)[1] for block in trigger['blocks']}
                require(set(criteria) == wanted, f'{target}: {aid} placement criteria')
                for block in trigger['blocks']:
                    condition = criteria['place_' + block.split(':', 1)[1]]['conditions']
                    if forge:
                        require(condition == {'block': block}, f'{target}: {aid} placed block')
                    else:
                        require(condition == {'location': [{'condition': 'minecraft:block_state_property', 'block': block}]},
                                f'{target}: {aid} placed block')
            elif kind == 'equipped':
                require(set(criteria) == {'worn'}, f'{target}: {aid} worn criterion')
                player = criteria['worn']['conditions']['player']
                require(len(player) == 1 and player[0]['condition'] == 'minecraft:entity_properties'
                        and player[0]['entity'] == 'this', f'{target}: {aid} player predicate')
                predicate = player[0]['predicate']
                equipment = predicate['minecraft:equipment' if target == '26.2' else 'equipment']
                require(set(equipment) == set(trigger['slots']), f'{target}: {aid} slots')
                for slot, item in trigger['slots'].items():
                    require(equipment[slot] == {'items': [item] if forge else item}, f'{target}: {aid} {slot}')
            else:
                require(set(criteria) == {'hit_' + item.split(':', 1)[1] for item in trigger['items']},
                        f'{target}: {aid} successful paid hit criteria')
    print(f'{target}: {len(expected_paths)} packaged advancements and bilingual text verified')


require(len(NODES) == 28 and sum(node['xp'] for node in NODES) == 1020, 'advancement contract totals')
require(Counter(node['frame'] for node in NODES) == {'task': 9, 'goal': 14, 'challenge': 5}, 'frame totals')
require(Counter(node['trigger']['kind'] for node in NODES) ==
        {'inventory': 22, 'placed': 3, 'equipped': 2, 'action': 1}, 'trigger totals')
require(len(sys.argv) > 1, 'Supply target=packaged-jar for at least one target')
for argument in sys.argv[1:]:
    name, jar_path = argument.split('=', 1)
    verify(name, jar_path)
