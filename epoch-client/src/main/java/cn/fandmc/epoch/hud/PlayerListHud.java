package cn.fandmc.epoch.hud;

import cn.fandmc.epoch.gui.*;
import java.util.*;

public final class PlayerListHud extends HudElement {
    private final PlayerListState state;
    private final ScoreboardState scoreboard;
    private long revision=-1,scoreRevision=-1;
    private List<PlayerListState.Player> players=List.of();
    public PlayerListHud(PlayerListState state,ScoreboardState scoreboard){this.state=state;this.scoreboard=scoreboard;}
    @Override public void render(GuiGraphics g,int width,int height,float delta){
        if(revision!=state.revision()||scoreRevision!=scoreboard.revision()){
            revision=state.revision();scoreRevision=scoreboard.revision();players=state.sorted(scoreboard);
        }
        if(players.isEmpty())return;
        int columns=(players.size()+19)/20,rows=(players.size()+columns-1)/columns;
        float cell=Math.min(240,(width-64f)/columns),w=cell*columns+24;
        String[] header=state.header().isBlank()?new String[0]:state.header().split("\\R",-1);
        String[] footer=state.footer().isBlank()?new String[0]:state.footer().split("\\R",-1);
        int headerLines=Math.min(header.length,5),footerLines=Math.min(footer.length,5);
        float x=(width-w)/2,y=18,top=y+38+headerLines*18,h=48+headerLines*18+rows*25+footerLines*18;
        panel(g,x,y,w,h,"在线玩家  "+state.size());
        g.save();g.clip(x+8,y+30,w-16,h-32);
        for(int i=0;i<headerLines;i++)g.centeredText(header[i],width/2f,y+47+i*18,13,UiTheme.TEXT);
        for(int i=0;i<players.size();i++){
            var p=players.get(i);int col=i/rows,row=i%rows;float px=x+12+col*cell,py=top+row*25;
            g.roundedRect(px,py,cell-4,23,3,UiTheme.SLOT);
            String name=p.displayName()!=null?p.displayName():scoreboard.decorate(p.name());
            g.save();g.clip(px+6,py,cell-56,23);g.text(name,px+6,py+16,12,p.gameMode()==3?UiTheme.MUTED:UiTheme.TEXT);g.restore();
            Integer score=scoreboard.listScore(p.name());if(score!=null)g.text(Integer.toString(score),px+cell-52,py+16,11,UiTheme.ACCENT);
            int bars=p.ping()<0?0:p.ping()<150?5:p.ping()<300?4:p.ping()<600?3:p.ping()<1000?2:1;
            for(int b=0;b<5;b++)g.fill(px+cell-24+b*3,py+17-(b+1)*2,2,(b+1)*2,b<bars?0xff9ac7ac:0xff484b55);
        }
        for(int i=0;i<footerLines;i++)g.centeredText(footer[i],width/2f,top+rows*25+16+i*18,12,UiTheme.MUTED);
        g.restore();
    }
}
