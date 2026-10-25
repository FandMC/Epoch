package cn.fandmc.epoch.gui;

import java.util.Objects;

/** A game-thread page. GPU objects and native window handles stay in the client. */
public abstract class Screen {
    private final String title;
    private ScreenHost host;
    private int width,height;

    protected Screen(String title) { this.title=Objects.requireNonNull(title); }
    public final String title() { return title; }
    public final int width() { return width; }
    public final int height() { return height; }
    protected final ScreenHost host() {
        if(host==null)throw new IllegalStateException("Screen is not open");
        return host;
    }
    protected final void close() { host().close(this); }
    public boolean closesOnEscape() { return true; }
    public boolean showsHud() { return true; }
    public abstract void render(GuiGraphics graphics,double mouseX,double mouseY,float tickDelta);
    public void tick() {}
    protected void onOpen() {}
    protected void onClose() {}
    protected void onResize(int width,int height) {}
    public void focusChanged(boolean focused) {}
    public boolean keyPressed(KeyInput input) { return false; }
    public boolean keyReleased(KeyInput input) { return false; }
    public boolean charTyped(int codePoint) { return false; }
    public boolean mousePressed(double x,double y,int button,int modifiers) { return false; }
    public boolean mouseReleased(double x,double y,int button,int modifiers) { return false; }
    public void mouseMoved(double x,double y) {}
    public boolean mouseScrolled(double x,double y,double horizontal,double vertical) { return false; }

    final void attach(ScreenHost host,int width,int height) {
        if(this.host!=null)throw new IllegalStateException("Screen is already open");
        this.host=Objects.requireNonNull(host);this.width=width;this.height=height;
        try { onOpen();onResize(width,height); }
        catch(RuntimeException|Error error) {
            try { onClose(); }catch(RuntimeException|Error cleanup) { error.addSuppressed(cleanup); }
            finally { this.host=null; }
            throw error;
        }
    }
    final void resize(int width,int height) {
        this.width=width;this.height=height;onResize(width,height);
    }
    final void detach() { try { onClose(); }finally { host=null; } }
}
