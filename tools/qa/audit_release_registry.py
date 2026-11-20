#!/usr/bin/env python3
"""Compare installed registries and follow the survival routes included in each candidate."""

import argparse
from collections import defaultdict
import csv
from dataclasses import dataclass
from hashlib import sha256
import json
from pathlib import Path
from zipfile import ZipFile


@dataclass(frozen=True)
class Route:
    category: str
    evidence: str
    target: tuple[str, str]
    ingredients: tuple[tuple[tuple[str, str], ...], ...] = ()


def namespace(path):
    return path if ':' in path else 'minecraft:' + path


def stack_id(value):
    return value.get('id', value.get('item', '')) if isinstance(value, dict) else value or ''


def values(obj, key):
    if isinstance(obj, dict):
        if key in obj:
            yield obj[key]
        for child in obj.values():
            yield from values(child, key)
    elif isinstance(obj, list):
        for child in obj:
            yield from values(child, key)


def options(ingredient, tags, visited=frozenset()):
    if isinstance(ingredient, list):
        return tuple(item for member in ingredient for item in options(member, tags, visited))
    if isinstance(ingredient, dict) and 'tag' in ingredient:
        ingredient = '#' + namespace(ingredient['tag'])
    else:
        ingredient = stack_id(ingredient)
    if not ingredient:
        return ()
    if ingredient.startswith('#'):
        tag = namespace(ingredient[1:])
        if tag in visited:
            return ()
        members = tags.get(tag)
        return options(members, tags, visited | {tag}) if members is not None else (('tag', tag),)
    return (('item', namespace(ingredient)),)


def csv_table(archive, path):
    return csv.DictReader(archive.read('com/industrialage/ia2/content/' + path).decode().splitlines(), delimiter='\t')


