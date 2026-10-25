package cn.fandmc.epoch.hud;

import java.util.*;

public final class EffectState {
    public record Effect(int id,int amplifier,int duration,boolean particles) { }
    private final Map<Integer,Effect> effects=new TreeMap<>();
    public Collection<Effect> values(){return Collections.unmodifiableCollection(effects.values());}
    public void put(int id,int amplifier,int duration,boolean particles){effects.put(id,new Effect(id,amplifier,duration,particles));}
    public void remove(int id){effects.remove(id);}
    public void clear(){effects.clear();}
    public void tick(){effects.replaceAll((id,e)->new Effect(id,e.amplifier(),Math.max(0,e.duration()-1),e.particles()));effects.values().removeIf(e->e.duration()==0);}
}
