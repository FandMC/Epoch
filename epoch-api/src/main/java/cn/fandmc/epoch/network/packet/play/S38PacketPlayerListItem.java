package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;
import java.util.*;

public record S38PacketPlayerListItem(int action,List<Entry> entries) implements Packet<ClientPlayPacketListener> {
    public record Property(String name,String value,String signature) { }
    public record Entry(UUID uuid,String name,List<Property> properties,int gameMode,int ping,String displayName) {
        public Entry { properties=List.copyOf(properties); }
    }
    public S38PacketPlayerListItem { entries=List.copyOf(entries); }
    public static S38PacketPlayerListItem read(PacketReader in)throws IOException {
        int action=in.varInt(),count=in.varInt();
        if(action<0||action>4||count<0||count>4096)throw new IOException("Invalid player list");
        List<Entry> entries=new ArrayList<>(count);
        for(int i=0;i<count;i++){
            UUID uuid=in.uuid();String name="",display=null;int mode=0,ping=0;List<Property> props=new ArrayList<>();
            if(action==0){name=in.string(16);int n=in.varInt();if(n<0||n>64)throw new IOException("Invalid profile properties");
                for(int j=0;j<n;j++){String key=in.string(32767),value=in.string(32767);props.add(new Property(key,value,in.bool()?in.string(32767):null));}}
            if(action==0||action==1)mode=in.varInt();
            if(action==0||action==2)ping=in.varInt();
            if(action==0||action==3)display=in.bool()?in.string(32767):null;
            entries.add(new Entry(uuid,name,props,mode,ping,display));
        }
        return new S38PacketPlayerListItem(action,entries);
    }
    @Override public void write(PacketWriter out)throws IOException {
        out.varInt(action).varInt(entries.size());for(var e:entries){out.longValue(e.uuid().getMostSignificantBits()).longValue(e.uuid().getLeastSignificantBits());
            if(action==0){out.string(e.name(),16).varInt(e.properties().size());for(var p:e.properties()){out.string(p.name(),32767).string(p.value(),32767).bool(p.signature()!=null);if(p.signature()!=null)out.string(p.signature(),32767);}}
            if(action==0||action==1)out.varInt(e.gameMode());if(action==0||action==2)out.varInt(e.ping());
            if(action==0||action==3){out.bool(e.displayName()!=null);if(e.displayName()!=null)out.string(e.displayName(),32767);}}
    }
    @Override public void handle(ClientPlayPacketListener listener)throws IOException {listener.onPlayerListItem(this);}
}
