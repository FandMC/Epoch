package cn.fandmc.epoch.gui;

import java.util.Objects;
import java.util.function.Consumer;

/** Single active page; serializes lifecycle changes and routes input on its owner thread. */
public final class ScreenManager implements ScreenHost {
    private final Thread owner=Thread.currentThread();
    private final Consumer<Screen> changed;
    private Screen current,pending;
    private boolean switching,hasPending;
    private int width,height;

    public ScreenManager(Consumer<Screen> changed) { this.changed=Objects.requireNonNull(changed); }
    @Override public Screen current() { checkThread();return current; }
    @Override public void show(Screen screen) {
        checkThread();
        pending=screen;hasPending=true;
        if(switching)return;
        switching=true;
        try {
            while(hasPending) {
                Screen next=pending;pending=null;hasPending=false;
                if(current==next)continue;
                Screen previous=current;current=null;
                if(previous!=null)previous.detach();
                if(next!=null) {
                    current=next;
                    try { next.attach(this,width,height); }
                    catch(RuntimeException|Error error) { current=null;changed.accept(null);throw error; }
                }
                changed.accept(current);
            }
        }finally { switching=false;pending=null;hasPending=false; }
    }
    @Override public void close(Screen requester) {
        checkThread();if(requester!=null&&current==requester)show(null);
    }
    public void resize(int width,int height) {
        checkThread();
        if(width<0||height<0)throw new IllegalArgumentException("Negative viewport size");
        this.width=width;this.height=height;
        if(current!=null)current.resize(width,height);
    }
    public void tick() { checkThread();if(current!=null)current.tick(); }
    public void render(GuiGraphics graphics,double x,double y,float tickDelta) {
        checkThread();if(current!=null)current.render(graphics,x,y,tickDelta);
    }
    public void focusChanged(boolean focused) { checkThread();if(current!=null)current.focusChanged(focused); }
    public boolean keyPressed(KeyInput input) {
        checkThread();Screen target=current;if(target==null)return false;
        boolean handled=target.keyPressed(input);
        if(!handled&&current==target&&!input.repeat()&&input.key()==KeyInput.ESCAPE&&target.closesOnEscape())close(target);
        return true; // A page owns the event even if it closes itself while handling it.
    }
    public boolean keyReleased(KeyInput input) {
        checkThread();if(current==null)return false;current.keyReleased(input);return true;
    }
    public boolean charTyped(int codePoint) {
        checkThread();if(current==null)return false;current.charTyped(codePoint);return true;
    }
    public boolean mousePressed(double x,double y,int button,int modifiers) {
        checkThread();if(current==null)return false;current.mousePressed(x,y,button,modifiers);return true;
    }
    public boolean mouseReleased(double x,double y,int button,int modifiers) {
        checkThread();if(current==null)return false;current.mouseReleased(x,y,button,modifiers);return true;
    }
    public void mouseMoved(double x,double y) { checkThread();if(current!=null)current.mouseMoved(x,y); }
    public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        checkThread();if(current==null)return false;current.mouseScrolled(x,y,horizontal,vertical);return true;
    }
    private void checkThread() {
        if(Thread.currentThread()!=owner)throw new IllegalStateException("Screen access must stay on the game thread");
    }
}
