package cn.fandmc.epoch.gui;

import java.util.function.Consumer;

public final class ChatScreen extends Screen {
    private final Consumer<String> send;
    private String input="";
    public ChatScreen(Consumer<String> send) { super("聊天");this.send=send; }
    @Override public boolean charTyped(int codePoint) {
        if(Character.isValidCodePoint(codePoint)&&!Character.isISOControl(codePoint)
                &&input.length()+Character.charCount(codePoint)<=100)input+=new String(Character.toChars(codePoint));
        return true;
    }
    @Override public boolean keyPressed(KeyInput key) {
        if(key.key()==KeyInput.BACKSPACE&&!input.isEmpty()){input=input.substring(0,input.offsetByCodePoints(input.length(),-1));return true;}
        if((key.key()==KeyInput.ENTER||key.key()==KeyInput.KEYPAD_ENTER)&&!key.repeat()){
            ScreenHost navigation=host();
            if(!input.isBlank())send.accept(input);
            // Sending may replace the page on failure; close ignores stale requesters.
            navigation.close(this);return true;
        }
        return false;
    }
    @Override public void render(GuiGraphics g,double mouseX,double mouseY,float tickDelta) {
        g.panel(12,height()-40,width()-24,28,UiTheme.RADIUS);g.save();
        try{g.clip(18,height()-40,width()-36,28);g.text("> "+input,20,height()-20,16,0xffffffff);}
        finally{g.restore();}
    }
}
