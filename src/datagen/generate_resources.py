#!/usr/bin/env python3
"""Generate the first playable IC2 resource/data pack for a specific game version.

Models reference the authored textures in assets/. Machine families, the measured
repair recipe book, Classic crafting layouts and loot data are versioned inputs.
The output pack is specific to the selected game and loader.
"""
from industrial_families import generate as generate_families

from world_resources import generate as generate_world

import argparse
import csv
import json
import tempfile
from generated_pack import publish
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument('--target', required=True, choices=('forge-1.19.2', 'neoforge-1.21.1', 'neoforge-26.1.2', 'neoforge-26.2'))
parser.add_argument('--output', required=True, type=Path)
parser.add_argument('--logistics', action='store_true', help='Include the separately registered pipe/tank batch')
parser.add_argument('--extended-energy', action='store_true', help='Include the registered storage and cable expansion')
parser.add_argument('--materials', action='store_true', help='Include the registered passive materials')
args = parser.parse_args()
forge = args.target == 'forge-1.19.2'
newest = args.target in ('neoforge-26.1.2', 'neoforge-26.2')
extended_energy = args.extended_energy or args.logistics
materials_enabled = args.materials or args.logistics
destination = args.output.resolve()
if destination == Path("/") or len(destination.parts) < 3:
    raise SystemExit("Choose a dedicated resource output directory")
destination.parent.mkdir(parents=True, exist_ok=True)
workspace = tempfile.TemporaryDirectory(prefix=".ic2-pack-", dir=destination.parent)
out = Path(workspace.name)

def write(path, value):
    file = out / (path + '.json')
    file.parent.mkdir(parents=True, exist_ok=True)
    file.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def asset(path, value): write('assets/ic2/' + path, value)
def data(path, value): write('data/ic2/' + path, value)
def mod(name): return 'ic2:' + name
def tex(name): return mod(name)

def block_model(name, value): asset('models/block/' + name, value)
def item_model(name, value): asset('models/item/' + name, value)
def blockstate(name, value): asset('blockstates/' + name, value)
def item_definition(name):
    if newest:
        asset('items/' + name, {'model': {'type': 'minecraft:model', 'model': mod('item/' + name)}})

def item_sprite(name, texture, tool=False):
    item_model(name, {'parent': 'minecraft:item/handheld' if tool else 'minecraft:item/generated', 'textures': {'layer0': tex(texture)}})
    item_definition(name)

def block_item(name, model=None):
    item_model(name, {'parent': mod('block/' + (model or name))})
    item_definition(name)

def cube_model(name, texture):
    block_model(name, {'parent': 'minecraft:block/cube_all', 'textures': {'all': tex(texture)}})
    blockstate(name, {'variants': {'': {'model': mod('block/' + name)}}})
    block_item(name)

for name, texture in {
    'machine': 'blocks/resource/machine',
    'tin_ore': 'blocks/resource/ore/tin_ore',
    'deepslate_tin_ore': 'blocks/resource/ore/deepslate_tin_ore',
    'rubber_leaves': 'blocks/resource/rubber_leaves',
}.items(): cube_model(name, texture)
block_model('rubber_leaves', {'parent': 'minecraft:block/cube_all', 'render_type': 'minecraft:cutout_mipped',
    'textures': {'all': tex('blocks/resource/rubber_leaves')}})
block_model('rubber_sapling', {'parent': 'minecraft:block/cross', 'render_type': 'minecraft:cutout', 'textures': {'cross': tex('blocks/resource/rubber_sapling')}})
blockstate('rubber_sapling', {'variants': {'': {'model': mod('block/rubber_sapling')}}})
item_sprite('rubber_sapling', 'blocks/resource/rubber_sapling')

# Explicit faces let every world-axis + resin-hole combination remain valid.
axes = {'x': ('east', 'west'), 'y': ('up', 'down'), 'z': ('north', 'south')}
holes = ['none'] + [f'{state}_{face}' for state in ('wet','dry') for face in ('north','east','south','west')]
variants = {}
for axis, ends in axes.items():
    for hole in holes:
        state_name = hole.split('_')[0] if hole != 'none' else None
        sides = 'rubber_wood_' + state_name + '_leftrightback' if state_name else 'rubber_wood_sides'
        end = 'rubber_wood_' + state_name + '_bottomtop' if state_name else 'rubber_wood_bottomtop'
        face_textures = {face: tex('blocks/resource/' + sides) for face in ('up','down','north','south','east','west')}
        for face in ends: face_textures[face] = tex('blocks/resource/' + end)
        if hole != 'none':
            state, face = hole.split('_')
            face_textures[face] = tex('blocks/resource/rubber_wood_' + state + '_front')
        face_textures['particle'] = tex('blocks/resource/' + sides)
        model = 'rubber_log_' + axis + '_' + hole
        block_model(model, {'parent': 'minecraft:block/cube', 'textures': face_textures})
        variants['axis=' + axis + ',resin_hole=' + hole] = {'model': mod('block/' + model)}
