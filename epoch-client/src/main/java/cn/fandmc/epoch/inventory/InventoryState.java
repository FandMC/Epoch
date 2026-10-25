package cn.fandmc.epoch.inventory;

import cn.fandmc.epoch.item.*;
import java.util.*;

/** Game-thread slots and cursor. Container slots include the player's final 36 slots. */
public final class InventoryState {
    private final ItemStack[] player=new ItemStack[45];
    private ItemStack[] container;
    private ItemStack cursor=ItemStack.EMPTY;
    private int windowId,selected,topSlots;private String title="背包",type="minecraft:inventory";
    private long revision;private short action;
    private final Map<Integer,Integer> properties=new HashMap<>();
    private boolean awaitingResync;
    private final Set<Short> pending=new HashSet<>();
    public InventoryState(){Arrays.fill(player,ItemStack.EMPTY);}
    public long revision(){return revision;}public int selected(){return selected;}public int windowId(){return windowId;}
    public int topSlots(){return topSlots;}public String title(){return title;}public String type(){return type;}
    public ItemStack cursor(){return cursor;}public ItemStack playerSlot(int slot){return slot>=0&&slot<45?player[slot]:ItemStack.EMPTY;}
    public ItemStack slot(int slot){ItemStack[] slots=windowId==0?player:container;return slots!=null&&slot>=0&&slot<slots.length?slots[slot]:ItemStack.EMPTY;}
    public short nextAction(){short value=++action;pending.add(value);return value;}
    public boolean awaitingResync(){return awaitingResync;}
    public int size(){return windowId==0?player.length:container==null?0:container.length;}
    public List<ItemStack> visibleItems(){List<ItemStack> result=new ArrayList<>(Arrays.asList(windowId==0?player:container));result.add(cursor);return result;}
    public void transaction(int window,short id,boolean accepted){if(window==windowId&&pending.remove(id)&&!accepted)awaitingResync=true;}
    public void selected(int slot){if(slot>=0&&slot<9){selected=slot;revision++;}}
    public void reset(){Arrays.fill(player,ItemStack.EMPTY);selected=0;close();revision++;}
    public void open(int id,String kind,String caption,int count){windowId=id;type=kind;title=caption;topSlots=count==0?switch(kind){case "minecraft:crafting_table"->10;case "minecraft:furnace"->3;case "minecraft:anvil"->3;case "minecraft:enchanting_table"->2;case "minecraft:brewing_stand"->4;case "minecraft:beacon"->1;default->0;}:count;container=new ItemStack[topSlots+36];Arrays.fill(container,ItemStack.EMPTY);System.arraycopy(player,9,container,topSlots,36);cursor=ItemStack.EMPTY;action=0;pending.clear();awaitingResync=false;properties.clear();revision++;}
    public void close(){windowId=topSlots=0;container=null;title="背包";type="minecraft:inventory";cursor=ItemStack.EMPTY;properties.clear();pending.clear();awaitingResync=false;action=0;revision++;}
    public void items(int id,List<ItemStack> values){
        if(id==0){for(int i=0;i<45;i++)player[i]=i<values.size()?values.get(i):ItemStack.EMPTY;}
        else if(id==windowId&&container!=null){if(values.size()!=container.length){topSlots=Math.max(0,values.size()-36);container=new ItemStack[values.size()];}for(int i=0;i<container.length;i++)container[i]=values.get(i);syncPlayer();}
        if(id==windowId){awaitingResync=false;pending.clear();}revision++;
    }
    public void set(int id,int slot,ItemStack item){
        if(id==-1){cursor=item;revision++;return;}
        if(id==-2){int mapped=slot<9?36+slot:slot>=36?44-slot:slot;if(mapped>=0&&mapped<45)player[mapped]=item;}
        else if(id==0&&slot>=0&&slot<45)player[slot]=item;
        else if(id==windowId&&container!=null&&slot>=0&&slot<container.length){container[slot]=item;syncPlayer();}revision++;
    }
    private void syncPlayer(){if(container.length>=topSlots+36)System.arraycopy(container,topSlots,player,9,36);}
    public void property(int id,int key,int value){if(id==windowId){properties.put(key,value);revision++;}}
    public int property(int key){return properties.getOrDefault(key,0);}
    /** Vanilla pickup/place prediction. Recipe results remain server authoritative. */
    public ItemStack click(int slot,int button){
        if(awaitingResync||button<0||button>1)return ItemStack.EMPTY;
        ItemStack target=slot(slot);
        if(slot==-999){if(!cursor.isEmpty())cursor=button==0||cursor.count()==1?ItemStack.EMPTY:cursor.withCount(cursor.count()-1);revision++;return ItemStack.EMPTY;}
        if(slot<0||slot>=size())return ItemStack.EMPTY;
        if(cursor.isEmpty()){
            int take=button==1?(target.count()+1)/2:target.count();
            if(take>0){cursor=target.withCount(take);set(windowId,slot,target.withCount(target.count()-take));}
        }else if(!accepts(slot,cursor)){
            if(target.sameItemAndData(cursor)&&target.count()+cursor.count()<=cursor.item().maximumStackSize()){
                cursor=cursor.withCount(cursor.count()+target.count());set(windowId,slot,ItemStack.EMPTY);
            }
        }else if(target.isEmpty()){
            int move=Math.min(button==1?1:cursor.count(),capacity(slot,cursor));set(windowId,slot,cursor.withCount(move));cursor=cursor.withCount(cursor.count()-move);
        }else if(target.sameItemAndData(cursor)){
            int move=Math.min(button==1?1:cursor.count(),capacity(slot,cursor)-target.count());
            if(move>0){set(windowId,slot,target.withCount(target.count()+move));cursor=cursor.withCount(cursor.count()-move);}
        }else if(cursor.count()<=capacity(slot,cursor)){set(windowId,slot,cursor);cursor=target;}
        revision++;return target;
    }
    private boolean accepts(int slot,ItemStack item){
        if(windowId==0){
            if(slot==0)return false;
            if(slot>=5&&slot<=8){String name=item.item().id().path();return switch(slot){
                case 5->name.endsWith("_helmet")||name.endsWith("_head")||name.endsWith("_skull")||name.equals("carved_pumpkin")||name.equals("pumpkin");
                case 6->name.endsWith("_chestplate");case 7->name.endsWith("_leggings");default->name.endsWith("_boots");};}
        }
        return !(windowId!=0&&((type.equals("minecraft:crafting_table")&&slot==0)
                ||((type.equals("minecraft:furnace")||type.equals("minecraft:anvil"))&&slot==2)
                ||(type.equals("minecraft:villager")&&slot==2)));
    }
    private int capacity(int slot,ItemStack stack){return windowId==0&&slot>=5&&slot<=8?1:stack.item().maximumStackSize();}
    public boolean canQuickMove(int slot){return !awaitingResync&&cursor.isEmpty()&&slot>=0&&slot<size()
            &&(windowId==0&&slot>=9||windowId!=0&&(type.equals("minecraft:container")||type.equals("minecraft:chest")||type.equals("minecraft:hopper")||type.equals("minecraft:dispenser")||type.equals("minecraft:dropper")));}
    /** Shift transfer for ordinary storage only; specialized machines need their own slot rules. */
    public ItemStack quickMove(int slot){
        if(!canQuickMove(slot))return ItemStack.EMPTY;
        ItemStack original=slot(slot);if(original.isEmpty())return ItemStack.EMPTY;
        int from,to,step=1;
        if(windowId==0){from=slot<36?36:9;to=slot<36?45:36;}
        else if(slot<topSlots){from=size()-1;to=topSlots-1;step=-1;}
        else{from=0;to=topSlots;}
        ItemStack left=original;
        for(int pass=0;pass<2&&!left.isEmpty();pass++)for(int i=from;i!=to&&!left.isEmpty();i+=step){
            ItemStack dest=slot(i);if(!accepts(i,left))continue;
            if(pass==0&&!dest.isEmpty()&&dest.sameItemAndData(left)){
                int count=Math.min(left.count(),capacity(i,left)-dest.count());if(count>0){set(windowId,i,dest.withCount(dest.count()+count));left=left.withCount(left.count()-count);}
            }else if(pass==1&&dest.isEmpty()){int count=Math.min(left.count(),capacity(i,left));set(windowId,i,left.withCount(count));left=left.withCount(left.count()-count);}
        }
        if(left.count()==original.count())return ItemStack.EMPTY;
        set(windowId,slot,left);return original;
    }
}
