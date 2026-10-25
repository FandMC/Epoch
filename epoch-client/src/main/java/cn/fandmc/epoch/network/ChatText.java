package cn.fandmc.epoch.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class ChatText {
    private ChatText() {}

    public static String formatted(String json) {
        try{return formatted(JsonParser.parseString(json),"",0);}catch(RuntimeException e){return json;}
    }
    private static String formatted(JsonElement value,String inherited,int depth){
        if(depth>32||value==null||value.isJsonNull())return "";
        if(value.isJsonPrimitive())return inherited+value.getAsString();
        if(value.isJsonArray()){StringBuilder out=new StringBuilder();for(var child:value.getAsJsonArray())out.append(formatted(child,inherited,depth+1));return out.toString();}
        var object=value.getAsJsonObject();String style=inherited;
        if(object.has("color")){String[] names={"black","dark_blue","dark_green","dark_aqua","dark_red","dark_purple","gold","gray","dark_gray","blue","green","aqua","red","light_purple","yellow","white"};String color=object.get("color").getAsString();for(int i=0;i<16;i++)if(names[i].equals(color))style="\u00a7"+Integer.toHexString(i);}
        String[] flags={"obfuscated","bold","strikethrough","underlined","italic"};for(int i=0;i<flags.length;i++)if(object.has(flags[i])&&object.get(flags[i]).getAsBoolean())style+="\u00a7"+"klmno".charAt(i);
        StringBuilder out=new StringBuilder("\u00a7r").append(style);
        if(object.has("text"))out.append(object.get("text").getAsString());
        if(object.has("translate")){out.append(object.get("translate").getAsString());if(object.has("with"))out.append(' ').append(formatted(object.get("with"),style,depth+1));}
        if(object.has("extra"))out.append(formatted(object.get("extra"),style,depth+1));return out.toString();
    }

    public static String plain(String json) {
        try { return plain(JsonParser.parseString(json), 0).replaceAll("\u00a7[0-9A-FK-ORa-fk-or]", ""); }
        catch (RuntimeException e) { return json; }
    }

    private static String plain(JsonElement value, int depth) {
        if (depth > 32 || value == null || value.isJsonNull()) return "";
        if (value.isJsonPrimitive()) return value.getAsString();
        StringBuilder text = new StringBuilder();
        if (value.isJsonArray()) {
            for (JsonElement element : value.getAsJsonArray()) text.append(plain(element, depth + 1));
        } else {
            JsonObject object = value.getAsJsonObject();
            if (object.has("text")) text.append(plain(object.get("text"), depth + 1));
            if (object.has("translate")) {
                text.append(plain(object.get("translate"), depth + 1));
                if (object.has("with")) text.append(" ").append(plain(object.get("with"), depth + 1));
            }
            if (object.has("extra")) text.append(plain(object.get("extra"), depth + 1));
        }
        return text.toString();
    }
}