blockstate('rubber_log', {'variants': variants})
block_item('rubber_log', 'rubber_log_y_none')

# Each operating machine has its own real front/side/top art. Facing=north is unrotated.
machines = {
    'generator': ('blocks/generator/electric/generator', 'leftrightback', False),
    'iron_furnace': ('blocks/machine/processing/basic/iron_furnace', 'leftrightback', False),
    'macerator': ('blocks/machine/processing/basic/macerator', 'leftrightback', True),
    'electric_furnace': ('blocks/machine/processing/basic/electric_furnace', 'leftrightback', False),
    'compressor': ('blocks/machine/processing/basic/compressor', 'leftrightback', False),
    'extractor': ('blocks/machine/processing/basic/extractor', 'leftright', False),
}
rotations = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
for name, (base, sides, active_top) in machines.items():
    variants = {}
    for active in (False, True):
        model = name + ('_active' if active else '')
        textures = {'north': tex(base + '_front' + ('_active' if active else '')),
                    'south': tex(base + ('_back' if name == 'extractor' else '_' + sides)),
                    'west': tex(base + '_' + sides + ('_active' if active and name == 'extractor' else '')),
                    'east': tex(base + '_' + sides + ('_active' if active and name == 'extractor' else '')),
                    'up': tex(base + '_top' + ('_active' if active and active_top else '')),
                    'down': tex(base + '_bottom'), 'particle': tex(base + '_' + sides)}
        block_model(model, {'parent': 'minecraft:block/cube', 'textures': textures})
        for facing, degrees in rotations.items():
            entry = {'model': mod('block/' + model)}
            if degrees: entry['y'] = degrees
            variants[f'active={str(active).lower()},facing={facing}'] = entry
    blockstate(name, {'variants': variants})
    block_item(name)
base = 'blocks/wiring/storage/batbox_'
block_model('batbox', {'parent': 'minecraft:block/cube', 'textures': {
    'north': tex(base + 'front'), 'south': tex(base + 'back'), 'west': tex(base + 'leftright'),
    'east': tex(base + 'leftright'), 'up': tex(base + 'top'), 'down': tex(base + 'bottom'),
    'particle': tex(base + 'leftright')}})
variants = {}
for facing, degrees in rotations.items():
    v = {'model': mod('block/batbox')}
    if degrees: v['y'] = degrees
    variants['facing=' + facing] = v
variants['facing=up'] = {'model': mod('block/batbox'), 'x': 270}
variants['facing=down'] = {'model': mod('block/batbox'), 'x': 90}
blockstate('batbox', {'variants': variants})
block_item('batbox')
if extended_energy:
    for name in ('cesu','mfe','mfsu'):
        base='blocks/wiring/storage/'+name+'_'
        side=base+('leftrightback' if name=='mfsu' else 'leftright')
        block_model(name, {'parent':'minecraft:block/cube','textures':{
            'north':tex(base+'front'), 'south':tex(side if name=='mfsu' else base+'back'),
            'west':tex(side),'east':tex(side),'up':tex(base+'bottomtop'),
            'down':tex(base+'bottomtop'),'particle':tex(side)}})
        variants={}
        for facing,degrees in rotations.items():
            v={'model':mod('block/'+name)}
            if degrees:v['y']=degrees
            variants['facing='+facing]=v
        variants['facing=up']={'model':mod('block/'+name),'x':270}
        variants['facing=down']={'model':mod('block/'+name),'x':90}
        blockstate(name,{'variants':variants});block_item(name)

# Multipart cable geometry: independent centre and six arms match native connections.
directions = ('north','south','west','east','down','up')
dye_order = ('white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray',
             'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red')
cables = [
    ('copper_cable', 'copper_cable_0', 'copper_cable_0', 6, False),
    ('insulated_copper_cable', 'copper_cable_1_black', 'copper_cable_1', 5, False),
    ('tin_cable', 'tin_cable_0', 'tin_cable_0', 6, False),
    ('insulated_tin_cable', 'tin_cable_1_black', 'tin_cable_1', 5, False),
]
if extended_energy:
    cables += [
        ('gold_cable','gold_cable_0','gold_cable_0',6.5,False),
        ('insulated_gold_cable','gold_cable_1_black','gold_cable_1',5.5,False),
        ('double_insulated_gold_cable','gold_cable_2_black','gold_cable_2',4.5,False),
        ('iron_cable','iron_cable_0','iron_cable_0',5,False),
        ('insulated_iron_cable','iron_cable_1_black','iron_cable_1',4,False),
        ('double_insulated_iron_cable','iron_cable_2_black','iron_cable_2',3,False),
        ('triple_insulated_iron_cable','iron_cable_3_black','iron_cable_3',2,False),
        ('glass_fibre_cable','glass_cable_0_white','glass_cable',6,False),
        ('detector_cable','detector_cable_0','detector_cable',4,True),
        ('splitter_cable','splitter_cable_0','splitter_cable',4,True),
    ]
