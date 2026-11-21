"""Exercise cell consumption and returned containers on an installed Forge production JAR."""

import argparse
from hashlib import sha256
import json
from pathlib import Path
import shutil
import time
from zipfile import ZipFile

from packaged_playable import boot, items


def digest(path):
    return sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('jar', 'libraries', 'eula', 'output'):
        parser.add_argument('--' + name, required=True, type=Path)
    parser.add_argument('--java', default='java')
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    if 'eula=true' not in args.eula.read_text().splitlines():
        parser.error('An accepted Forge EULA is required')
    with ZipFile(args.jar) as archive:
        names = archive.namelist()
        if any('/forge/qa/' in name for name in names):
            parser.error('Use the production JAR, not the QA fixture')
        labels = json.loads(archive.read('assets/ic2/lang/zh_cn.json'))
        for key, value in {'item.ic2.water_cell':'水单元', 'item.ic2.lava_cell':'熔岩单元',
                           'item.ic2.air_cell':'压缩空气单元', 'item.ic2.fluid_cell':'通用流体单元'}.items():
            if labels.get(key) != value:
                raise AssertionError(f'Incorrect packaged Chinese label {key}: {labels.get(key)}')
    (output/'libraries').symlink_to(args.libraries.resolve(), target_is_directory=True)
    shutil.copy2(args.eula, output/'eula.txt')
    (output/'mods').mkdir()
    installed = output/'mods'/args.jar.name
    shutil.copy2(args.jar, installed)
    fingerprint = digest(installed)
    if fingerprint != digest(args.jar):
        raise AssertionError('Installed JAR differs from candidate')
    (output/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\n'
        'level-type=minecraft:flat\ngenerate-structures=false\nview-distance=2\nsimulation-distance=2\n')
    command = [args.java, '-Xmx1400M', '-XX:ActiveProcessorCount=4',
               '@libraries/net/minecraftforge/forge/1.19.2-43.5.0/unix_args.txt', 'nogui']
    single, queued, no_power = '2 150 2', '4 150 2', '6 150 2'
    wrong, blocked, partial, geo7000, geo7001 = ('2 150 6', '4 150 6', '6 150 6', '8 150 6', '10 150 6')
    semi, heater, furnace = '14 150 8', '18 150 8', '19 150 8'
    with boot(output, command, 'fluid-regression') as server:
        server.send('gamerule doMobSpawning false', 'forceload add 0 0 31 31',
            'fill 0 149 0 22 149 11 minecraft:stone',
            f'setblock {single} ic2:ore_washing_plant',
            f'data merge block {single} {{ic2_energy:16000.0d,Items:[{{Slot:0b,id:"ic2:crushed_copper",Count:11b}},{{Slot:6b,id:"ic2:water_cell",Count:1b}}]}}',
            f'setblock {queued} ic2:ore_washing_plant',
            f'data merge block {queued} {{ic2_energy:16000.0d,Items:[{{Slot:6b,id:"ic2:water_cell",Count:2b}}]}}',
            f'setblock {no_power} ic2:ore_washing_plant',
            f'data merge block {no_power} {{Items:[{{Slot:0b,id:"ic2:crushed_copper",Count:1b}},{{Slot:6b,id:"ic2:water_cell",Count:1b}}]}}',
            f'setblock {wrong} ic2:geo_generator',
            f'data merge block {wrong} {{Items:[{{Slot:0b,id:"ic2:water_cell",Count:1b}}]}}',
            f'setblock {blocked} ic2:geo_generator',
            f'data merge block {blocked} {{Items:[{{Slot:0b,id:"ic2:lava_cell",Count:1b}},{{Slot:1b,id:"minecraft:stone",Count:64b}}]}}',
            f'setblock {partial} ic2:geo_generator',
            f'data merge block {partial} {{Items:[{{Slot:0b,id:"ic2:fluid_cell",Count:1b,tag:{{Fluid:{{FluidName:"minecraft:lava",Amount:250}}}}}}]}}',
            f'setblock {geo7000} ic2:geo_generator',
            f'data merge block {geo7000} {{millibuckets:7000,Items:[{{Slot:0b,id:"ic2:lava_cell",Count:1b}}]}}',
            f'setblock {geo7001} ic2:geo_generator',
            f'data merge block {geo7001} {{millibuckets:7001,Items:[{{Slot:0b,id:"ic2:lava_cell",Count:1b}}]}}',
            f'setblock {semi} ic2:semifluid_generator',
            f'data merge block {semi} {{ic2_input_fluid:{{FluidName:"ic2:biogas",Amount:1000}},Items:[{{Slot:0b,id:"ic2:fluid_cell",Count:1b,tag:{{Fluid:{{FluidName:"ic2:biogas",Amount:1000}}}}}}]}}',
            f'setblock {heater} ic2:fluid_heat_generator[facing=east]',
            f'data merge block {heater} {{Items:[{{Slot:0b,id:"ic2:lava_cell",Count:2b}}]}}',
            f'setblock {furnace} ic2:blast_furnace[facing=west]',
            f'data merge block {furnace} {{ic2_heat:60000,Items:[{{Slot:0b,id:"minecraft:iron_ingot",Count:1b}},{{Slot:4b,id:"ic2:air_cell",Count:2b}}]}}',
            'scoreboard objectives add ic2fluid dummy', 'say IC2_FLUID_FIXTURE_READY')
        if not server.wait('[Server] IC2_FLUID_FIXTURE_READY', 20):
            raise AssertionError('World setup did not finish')
        time.sleep(3)
        server.send(f'execute store result score #geo250 ic2fluid run data get block {partial} millibuckets',
                    f'execute store result score #geoEU ic2fluid run data get block {partial} eu',
                    f'execute store result score #semi ic2fluid run data get block {semi} ic2_input_fluid.Amount',
                    f'execute store result score #semiEU ic2fluid run data get block {semi} ic2_energy',
                    f'execute store result score #air ic2fluid run data get block {furnace} ic2_input_fluid.Amount',
                    f'execute store result score #heat ic2fluid run data get block {heater} ic2_input_fluid.Amount')
        server.require('idle_and_unpowered_washers_keep_cells', items(queued, 'ic2:water_cell', 2, 6),
                       f'unless data block {queued} ic2_input_fluid', items(no_power, 'ic2:water_cell', 1, 6),
                       f'unless data block {no_power} ic2_input_fluid')
        server.require('geothermal_wrong_and_blocked_do_not_consume', items(wrong, 'ic2:water_cell', 1),
                       items(blocked, 'ic2:lava_cell', 1), f'if data block {wrong} {{millibuckets:0}}',
                       f'if data block {blocked} {{millibuckets:0}}')
        server.require('geothermal_7000_and_7001_keep_next_cell', items(geo7000, 'ic2:lava_cell', 1),
                       items(geo7001, 'ic2:lava_cell', 1), f'unless data block {geo7000} Items[{{Slot:1b}}]',
                       f'unless data block {geo7001} Items[{{Slot:1b}}]')
        server.require('partial_lava_cell_returns_and_generates', f'unless data block {partial} Items[{{Slot:0b}}]',
                       items(partial, 'ic2:fluid_cell', 1, 1), 'if score #geo250 ic2fluid matches 1..249',
                       'if score #geoEU ic2fluid matches 1..')
        server.require('semifluid_uses_biogas_and_retains_queued_cell', items(semi, 'ic2:fluid_cell', 1),
                       f'unless data block {semi} Items[{{Slot:9b}}]', 'if score #semi ic2fluid matches 1..999',
                       'if score #semiEU ic2fluid matches 1..')
        server.require('heated_furnace_uses_one_air_and_heater_one_lava_cell', items(furnace, 'ic2:air_cell', 1, 4),
                       items(furnace, 'ic2:empty_cell', 1, 13), 'if score #air ic2fluid matches 1..999',
                       items(heater, 'ic2:lava_cell', 1, 0), items(heater, 'ic2:empty_cell', 1, 9),
                       'if score #heat ic2fluid matches 1..999')
        server.send(f'data merge block {queued} {{Items:[{{Slot:0b,id:"ic2:crushed_copper",Count:1b}},'
                    '{Slot:6b,id:"ic2:water_cell",Count:2b}]}')
        server.require('first_water_cell_fills_liter_and_leaves_second',
                       items(queued, 'ic2:water_cell', 1, 6), items(queued, 'ic2:empty_cell', 1, 14),
                       f'if data block {queued} {{ic2_input_fluid:{{FluidName:"minecraft:water",Amount:1000}}}}')
        server.require('single_cell_starts_real_processing',
                       items(single, 'ic2:empty_cell', 1, 14),
                       f'if data block {single} {{ic2_input_fluid:{{FluidName:"minecraft:water",Amount:1000}}}}',
                       f'unless data block {single} {{ic2_progress:0}}')
        for completed in range(1, 11):
            # Existing GameTest and Neo native tests verify natural cycle duration separately.
            server.send(f'data merge block {single} {{ic2_energy:16000.0d,ic2_progress:499}}')
            fluid = (f'if data block {single} {{ic2_input_fluid:{{FluidName:"minecraft:water",Amount:{1000-completed*100}}}}}'
                     if completed < 10 else f'unless data block {single} ic2_input_fluid')
            server.require(f'washer_batch_{completed}_uses_100mb',
                           items(single, 'ic2:purified_copper', completed, 9),
                           items(single, 'ic2:empty_cell', 1, 14), fluid)
        time.sleep(1)
        server.require('eleventh_job_waits_without_water', items(single, 'ic2:crushed_copper', 1, 0),
                       f'if data block {single} {{ic2_progress:0}}', f'unless data block {single} ic2_input_fluid')
        server.state(single, queued, wrong, blocked, partial, geo7000, geo7001, semi, heater, furnace)
        checks = list(server.checks)
    if digest(installed) != fingerprint or digest(args.jar) != fingerprint:
        raise AssertionError('Production JAR changed during the run')
    (output/'result.json').write_text(json.dumps({'status':'passed', 'jar':str(args.jar.resolve()),
        'sha256':fingerprint, 'checks':checks, 'chineseLabels':'passed'}, ensure_ascii=False, indent=2)+'\n')
    print(f'PASS {len(checks)} checks; {fingerprint}')


if __name__ == '__main__':
    main()
