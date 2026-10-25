package cn.fandmc.epoch.gui;

import java.util.List;
import java.util.function.Supplier;

public final class MainMenuScreen extends ButtonScreen {
    private final String server;
    private final Supplier<String> status,notice;
    private final Runnable connect,refresh,quit;
    public MainMenuScreen(String server,Supplier<String> status,Supplier<String> notice,
                          Runnable connect,Runnable refresh,Runnable quit) {
        super("Epoch");this.server=server;this.status=status;this.notice=notice;
        this.connect=connect;this.refresh=refresh;this.quit=quit;
    }
    @Override public boolean closesOnEscape() { return false; }
    @Override public boolean showsHud() { return false; }
    @Override protected List<Button> buttons() {
        float x=width()/2f-160,y=height()/2f;
        return List.of(new Button(x,y,320,48,"连接服务器",true,connect),
                new Button(x,y+62,150,40,"刷新状态",true,refresh),
                new Button(x+170,y+62,150,40,"退出",true,quit));
    }
    @Override public boolean keyPressed(KeyInput input) {
        if(input.repeat())return false;
        if(input.key()==KeyInput.ENTER||input.key()==KeyInput.KEYPAD_ENTER){connect.run();return true;}
        if(input.key()==KeyInput.ESCAPE){quit.run();return true;}
        return false;
    }
    @Override public void render(GuiGraphics g,double mouseX,double mouseY,float tickDelta) {
        float x=width()/2f-160,y=height()/2f;
        g.fill(0,0,width(),height(),0xff181a20);g.fill(0,height()-3,width(),3,UiTheme.ACCENT);
        g.panel(x-32,y-162,384,296,12);
        g.text("EPOCH",x-2,y-112,58,UiTheme.TEXT);
        g.text("Minecraft Java 1.8.9",x,y-70,19,UiTheme.MUTED);
        g.text(server,x,y-30,16,UiTheme.MUTED);renderButtons(g,mouseX,mouseY);
        g.text(status.get(),30,height()-80,15,UiTheme.MUTED);g.text(notice.get(),30,height()-50,15,UiTheme.TEXT);
    }
}
