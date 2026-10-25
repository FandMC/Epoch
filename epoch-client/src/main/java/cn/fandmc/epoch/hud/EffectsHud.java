package cn.fandmc.epoch.hud;

import cn.fandmc.epoch.gui.*;
import cn.fandmc.epoch.asset.ResourceId;

public final class EffectsHud extends HudElement {
    private static final ResourceId ICONS=ResourceId.parse("minecraft:gui/container/inventory");
    private static final String[] KEYS={"","moveSpeed","moveSlowdown","digSpeed","digSlowDown","damageBoost","heal","harm","jump","confusion","regeneration","resistance","fireResistance","waterBreathing","invisibility","blindness","nightVision","hunger","weakness","poison","wither","healthBoost","absorption","saturation"};
    private static final int[] ICON={-1,0,1,2,3,4,-1,-1,10,11,7,14,15,16,8,13,12,9,5,6,17,18,18,-1};
    private static final String[] LEVELS={"I","II","III","IV","V","VI","VII","VIII","IX","X"};
    private final EffectState state;
    private final GuiResources resources;
    public EffectsHud(EffectState state,GuiResources resources){this.state=state;this.resources=resources;}
    @Override public void render(GuiGraphics g,int width,int height,float delta){
        if(state.values().isEmpty())return;
        int max=Math.max(1,(height-190)/28),count=Math.min(max,state.values().size());
        float x=16,y=78,w=220;panel(g,x,y,w,36+28*count,"状态效果");
        int row=0;for(var effect:state.values()){
            if(row==count)break;float top=y+34+28*row++;
            int id=effect.id();String name=id>0&&id<KEYS.length?resources.text("potion."+KEYS[id]):"效果 "+id;
            if(effect.amplifier()>0)name+=" "+(effect.amplifier()<LEVELS.length?LEVELS[effect.amplifier()]:effect.amplifier()+1);
            int icon=id>=0&&id<ICON.length?ICON[id]:-1;
            if(icon>=0)g.sprite(ICONS,icon%8*18,198+icon/8*18,18,18,x+12,top+2,18,18);
            g.text(name,x+40,top+12,12,UiTheme.TEXT);
            int seconds=(effect.duration()+19)/20;
            String duration=seconds/60+":"+(seconds%60<10?"0":"")+seconds%60;
            g.text(duration,x+40,top+25,10,seconds<=10?0xffffbc70:UiTheme.MUTED);
        }
    }
}
