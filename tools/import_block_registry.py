"""Rebuild Epoch's flat registry from pinned minecraft-data and local 1.8 assets.
Only IDs present in data/pc/1.8/blocks.json are accepted. No network access.
"""
import json
from copy import deepcopy
from itertools import product
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'tools/data'
OUT = ROOT / 'epoch-client/src/main/resources/epoch'
blocks = json.loads((DATA / 'blocks-1.8.json').read_text(encoding='utf-8'))
legacy = json.loads((DATA / 'legacy.json').read_text(encoding='utf-8'))['blocks']
collisions = json.loads((DATA / 'collision-shapes-1.8.json').read_text(encoding='utf-8'))
materials = json.loads((DATA / 'block-materials-1.8.json').read_text(encoding='utf-8'))
render_flags = json.loads((DATA / 'block-render-flags-1.8.json').read_text(encoding='utf-8'))
light = json.loads((DATA / 'block-light-1.8.json').read_text(encoding='utf-8'))
assets = ROOT / 'assets/minecraft/blockstates'
aliases = {
    'grass_block': 'grass', 'polished_granite': 'smooth_granite',
    'polished_diorite': 'smooth_diorite', 'polished_andesite': 'smooth_andesite',
    'bricks': 'brick_block', 'stone_bricks': 'stonebrick', 'mossy_stone_bricks': 'mossy_stonebrick',
    'cracked_stone_bricks': 'cracked_stonebrick', 'chiseled_stone_bricks': 'chiseled_stonebrick',
    'nether_bricks': 'nether_brick', 'lily_pad': 'waterlily', 'melon': 'melon_block',
    'terracotta': 'hardened_clay', 'cobweb': 'web', 'dead_bush': 'dead_bush',
    'short_grass': 'tall_grass', 'grass': 'tall_grass', 'tall_grass': 'double_grass',
    'large_fern': 'double_fern', 'lilac': 'syringa', 'peony': 'paeonia',
    'dirt_path': 'grass_path', 'oak_door': 'wooden_door', 'oak_trapdoor': 'trapdoor',
    'oak_button': 'wooden_button', 'oak_pressure_plate': 'wooden_pressure_plate',
    'oak_fence': 'fence', 'oak_fence_gate': 'fence_gate', 'snow_block': 'snow',
    'snow': 'snow_layer', 'red_bed': 'bed', 'bricks_slab': 'brick_slab',
    'petrified_oak_slab': 'wood_old_slab', 'bricks_stairs': 'brick_stairs',
    'cobblestone_stairs': 'stone_stairs',
    'wall_torch': 'torch', 'redstone_wall_torch': 'redstone_torch',
    'powered_rail': 'golden_rail', 'spawner': 'mob_spawner', 'sugar_cane': 'reeds',
    'smooth_stone_slab': 'stone_slab', 'smooth_stone': 'stone_double_slab',
    'end_stone_bricks': 'end_bricks', 'nether_portal': 'portal',
    'note_block': 'noteblock', 'nether_quartz_ore': 'quartz_ore',
    # The 1.8 name "smooth" is today's cut sandstone, not the seamless double slab.
    'cut_sandstone': 'smooth_sandstone', 'cut_red_sandstone': 'smooth_red_sandstone',
    'infested_stone': 'stone_monster_egg',
    'infested_cobblestone': 'cobblestone_monster_egg',
    'infested_stone_bricks': 'stone_brick_monster_egg',
    'infested_mossy_stone_bricks': 'mossy_brick_monster_egg',
    'infested_cracked_stone_bricks': 'cracked_brick_monster_egg',
    'infested_chiseled_stone_bricks': 'chiseled_brick_monster_egg',
}

def parse_state(value):
    name, _, tail = value.partition('[')
    props = dict(item.split('=', 1) for item in tail.rstrip(']').split(',') if item)
    # Waterlogging does not exist in Protocol 47.
    props.pop('waterlogged', None)
    return name, props

def key(name, props):
    return name + ('[' + ','.join(f'{k}={v}' for k, v in sorted(props.items())) + ']' if props else '')

def connection_kind(name):
    if name.endswith('_fence_gate'): return 'gate'
    if name.endswith('_fence'): return 'fence'
    if name.endswith('_wall'): return 'wall'
    if name.endswith('_pane') or name == 'minecraft:iron_bars': return 'pane'
    return None

