package cn.fandmc.epoch.hud;

import cn.fandmc.epoch.core.GameState;
import cn.fandmc.epoch.gui.*;

/** Owns presentation components; game and network state stay outside the renderer. */
public final class HudRenderer {
    private final GameState game;
    private final ScoreboardHud scoreboard;
    private final EffectsHud effects;
    private final PlayerListHud playerList;
    private final HotbarHud hotbar;
    public HudRenderer(GameState game,GuiResources resources){
        this.game=game;scoreboard=new ScoreboardHud(game.scoreboard,()->game.username);
        effects=new EffectsHud(game.effects,resources);playerList=new PlayerListHud(game.playerList,game.scoreboard);hotbar=new HotbarHud(game,resources);
    }
    public void render(GuiGraphics g,int width,int height,float delta,boolean tab,boolean crosshair,boolean debug,String performance,String position){
        g.panel(16,16,184,42,UiTheme.RADIUS);g.text("EPOCH",28,43,17,UiTheme.TEXT);
        g.text(game.username,99,42,11,UiTheme.MUTED);
        scoreboard.render(g,width,height,delta);effects.render(g,width,height,delta);hotbar.render(g,width,height,delta);
        if(crosshair){g.fill(width/2f-5,height/2f,11,1,0xddeeeeee);g.fill(width/2f,height/2f-5,1,11,0xddeeeeee);}
        int skip=Math.max(0,game.chat().size()-6),i=0,row=0;float chatWidth=Math.min(510,width*.43f);
        for(String line:game.chat()){
            if(i++<skip)continue;float y=height-116-(Math.min(6,game.chat().size())-1-row++)*21;
            g.roundedRect(16,y-16,chatWidth,21,3,0x80222326);g.save();g.clip(22,y-17,chatWidth-12,22);g.text(line,23,y,12,UiTheme.TEXT);g.restore();
        }
        if(!game.actionBar().isEmpty()){
            int alpha=(int)(255*Math.clamp((game.actionBarTicks()-delta)/20f,0,1));
            g.centeredText(game.actionBar(),width/2f,height-112,15,(alpha<<24)|0xffffff);
        }
        if(debug){g.panel(16,60,Math.min(width-32,860),60,UiTheme.RADIUS);g.text(performance,28,83,13,UiTheme.TEXT);g.text(position,28,105,12,UiTheme.MUTED);}
        if(tab)playerList.render(g,width,height,delta);
    }
}
