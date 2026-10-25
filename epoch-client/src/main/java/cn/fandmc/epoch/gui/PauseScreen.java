package cn.fandmc.epoch.gui;

import java.util.List;

/** Multiplayer keeps ticking while this menu owns input. */
public final class PauseScreen extends ButtonScreen {
    private final Runnable disconnect;
    public PauseScreen(Runnable disconnect) { super("游戏菜单");this.disconnect=disconnect; }
    @Override protected List<Button> buttons() {
        float x=width()/2f-160,y=height()/2f;
        return List.of(new Button(x,y,320,46,"返回游戏",true,this::close),
                new Button(x,y+60,320,46,"断开连接",true,disconnect));
    }
    @Override public void render(GuiGraphics g,double mouseX,double mouseY,float tickDelta) {
        g.fill(0,0,width(),height(),0x680c0d12);
        g.panel(width()/2f-188,height()/2f-100,376,232,10);
        g.centeredText(title(),width()/2f,height()/2f-44,32,UiTheme.TEXT);
        renderButtons(g,mouseX,mouseY);
    }
}