for name, world_tex, icon, lo, has_active in cables:
    paintable = 'insulated_' in name or name == 'glass_fibre_cable'
    hi=16-lo
    ends={'north':([lo,lo,0],[hi,hi,lo]),'south':([lo,lo,hi],[hi,hi,16]),
          'west':([0,lo,lo],[lo,hi,hi]),'east':([hi,lo,lo],[16,hi,hi]),
          'down':([lo,0,lo],[hi,lo,hi]),'up':([lo,hi,lo],[hi,16,hi])}
    def cable_part(part, start, end, active=False):
        faces = {face: {'texture': '#cable'} for face in ('down','up','north','south','west','east')}
        model=name+'_'+part+('_active' if active else '')
        texture=world_tex+('_active' if active else '')
        block_model(model, {'parent': 'minecraft:block/block', 'textures': {
            'particle': tex('blocks/wiring/cable/' + texture), 'cable': tex('blocks/wiring/cable/' + texture)},
            'elements': [{'from': start, 'to': end, 'faces': faces}]})
    parts=[]
    for active in ((False,True) if has_active else (False,)):
        suffix='_active' if active else ''
        cable_part('core',[lo,lo,lo],[hi,hi,hi],active)
        core={'apply':{'model':mod('block/'+name+'_core'+suffix)}}
        if has_active:core['when']={'active':str(active).lower()}
        if paintable:core['when']={'color':'0'}
        parts.append(core)
        for direction,(start,end) in ends.items():
            cable_part(direction,start,end,active)
            condition={direction:'true'}
            if has_active:condition['active']=str(active).lower()
            if paintable:condition['color']='0'
            parts.append({'when':condition,'apply':{'model':mod('block/'+name+'_'+direction+suffix)}})
    if paintable:
        texture_base = world_tex.rsplit('_', 1)[0]
        for color, dye in enumerate(dye_order, 1):
            texture = tex('blocks/wiring/cable/' + texture_base + '_' + dye)
            for part in ('core', *ends):
                parent = name + '_' + part
                model = parent + '_' + dye
                block_model(model, {'parent': mod('block/' + parent),
                                     'textures': {'particle': texture, 'cable': texture}})
                condition = {'color': str(color)}
                if part != 'core': condition[part] = 'true'
                parts.append({'when': condition, 'apply': {'model': mod('block/' + model)}})
    blockstate(name, {'multipart': parts})
    item_sprite(name, 'items/cable/' + icon)

for name, texture in {
    'raw_tin':'resource/raw/tin', 'tin_ingot':'resource/ingot/tin', 'resin':'resource/resin',
    'rubber':'crafting/rubber', 'iron_plate':'resource/plate/iron', 'copper_plate':'resource/plate/copper',
    'tin_plate':'resource/plate/tin', 'tin_casing':'resource/casing/tin', 'circuit':'crafting/circuit',
    're_battery':'battery/re_battery_0', 'crushed_tin':'resource/crushed/tin', 'crushed_copper':'resource/crushed/copper',
    'bronze_dust':'resource/dust/bronze', 'bronze_ingot':'resource/ingot/bronze', 'coal_dust':'resource/dust/coal',
    'iron_dust':'resource/dust/iron', 'bronze_helmet':'armor/bronze_helmet',
    'bronze_chestplate':'armor/bronze_chestplate', 'bronze_leggings':'armor/bronze_leggings', 'bronze_boots':'armor/bronze_boots',
}.items(): item_sprite(name, 'items/' + texture)
if forge:
    # Forge registers ic2:charge as the stack's 0..1 EU fraction.
    for level in range(1, 5):
        item_sprite('re_battery_' + str(level), 'items/battery/re_battery_' + str(level))
    item_model('re_battery', {'parent': 'minecraft:item/generated',
        'textures': {'layer0': tex('items/battery/re_battery_0')},
        'overrides': [{'predicate': {'ic2:charge': level / 4},
                       'model': mod('item/re_battery_' + str(level))} for level in range(1, 5)]})
for name in ('treetap','forge_hammer','cutter','bronze_pickaxe'):
    item_sprite(name, 'items/tool/' + name, True)
