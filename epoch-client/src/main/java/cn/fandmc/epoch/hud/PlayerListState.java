package cn.fandmc.epoch.hud;

import cn.fandmc.epoch.network.ChatText;
import cn.fandmc.epoch.network.packet.play.S38PacketPlayerListItem;
import java.util.*;

public final class PlayerListState {
    public record Player(UUID uuid,String name,int gameMode,int ping,String displayName,List<S38PacketPlayerListItem.Property> properties) { }
    private final Map<UUID,Player> players=new HashMap<>();
    private String header="",footer="";private long revision;
    public long revision(){return revision;}
    public String header(){return header;}public String footer(){return footer;}
    public int size(){return players.size();}
    public void clear(){players.clear();header=footer="";revision++;}
    public void header(String h,String f){header=ChatText.formatted(h);footer=ChatText.formatted(f);revision++;}
    public void update(S38PacketPlayerListItem packet){for(var e:packet.entries()){
        Player old=players.get(e.uuid());if(packet.action()==4){players.remove(e.uuid());continue;}
        if(packet.action()==0)players.put(e.uuid(),new Player(e.uuid(),e.name(),e.gameMode(),e.ping(),e.displayName()==null?null:ChatText.formatted(e.displayName()),e.properties()));
        else if(old!=null)players.put(e.uuid(),new Player(old.uuid(),old.name(),packet.action()==1?e.gameMode():old.gameMode(),packet.action()==2?e.ping():old.ping(),packet.action()==3?(e.displayName()==null?null:ChatText.formatted(e.displayName())):old.displayName(),old.properties()));
    }revision++;}
    public List<Player> sorted(ScoreboardState scoreboard){return players.values().stream().sorted(Comparator.comparing((Player p)->p.gameMode()==3).thenComparing(p->{var t=scoreboard.team(p.name());return t==null?"":t.name();}).thenComparing(Player::name,String.CASE_INSENSITIVE_ORDER)).limit(80).toList();}
}
