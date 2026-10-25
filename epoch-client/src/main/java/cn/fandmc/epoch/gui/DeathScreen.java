package cn.fandmc.epoch.gui;

import cn.fandmc.epoch.core.GameState.LifeState;
import java.util.List;
import java.util.function.Supplier;

public final class DeathScreen extends ButtonScreen {
    private final Supplier<LifeState> state;
    private final Runnable respawn,disconnect;
    private int ticks;
    public DeathScreen(Supplier<LifeState> state,Runnable respawn,Runnable disconnect) {
        super("你死了！");this.state=state;this.respawn=respawn;this.disconnect=disconnect;
    }
    @Override public boolean closesOnEscape() { return false; }
    @Override public boolean showsHud() { return false; }
    @Override protected void onOpen() { ticks=0; }
    @Override public void tick() { if(ticks<20)ticks++; }
    public boolean canRespawn() { return ticks>=20&&state.get()==LifeState.DEAD; }
    @Override protected List<Button> buttons() {
        float x=width()/2f-160,y=height()/2f;
        return List.of(new Button(x,y,320,46,state.get()==LifeState.DEAD?"重生":"正在重生…",canRespawn(),respawn),
                new Button(x,y+60,320,46,"返回主菜单",true,disconnect));
    }
    @Override public boolean keyPressed(KeyInput input) {
        if(!input.repeat()&&(input.key()==KeyInput.ENTER||input.key()==KeyInput.KEYPAD_ENTER)&&canRespawn()){respawn.run();return true;}
        return false;
    }
    @Override public void render(GuiGraphics g,double mouseX,double mouseY,float tickDelta) {
        float x=width()/2f-160,y=height()/2f;
        g.fill(0,0,width(),height(),0x99571016);g.panel(x-30,y-142,380,278,12);
        g.centeredText(title(),width()/2f,y-78,42,0xffffeeee);
        String message=switch(state.get()) {
            case RESPAWN_REQUESTED -> "正在等待服务器确认重生…";
            case RESPAWNING -> "正在接收重生位置…";
            default -> "";
        };
        g.centeredText(message,width()/2f,y-32,17,UiTheme.MUTED);renderButtons(g,mouseX,mouseY);
    }
}