if materials_enabled:
    with (Path(__file__).parent / 'material-assets.tsv').open(encoding='utf-8',newline='') as f:
        for material in csv.DictReader(f,delimiter='\t'):
            item_sprite(material['id'].removeprefix('ic2:'), material['texture'])
if newest:
    asset('equipment/bronze', {'layers': {'humanoid': [{'texture': 'ic2:bronze'}],
        'humanoid_leggings': [{'texture': 'ic2:bronze'}]}})

if args.logistics:
    for name, texture in {
        'bronze_plate':'resource/plate/bronze', 'bronze_casing':'resource/casing/bronze',
        'steel_plate':'resource/plate/steel', 'steel_casing':'resource/casing/steel',
        'steel_ingot':'resource/ingot/steel', 'gold_plate':'resource/plate/gold',
        'gold_casing':'resource/casing/gold', 'empty_cell':'cell/empty_cell',
    }.items(): item_sprite(name, 'items/' + texture)
    for name in ('wrench','crowbar'):
        item_sprite(name, 'items/tool/' + name, True)
    for tier in ('lv','mv'):
        name = 'pump_cover_' + tier
        item_model(name, {'parent':'minecraft:item/generated', 'textures': {
            'layer0':tex('items/cover/pump_background'),
            'layer1':tex('items/cover/pump_foreground_' + tier)}})
        item_definition(name)
    for name in ('bronze_tank','iron_tank','steel_tank','iridium_tank'):
        bronze = name == 'bronze_tank'
        block_model(name, {'parent':'minecraft:block/cube_bottom_top','textures':{
            'side':tex('blocks/bronze_tank_side' if bronze else 'blocks/machine/fluid/tank_side'),
            'top':tex('blocks/bronze_tank_cap' if bronze else 'blocks/machine/fluid/tank_top'),
            'bottom':tex('blocks/bronze_tank_cap' if bronze else 'blocks/machine/fluid/tank_bottom')}})
        blockstate(name, {'variants':{'':{'model':mod('block/' + name)}}})
        block_item(name)
    # All 16 pipe IDs share four accurately sized multipart geometries per material.
    for material in ('bronze','steel'):
        for size, lo in [('tiny',6),('small',5),('medium',4),('large',3)]:
            hi=16-lo
            stem='transport/' + material + '_' + size
            textures={'particle':tex(f'blocks/transport/{material}/pipe_side'),
                      'side':tex(f'blocks/transport/{material}/pipe_side'),
                      'end':tex(f'blocks/transport/{material}/pipe_{size}')}
            def part(start,end,end_faces=()):
                faces={face:{'texture':'#end' if face in end_faces else '#side'}
                       for face in ('down','up','north','south','west','east')}
                for face in end_faces: faces[face]['uv']=[0,0,16,16]
                return {'from':start,'to':end,'faces':faces}
            pieces={
                'core':part([lo,lo,lo],[hi,hi,hi]),
                'north':part([lo,lo,0],[hi,hi,lo],('north',)),
                'south':part([lo,lo,hi],[hi,hi,16],('south',)),
                'west':part([0,lo,lo],[lo,hi,hi],('west',)),
                'east':part([hi,lo,lo],[16,hi,hi],('east',)),
                'down':part([lo,0,lo],[hi,lo,hi],('down',)),
                'up':part([lo,hi,lo],[hi,16,hi],('up',)),
                'outlet':part([lo-.15,lo-.15,lo-.4],[hi+.15,hi+.15,lo+.1],('north',)),
            }
            for part_name, element in pieces.items():
                block_model(stem+'_'+part_name, {'parent':'minecraft:block/block','textures':textures,'elements':[element]})
            block_model(stem+'_inventory', {'parent':'minecraft:block/block','textures':textures,
                'elements':[pieces['core'],pieces['west'],pieces['east']]})
            for kind in ('item','fluid'):
                name=f'{material}_{kind}_pipe_{size}'
                parts=[{'apply':{'model':mod('block/'+stem+'_core')}}]
                parts += [{'when':{direction:'true'},'apply':{'model':mod('block/'+stem+'_'+direction)}}
                          for direction in directions]
                if kind == 'item':
                    for facing, degrees in rotations.items():
                        model={'model':mod('block/'+stem+'_outlet')}
                        if degrees:model['y']=degrees
                        parts.append({'when':{'facing':facing},'apply':model})
                    for facing,degrees in [('up',270),('down',90)]:
                        parts.append({'when':{'facing':facing},'apply':{'model':mod('block/'+stem+'_outlet'),'x':degrees}})
                blockstate(name, {'multipart':parts})
                block_item(name, stem+'_inventory')

