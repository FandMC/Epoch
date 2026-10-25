package cn.fandmc.epoch.player;

import cn.fandmc.epoch.block.BlockContact;
import cn.fandmc.epoch.network.packet.play.S08PacketPlayerPosLook;
import cn.fandmc.epoch.physics.Box;
import java.util.HashMap;
import java.util.Map;

public final class PlayerState implements BlockContact {
    public final PlayerAttributes attributes = new PlayerAttributes();
    public final Map<Integer,Integer> effects = new HashMap<>();
    public float health=20, saturation=5;
    public int food=20;
    public double x, y, z;
    public float yaw, pitch;
    public boolean positioned, onGround;
    public double velocityX, velocityY, velocityZ;
    public double previousX,previousY,previousZ;
    public boolean sneaking,sprinting,horizontalCollision,flying,allowFlying,noClip;
    public boolean inWeb;
    public float flySpeed=.05f,walkSpeed=.1f;
    public int abilityFlags;

    @Override public void markInWeb(){inWeb=true;}
    @Override public void scaleVelocity(double x,double y,double z){velocityX*=x;velocityY*=y;velocityZ*=z;}

    public void beginTick(){previousX=x;previousY=y;previousZ=z;}
    public Box bounds(){double half=.6f/2f;return new Box(x-half,y,z-half,x+half,y+1.8f,z+half);}
    public double renderX(float alpha){return previousX+(x-previousX)*alpha;}
    public double renderY(float alpha){return previousY+(y-previousY)*alpha;}
    public double renderZ(float alpha){return previousZ+(z-previousZ)*alpha;}
    public float eyeHeight(){return sneaking?1.54f:1.62f;}

    public void reset() {
        attributes.reset();effects.clear();health=20;food=20;saturation=5;
        x = y = z = velocityX = velocityY = velocityZ = 0;
        yaw = pitch = 0;
        positioned = onGround = false;
        previousX=previousY=previousZ=0;
        sneaking=sprinting=horizontalCollision=flying=allowFlying=noClip=false;
        inWeb=false;
        flySpeed=.05f;walkSpeed=.1f;abilityFlags=0;
    }

    public void correct(S08PacketPlayerPosLook position) {
        int flags = position.flags();
        if ((flags & 1) == 0) velocityX = 0;
        if ((flags & 2) == 0) velocityY = 0;
        if ((flags & 4) == 0) velocityZ = 0;
        x = position.x() + ((flags & 1) != 0 ? x : 0);
        y = position.y() + ((flags & 2) != 0 ? y : 0);
        z = position.z() + ((flags & 4) != 0 ? z : 0);
        yaw = position.yaw() + ((flags & 8) != 0 ? yaw : 0);
        pitch = position.pitch() + ((flags & 16) != 0 ? pitch : 0);
        onGround = false;
        positioned = true;
        previousX=x;previousY=y;previousZ=z;
    }

}
