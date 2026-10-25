package cn.fandmc.epoch.asset;

import com.google.gson.*;
import java.io.IOException;
import java.util.*;

public record TextureAnimation(List<Frame> frames, boolean interpolate) {
    public record Frame(int index, int ticks) {}
    public TextureAnimation { frames = List.copyOf(frames); }
    public static TextureAnimation parse(byte[] data, int frameCount, String source) throws IOException {
        try {
            JsonObject animation = JsonResources.object(data,source).getAsJsonObject("animation");
            if (animation == null) return new TextureAnimation(List.of(new Frame(0,1)),false);
            int time=animation.has("frametime")?animation.get("frametime").getAsInt():1;
            if(time<1)throw new IOException("Invalid frame time: "+source);
            List<Frame> frames=new ArrayList<>();
            if(animation.has("frames"))for(JsonElement element:animation.getAsJsonArray("frames")){
                int index=element.isJsonPrimitive()?element.getAsInt():element.getAsJsonObject().get("index").getAsInt();
                int duration=element.isJsonObject()&&element.getAsJsonObject().has("time")?element.getAsJsonObject().get("time").getAsInt():time;
                if(index<0||index>=frameCount||duration<1)throw new IOException("Invalid animation frame: "+source);
                frames.add(new Frame(index,duration));
            }
            if(frames.isEmpty())for(int i=0;i<frameCount;i++)frames.add(new Frame(i,time));
            return new TextureAnimation(frames,animation.has("interpolate")&&animation.get("interpolate").getAsBoolean());
        }catch(RuntimeException e){throw new IOException("Invalid animation metadata: "+source,e);}
    }
    public int frame(long ticks){long total=frames.stream().mapToLong(Frame::ticks).sum();long time=Math.floorMod(ticks,total);for(Frame frame:frames){if(time<frame.ticks())return frame.index();time-=frame.ticks();}return 0;}
}