# All recipe declarations use the same layouts as the observed 39-item slice.
# Tool remainder is implemented by the native item classes on each platform.
R = {
 'treetap':([' P ','PPP','P  '], {'P':'#minecraft:planks'},1),
 'forge_hammer':(['II ','ISS','II '], {'I':'minecraft:iron_ingot','S':'minecraft:stick'},1),
 'cutter':(['P P',' P ','I I'], {'P':'ic2:iron_plate','I':'minecraft:iron_ingot'},1),
 'circuit':(['CCC','RPR','CCC'], {'C':'ic2:insulated_copper_cable','R':'minecraft:redstone','P':'ic2:iron_plate'},1),
 'machine':(['PPP','P P','PPP'], {'P':'ic2:iron_plate'},1),
 're_battery':([' C ','TRT','TRT'], {'C':'ic2:insulated_tin_cable','T':'ic2:tin_casing','R':'minecraft:redstone'},1),
 'iron_furnace':([' P ','P P','PFP'], {'P':'ic2:iron_plate','F':'minecraft:furnace'},1),
 'generator':([' B ','PPP',' F '], {'B':'ic2:re_battery','P':'ic2:iron_plate','F':'ic2:iron_furnace'},1),
 'batbox':(['PCP','BBB','PPP'], {'P':'#minecraft:planks','C':'ic2:insulated_tin_cable','B':'ic2:re_battery'},1),
 'macerator':(['FFF','CMC',' E '], {'F':'minecraft:flint','C':'minecraft:cobblestone','M':'ic2:machine','E':'ic2:circuit'},1),
 'electric_furnace':([' C ','RFR'], {'C':'ic2:circuit','R':'minecraft:redstone','F':'ic2:iron_furnace'},1),
 'compressor':(['S S','SMS','SCS'], {'S':'minecraft:stone','M':'ic2:machine','C':'ic2:circuit'},1),
 'extractor':(['TMT','TCT'], {'T':'ic2:treetap','M':'ic2:machine','C':'ic2:circuit'},1),
 'bronze_pickaxe':(['BBB',' S ',' S '], {'B':'ic2:bronze_ingot','S':'minecraft:stick'},1),
 'bronze_helmet':(['BBB','B B'], {'B':'ic2:bronze_ingot'},1),
 'bronze_chestplate':(['B B','BBB','BBB'], {'B':'ic2:bronze_ingot'},1),
 'bronze_leggings':(['BBB','B B','B B'], {'B':'ic2:bronze_ingot'},1),
 'bronze_boots':(['B B','B B'], {'B':'ic2:bronze_ingot'},1),
}
shapeless = {
 'iron_plate':(['minecraft:iron_ingot','ic2:forge_hammer'],1),
 'copper_plate':(['minecraft:copper_ingot','ic2:forge_hammer'],1),
 'tin_plate':(['ic2:tin_ingot','ic2:forge_hammer'],1),
 'tin_casing':(['ic2:tin_plate','ic2:forge_hammer'],2),
 'copper_cable':(['ic2:copper_plate','ic2:cutter'],2),
 'tin_cable':(['ic2:tin_plate','ic2:cutter'],3),
 'insulated_copper_cable':(['ic2:rubber','ic2:copper_cable'],1),
 'insulated_tin_cable':(['ic2:rubber','ic2:tin_cable'],1),
 'bronze_dust':(['ic2:crushed_tin'] + ['ic2:crushed_copper'] * 3,4),
}
if extended_energy:
    shapeless['insulated_iron_cable']=(['ic2:rubber','ic2:iron_cable'],1)
    shapeless['double_insulated_iron_cable']=(['ic2:rubber','ic2:insulated_iron_cable'],1)
    shapeless['triple_insulated_iron_cable']=(['ic2:rubber','ic2:double_insulated_iron_cable'],1)
    R['detector_cable']=([' C ','RIR',' R '],{'C':'ic2:circuit','R':'minecraft:redstone',
        'I':'ic2:triple_insulated_iron_cable'},1)
    R['splitter_cable']=([' R ','ILI',' R '],{'R':'minecraft:redstone','I':'ic2:triple_insulated_iron_cable',
        'L':'minecraft:lever'},1)
