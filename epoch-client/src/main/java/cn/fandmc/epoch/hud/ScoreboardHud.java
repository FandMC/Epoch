package cn.fandmc.epoch.hud;

import cn.fandmc.epoch.gui.*;
import java.util.*;

/** Sidebar snapshot rebuilt only when server scoreboard data changes. */
public final class ScoreboardHud extends HudElement {
    private final ScoreboardState state;
    private final java.util.function.Supplier<String> username;
    private long revision=-1;
    private ScoreboardState.Objective objective;
    private List<ScoreboardState.Row> rows=List.of();
    private float panelWidth;
    public ScoreboardHud(ScoreboardState state,java.util.function.Supplier<String> username){this.state=state;this.username=username;}
    @Override public void render(GuiGraphics g,int width,int height,float delta){
        if(revision!=state.revision()){
            revision=state.revision();objective=state.sidebar(username.get());rows=state.rows(objective);
            panelWidth=objective==null?0:g.textWidth(objective.title(),14)+28;
            for(var row:rows)panelWidth=Math.max(panelWidth,g.textWidth(row.label(),13)+g.textWidth(Integer.toString(row.score()),12)+42);
            panelWidth=Math.min(Math.max(180,panelWidth),Math.max(180,width*.45f));
        }
        if(objective==null||rows.isEmpty())return;
        float h=36+rows.size()*21,x=width-panelWidth-16,y=Math.max(74,(height-h)/2f);
        panel(g,x,y,panelWidth,h,objective.title());
        g.save();g.clip(x+8,y+30,panelWidth-16,h-30);
        int i=0;for(var row:rows){float baseline=y+47+i++*21;String score=Integer.toString(row.score());
            g.text(row.label(),x+12,baseline,13,UiTheme.TEXT);
            g.text(score,x+panelWidth-12-g.textWidth(score,12),baseline,12,UiTheme.ACCENT);
        }g.restore();
    }
}
