"""Exercise the Forge release's reactor/chamber, transformer and geothermal block in a real save."""
import argparse
import json
import os
import shutil
from pathlib import Path
from packaged_playable import block, boot, energy, items, merge, seed_eu
from packaged_server import digest


def player_utilities(server):
    """Test visible inventories, a full coolant vessel, native game entities and exact balances."""
    server.send('fill 4 178 4 8 182 8 ic2:reactor_vessel hollow',
                block('6 180 6','ic2:nuclear_reactor'),block('4 180 6','ic2:reactor_redstone_port'),
                block('8 180 6','ic2:reactor_access_hatch'),block('6 182 6','ic2:reactor_fluid_port'))
    for pos in ('5 180 6','7 180 6','6 179 6','6 181 6','6 180 5','6 180 7'):
        server.send(block(pos,'ic2:reactor_chamber'))
    server.require('all_six_reactor_chambers','if data block 6 180 6 {chambers:6}')
    server.send(merge('6 180 6','{coolant:1000,Items:[{Slot:0b,id:"ic2:uranium_fuel_rod",Count:1b},{Slot:1b,id:"ic2:reactor_heat_vent",Count:1b}]}'),
                block('5 190 5','ic2:trade_o_mat'),block('6 190 5','chest'),
                merge('6 190 5','{Items:[{Slot:0b,id:"minecraft:coal",Count:4b}]}'),
                merge('5 190 5','{Items:[{Slot:0b,id:"minecraft:diamond",Count:1b},{Slot:1b,id:"minecraft:coal",Count:2b},{Slot:2b,id:"minecraft:diamond",Count:3b}]}'),
                block('5 200 5','ic2:nuclear_reactor'),block('6 200 5','ic2:rci_rsh[facing=east]'),
                merge('5 200 5','{Items:[{Slot:0b,id:"ic2:rsh_condensator",Count:1b,tag:{ic2_reactor_use:17000}},{Slot:1b,id:"ic2:rsh_condensator",Count:1b,tag:{ic2_reactor_use:17001}}]}'),
                merge('6 200 5','{eu:2000.0d,Items:[{Slot:0b,id:"minecraft:redstone_block",Count:2b}]}'),
                block('8 211 8','ic2:iridium_storage_box'),
                merge('8 211 8','{Items:[{Slot:0b,id:"minecraft:emerald",Count:3b},{Slot:125b,id:"minecraft:diamond",Count:7b}]}'),
                'setblock 8 211 8 air destroy')
    server.require('portable_drop_contains_last_of_126_slots',
                   'if entity @e[type=minecraft:item,nbt={Item:{id:"ic2:iridium_storage_box",tag:{BlockEntityTag:{Items:[{Slot:0b,id:"minecraft:emerald",Count:3b},{Slot:125b,id:"minecraft:diamond",Count:7b}]}}}}]')
    server.require('vending_shop_two_atomic_trades','if data block 5 190 5 {trades:2,Items:[{Slot:2b,id:"minecraft:diamond",Count:1b},{Slot:3b,id:"minecraft:coal",Count:4b}]}','if data block 6 190 5 {Items:[{id:"minecraft:diamond",Count:2b}]}')
    server.require('injector_strict_threshold_and_payment',
                   'if data block 5 200 5 {Items:[{Slot:0b,tag:{ic2_reactor_use:17000}},{Slot:1b,tag:{ic2_reactor_use:0}}]}',
                   'if data block 6 200 5 {eu:1000.0d,Items:[{Slot:0b,id:"minecraft:redstone_block",Count:1b}]}')
    server.send(block('3 230 5','ic2:itnt'),block('8 230 5','ic2:classic_nuke'),block('13 230 5','ic2:nuke'),
                block('3 229 5','redstone_block'),block('8 229 5','redstone_block'),block('13 229 5','redstone_block'))
    server.require('two_native_fuses_empty_device_stays','if entity @e[type=ic2:nuke]','if entity @e[type=ic2:itnt]','if block 13 230 5 ic2:nuke')
    server.send('kill @e[type=ic2:nuke]','kill @e[type=ic2:itnt]')
    server.send(block('3 180 6','redstone_block'))
    server.require('six_reference_cycles_48_hot_952_cold',
                   'if data block 6 180 6 {coolant:952,hot_coolant:48,hull_heat:0,Items:[{Slot:0b,tag:{ic2_reactor_use:6}}]}')
    server.send(block('3 180 6','air'))


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--java', required=True)
    p.add_argument('--libraries', required=True, type=Path)
    p.add_argument('--jar', required=True, type=Path)
    p.add_argument('--eula', required=True, type=Path)
    p.add_argument('--instance', required=True, type=Path)
    args = p.parse_args()
    directory = args.instance.resolve()
    directory.mkdir(parents=True, exist_ok=False)
    if 'eula=true' not in args.eula.read_text().splitlines():
        p.error('Provide an accepted Forge server eula.txt')
    (directory/'libraries').symlink_to(args.libraries.resolve(),target_is_directory=True)
    (directory/'mods').mkdir()
    jar=directory/'mods'/args.jar.name
    shutil.copy2(args.jar,jar)
    fingerprint=digest(jar)
    shutil.copy2(args.eula, directory/'eula.txt')
    (directory/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\nlevel-seed=3060\nview-distance=2\nsimulation-distance=2\ngenerate-structures=false\n')
    opts='win_args.txt' if os.name=='nt' else 'unix_args.txt'
    command=[args.java,'-Xmx1200M',f'@libraries/net/minecraftforge/forge/1.19.2-43.5.0/{opts}','nogui']
    record={}
    with boot(directory,command,'power-write') as server:
        server.send('forceload add 0 0 15 15',block('3 150 3','ic2:nuclear_reactor'),block('4 150 3','ic2:reactor_chamber'),
                    block('5 150 3','ic2:batbox[facing=east]'),block('3 150 2','redstone_block'),
                    merge('3 150 3','{Items:[{Slot:0b,id:"ic2:uranium_fuel_rod",Count:1b}]}'),
                    block('3 150 7','ic2:geo_generator'),block('4 150 7','ic2:batbox[facing=east]'),merge('3 150 7','{Items:[{Slot:0b,id:"minecraft:lava_bucket",Count:1b}]}'),
                    block('3 150 11','ic2:mfe[facing=east]'),block('4 150 11','ic2:mv_transformer[facing=west]'),block('5 150 11','ic2:cesu[facing=east]'),seed_eu('3 150 11',1536))
        server.require('real_reactor_generates_5', 'if data block 3 150 3 {output:5.0d,chambers:1}', 'unless data block 5 150 3 {ic2_energy:0.0d}')
        server.require('native_transformer_balanced',energy('5 150 11',1536),energy('3 150 11',0))
        server.send(block('3 150 2','air'))
        server.require('reactor_stopped','if data block 3 150 3 {output:0.0d}','unless data block 3 150 3 {hull_heat:0}')
        for key,path in [('heat','hull_heat'),('wear','Items[0].tag.ic2_reactor_use')]:
            server.send(f'execute store result storage power:baseline {key} int 1 run data get block 3 150 3 {path}')
        server.send('execute store result storage power:baseline energy int 1 run data get block 5 150 3 ic2_energy')
        server.require('geothermal_delivered_10000',energy('4 150 7',10000),items('3 150 7','minecraft:bucket',1,1),seconds=65)
        player_utilities(server)
        record['write']=server.checks
    with boot(directory,command,'power-read') as server:
        server.send('scoreboard objectives add measure dummy', 'execute store result score #heat measure run data get block 3 150 3 hull_heat',
                    'execute store result score #oldheat measure run data get storage power:baseline heat',
                    'execute store result score #wear measure run data get block 3 150 3 Items[0].tag.ic2_reactor_use',
                    'execute store result score #oldwear measure run data get storage power:baseline wear',
                    'execute store result score #eu measure run data get block 5 150 3 ic2_energy',
                    'execute store result score #oldeu measure run data get storage power:baseline energy')
        server.require('exact_restart_heat_wear_eu','if score #heat measure = #oldheat measure','if score #wear measure = #oldwear measure',
                       'if score #eu measure = #oldeu measure','if score #wear measure matches 1..','if data block 3 150 3 {chambers:1,output:0.0d}')
        server.send(block('3 150 2','redstone_block'))
        server.require('resumed_generation','if data block 3 150 3 {output:5.0d}')
        record['read']=server.checks
    assert digest(jar)==fingerprint
    (directory/'result.json').write_text(json.dumps({'jar':jar.name,'sha256':fingerprint,'checks':record},indent=2)+'\n')


if __name__=='__main__':
    main()