if args.logistics:
    for material in ('bronze','steel'):
        for size,count in [('tiny',6),('small',3),('medium',2),('large',1)]:
            fluid=f'{material}_fluid_pipe_{size}'
            item=f'{material}_item_pipe_{size}'
            component=f'ic2:{material}_' + ('casing' if size in ('tiny','small') else 'plate')
            pattern=['PPP','   ','PPP'] if size in ('tiny','medium') else ['P P','P P','P P']
            R[fluid]=(pattern,{'P':component},count)
            shapeless[item]=([mod(fluid),'minecraft:hopper'],1)
    # Bronze is confirmed by the live recipe book; steel requires a separate
    # production route and must not acquire an invented hammer shortcut.
    shapeless['bronze_plate']=(['ic2:bronze_ingot','ic2:forge_hammer'],1)
    shapeless['bronze_casing']=(['ic2:bronze_plate','ic2:forge_hammer'],2)
    shapeless['gold_plate']=(['minecraft:gold_ingot','ic2:forge_hammer'],1)
    shapeless['gold_casing']=(['ic2:gold_plate','ic2:forge_hammer'],2)
    shapeless['gold_cable']=(['ic2:gold_plate','ic2:cutter'],4)
    shapeless['insulated_gold_cable']=(['ic2:rubber','ic2:gold_cable'],1)
    shapeless['double_insulated_gold_cable']=(['ic2:rubber','ic2:insulated_gold_cable'],1)
    R['wrench']=(['B B','BBB',' B '],{'B':'ic2:bronze_ingot'},1)
    R['wrench_down']=([' B ','BBB','B B'],{'B':'ic2:bronze_ingot'},1)
    R['crowbar']=([' DB','DBD','BD '],{'D':'minecraft:red_dye','B':'ic2:bronze_ingot'},1)
    R['empty_cell']=([' T ','TGT',' T '],{'T':'ic2:tin_casing','G':'minecraft:glass_pane'},1)
    for material in ('bronze','iron','steel'):
        R[material+'_tank']=(['PCP','C C','PCP'],{'P':f'ic2:{material}_plate','C':'ic2:empty_cell'},1)
    shapeless['pump_cover_lv']=(['ic2:bronze_fluid_pipe_medium','minecraft:piston','ic2:insulated_copper_cable'],1)
    shapeless['pump_cover_mv']=(['ic2:steel_fluid_pipe_medium','minecraft:piston','ic2:insulated_gold_cable'],1)
recipe_dir = 'recipes/' if forge else 'recipe/'
def ingredient(name):
    if newest: return name
    return {('tag' if name.startswith('#') else 'item'): name.removeprefix('#')}
def result(name,count=1):
    return {('item' if forge else 'id'): mod(name), 'count': count}
for name, (pattern, keys, count) in R.items():
    data(recipe_dir + name, {'type': 'minecraft:crafting_shaped', 'pattern': pattern,
        'key': {k: ingredient(v) for k,v in keys.items()},
        'result': result('wrench' if name == 'wrench_down' else name,count)})
for name, (inputs,count) in shapeless.items():
    data(recipe_dir + name, {'type':'minecraft:crafting_shapeless', 'ingredients':[ingredient(x) for x in inputs], 'result': result(name,count)})
base_smelting=[('raw_tin','tin_ingot',0.5),('resin','rubber',0.3),('bronze_dust','bronze_ingot',0.0)]
for source, product, xp in base_smelting:
    data(recipe_dir + product + '_from_' + source, {'type':'minecraft:smelting', 'ingredient':ingredient(mod(source)),
        'result': mod(product) if forge else {'id': mod(product)}, 'experience':xp, 'cookingtime':200})
if materials_enabled:
    observed = json.loads((Path(__file__).parent / 'observed-material-recipes.json').read_text(encoding='utf-8'))
    current_names = set(R) | set(shapeless)
    for entry in observed['recipes']:
        source_name=entry['source_id'].split('/',1)[1]
        if source_name in current_names:
            continue
        output=entry['output']
        if entry['kind']=='smelting' and any(entry['ingredient']==mod(source) and output==mod(product)
                                              for source,product,_ in base_smelting):
            continue
        stack={('item' if forge else 'id'):output,'count':entry['count']}
        if entry['kind']=='shaped':
            alphabet=iter('ABCDEFGHI')
            symbols={}
            pattern=[]
            for row in entry['shape']:
                line=''
                for item in row:
                    if item is None:line+=' '
                    else:
                        if item not in symbols:symbols[item]=next(alphabet)
                        line+=symbols[item]
                pattern.append(line)
            recipe={'type':'minecraft:crafting_shaped','pattern':pattern,
                    'key':{symbol:ingredient(item) for item,symbol in symbols.items()},'result':stack}
        elif entry['kind']=='shapeless':
            recipe={'type':'minecraft:crafting_shapeless','ingredients':[ingredient(x) for x in entry['ingredients']],
                    'result':stack}
        else:
            recipe={'type':'minecraft:smelting','ingredient':ingredient(entry['ingredient']),
                    'result':output if forge else {'id':output},'experience':entry['xp'],
                    'cookingtime':entry['duration_ticks']}
        data(recipe_dir+'observed/'+source_name,recipe)

