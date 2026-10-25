"""Generate item compatibility and visual data from the installed 1.8 registry."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/'tools/data'
legacy=json.loads((DATA/'legacy.json').read_text())['items']
properties=json.loads((DATA/'item-properties-1.8.json').read_text())
models=json.loads((DATA/'item-models-1.8.json').read_text())['variants']
variants={}; definitions={}
for key,visual in models.items():
    identifier,meta=map(int,key.split(':'))
    source=properties['items'][str(identifier)]
    name=legacy.get(key) or legacy.get(f'{identifier}:0') or source['name']
    if identifier==383:
        eggs={50:'creeper',51:'skeleton',52:'spider',54:'zombie',55:'slime',56:'ghast',57:'zombified_piglin',58:'enderman',59:'cave_spider',60:'silverfish',61:'blaze',62:'magma_cube',65:'bat',66:'witch',67:'endermite',68:'guardian',90:'pig',91:'sheep',92:'cow',93:'chicken',94:'squid',95:'wolf',96:'mooshroom',98:'ocelot',100:'horse',101:'rabbit',120:'villager'}
        name='minecraft:'+eggs.get(meta,'pig')+'_spawn_egg'
    if identifier==373: name='minecraft:splash_potion' if meta&16384 else 'minecraft:potion'
    variants[key]=dict(name=name,model=visual.get('model',''),translation=visual['translation'],colors=visual['colors'])
    definitions.setdefault(name,dict(maximumStackSize=source['maximumStackSize'],maximumDamage=source['maximumDamage']))
output=ROOT/'epoch-client/src/main/resources/epoch/network/1.8.9/items.json'
output.write_text(json.dumps(dict(source=properties['source'],definitions=definitions,variants=variants),indent=2)+'\n')
print(f'Wrote {len(variants)} verified item variants, {len(definitions)} flattened types')