def appearance(name, props, block, meta):
    short = name.split(':')[1]
    resource = aliases.get(short, short)
    p = dict(props)
    if short in ('brown_mushroom_block', 'red_mushroom_block'):
        resource = short
        p = {'variant': mushroom_variant(props)}
    if short == 'mushroom_stem':
        # Both legacy stem variants use the same stem textures in the 1.8 assets.
        resource = 'brown_mushroom_block'
        p = {'variant': 'all_stem' if props.get('up') == 'true' else 'stem'}
    if short in ('sponge', 'wet_sponge'):
        resource = 'sponge'
        p = {'wet': str(short == 'wet_sponge').lower()}
    if short in ('anvil', 'chipped_anvil', 'damaged_anvil'):
        resource = 'anvil'
        p['damage'] = {'anvil': '0', 'chipped_anvil': '1', 'damaged_anvil': '2'}[short]
    if short in ('smooth_quartz', 'smooth_sandstone', 'smooth_red_sandstone'):
        resource = short.removeprefix('smooth_') + '_double_slab'
        p = {'variant': 'all'}
    if short.endswith('_terracotta'):
        resource = short.replace('_terracotta', '_stained_hardened_clay')
    resource = resource.replace('light_gray_', 'silver_')
    if short.endswith('_wood'):
        resource = short.removesuffix('_wood') + '_log'
        p['axis'] = 'none'
    if short.endswith('_slab'):
        slab_type = p.pop('type', 'bottom')
        if slab_type == 'double':
            resource = resource.replace('_slab', '_double_slab')
        else:
            p['half'] = slab_type
    if short.endswith('_stairs'):
        p.setdefault('shape', 'straight')
        # Old resource variants flip left/right when the model is rotated upside down.
        # Runtime shape names retain their world-space meaning for either half.
        if p.get('half') == 'top' and p['shape'] != 'straight':
            stem, side = p['shape'].rsplit('_', 1)
            p['shape'] = stem + ('_right' if side == 'left' else '_left')
    if short.endswith('_button'):
        face = p.pop('face', 'wall')
        if face != 'wall': p['facing'] = 'up' if face == 'floor' else 'down'
    if short.endswith('_fence') or short.endswith('_pane') or short in ('iron_bars', 'cobblestone_wall', 'mossy_cobblestone_wall'):
        for direction in ('north', 'south', 'east', 'west'): p.setdefault(direction, 'false')
        p.setdefault('up', 'true')
    if short.endswith('_leaves'):
        p['decayable'] = str(not (meta & 4)).lower()
        p['check_decay'] = str(bool(meta & 8)).lower()
    if short in ('torch', 'redstone_torch'): p['facing'] = 'up'
    if short in ('redstone_torch', 'redstone_wall_torch') and p.get('lit') == 'false': resource = 'unlit_redstone_torch'
    if short == 'furnace' and p.get('lit') == 'true': resource = 'lit_furnace'
    if short == 'redstone_ore' and p.get('lit') == 'true': resource = 'lit_redstone_ore'
    if short == 'redstone_lamp' and p.get('lit') == 'true': resource = 'lit_redstone_lamp'
    if short.endswith('_door'):
        p.setdefault('hinge', 'left'); p.setdefault('facing', 'north'); p.setdefault('open', 'false')
    if short in ('pumpkin', 'carved_pumpkin'): resource = 'pumpkin'
    if short == 'jack_o_lantern': resource = 'lit_pumpkin'
    if short == 'quartz_pillar': resource = 'quartz_column'
    if short == 'repeater': resource = 'powered_repeater' if p.get('powered') == 'true' else 'unpowered_repeater'
    if short == 'comparator': resource = 'powered_comparator' if p.get('powered') == 'true' else 'unpowered_comparator'
    if short == 'daylight_detector':
        resource = 'daylight_detector_inverted' if p.pop('inverted', 'false') == 'true' else 'daylight_detector'
    if short == 'sandstone': resource = 'sandstone'
    if short == 'dandelion': resource = 'dandelion'
    # An unresolved resource remains explicit; the runtime reports it, never uses a random cube.
    return 'minecraft:' + resource, p

