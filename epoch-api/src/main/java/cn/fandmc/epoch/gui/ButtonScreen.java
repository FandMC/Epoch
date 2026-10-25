package cn.fandmc.epoch.gui;

import java.util.List;

/** Base for pages whose interactive controls are buttons. */
public abstract class ButtonScreen extends Screen {
    protected ButtonScreen(String title) { super(title); }
    protected abstract List<Button> buttons();
    protected final void renderButtons(GuiGraphics graphics,double x,double y) {
        for(Button button:buttons())button.render(graphics,x,y);
    }
    @Override public boolean mousePressed(double x,double y,int button,int modifiers) {
        for(Button control:buttons())if(control.click(x,y,button))return true;
        return false;
    }
}
