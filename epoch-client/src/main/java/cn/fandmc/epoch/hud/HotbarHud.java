package cn.fandmc.epoch.hud;

import cn.fandmc.epoch.core.GameState;
import cn.fandmc.epoch.gui.*;

public final class HotbarHud extends HudElement {
    private final GameState game;
    private final GuiResources resources;
    private float selected=-1;
    private long lastFrame;
    private cn.fandmc.epoch.item.ItemStack shown=cn.fandmc.epoch.item.ItemStack.EMPTY;
    private long shownUntil;
    public HotbarHud(GameState game,GuiResources resources){this.game=game;this.resources=resources;}
    @Override public void render(GuiGraphics g,int width,int height,float delta){
        float w=9*36+12,x=(width-w)/2,y=height-56;
        g.panel(x,y,w,44,UiTheme.RADIUS);
        int active=game.inventory.selected();long now=System.nanoTime();
        float seconds=lastFrame==0?1:Math.min(.1f,(now-lastFrame)/1e9f);lastFrame=now;
        selected=selected<0?active:selected+(active-selected)*(1-(float)Math.exp(-20*seconds));
        g.roundedRect(x+6+selected*36,y+5,34,34,4,0x407da9ed);
        g.fill(x+14+selected*36,y+39,18,2,UiTheme.ACCENT);
        for(int i=0;i<9;i++){float sx=x+8+i*36;g.item(game.inventory.playerSlot(36+i),sx,y+8,28);g.text(Integer.toString(i+1),sx+1,y+11,8,0xffb7bfcc);}
        var stack=game.inventory.playerSlot(36+active);
        if(!stack.equals(shown)){shown=stack;shownUntil=now+2_000_000_000L;}
        if(!stack.isEmpty()&&now<shownUntil)g.centeredText(resources.itemName(stack),width/2f,y-35,13,UiTheme.TEXT);
        if(game.gameMode!=1&&game.gameMode!=3){
            bar(g,x+7,y-17,150,game.player.health/20f,0xffdf8490);
            bar(g,x+w-157,y-17,150,game.player.food/20f,0xffddb278);
            g.text("生命 "+(int)Math.ceil(game.player.health),x+8,y-21,11,UiTheme.TEXT);
            String food="饱食 "+game.player.food;g.text(food,x+w-8-g.textWidth(food,11),y-21,11,UiTheme.TEXT);
        }
        bar(g,x+7,y-6,w-14,game.experienceProgress,0xff9ecf9b);
        if(game.experienceLevel>0)g.centeredText(Integer.toString(game.experienceLevel),width/2f,y-20,12,0xffb3e4a8);
        for(int i=0;i<4;i++)g.item(game.inventory.playerSlot(5+i),x+w+12+i*25,y+13,23);
    }
    private static void bar(GuiGraphics g,float x,float y,float width,float fraction,int color){g.roundedRect(x,y,width,4,2,0x99222326);g.roundedRect(x,y,width*Math.clamp(fraction,0,1),4,2,color);}
}