def mushroom_variant(props):
    sides = ''.join(direction[0] for direction in ('north', 'east', 'south', 'west')
                    if props.get(direction) == 'true')
    if all(props.get(direction) == 'true' for direction in ('north', 'east', 'south', 'west', 'up', 'down')):
        return 'all_outside'
    if not any(props.get(direction) == 'true' for direction in ('north', 'east', 'south', 'west', 'up', 'down')):
        return 'all_inside'
    if props.get('up') != 'true' and props.get('down') != 'true':
        return 'all_stem'
    if sides == '': return 'center'
    return {
        'nw': 'north_west', 'n': 'north', 'ne': 'north_east', 'w': 'west',
        'e': 'east', 'sw': 'south_west', 's': 'south', 'es': 'south_east',
    }[sides]

states = {}
mapping = {}
recognized = 0
for block in blocks:
    for meta in range(16):
        wire = block['id'] * 16 + meta
        value = legacy.get(f"{block['id']}:{meta}")
        # BlockPane inherits Block.getStateFromMeta: every metadata nibble selects
        # the default state. Stained panes override it to preserve the color.
        if block['id'] in (101, 102): value = legacy[f"{block['id']}:0"]
        if block['id'] == 25: value = legacy['25:0']
        if block['id'] == 78: value = legacy[f'78:{meta & 7}']
        if block['id'] == 111: value = legacy['111:0']
        if block['id'] == 144:
            facing = {2: 'north', 3: 'south', 4: 'west', 5: 'east'}.get(meta & 7)
            value = f'minecraft:skeleton_wall_skull[facing={facing}]' if facing else 'minecraft:skeleton_skull[rotation=0]'
        # Quartz accepts exactly variants 0..4; out-of-range metadata selects the default.
        if block['id'] == 155: value = legacy[f"155:{meta if meta <= 4 else 0}"]
        # Upper door metadata ignores bit 2; the other half supplies the missing properties.
        if block['id'] in (64, 71, 193, 194, 195, 196, 197):
            value = legacy[f"{block['id']}:{meta & 11 if meta & 8 else meta}"]
        if value is None:
            name, props = f"epoch:unmapped_legacy_{block['id']}_{meta}", {}
        else:
            name, props = parse_state(value)
            recognized += 1
        # Metadata stores only direction and half. Shape is resolved from neighbors.
        if name.endswith('_stairs'): props['shape'] = 'straight'
        if name.endswith('_door'):
            if props['half'] == 'upper': props.update(facing='north', open='false')
            else: props.update(hinge='left', powered='false')
        kind = connection_kind(name)
        if kind in ('fence', 'wall', 'pane'):
            for direction in ('north', 'east', 'south', 'west'): props[direction] = 'false'
            if kind == 'wall': props['up'] = 'false'
        if kind == 'gate': props['in_wall'] = 'false'
        # Floor/ceiling buttons have no horizontal orientation in the old wire format.
        if name.endswith('_button') and props.get('face') in ('floor', 'ceiling'): props['facing'] = 'north'
        if block['id'] in (8, 9, 10, 11): props['flowing'] = str(block['id'] in (8, 10)).lower()
        if block['id'] in (18, 161): props['check_decay'] = str(bool(meta & 8)).lower()
        identity = key(name, props)
        resource, resource_props = appearance(name, props, block, meta)
        shape = collisions['blocks'][block['name']]
        if isinstance(shape, list): shape = shape[meta]
        definition = dict(name=name, properties=props, resource=resource, resourceProperties=resource_props,
                          opaque=not block['transparent'], collidable=block['boundingBox'] != 'empty',
                          emission=light['blocks'][block['name']]['emission'], recognized=value is not None,
                          lightOpacity=min(15, light['blocks'][block['name']]['opacity']),
                          neighborBrightness=light['blocks'][block['name']]['neighborBrightness'],
                          opaqueCube=light['blocks'][block['name']]['opaqueCube'],
                          fullCube=light['blocks'][block['name']]['fullCube'],
                          translucent=light['blocks'][block['name']]['translucent'],
                          ambientOcclusion=light['blocks'][block['name']]['ambientOcclusion'],
                          blocksMovement=materials['blocks'][block['name']]['blocksMovement'],
                          solidForFluid=materials['blocks'][block['name']]['solidForFluid'],
                          normalCube=render_flags['blocks'][block['name']]['normalCube'],
                          providesPower=render_flags['blocks'][block['name']]['providesPower'],
                          collision=collisions['shapes'][str(shape)])
        states.setdefault(identity, definition)
        mapping[str(wire)] = identity