# Native loot tables and tool tags; the platform block implementation controls harvest requirements.
loot_dir = 'loot_tables/blocks/' if forge else 'loot_table/blocks/'
tag_dir = 'tags/blocks/' if forge else 'tags/block/'
survives = {'condition':'minecraft:survives_explosion'}
def self_drop(name):
    data(loot_dir + name, {'type':'minecraft:block','pools':[{'rolls':1,'conditions':[survives],
        'entries':[{'type':'minecraft:item','name':mod(name)}]}]})
for name in ('rubber_log','rubber_sapling','machine','iron_furnace','generator','batbox','macerator',
             'electric_furnace','compressor','extractor','copper_cable','insulated_copper_cable','tin_cable','insulated_tin_cable'):
    self_drop(name)
if extended_energy:
    for name in ('cesu','mfe','mfsu'):
        self_drop(name)
    for name,_,_,_,_ in cables[4:]:
        self_drop(name)
if args.logistics:
    for name in ('bronze_tank','iron_tank','steel_tank','iridium_tank'):
        self_drop(name)
    for material in ('bronze','steel'):
        for kind in ('item','fluid'):
            for size in ('tiny','small','medium','large'):
                self_drop(f'{material}_{kind}_pipe_{size}')
# Ordinary macerator mining recovers the casing; the wrench path is handled by the block.
data(loot_dir + 'macerator', {'type':'minecraft:block','pools':[{'rolls':1,'conditions':[survives],
    'entries':[{'type':'minecraft:item','name':mod('machine')}]}]})
if forge:
    silk_pred = {'enchantments':[{'enchantment':'minecraft:silk_touch','levels':{'min':1}}]}
else:
    silk_pred = {'predicates':{'minecraft:enchantments':[{'enchantments':'minecraft:silk_touch','levels':{'min':1}}]}}
silk = {'condition':'minecraft:match_tool','predicate':silk_pred}
for name in ('tin_ore','deepslate_tin_ore'):
    data(loot_dir + name, {'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:alternatives','children':[
        {'type':'minecraft:item','name':mod(name),'conditions':[silk]},
        {'type':'minecraft:item','name':mod('raw_tin'),'functions':[{'function':'minecraft:apply_bonus',
            'enchantment':'minecraft:fortune','formula':'minecraft:ore_drops'}, {'function':'minecraft:explosion_decay'}]}]}]}]})
shears = {'condition':'minecraft:match_tool','predicate':{'items':['minecraft:shears'] if forge else 'minecraft:shears'}}
data(loot_dir + 'rubber_leaves', {'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:alternatives','children':[
    {'type':'minecraft:item','name':mod('rubber_leaves'),'conditions':[{'condition':'minecraft:alternative' if forge else 'minecraft:any_of','terms':[shears,silk]}]},
    {'type':'minecraft:item','name':mod('rubber_sapling'),'conditions':[survives,{'condition':'minecraft:table_bonus',
      'enchantment':'minecraft:fortune','chances':[1/35, 1/28, 1/21, 1/14, 1/7]}]}]}]}]})
def minecraft_tag(path, values):
    write('data/minecraft/' + tag_dir + path, {'replace':False,'values':[mod(x) for x in values]})
pickaxe = ['tin_ore','deepslate_tin_ore','machine','iron_furnace','generator','batbox','macerator',
    'electric_furnace','compressor','extractor','copper_cable','insulated_copper_cable','tin_cable','insulated_tin_cable']
if extended_energy:
    pickaxe += ['cesu','mfe','mfsu'] + [name for name,_,_,_,_ in cables[4:]]
if args.logistics:
    pickaxe += ['bronze_tank','iron_tank','steel_tank','iridium_tank']
    pickaxe += [f'{material}_{kind}_pipe_{size}' for material in ('bronze','steel')
                for kind in ('item','fluid') for size in ('tiny','small','medium','large')]
pickaxe += ['iron_storage_box','bronze_storage_box','steel_storage_box','iridium_storage_box','personal_chest']
minecraft_tag('mineable/pickaxe', pickaxe)
minecraft_tag('needs_stone_tool', ('tin_ore','deepslate_tin_ore'))
minecraft_tag('mineable/axe', ('rubber_log','wooden_storage_box'))
minecraft_tag('logs', ('rubber_log',))
minecraft_tag('leaves', ('rubber_leaves',))
data(('tags/items/' if forge else 'tags/item/') + 'bronze_ingots',
     {'replace':False,'values':[mod('bronze_ingot')]})

