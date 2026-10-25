package cn.fandmc.epoch.network.mapping;

import cn.fandmc.epoch.asset.*;
import cn.fandmc.epoch.item.*;
import cn.fandmc.epoch.nbt.*;
import cn.fandmc.epoch.network.codec.SlotData;
import java.io.IOException;
import java.util.*;

/** All numeric item identities remain here, before items enter inventory state. */
public final class LegacyItemMapping {
    public record Visual(ResourceId model,String translation,int[] colors) { }
    private final Map<Integer,Item> wireTypes=new HashMap<>();
    private final Map<Integer,Visual> visuals=new HashMap<>();
    private final Map<ResourceId,Integer> outgoing=new HashMap<>();
    private record StackKey(ResourceId item,int damage,NbtCompound tag) { }
    private final Map<StackKey,SlotData> received=Collections.synchronizedMap(new LinkedHashMap<>(128,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<StackKey,SlotData> entry){return size()>4096;}
    });
    private static StackKey stackKey(ItemStack stack){return new StackKey(stack.item().id(),stack.damage(),stack.tag());}
    private final ItemRegistry registry;
    public LegacyItemMapping()throws IOException {
        var data=JsonResources.bundled("/epoch/network/1.8.9/items.json");List<Item> types=new ArrayList<>();
        for(var entry:data.getAsJsonObject("definitions").entrySet()){var v=entry.getValue().getAsJsonObject();int damage=v.get("maximumDamage").getAsInt();var id=ResourceId.parse(entry.getKey());types.add(damage>0?new DamageableItem(id,damage):new Item(id,v.get("maximumStackSize").getAsInt()));}
        registry=new ItemRegistry(types);
        for(var entry:data.getAsJsonObject("variants").entrySet()){
            String[] parts=entry.getKey().split(":");int key=Integer.parseInt(parts[0])<<16|Integer.parseInt(parts[1]);var v=entry.getValue().getAsJsonObject();var id=ResourceId.parse(v.get("name").getAsString());
            wireTypes.put(key,registry.item(id));outgoing.putIfAbsent(id,key);var a=v.getAsJsonArray("colors");int[] colors={a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt()};
            String model=v.get("model").getAsString();visuals.put(key,new Visual(model.isEmpty()?null:ResourceId.parse(model),v.get("translation").getAsString(),colors));
        }
    }
    public ItemRegistry registry(){return registry;}
    private int key(SlotData data){int key=data.id()<<16|data.metadata();return wireTypes.containsKey(key)?key:data.id()<<16;}
    public ItemStack decode(SlotData data)throws IOException {
        if(data.id()<0)return ItemStack.EMPTY;
        Item item=wireTypes.get(key(data));if(item==null)throw new IOException("Unsupported item: "+data.id());
        NbtCompound tag=data.tag();
        if(data.id()==373){Map<String,Object> tags=new LinkedHashMap<>(tag.values());int base=data.metadata()&15;String[] names={"water","regeneration","swiftness","fire_resistance","poison","healing","night_vision","water","weakness","strength","slowness","leaping","harming","water_breathing","invisibility","water"};String potion=base==0?switch(data.metadata()&127){case 16->"awkward";case 32->"thick";case 64->"mundane";default->"water";}:names[base];if(base!=0){if((data.metadata()&32)!=0)potion="strong_"+potion;else if((data.metadata()&64)!=0)potion="long_"+potion;}tags.put("Potion","minecraft:"+potion);tag=new NbtCompound(tags);}
        ItemStack stack=new ItemStack(item,data.count(),item instanceof DamageableItem?data.metadata():0,tag);received.put(stackKey(stack),data);return stack;
    }
    public SlotData encode(ItemStack stack){
        if(stack.isEmpty())return SlotData.EMPTY;
        SlotData original=received.get(stackKey(stack));if(original!=null)return new SlotData(original.id(),stack.count(),original.metadata(),original.tag());
        int key=outgoing.get(stack.item().id());NbtCompound tag=stack.tag();int metadata=stack.item() instanceof DamageableItem?stack.damage():key&65535;
        if(stack.item().id().path().equals("potion")||stack.item().id().path().equals("splash_potion")){
            String potion=tag.string("Potion","minecraft:water").replace("minecraft:","");
            boolean strong=potion.startsWith("strong_"),extended=potion.startsWith("long_");potion=potion.replace("strong_","").replace("long_","");
            int base=switch(potion){case "regeneration"->1;case "swiftness"->2;case "fire_resistance"->3;case "poison"->4;case "healing"->5;case "night_vision"->6;case "weakness"->8;case "strength"->9;case "slowness"->10;case "leaping"->11;case "harming"->12;case "water_breathing"->13;case "invisibility"->14;default->0;};
            metadata=base==0?switch(potion){case "awkward"->16;case "thick"->32;case "mundane"->64;default->0;}:8192|base|(strong?32:extended?64:0);
            if(stack.item().id().path().equals("splash_potion"))metadata=(metadata&~8192)|16384;
            Map<String,NbtTag> values=new LinkedHashMap<>(tag.values());values.remove("Potion");tag=new NbtCompound(values);
        }
        return new SlotData(key>>>16,stack.count(),metadata,tag);
    }
    public Visual visual(ItemStack stack){if(stack.isEmpty())return null;SlotData data=encode(stack);return visuals.get(key(data));}
}