# The pinned shape archive contains 3/16 ladders; the local 1.8.9 BlockLadder
# uses 2/16 (ahr.a(adq,cj), 0.125f). Keep that distinction out of runtime IDs.
for definition in states.values():
    if definition['name'] == 'minecraft:snow':
        # A single layer has a zero-height collision plane in 1.8, not null.
        # It can stop a falling entity even though it has no side wall.
        height = (int(definition['properties']['layers']) - 1) / 8
        definition['collision'] = [[0, 0, 0, 1, height, 1]]
    if definition['name'] == 'minecraft:lily_pad':
        definition['collision'] = [[0, 0, 0, 1, 1 / 64, 1]]
    if definition['name'] == 'minecraft:ladder':
        definition['collision'] = {
            'north': [[0, 0, .875, 1, 1, 1]],
            'south': [[0, 0, 0, 1, 1, .125]],
            'west': [[.875, 0, 0, 1, 1, 1]],
            'east': [[0, 0, 0, .125, 1, 1]],
        }[definition['properties']['facing']]

def door_collision(props):
    facing, opened, right = props['facing'], props['open'] == 'true', props['hinge'] == 'right'
    plane = {'east': ('south' if right else 'north') if opened else 'west',
             'south': ('west' if right else 'east') if opened else 'north',
             'west': ('north' if right else 'south') if opened else 'east',
             'north': ('east' if right else 'west') if opened else 'south'}[facing]
    return [{'west': [0, 0, 0, .1875, 1, 1], 'east': [.8125, 0, 0, 1, 1, 1],
             'north': [0, 0, 0, 1, 1, .1875], 'south': [0, 0, .8125, 1, 1, 1]}[plane]]

door_definitions = {d['name']: d for d in states.values() if d['name'].endswith('_door')}
for definition in door_definitions.values():
    for half, facing, hinge, opened, powered in product(('lower', 'upper'), ('east', 'south', 'west', 'north'),
                                                       ('left', 'right'), ('false', 'true'), ('false', 'true')):
        derived = deepcopy(definition)
        derived['properties'] = dict(half=half, facing=facing, hinge=hinge, open=opened, powered=powered)
        derived['resourceProperties'] = dict(derived['properties'])
        derived['collision'] = door_collision(derived['properties'])
        states[key(derived['name'], derived['properties'])] = derived

def stair_collision(props):
    top = props['half'] == 'top'
    low, high = (0.0, 0.5) if top else (0.5, 1.0)
    boxes = [[0.0, 0.5 if top else 0.0, 0.0, 1.0, 1.0 if top else 0.5, 1.0]]
    dx, dz = {'east': (1, 0), 'south': (0, 1), 'west': (-1, 0), 'north': (0, -1)}[props['facing']]
    shape = props['shape']
    # Left and right are relative to the stair's ascending direction.
    sx, sz = (dz, -dx) if shape.endswith('_left') else (-dz, dx)
    for x in (0, 1):
        for z in (0, 1):
            forward = (x * 2 - 1) * dx + (z * 2 - 1) * dz > 0
            side = (x * 2 - 1) * sx + (z * 2 - 1) * sz > 0
            occupied = forward and side if shape.startswith('outer_') else forward or side if shape.startswith('inner_') else forward
            if occupied: boxes.append([x * .5, low, z * .5, (x + 1) * .5, high, (z + 1) * .5])
    return boxes

for definition in list(states.values()):
    if not definition['name'].endswith('_stairs'): continue
    for shape in ('straight', 'inner_left', 'inner_right', 'outer_left', 'outer_right'):
        derived = deepcopy(definition)
        derived['properties']['shape'] = shape
        resource_shape = shape
        if derived['properties']['half'] == 'top' and shape != 'straight':
            stem, side = shape.rsplit('_', 1)
            resource_shape = stem + ('_right' if side == 'left' else '_left')
        derived['resourceProperties']['shape'] = resource_shape
        derived['collision'] = stair_collision(derived['properties'])
        states[key(derived['name'], derived['properties'])] = derived

