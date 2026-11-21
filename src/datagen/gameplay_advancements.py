"""Generate native advancement data from the project's bilingual gameplay contract."""

from collections import Counter
import json


def translation_key(advancement_id, part):
    return 'advancements.ic2.' + advancement_id.split(':', 1)[1].replace('/', '.') + '.' + part


def load_contract(repo):
    path = repo / 'spec/registry/gameplay-advancements.json'
    contract = json.loads(path.read_text(encoding='utf-8'))
    nodes = contract['advancements']
    ids = {node['id'] for node in nodes}
    if (contract.get('schemaVersion') != 1 or len(nodes) != 28 or len(ids) != len(nodes)
            or sum(node['xp'] for node in nodes) != 1020
            or Counter(node['frame'] for node in nodes) != {'task': 9, 'goal': 14, 'challenge': 5}
            or Counter(node['trigger']['kind'] for node in nodes) != {
                'inventory': 22, 'placed': 3, 'equipped': 2, 'action': 1}):
        raise ValueError('Gameplay advancement contract has an unexpected shape')
    for node in nodes:
        if not node['id'].startswith('ic2:industry/') or not node['icon'].startswith('ic2:'):
            raise ValueError('Advancement ID or icon outside the IC2 namespace')
        if node['parent'] is not None and node['parent'] not in ids:
            raise ValueError('Unknown advancement parent: ' + node['id'])
        for part in ('title', 'description'):
            if set(node[part]) != {'zh_cn', 'en_us'} or not all(node[part].values()):
                raise ValueError('Missing bilingual advancement text: ' + node['id'])
    return nodes


def generate(repo, target, write):
    nodes = load_contract(repo)
    forge = target == 'forge-1.19.2'
    modern_equipment = target == 'neoforge-26.2'
    background = ('minecraft:gui/advancements/backgrounds/stone'
                  if target.startswith('neoforge-26.')
                  else 'minecraft:textures/gui/advancements/backgrounds/stone.png')
    directory = 'advancements/' if forge else 'advancement/'
    languages = {lang: json.loads((repo / 'assets/ic2/lang' / (lang + '.json')).read_text(encoding='utf-8'))
                 for lang in ('en_us', 'zh_cn')}
    for node in nodes:
        trigger = node['trigger']
        kind = trigger['kind']
        def item_predicate(item):
            return {'items': [item] if forge else item}
        if kind == 'inventory':
            if trigger['require'] == 'all':
                items = [item_predicate(item) for item in trigger['items']]
            elif trigger['require'] == 'any':
                items = [{'items': trigger['items']}]
            else:
                raise ValueError('Unknown inventory requirement')
            criteria = {'acquired': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': items}}}
        elif kind == 'placed':
            criteria = {}
            for block in trigger['blocks']:
                condition = ({'block': block} if forge else {'location': [
                    {'condition': 'minecraft:block_state_property', 'block': block}]})
                criteria['place_' + block.split(':', 1)[1]] = {
                    'trigger': 'minecraft:placed_block', 'conditions': condition}
        elif kind == 'equipped':
            slots = trigger['slots']
            if set(slots) != {'head', 'chest', 'legs', 'feet'}:
                raise ValueError('Equipment criteria must specify the four armor slots')
            equipment = {slot: item_predicate(item) for slot, item in slots.items()}
            predicate = {('minecraft:equipment' if modern_equipment else 'equipment'): equipment}
            criteria = {'worn': {'trigger': 'minecraft:tick', 'conditions': {'player': [{
                'condition': 'minecraft:entity_properties', 'entity': 'this', 'predicate': predicate}]}}}
        elif kind == 'action' and trigger['event'] == 'powered_tool_hit':
            criteria = {'hit_' + item.split(':', 1)[1]: {'trigger': 'minecraft:impossible'}
                        for item in trigger['items']}
        else:
            raise ValueError('Unsupported advancement trigger: ' + kind)
        display = {'icon': {('item' if forge else 'id'): node['icon']},
                   'title': {'translate': translation_key(node['id'], 'title')},
                   'description': {'translate': translation_key(node['id'], 'description')},
                   'frame': node['frame'], 'show_toast': node['parent'] is not None,
                   'announce_to_chat': node['parent'] is not None, 'hidden': False}
        if node['parent'] is None:
            display['background'] = background
        for lang, strings in languages.items():
            for part in ('title', 'description'):
                key = translation_key(node['id'], part)
                if strings.get(key) != node[part][lang]:
                    raise ValueError('Advancement translation differs from contract: ' + lang + '/' + key)
        value = {'display': display, 'criteria': criteria, 'requirements': [list(criteria)]}
        if node['parent'] is not None:
            value['parent'] = node['parent']
        if node['xp']:
            value['rewards'] = {'experience': node['xp']}
        write('data/ic2/' + directory + node['id'].split(':', 1)[1], value)