def unpack(archive, registry):
    entries = set(archive.namelist())
    tags = {}
    fluids = {'ic2:' + row['path'] for row in csv_table(archive, 'industrial-fluids.tsv')}
    for filename in sorted(entries):
        parts = filename.split('/', 4)
        if len(parts) == 5 and parts[0] == 'data' and parts[2] == 'tags' and parts[3] in ('item', 'items') and filename.endswith('.json'):
            tags[parts[1] + ':' + parts[4][:-5]] = json.loads(archive.read(filename))['values']
    for row in csv_table(archive, 'processing-tags.tsv'):
        # Metadata here also tracks builtin ore/material aliases used by device recipes.
        name, member = list(row.values())[:2]
        tags.setdefault(name.removeprefix('#'), []).extend(filter(None, (member or '').split(',')))

    routes = []
    for filename in sorted(entries):
        if not filename.endswith('.json') or not filename.startswith('data/ic2/'):
            continue
        path = filename.split('/', 3)
        data = json.loads(archive.read(filename))
        if path[2] in ('recipe', 'recipes'):
            output = stack_id(data.get('result'))
            if output.startswith('ic2:'):
                ingredients = list(data.get('key', {}).values()) + data.get('ingredients', [])
                if 'ingredient' in data:
                    ingredients.append(data['ingredient'])
                routes.append(Route('recipe', path[3][:-5], ('item', output),
                                    tuple(options(ingredient, tags) for ingredient in ingredients)))
        elif path[2] in ('loot_table', 'loot_tables') and path[3].startswith('blocks/'):
            origin = ('block', 'ic2:' + path[3].removeprefix('blocks/')[:-5])
            for pool in data.get('pools', []):
                for name in values(pool, 'name'):
                    if isinstance(name, str) and ('item', name) in registry:
                        routes.append(Route('loot', origin[1], ('item', name), ((origin,),)))
        elif path[2] in ('loot_table', 'loot_tables') and path[3].startswith('chests/iridium_'):
            modifier = 'data/ic2/loot_modifiers/' + path[3].removeprefix('chests/')
            if modifier in entries:
                conditions = json.loads(archive.read(modifier))['conditions']
                for origin in values(conditions, 'loot_table_id'):
                    for name in values(data, 'name'):
                        if ('item', name) in registry:
                            routes.append(Route('exploration', origin, ('item', name)))
        elif path[2] == 'worldgen' and path[3].startswith('configured_feature/'):
            for name in values(data, 'Name'):
                if ('block', name) in registry:
                    routes.append(Route('worldgen', path[3][:-5], ('block', name)))

    workshop = {
        'blast_furnace': 'blast_furnace', 'block_cutter': 'block_cutter',
        'canner_bottle': 'canner', 'canner_enrich': 'canner', 'centrifuge': 'centrifuge',
        'compressor': 'compressor', 'extractor': 'extractor', 'fluid_heat_exchanger': 'fluid_heat_exchanger',
        'macerator': 'macerator', 'metal_former_cutting': 'metal_former',
        'metal_former_extruding': 'metal_former', 'metal_former_rolling': 'metal_former',
        'ore_washer': 'ore_washing_plant', 'ore_washing_plant': 'ore_washing_plant', 'recycler': 'recycler',
    }
    single_recipes = list(csv_table(archive, 'processing-recipes.tsv'))
    if 'com/industrialage/ia2/content/classic-processing.tsv' in entries:
        single_recipes.extend(csv_table(archive, 'classic-processing.tsv'))
    for row in single_recipes:
        machine = ('block', 'ic2:' + row['machine'])
        if machine in registry and ('item', row['output']) in registry:
            routes.append(Route('device', row['recipe'], ('item', row['output']),
                                ((machine,), options(row['input'], tags))))
    for row in csv_table(archive, 'workshop-recipes.tsv'):
        kind = workshop.get(row['machine'])
        if not kind:
            continue
        machine = ('block', 'ic2:' + kind)
        ingredients = []
        for raw in row['inputs'].split(','):
            item = raw.split('@')[0]
            if item in fluids:
                ingredients.append((('block', item),))
            elif item:
                ingredients.append(options(item, tags))
        for encoded in row['outputs'].split(','):
            product = encoded.split('@')[0]
            target = ('block' if product in fluids else 'item', product)
            if machine in registry and target in registry:
                routes.append(Route('device', row['recipe'], target, ((machine,), *ingredients)))

    def add(category, reason, results, requirements=()):
        for key in results:
            kind, name = key.split('/', 1)
            target = (kind, 'ic2:' + name)
            inputs = tuple(((role, 'ic2:' + part),) for role, part in (req.split('/', 1) for req in requirements))
            if target in registry:
                routes.append(Route(category, reason, target, inputs))

    def blocks(*paths):
        return tuple('block/' + path for path in paths)

    def items(*paths):
        return tuple('item/' + path for path in paths)

    add('world-use', 'tap a grown rubber tree', items('resin'), blocks('rubber_log') + items('treetap'))
    add('world-use', 'axe strip', blocks('stripped_rubber_log'), blocks('rubber_log'))
    add('world-use', 'axe strip', blocks('stripped_rubber_wood'), blocks('rubber_wood'))
    add('world-use', 'place rubber sign on a wall', blocks('rubber_wall_sign'), items('rubber_sign'))
    add('world-use', 'recover wrecked rubber dinghy', items('broken_rubber_boat'), items('rubber_boat'))
    add('world-use', 'breeding and mature crop break', items('crop_seed_bag'), blocks('crop_stick'))
    for group, ident in registry:
        path = ident.removeprefix('ic2:')
        if group == 'block' and path.endswith('_crop'):
            add('world-use', 'plant, grow or cross crop sticks', blocks(path), items('crop_stick'))
        if group == 'block' and path.endswith('_foam_cable'):
            add('world-use', 'spray placed cable with construction foam', blocks(path),
                items('foam_sprayer', path.removesuffix('_foam_cable') + '_cable') + blocks('construction_foam'))
        if group == 'block' and path.endswith('_wall') and ('item', ident) in registry:
            dye = path.removesuffix('_wall')
            if ('item', 'ic2:' + dye + '_painter') in registry:
                add('world-use', 'paint hardened foam', blocks(path), items(dye + '_painter') + blocks('foam'))
    crops = {'bobs_yer_uncle_ranks_berry':'bobs_yer_uncle_ranks_berries_crop', 'coffee_beans':'coffee_crop',
             'grin_powder':'venomilia_crop', 'hops':'hops_crop', 'milk_wart':'milk_wart_crop',
             'oil_berry':'oil_berries_crop', 'terra_wart':'terra_wart_crop', 'weed':'weed_crop'}
    for product, crop in crops.items():
        add('world-use', 'crop harvest', items(product), blocks(crop))
    add('world-use', 'spray mixed foam', blocks('foam'), items('foam_sprayer') + blocks('construction_foam'))
    add('world-use', 'cure the foam around reinforced iron scaffold', blocks('reinforced_stone'),
        items('foam_sprayer', 'reinforced_iron_scaffold') + blocks('construction_foam'))
    add('world-use', 'camouflage hardened foam', blocks('obscured_wall'), items('obscurator') + blocks('foam'))
    add('world-use', 'deployed miner pipe end', blocks('mining_pipe_tip'), blocks('miner') + items('mining_pipe'))

    direct_products = {
        'classic_electrolyzer': ('electrolyzed_water_cell',), 'classic_canner': ('filled_fuel_can',), 'coke_kiln': ('coke',),
        'fermenter': ('fertilizer',), 'recycler': ('scrap',),
        'solid_canner': ('filled_tin_can',), 'solid_heat_generator': ('ashes',), 'mass_fabricator': ('uu_matter',),
    }
    for machine, products in direct_products.items():
        add('device', 'native ' + machine, items(*products), blocks(machine))
    fluid_producers = {
        'compressor': ('air',), 'fermenter': ('biogas',), 'reactor_vessel': ('hot_coolant',),
        'steam_generator': ('steam', 'superheated_steam'), 'solar_distiller': ('distilled_water',),
        'condenser': ('distilled_water',), 'electrolyzer': ('hydrogen', 'oxygen'), 'matter_generator': ('uu_matter',),
        'stirling_kinetic_generator': ('hot_water',), 'liquid_heat_exchanger': ('coolant', 'pahoehoe_lava'),
        'coke_kiln': ('creosote',),
    }
    for machine, products in fluid_producers.items():
        add('device', 'fluid output of ' + machine, blocks(*products), blocks(machine))
    for liquid in sorted(fluids):
        path = liquid.removeprefix('ic2:')
        add('world-use', 'fill bucket from produced fluid', items(path + '_bucket'), blocks(path))
    for cell in ('water', 'lava', 'air'):
        requirements = items('empty_cell') if cell in ('water', 'lava') else items('empty_cell') + blocks(cell)
        add('world-use', 'fill cell with ' + cell, items(cell + '_cell'), requirements)
    add('world-use', 'process and deplete uranium fuel in an active reactor',
        items('depleted_uranium_fuel_rod', 'depleted_dual_uranium_fuel_rod', 'depleted_quad_uranium_fuel_rod',
              'depleted_mox_fuel_rod', 'depleted_dual_mox_fuel_rod', 'depleted_quad_mox_fuel_rod'), blocks('nuclear_reactor'))
    add('device', 'classic canner fuel can', items('filled_fuel_can'), blocks('classic_canner') + items('empty_fuel_can','coalfuel_cell'))
    add('device', 'classic electrolyzer water cell', items('electrolyzed_water_cell'), blocks('classic_electrolyzer') + items('water_cell'))
    add('device', 'reactor breeder conversion', items('re_enriched_uranium'), blocks('nuclear_reactor') + items('depleted_isotope_fuel_rod','uranium_fuel_rod'))
    for kind, name in registry:
        if kind == 'block' and ('item', name) in registry and name not in fluids:
            routes.append(Route('placement', name, (kind, name), ((('item', name),),)))

    return routes