# Vanilla tree and ore definitions; loader biome modifier adds them to real worlds.
ore = {'type':'minecraft:ore','config':{'size':8,'discard_chance_on_air_exposure':0.0,'targets':[
 {'target':{'predicate_type':'minecraft:tag_match','tag':'minecraft:stone_ore_replaceables'},'state':{'Name':mod('tin_ore')}},
 {'target':{'predicate_type':'minecraft:tag_match','tag':'minecraft:deepslate_ore_replaceables'},'state':{'Name':mod('deepslate_tin_ore')}}]}}
data('worldgen/configured_feature/tin_ore', ore)
data('worldgen/placed_feature/tin_ore', {'feature':mod('tin_ore'),'placement':[
 {'type':'minecraft:count','count':12},{'type':'minecraft:in_square'}, {'type':'minecraft:height_range',
 'height':{'type':'minecraft:uniform','min_inclusive':{'absolute':-24},'max_inclusive':{'absolute':64}}}, {'type':'minecraft:biome'}]})
def state(name, **properties): return {'Name':mod(name),'Properties':properties}
wood_states = [{'data':state('rubber_log',axis='y',resin_hole=hole), 'weight':6 if hole=='none' else 1}
               for hole in ('none','wet_north','wet_east','wet_south','wet_west')]
tree_config = {
 'decorators':[], 'foliage_placer':{'type':'minecraft:blob_foliage_placer','height':3,'offset':0,'radius':2},
 'foliage_provider':{'type':'minecraft:simple_state_provider','state':state('rubber_leaves',distance='7',persistent='false',waterlogged='false')},
 'ignore_vines':True, 'minimum_size':{'type':'minecraft:two_layers_feature_size','limit':1,'lower_size':0,'upper_size':1},
 'trunk_placer':{'type':'minecraft:straight_trunk_placer','base_height':5,'height_rand_a':2,'height_rand_b':0},
 'trunk_provider':{'type':'minecraft:weighted_state_provider','entries':wood_states}}
if newest:
    tree_config['below_trunk_provider']={'type':'minecraft:simple_state_provider','state':{'Name':'minecraft:dirt'}}
else:
    tree_config['dirt_provider']={'type':'minecraft:simple_state_provider','state':{'Name':'minecraft:dirt'}}
    tree_config['force_dirt']=False
data('worldgen/configured_feature/rubber_tree', {'type':'minecraft:tree','config':tree_config})
data('worldgen/placed_feature/rubber_tree', {'feature':mod('rubber_tree'),'placement':[
 {'type':'minecraft:rarity_filter','chance':6}, {'type':'minecraft:in_square'},
 {'type':'minecraft:surface_water_depth_filter','max_water_depth':0}, {'type':'minecraft:heightmap','heightmap':'OCEAN_FLOOR'},
 {'type':'minecraft:biome'}, {'type':'minecraft:block_predicate_filter','predicate':{'type':'minecraft:would_survive',
 'state':state('rubber_sapling',stage='0')}}]})
loader='forge' if forge else 'neoforge'
for name,biomes,step in [('tin_ore','#minecraft:is_overworld','underground_ores'), ('rubber_tree','#minecraft:is_forest','vegetal_decoration')]:
    data(loader + '/biome_modifier/add_' + name, {'type':loader + ':add_features','biomes':biomes,'features':mod(name),'step':step})

repo = Path(__file__).resolve().parents[2]
# The game stitches legacy IC2 sprite names only when its atlas definition explicitly includes them.
# Since Minecraft 26 the item atlas is separate; block models used in inventories keep block sprites.
if not forge:
    blocks = {"type":"minecraft:directory", "source":"blocks", "prefix":"blocks/"}
    items = {"type":"minecraft:directory", "source":"items", "prefix":"items/"}
    write("assets/minecraft/atlases/blocks", {"sources":[blocks] if newest else [blocks, items]})
    if newest:
        write("assets/minecraft/atlases/items", {"sources":[items]})

generate_world(repo, out, args.target, write)
generate_families(repo, out, args.target, write)

# The original Classic game profile has distinct machine IDs that coexist with Experimental.
classic = json.loads((repo / "spec/registry/classic-playable-recipes.json").read_text())
for entry in classic["recipes"]:
    if "pattern" in entry:
        recipe = {"type": "minecraft:crafting_shaped", "pattern": entry["pattern"],
                  "key": {k: ingredient(v) for k, v in entry["key"].items()}}
    else:
        recipe = {"type": "minecraft:crafting_shapeless",
                  "ingredients": [ingredient(v) for v in entry["ingredients"]]}
    recipe["result"] = result(entry["output"], entry["count"])
    data(recipe_dir + "classic/" + entry["output"], recipe)

count = sum(1 for _ in out.rglob("*.json"))
publish(out, destination)
workspace.cleanup()
print(f"{args.target}: generated {count} JSON files in {destination}")
