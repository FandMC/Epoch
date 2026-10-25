package cn.fandmc.epoch.gui;

import java.util.List;
import java.util.function.Supplier;

public final class ConnectionScreen extends ButtonScreen {
    private final Supplier<String> message;
    private final Runnable cancel;
    public ConnectionScreen(Supplier<String> message,Runnable cancel) {
        super("连接服务器");this.message=message;this.cancel=cancel;
    }
    @Override public boolean closesOnEscape() { return false; }
    @Override public boolean showsHud() { return false; }
    @Override protected List<Button> buttons() {
        return List.of(new Button(width()/2f-160,height()/2f+30,320,44,"取消连接",true,cancel));
    }
    @Override public boolean keyPressed(KeyInput input) {
        if(input.key()==KeyInput.ESCAPE&&!input.repeat()){cancel.run();return true;}return false;
    }
    @Override public void render(GuiGraphics g,double mouseX,double mouseY,float tickDelta) {
        g.fill(0,0,width(),height(),0xff181a20);
        g.panel(width()/2f-200,height()/2f-88,400,190,10);
        g.centeredText(message.get(),width()/2f,height()/2f-20,22,UiTheme.TEXT);
        renderButtons(g,mouseX,mouseY);
    }
}
