package cn.fandmc.epoch.gui;

/** One independently laid-out in-game overlay; has no window or module-system dependency. */
public abstract class HudElement {
    public abstract void render(GuiGraphics graphics,int width,int height,float tickDelta);
    protected final void panel(GuiGraphics graphics,float x,float y,float width,float height,String title) {
        graphics.panel(x,y,width,height,UiTheme.RADIUS);
        graphics.roundedRect(x+10,y+11,3,12,1.5f,UiTheme.ACCENT);
        graphics.text(title,x+21,y+23,14,UiTheme.TEXT);
    }
}
