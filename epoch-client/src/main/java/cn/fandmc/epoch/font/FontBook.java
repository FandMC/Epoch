package cn.fandmc.epoch.font;

import io.github.humbleui.skija.*;
import java.util.*;

/** Bounded glyph-run cache; unchanged HUD labels reuse native TextBlobs. */
public final class FontBook implements AutoCloseable {
    private record FontKey(String family,int size,boolean bold,boolean italic) { }
    private record TextKey(String text,int size) { }
    private record Run(TextBlob blob,float x,float width,int color,boolean underline,boolean strike) { }
    private record Layout(List<Run> runs,float width) implements AutoCloseable {
        public void close(){runs.forEach(r->{if(r.blob()!=null)r.blob().close();});}
    }
    private static final int[] COLORS={0x000000,0x0000AA,0x00AA00,0x00AAAA,0xAA0000,0xAA00AA,0xFFAA00,0xAAAAAA,0x555555,0x5555FF,0x55FF55,0x55FFFF,0xFF5555,0xFF55FF,0xFFFF55,0xFFFFFF};
    private final Map<FontKey,Font> fonts=new HashMap<>();
    private final Map<String,Typeface> faces=new HashMap<>();
    private final Map<Integer,String> fallback=new HashMap<>();
    private final LinkedHashMap<TextKey,Layout> layouts=new LinkedHashMap<>(128,.75f,true);
    public Font font(String family,int size){return font(family,size,false,false);}
    private Font font(String family,int size,boolean bold,boolean italic){
        return fonts.computeIfAbsent(new FontKey(family,size,bold,italic),key->{
            String id=family+":"+bold+":"+italic;
            Typeface face=faces.computeIfAbsent(id,k->FontMgr.getDefault().matchFamilyStyle(family,bold?(italic?FontStyle.BOLD_ITALIC:FontStyle.BOLD):(italic?FontStyle.ITALIC:FontStyle.NORMAL)));
            if(face==null)throw new IllegalStateException("Font unavailable: "+family);
            return new Font(face,size).setSubpixel(true).setEdging(FontEdging.ANTI_ALIAS);
        });
    }
    private Layout layout(String value,int size){
        if(value.length()>8192)value=value.substring(0,8192);
        TextKey key=new TextKey(value,size);Layout existing=layouts.get(key);if(existing!=null)return existing;
        List<Run> runs=new ArrayList<>();StringBuilder text=new StringBuilder();Font current=null;
        boolean bold=false,italic=false,underline=false,strike=false;int color=-1;float x=0;
        for(int i=0;i<value.length();){
            int cp=value.codePointAt(i);i+=Character.charCount(cp);
            if(cp==0xA7&&i<value.length()){
                if(!text.isEmpty()){x=append(runs,text.toString(),current,x,color,underline,strike);text.setLength(0);}
                char code=Character.toLowerCase(value.charAt(i++));int rgb=Character.digit(code,16);
                if(rgb>=0){color=COLORS[rgb];bold=italic=underline=strike=false;}
                else switch(code){case 'l'->bold=true;case 'o'->italic=true;case 'n'->underline=true;case 'm'->strike=true;case 'r'->{color=-1;bold=italic=underline=strike=false;}default->{}}
                current=null;continue;
            }
            if(cp==0xFE0E||cp==0xFE0F)continue;
            String family=fallback.computeIfAbsent(cp,c->{if(font("Segoe UI",size).getUTF32Glyph(c)!=0)return "Segoe UI";try(Typeface face=FontMgr.getDefault().matchFamilyStyleCharacter(null,FontStyle.NORMAL,null,c)){return face==null?"Segoe UI":face.getFamilyName();}});
            Font selected=font(family,size,bold,italic);
            if(current!=selected&&!text.isEmpty()){x=append(runs,text.toString(),current,x,color,underline,strike);text.setLength(0);}
            current=selected;text.appendCodePoint(cp);
        }
        if(!text.isEmpty())x=append(runs,text.toString(),current,x,color,underline,strike);
        Layout result=new Layout(List.copyOf(runs),x);layouts.put(key,result);
        while(layouts.size()>1024){var iterator=layouts.entrySet().iterator();iterator.next().getValue().close();iterator.remove();}
        return result;
    }
    private static float append(List<Run> runs,String text,Font font,float x,int color,boolean underline,boolean strike){
        short[] glyphs=font.getStringGlyphs(text);float[] widths=font.getWidths(glyphs),positions=new float[glyphs.length];float width=0;
        for(int i=0;i<glyphs.length;i++){positions[i]=width;width+=widths[i];}
        runs.add(new Run(TextBlob.makeFromPosH(glyphs,positions,0,font),x,width,color,underline,strike));return x+width;
    }
    public void draw(Canvas canvas,String text,float x,float y,int size,Paint paint){
        int original=paint.getColor();try{for(Run run:layout(text,size).runs()){
            paint.setColor(run.color()<0?original:(original&0xff000000)|run.color());
            if(run.blob()!=null)canvas.drawTextBlob(run.blob(),x+run.x(),y,paint);
            if(run.underline())canvas.drawRect(io.github.humbleui.types.Rect.makeXYWH(x+run.x(),y+1,run.width(),1),paint);
            if(run.strike())canvas.drawRect(io.github.humbleui.types.Rect.makeXYWH(x+run.x(),y-size*.3f,run.width(),1),paint);
        }}finally{paint.setColor(original);}
    }
    public float width(String text,int size){return layout(text,size).width();}
    public int cachedLayouts(){return layouts.size();}
    @Override public void close(){layouts.values().forEach(Layout::close);layouts.clear();fonts.values().forEach(Font::close);faces.values().forEach(Typeface::close);fonts.clear();faces.clear();fallback.clear();}
}