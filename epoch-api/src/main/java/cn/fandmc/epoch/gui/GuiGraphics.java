package cn.fandmc.epoch.gui;

/** Frame-scoped drawing in logical window pixels. Colors are packed ARGB. */
public interface GuiGraphics {
    void fill(float x,float y,float width,float height,int color);
    void roundedRect(float x,float y,float width,float height,float radius,int color);
    /** Text y is the baseline; size is measured in logical pixels. */
    void text(String text,float x,float y,int size,int color);
    float textWidth(String text,int size);
    void save();
    void scale(float x,float y);
    void clip(float x,float y,float width,float height);
    void restore();

    /** Draw an actual resource-backed item, including its count and durability. */
    void item(cn.fandmc.epoch.item.ItemStack stack,float x,float y,float size);
    /** Source rectangle is measured in original texture pixels. */
    void sprite(cn.fandmc.epoch.asset.ResourceId texture,float sx,float sy,float sw,float sh,
                float x,float y,float width,float height);

    default void panel(float x,float y,float width,float height,float radius) {
        roundedRect(x,y,width,height,radius,UiTheme.PANEL);
    }

    default void centeredText(String text,float centerX,float y,int size,int color) {
        text(text,centerX-textWidth(text,size)/2,y,size,color);
    }
}
