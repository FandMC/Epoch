package cn.fandmc.epoch.gui;

import java.util.Objects;

/** A shared button: one rectangle is used for both drawing and hit testing. */
public final class Button {
    private final float x,y,width,height;
    private final String label;
    private final boolean enabled;
    private final Runnable action;
    public Button(float x,float y,float width,float height,String label,boolean enabled,Runnable action) {
        this.x=x;this.y=y;this.width=width;this.height=height;
        this.label=Objects.requireNonNull(label);this.enabled=enabled;this.action=Objects.requireNonNull(action);
    }
    public boolean contains(double x,double y) { return x>=this.x&&x<this.x+width&&y>=this.y&&y<this.y+height; }
    public boolean click(double x,double y,int button) {
        if(button!=0||!enabled||!contains(x,y))return false;
        action.run();return true;
    }
    public void render(GuiGraphics graphics,double mouseX,double mouseY) {
        graphics.roundedRect(x,y,width,height,UiTheme.RADIUS,!enabled?0xff292a30:contains(mouseX,mouseY)?0xff43536b:0xff333842);
        graphics.centeredText(label,x+width/2,y+height/2+6,18,enabled?UiTheme.TEXT:UiTheme.MUTED);
    }
}