def connected_collision(kind, props):
    n, e, s, w = (props[d] == 'true' for d in ('north', 'east', 'south', 'west'))
    if kind == 'wall':
        x0, x1, z0, z1 = (0.0 if w else .25), (1.0 if e else .75), (0.0 if n else .25), (1.0 if s else .75)
        if n and s and not e and not w: x0, x1 = .3125, .6875
        if e and w and not n and not s: z0, z1 = .3125, .6875
        return [[x0, 0.0, z0, x1, 1.5, z1]]
    if kind == 'pane':
        # 1.8 collision has two crossing strips and no extra center post.
        # A single arm stops at 8/16; 1.9+ extends it by another 1/16.
        isolated = not (n or e or s or w)
        boxes = []
        if (w and e) or isolated:
            boxes.append([0.0, 0.0, .4375, 1.0, 1.0, .5625])
        elif w:
            boxes.append([0.0, 0.0, .4375, .5, 1.0, .5625])
        elif e:
            boxes.append([.5, 0.0, .4375, 1.0, 1.0, .5625])
        if (n and s) or isolated:
            boxes.append([.4375, 0.0, 0.0, .5625, 1.0, 1.0])
        elif n:
            boxes.append([.4375, 0.0, 0.0, .5625, 1.0, .5])
        elif s:
            boxes.append([.4375, 0.0, .5, .5625, 1.0, 1.0])
        return boxes
    boxes = [[.375, 0.0, 0.0 if n else .375, .625, 1.5, 1.0 if s else .625]]
    if e or w: boxes.append([0.0 if w else .375, 0.0, .375, 1.0 if e else .625, 1.5, .625])
    return boxes

for definition in list(states.values()):
    kind = connection_kind(definition['name'])
    if kind is None: continue
    for variant in range(2 if kind == 'gate' else 32 if kind == 'wall' else 16):
        derived = deepcopy(definition)
        props, resource_props = derived['properties'], derived['resourceProperties']
        if kind == 'gate':
            props['in_wall'] = resource_props['in_wall'] = str(bool(variant)).lower()
        else:
            for index, direction in enumerate(('east', 'south', 'west', 'north')):
                props[direction] = resource_props[direction] = str(bool(variant & (1 << index))).lower()
            if kind == 'wall': props['up'] = resource_props['up'] = str(bool(variant & 16)).lower()
            derived['collision'] = connected_collision(kind, props)
        states[key(derived['name'], props)] = derived
# Directional wire connections and repeater locks are derived from neighbors.
for definition in list(states.values()):
    name = definition['name']
    if name == 'minecraft:redstone_wire':
        for directions in product(('none', 'side', 'up'), repeat=4):
            derived = deepcopy(definition)
            for direction, value in zip(('east', 'south', 'west', 'north'), directions):
                derived['properties'][direction] = derived['resourceProperties'][direction] = value
            states[key(name, derived['properties'])] = derived
    elif name == 'minecraft:repeater':
        derived = deepcopy(definition)
        derived['properties']['locked'] = derived['resourceProperties']['locked'] = 'true'
        states[key(name, derived['properties'])] = derived
OUT.mkdir(parents=True, exist_ok=True)
revision = (DATA / 'minecraft-data-revision.txt').read_text(encoding='utf-8-sig').strip()
source = dict(project='PrismarineJS/minecraft-data', revision=revision, protocol=47,
              materials=materials['source'], light=light['source'],
              paneCollision='1.8.9',
              ladderCollision='1.8.9',
              note='Filtered by 1.8 IDs; later-version wire definitions are not imported. Derived neighbor states remain separate. Pane and iron-bar collision matches the 1.8.9 client, including half-block arm endpoints and the isolated cross.')
(OUT / 'block-states.json').write_text(json.dumps(dict(source=source, states=states), indent=2) + '\n', encoding='utf-8')
(OUT / 'network/1.8.9').mkdir(parents=True, exist_ok=True)
(OUT / 'network/1.8.9/blocks.json').write_text(json.dumps(dict(source=source, states=mapping), indent=2) + '\n', encoding='utf-8')
print(f'{len(blocks)} block IDs; {recognized} named wire states; {len(mapping)-recognized} explicit unmapped states; {len(states)} interned identities')