def closure(routes):
    available = defaultdict(set)

    def reachable(key):
        kind, name = key
        return key in available or name.startswith('minecraft:')

    pending = set(routes)
    while pending:
        ready = {route for route in pending if all(any(reachable(key) for key in alternatives) for alternatives in route.ingredients)}
        if not ready:
            break
        pending.difference_update(ready)
        for route in ready:
            available[route.target].add((route.category, route.evidence))
    return available


def read_instance(directory):
    registry = {tuple(line.split('\t')) for line in (directory / 'registry.tsv').read_text().splitlines()}
    jars = [item for item in (directory / 'mods').glob('ic2-*-0.1.0.jar')]
    if len(jars) != 1:
        raise ValueError(f'Expected one candidate JAR in {directory}, found {len(jars)}')
    jar = jars[0]
    with ZipFile(jar) as archive:
        routes = closure(unpack(archive, registry))
        missing_art = []
        members = set(archive.namelist())
        for kind, ident in registry:
            name = ident.removeprefix('ic2:')
            asset = f'assets/ic2/{"blockstates" if kind == "block" else "models/item"}/{name}.json'
            if asset not in members:
                missing_art.append(ident)
    return registry, routes, {'artifact': jar.name, 'sha256': sha256(jar.read_bytes()).hexdigest(),
                              'missing_model_or_blockstate': sorted(missing_art)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--reference', type=Path, required=True)
    parser.add_argument('--reference-observation', type=Path, required=True)
    parser.add_argument('--target', action='append', required=True, help='LABEL:INSTANCE_DIRECTORY')
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    manifest = json.loads(args.reference.read_text())
    reference = {(k, value) for k in ('block', 'item') for value in manifest['registries']['minecraft:' + k]}
    observed = list(csv.reader(args.reference_observation.open(), delimiter='\t'))
    native_recipes = {row[2] for row in observed if row[0] == 'recipe'}
    creative_tab = {row[1] for row in observed if row[0] == 'creative'}
    hosts = {}
    for entry in args.target:
        label, directory = entry.split(':', 1)
        hosts[label] = read_instance(Path(directory))

    args.output.mkdir(parents=True, exist_ok=True)
    summaries = {}
    manual = {('item', 'ic2:remote_interface_upgrade'): 'creative-only',
              ('item', 'ic2:debug_item'): 'internal',
              ('item', 'ic2:creative_generator'): 'creative-only',
              ('block', 'ic2:creative_generator'): 'creative-only'}

    def classify(key, registry, routes):
        if key not in registry:
            return 'missing'
        if key in manual:
            return manual[key]
        reasons = routes.get(key, set())
        tags = sorted({route for route, _ in reasons})
        return ','.join(tags) if tags else 'unverified'

    with (args.output / 'acquisition.tsv').open('w', encoding='utf-8', newline='') as results:
        output = csv.writer(results, delimiter='\t', lineterminator='\n')
        labels = list(hosts)
        output.writerow(['registry', 'id', 'reference_recipe', 'reference_creative_tab', *labels, 'candidate_routes'])
        for kind, name in sorted(reference):
            key = (kind, name)
            evidence = sorted({route for _, routes, _ in hosts.values() for route in routes.get(key, set())})
            output.writerow([kind, name, kind == 'item' and name in native_recipes,
                             kind == 'item' and name in creative_tab,
                             *(classify(key, registry, routes) for registry, routes, _ in hosts.values()),
                             ' | '.join(f'{kind}={data}' for kind, data in evidence[:4])])
        for label in labels:
            registry, routes, metadata = hosts[label]
            summaries[label] = {**metadata,
                'reference_block_matches': sum(kind == 'block' for kind, _ in reference & registry),
                'reference_item_matches': sum(kind == 'item' for kind, _ in reference & registry),
                'missing': sorted(reference - registry), 'additional': sorted(registry - reference),
                'unverified_paths': sorted((kind, ident) for kind, ident in reference
                                           if classify((kind, ident), registry, routes) == 'unverified')}
    (args.output / 'summary.json').write_text(json.dumps({'reference': manifest['reference'], 'targets': summaries}, indent=2) + '\n')


if __name__ == '__main__':
    main()
