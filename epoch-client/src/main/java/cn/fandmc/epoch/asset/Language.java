package cn.fandmc.epoch.asset;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** UTF-8 legacy language assets, with English fallback. */
public final class Language {
    private final Map<String,String> entries=new HashMap<>();
    public Language(AssetDirectory assets)throws IOException {load(assets,"en_US");load(assets,"zh_CN");}
    private void load(AssetDirectory assets,String locale)throws IOException {
        var bytes=assets.find("minecraft/lang/"+locale+".lang");if(bytes.isEmpty())return;
        for(String line:new String(bytes.get(),StandardCharsets.UTF_8).split("\\R")){
            int equal=line.indexOf('=');if(equal>0&&!line.startsWith("#"))entries.put(line.substring(0,equal),line.substring(equal+1));
        }
    }
    public String text(String key){return entries.getOrDefault(key,key);}
}
