package cn.fandmc.epoch.hud;

import cn.fandmc.epoch.network.packet.play.*;
import java.util.*;

/** Game-thread scoreboard, including team decorations and color-specific sidebar slots. */
public final class ScoreboardState {
    public record Objective(String name,String title,String type) { }
    public record Row(String owner,String label,int score) { }
    public record Team(String name,String prefix,String suffix,int color) { }
    private final Map<String,Objective> objectives=new HashMap<>();
    private final Map<String,Map<String,Integer>> scores=new HashMap<>();
    private final Map<Integer,String> displays=new HashMap<>();
    private final Map<String,Team> teams=new HashMap<>();
    private final Map<String,String> membership=new HashMap<>();
    private long revision;
    public long revision(){return revision;}
    public void clear(){objectives.clear();scores.clear();displays.clear();teams.clear();membership.clear();revision++;}
    public void objective(S3BPacketScoreboardObjective p){
        if(p.mode()==1){objectives.remove(p.name());scores.remove(p.name());displays.values().removeIf(p.name()::equals);}
        else objectives.put(p.name(),new Objective(p.name(),p.title(),p.type()));revision++;
    }
    public void score(S3CPacketUpdateScore p){
        if(p.action()==1){if(p.objective().isEmpty())scores.values().forEach(s->s.remove(p.owner()));else{var s=scores.get(p.objective());if(s!=null)s.remove(p.owner());}}
        else scores.computeIfAbsent(p.objective(),k->new HashMap<>()).put(p.owner(),p.value());revision++;
    }
    public void display(S3DPacketDisplayScoreboard p){if(p.objective().isEmpty())displays.remove(p.position());else displays.put(p.position(),p.objective());revision++;}
    public void team(S3EPacketTeams p){
        if(p.mode()==0)membership.values().removeIf(p.name()::equals);
        if(p.mode()==1){teams.remove(p.name());membership.values().removeIf(p.name()::equals);}
        if(p.mode()==0||p.mode()==2)teams.put(p.name(),new Team(p.name(),p.prefix(),p.suffix(),p.color()));
        if(p.mode()==0||p.mode()==3)for(String name:p.members())membership.put(name,p.name());
        if(p.mode()==4)for(String name:p.members())membership.remove(name,p.name());revision++;
    }
    public Team team(String name){return teams.get(membership.get(name));}
    public String decorate(String name){Team t=team(name);return t==null?name:t.prefix()+name+t.suffix();}
    public Objective sidebar(String player){Team t=team(player);Objective colored=t!=null&&t.color()>=0?objectives.get(displays.get(3+t.color())):null;return colored!=null?colored:objectives.get(displays.get(1));}
    public List<Row> rows(Objective objective){
        if(objective==null)return List.of();
        return scores.getOrDefault(objective.name(),Map.of()).entrySet().stream().filter(e->!e.getKey().startsWith("#"))
                .sorted(Map.Entry.<String,Integer>comparingByValue().reversed().thenComparing(Map.Entry::getKey,String.CASE_INSENSITIVE_ORDER))
                .limit(15).map(e->new Row(e.getKey(),decorate(e.getKey()),e.getValue())).toList();
    }
    public Integer listScore(String name){var values=scores.get(displays.get(0));return values==null?null:values.get(name);}
}
