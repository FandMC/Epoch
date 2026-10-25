package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;
import java.util.*;

public record S3EPacketTeams(String name,int mode,String title,String prefix,String suffix,int flags,String visibility,int color,List<String> members) implements Packet<ClientPlayPacketListener> {
    public S3EPacketTeams {members=List.copyOf(members);}
    public static S3EPacketTeams read(PacketReader in)throws IOException {
        String name=in.string(16);int mode=in.unsignedByte();if(mode>4)throw new IOException("Invalid team mode");
        String title="",prefix="",suffix="",visibility="always";int flags=0,color=-1;List<String> members=new ArrayList<>();
        if(mode==0||mode==2){title=in.string(32);prefix=in.string(16);suffix=in.string(16);flags=in.unsignedByte();visibility=in.string(32);color=in.byteValue();}
        if(mode==0||mode==3||mode==4){int n=in.varInt();if(n<0||n>16384)throw new IOException("Invalid team size");for(int i=0;i<n;i++)members.add(in.string(40));}
        return new S3EPacketTeams(name,mode,title,prefix,suffix,flags,visibility,color,members);
    }
    @Override public void write(PacketWriter out)throws IOException {
        out.string(name,16).byteValue(mode);if(mode==0||mode==2)out.string(title,32).string(prefix,16).string(suffix,16).byteValue(flags).string(visibility,32).byteValue(color);
        if(mode==0||mode==3||mode==4){out.varInt(members.size());for(String member:members)out.string(member,40);}
    }
    @Override public void handle(ClientPlayPacketListener listener)throws IOException {listener.onTeams(this);}
}
