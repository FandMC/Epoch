package cn.fandmc.epoch.skia;

import cn.fandmc.epoch.font.FontBook;
import cn.fandmc.epoch.gui.GuiGraphics;
import cn.fandmc.epoch.gui.GuiResources;
import cn.fandmc.epoch.gui.UiTheme;
import cn.fandmc.epoch.item.*;
import cn.fandmc.epoch.asset.ResourceId;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.Rect;
import io.github.humbleui.types.RRect;

/** Adapter borrowed for one frame; native resources remain owned by the renderer. */
public final class SkiaGuiGraphics implements GuiGraphics {
    private final Canvas canvas;
    private final FontBook fonts;
    private final Paint paint;
    private final HudBackdrop backdrop;
    private final GuiResources resources;
    public SkiaGuiGraphics(Canvas canvas,FontBook fonts,Paint paint) {
        this(canvas,fonts,paint,null,null);
    }
    public SkiaGuiGraphics(Canvas canvas,FontBook fonts,Paint paint,HudBackdrop backdrop,GuiResources resources) {
        this.canvas=canvas;this.fonts=fonts;this.paint=paint;this.backdrop=backdrop;this.resources=resources;
    }
    @Override public void fill(float x,float y,float width,float height,int color) {
        paint.setColor(color);canvas.drawRect(Rect.makeXYWH(x,y,width,height),paint);
    }
    @Override public void roundedRect(float x,float y,float width,float height,float radius,int color) {
        paint.setColor(color);canvas.drawRRect(RRect.makeXYWH(x,y,width,height,radius),paint);
    }
    @Override public void text(String text,float x,float y,int size,int color) {
        paint.setColor(color);fonts.draw(canvas,text,x,y,size,paint);
    }
    @Override public float textWidth(String text,int size) { return fonts.width(text,size); }
    @Override public void save() { canvas.save(); }
    @Override public void scale(float x,float y) { canvas.scale(x,y); }
    @Override public void clip(float x,float y,float width,float height) { canvas.clipRect(Rect.makeXYWH(x,y,width,height)); }
    @Override public void restore() { canvas.restore(); }
    @Override public void panel(float x,float y,float width,float height,float radius) {
        roundedRect(x-1,y+2,width+2,height+2,radius+1,0x28000000);
        if(backdrop!=null)backdrop.draw(canvas,x,y,width,height,radius);
        roundedRect(x,y,width,height,radius,UiTheme.PANEL);
        fill(x+radius,y,width-2*radius,1,UiTheme.OUTLINE);
    }
    @Override public void sprite(ResourceId texture,float sx,float sy,float sw,float sh,float x,float y,float width,float height) {
        if(resources==null)return;var image=resources.texture(texture);if(image==null)return;
        canvas.drawImageRect(image,Rect.makeXYWH(sx,sy,sw,sh),Rect.makeXYWH(x,y,width,height),SamplingMode.DEFAULT,null,true);
    }
    @Override public void item(ItemStack stack,float x,float y,float size) {
        if(stack.isEmpty())return;var image=resources==null?null:resources.item(stack);
        if(image!=null)canvas.drawImageRect(image,Rect.makeWH(image.getWidth(),image.getHeight()),Rect.makeXYWH(x,y,size,size),SamplingMode.LINEAR,null,true);
        else text("?",x+size/3,y+size*.75f,14,UiTheme.MUTED);
        if(stack.item() instanceof DamageableItem durable&&stack.damage()>0){
            float ratio=Math.clamp(1-stack.damage()/(float)durable.maximumDamage(),0,1);
            fill(x+2,y+size-3,size-4,3,0xee101014);
            int color=0xff000000|((int)(255*(1-ratio))<<16)|((int)(255*ratio)<<8);
            fill(x+2,y+size-3,(size-4)*ratio,2,color);
        }
        if(stack.count()>1){String count=Integer.toString(stack.count());float tx=x+size-textWidth(count,11);
            text(count,tx+1,y+size+1,11,0xdd000000);text(count,tx,y+size,11,UiTheme.TEXT);}
    }
}
