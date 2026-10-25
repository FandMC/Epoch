package cn.fandmc.epoch.gui;

import cn.fandmc.epoch.asset.*;
import cn.fandmc.epoch.item.ItemStack;
import cn.fandmc.epoch.network.mapping.LegacyItemMapping;
import cn.fandmc.epoch.render.ItemIcons;
import io.github.humbleui.skija.*;
import java.io.IOException;
import java.util.*;

/** Render-thread resource owner shared by every screen and HUD element. */
public final class GuiResources implements AutoCloseable {
    private final AssetDirectory assets;
    private final Map<ResourceId,Image> images=new HashMap<>();
    private final Set<ResourceId> missing=new HashSet<>();
    private final ItemIcons icons;
    private final Language language;
    private LegacyItemMapping mapping;
    public GuiResources(AssetDirectory assets)throws IOException {
        this.assets=assets;language=new Language(assets);icons=new ItemIcons(assets);
    }
    public void mapping(LegacyItemMapping mapping){this.mapping=mapping;}
    public String text(String key){return language.text(key);}
    public String itemName(ItemStack stack){
        if(stack.isEmpty())return "";
        String custom=stack.tag().compound("display").string("Name","");if(!custom.isEmpty())return custom;
        var visual=mapping==null?null:mapping.visual(stack);
        return visual==null?stack.item().id().toString():language.text(visual.translation());
    }
    public Image item(ItemStack stack){return mapping==null||stack.isEmpty()?null:icons.image(stack,mapping);}
    public void prepare(Collection<ItemStack> stacks,DirectContext context){if(mapping!=null)icons.prepare(stacks,mapping,context);}
    public Image texture(ResourceId resource){
        if(missing.contains(resource))return null;
        return images.computeIfAbsent(resource,id->{
            try{return Image.makeFromEncoded(assets.read(id.assetPath("textures",".png")));}
            catch(IOException|RuntimeException e){missing.add(id);System.getLogger(GuiResources.class.getName()).log(System.Logger.Level.WARNING,"Missing UI texture: "+id);return null;}
        });
    }
    public int iconCount(){return icons.cachedCount();}
    public Set<ResourceId> unresolved(){return icons.unresolved();}
    @Override public void close(){images.values().forEach(Image::close);images.clear();icons.close();}
}
