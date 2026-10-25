package cn.fandmc.epoch.gui;

import cn.fandmc.epoch.inventory.InventoryState;
import cn.fandmc.epoch.item.ItemStack;
import cn.fandmc.epoch.nbt.NbtString;
import java.util.*;

/** Slot IDs are server window indices, shared by hit testing and rendering. */
public final class InventoryScreen extends Screen {
    @FunctionalInterface public interface Click {void click(int slot,int button,boolean shift);}
    private record Slot(int id,float x,float y) {}
    private final InventoryState inventory;
    private final GuiResources resources;
    private final Click click;
    private final Runnable closeWindow;
    private final int windowId;
    private List<Slot> slots=List.of();
    private float left,top,panelWidth,panelHeight,bodyBottom;
    private int laidOutSize;
    private float scale=1;
    public InventoryScreen(InventoryState inventory,GuiResources resources,Click click,Runnable closeWindow){
        super(inventory.title());this.inventory=inventory;this.resources=resources;this.click=click;this.closeWindow=closeWindow;windowId=inventory.windowId();
    }
    public int windowId(){return windowId;}
    @Override public boolean showsHud(){return false;}
    @Override protected void onResize(int width,int height){layout();}
    private void layout(){
        List<Slot> list=new ArrayList<>();panelWidth=9*34+28;
        int rows=windowId==0?3:(inventory.topSlots()+8)/9;
        panelHeight=windowId==0?358:rows*34+220;
        scale=Math.min(1,Math.min(width()/(panelWidth+24),height()/(panelHeight+24)));
        scale=Math.max(.1f,scale);left=(width()/scale-panelWidth)/2;top=Math.max(12,(height()/scale-panelHeight)/2);
        if(windowId==0){
            for(int i=0;i<4;i++)list.add(new Slot(5+i,left+14,top+43+i*34));
            for(int i=0;i<4;i++)list.add(new Slot(1+i,left+174+(i%2)*34,top+49+(i/2)*34));
            list.add(new Slot(0,left+270,top+66));
            bodyBottom=top+198;
        }else{
            for(int i=0;i<inventory.topSlots();i++)list.add(new Slot(i,left+14+(i%9)*34,top+43+(i/9)*34));
            bodyBottom=top+43+rows*34+17;
        }
        for(int i=0;i<27;i++)list.add(new Slot(windowId==0?9+i:inventory.topSlots()+i,left+14+(i%9)*34,bodyBottom+(i/9)*34));
        for(int i=0;i<9;i++)list.add(new Slot(windowId==0?36+i:inventory.topSlots()+27+i,left+14+i*34,bodyBottom+110));
        panelHeight=bodyBottom-top+160;slots=List.copyOf(list);laidOutSize=inventory.size();
    }
    @Override protected void onClose(){closeWindow.run();}
    @Override public boolean keyPressed(KeyInput key){if(key.key()==69&&!key.repeat()){close();return true;}return false;}
    @Override public boolean mousePressed(double x,double y,int button,int modifiers){
        if(button!=0&&button!=1)return true;
        x/=scale;y/=scale;
        for(Slot slot:slots)if(inside(slot,x,y)){click.click(slot.id(),button,(modifiers&1)!=0);return true;}
        if(x<left||x>=left+panelWidth||y<top||y>=top+panelHeight)click.click(-999,button,false);
        return true;
    }
    private static boolean inside(Slot slot,double x,double y){return x>=slot.x()&&x<slot.x()+30&&y>=slot.y()&&y<slot.y()+30;}
    @Override public void render(GuiGraphics g,double mouseX,double mouseY,float delta){
        if(laidOutSize!=inventory.size())layout();
        g.fill(0,0,width(),height(),0x680c0d12);
        g.save();g.scale(scale,scale);mouseX/=scale;mouseY/=scale;
        g.panel(left,top,panelWidth,panelHeight,UiTheme.RADIUS);
        g.fill(left+14,top+16,3,12,UiTheme.ACCENT);g.text(title(),left+25,top+27,16,UiTheme.TEXT);
        if(windowId==0){g.text("合成",left+174,top+42,11,UiTheme.MUTED);g.text("→",left+245,top+86,18,UiTheme.MUTED);}
        else if(inventory.type().equals("minecraft:furnace")){
            int maximum=inventory.property(3);if(maximum>0)g.text("烧炼 "+Math.clamp(inventory.property(2)*100/maximum,0,100)+"%",left+160,top+27,12,UiTheme.ACCENT);
        }
        g.text("背包",left+14,bodyBottom-7,11,UiTheme.MUTED);
        Slot hovered=null;
        for(Slot slot:slots){
            boolean over=inside(slot,mouseX,mouseY);if(over)hovered=slot;
            g.roundedRect(slot.x(),slot.y(),30,30,4,over?0x505d6a80:UiTheme.SLOT);
            g.item(inventory.slot(slot.id()),slot.x()+2,slot.y()+2,26);
        }
        if(inventory.awaitingResync())g.text("等待服务器同步…",left+14,top+panelHeight-10,10,UiTheme.MUTED);
        if(!inventory.cursor().isEmpty())g.item(inventory.cursor(),(float)mouseX-13,(float)mouseY-13,26);
        else if(hovered!=null&&!inventory.slot(hovered.id()).isEmpty())tooltip(g,inventory.slot(hovered.id()),(float)mouseX,(float)mouseY);
        g.restore();
    }
    private void tooltip(GuiGraphics g,ItemStack stack,float x,float y){
        List<String> lines=new ArrayList<>();lines.add(resources.itemName(stack));
        for(var value:stack.tag().compound("display").list("Lore"))if(value instanceof NbtString text&&lines.size()<12)lines.add(text.value());
        float w=0;for(String line:lines)w=Math.max(w,g.textWidth(line,12));w=Math.min(width()/scale-24,w+20);
        float h=lines.size()*18+14;x=Math.min(x+14,width()/scale-w-12);y=Math.max(8,Math.min(y-10,height()/scale-h-8));
        g.panel(x,y,w,h,5);g.save();g.clip(x+8,y+4,w-16,h-8);int i=0;
        for(String line:lines)g.text(line,x+10,y+20+18*i++,12,i==1?UiTheme.TEXT:UiTheme.MUTED);g.restore();
    }
}
