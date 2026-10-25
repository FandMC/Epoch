package cn.fandmc.epoch.player;

import cn.fandmc.epoch.block.Block;
import cn.fandmc.epoch.physics.Box;
import cn.fandmc.epoch.physics.CollisionWorld;
import cn.fandmc.epoch.physics.FluidPhysics;
import cn.fandmc.epoch.physics.MovementMath;
import cn.fandmc.epoch.world.ClientWorld;
import java.util.List;

/** Fixed 20 Hz integration. Float rounding is part of the movement protocol's behavior. */
public final class PlayerController {
    private record Motion(Box box, double x, double y, double z, double clippedY) {
        double horizontalSquared() { return x*x+z*z; }
    }
    private int jumpCooldown;
    private boolean previousSprinting;

    public void reset() { jumpCooldown=0;previousSprinting=false; }

    public void tick(ClientWorld world, PlayerState player, MovementInput input) {
        player.beginTick();
        if (!player.positioned || !world.hasChunk((int)Math.floor(player.x)>>4,(int)Math.floor(player.z)>>4)) return;
        boolean touchingWater=FluidPhysics.applyWater(world,player);
        boolean water=touchingWater&&!player.flying;
        boolean lava=!player.flying&&FluidPhysics.touchesLava(world,player.bounds());
        // Tiny residual velocity is cleared before jump/input acceleration, not afterwards.
        if (Math.abs(player.velocityX)<.005) player.velocityX=0;
        if (Math.abs(player.velocityY)<.005) player.velocityY=0;
        if (Math.abs(player.velocityZ)<.005) player.velocityZ=0;
        if (jumpCooldown>0) jumpCooldown--;
        if (!input.jump()) jumpCooldown=0;

        player.sneaking=input.sneak();
        float forward=input.forward(), side=input.sideways();
        if (input.sneak()) { forward=(float)(forward*.3D);side=(float)(side*.3D); }
        boolean canSprint=forward>=.8f&&(player.food>6||player.allowFlying)&&player.health>0;
        if (!player.sprinting&&input.sprint()&&canSprint&&!player.effects.containsKey(15)) player.sprinting=true;
        if (!canSprint||player.horizontalCollision) player.sprinting=false;
        if (player.health<=0) { forward=side=0;player.sprinting=false; }
        forward*=.98f;side*=.98f;

        boolean ladder=onClimbable(world,player)&&!player.flying&&!water&&!lava;
        float friction=.91f;
        if (player.onGround) {
            float slipperiness=blockAt(world,player.x,player.y-1,player.z).slipperiness();
            friction=slipperiness*.91f;
        }
        float acceleration=player.onGround
                ?player.attributes.movementSpeed(player.sprinting)*(.16277136f/(friction*friction*friction))
                :(previousSprinting?(float)((double).02f+.005999999865889549D):.02f);
        if (water||lava) acceleration=.02f;
        if (player.flying) {
            if (!player.onGround) acceleration=player.flySpeed*(player.sprinting?2f:1f);
            if (input.sneak()) player.velocityY-=player.flySpeed*3f;
            if (input.jump()&&player.health>0) player.velocityY+=player.flySpeed*3f;
        } else if (input.jump()&&player.health>0) {
            if (water||lava) player.velocityY+=(double).04f;
            else if (player.onGround&&jumpCooldown==0) {
                player.velocityY=.42f;
                Integer jumpBoost=player.effects.get(8);
                if (jumpBoost!=null) player.velocityY+=(jumpBoost+1)*.1f;
                jumpCooldown=10;
                if (player.sprinting) {
                    float angle=player.yaw*.017453292f;
                    player.velocityX-=MovementMath.sin(angle)*.2f;
                    player.velocityZ+=MovementMath.cos(angle)*.2f;
                }
            }
        }
        accelerate(player,side,forward,acceleration);
        if (ladder) {
            player.velocityX=Math.clamp(player.velocityX,-(double).15f,(double).15f);
            player.velocityZ=Math.clamp(player.velocityZ,-(double).15f,(double).15f);
            player.velocityY=Math.max(player.velocityY,-.15);
            if (player.sneaking&&player.velocityY<0) player.velocityY=0;
        }
        double flyingVertical=player.velocityY;
        double oldY=player.y;
        if (player.noClip) {
            player.x+=player.velocityX;player.y+=player.velocityY;player.z+=player.velocityZ;
            player.onGround=player.horizontalCollision=false;
        } else move(world,player,player.velocityX,player.velocityY,player.velocityZ,touchingWater);
        if (player.flying) {
            player.velocityX*=friction;player.velocityZ*=friction;player.velocityY=flyingVertical*.6D;
        } else if (water||lava) {
            double drag=water?(double).8f:.5D;
            player.velocityX*=drag;player.velocityY=player.velocityY*drag-.02D;player.velocityZ*=drag;
            if (player.horizontalCollision&&FluidPhysics.canExit(world,player.bounds().move(
                    player.velocityX,player.velocityY+(double).6f-player.y+oldY,player.velocityZ)))
                player.velocityY=(double).3f;
        } else {
            // Entry/exit can happen during move; the climbing impulse uses the new position.
            if (player.horizontalCollision&&onClimbable(world,player)) player.velocityY=.2D;
            player.velocityY=(player.velocityY-.08D)*(double).98f;
            player.velocityX*=friction;player.velocityZ*=friction;
        }
        previousSprinting=player.sprinting;
    }

    private static void accelerate(PlayerState player,float side,float forward,float acceleration) {
        float lengthSquared=side*side+forward*forward;
        if (lengthSquared<1.0e-4f) return;
        float length=(float)Math.sqrt(lengthSquared);
        float scale=acceleration/Math.max(1f,length);
        side*=scale;forward*=scale;
        // moveFlying multiplies then divides in float; folding this into a radians
        // constant changes lookup indices. Sprint-jump uses its separate constant.
        float angle=player.yaw*(float)Math.PI/180f,sin=MovementMath.sin(angle),cos=MovementMath.cos(angle);
        player.velocityX+=side*cos-forward*sin;
        player.velocityZ+=forward*cos+side*sin;
    }

    private static void move(ClientWorld world,PlayerState p,double dx,double dy,double dz,boolean touchingWater) {
        if (p.inWeb) {
            // The previous move's inside-block callback affects this displacement.
            // Clear momentum before collision handling; multiple web blocks do not stack.
            p.inWeb=false;
            dx*=.25D;dy*=(double).05f;dz*=.25D;
            p.velocityX=p.velocityY=p.velocityZ=0;
        }
        Box original=p.bounds();
        if (p.onGround&&p.sneaking) {
            while (dx!=0&&!supported(world,original.move(dx,-1,0))) dx=approachZero(dx,.05);
            while (dz!=0&&!supported(world,original.move(0,-1,dz))) dz=approachZero(dz,.05);
            while (dx!=0&&dz!=0&&!supported(world,original.move(dx,-1,dz))) { dx=approachZero(dx,.05);dz=approachZero(dz,.05); }
        }
        List<Box> obstacles=CollisionWorld.boxes(world,original.sweep(dx,dy,dz));
        Motion normal=clip(original,dx,dy,dz,obstacles),best=normal;
        if ((p.onGround||dy<0&&normal.y()!=dy)&&(normal.x()!=dx||normal.z()!=dz)) {
            double height=.6f;
            List<Box> stepBoxes=CollisionWorld.boxes(world,original.sweep(dx,height,dz));
            Motion direct=clip(original,dx,height,dz,stepBoxes);
            // Also test clearance above the full horizontal sweep, as required by the 1.8 step fix.
            double rise=height;Box sweep=original.sweep(dx,0,dz);
            for (Box obstacle:stepBoxes) rise=obstacle.clipY(sweep,rise);
            Motion horizontal=clip(original.move(0,rise,0),dx,0,dz,stepBoxes);
            Motion wide=new Motion(horizontal.box(),horizontal.x(),rise,horizontal.z(),rise);
            Motion up=wide.horizontalSquared()>direct.horizontalSquared()?wide:direct;
            double down=-up.y();
            for (Box obstacle:stepBoxes) down=obstacle.clipY(up.box(),down);
            Motion step=new Motion(up.box().move(0,down,0),up.x(),up.y()+down,up.z(),down);
            if (step.horizontalSquared()>normal.horizontalSquared()) best=step;
        }
        p.x=(best.box().minX()+best.box().maxX())/2;p.y=best.box().minY();p.z=(best.box().minZ()+best.box().maxZ())/2;
        p.horizontalCollision=best.x()!=dx||best.z()!=dz;
        p.onGround=dy<0&&best.clippedY()!=dy;
        // Fall-state handling checks water again at the new position only when
        // the tick began dry. Its current precedes collision velocity resets
        // and drag; entering water does not change this tick's travel branch.
        if (!touchingWater) FluidPhysics.applyWater(world,p);
        if (best.x()!=dx) p.velocityX=0;
        if (best.z()!=dz) p.velocityZ=0;
        if (best.clippedY()!=dy) {
            p.velocityY=blockAt(world,p.x,p.y-.2D,p.z).landingVelocity(p.velocityY,p.sneaking);
        }
        applyBlockContacts(world,p);
    }

    /** Inside-block callbacks use the contracted body, after collision resets and before travel drag. */
    private static void applyBlockContacts(ClientWorld world,PlayerState player) {
        Box box=player.bounds();
        int minX=(int)Math.floor(box.minX()+.001D),maxX=(int)Math.floor(box.maxX()-.001D);
        int minY=(int)Math.floor(box.minY()+.001D),maxY=(int)Math.floor(box.maxY()-.001D);
        int minZ=(int)Math.floor(box.minZ()+.001D),maxZ=(int)Math.floor(box.maxZ()-.001D);
        for(int x=minX>>4;x<=maxX>>4;x++)for(int z=minZ>>4;z<=maxZ>>4;z++)
            if(!world.hasChunk(x,z))return;
        for(int x=minX;x<=maxX;x++)for(int y=minY;y<=maxY;y++)for(int z=minZ;z<=maxZ;z++){
            world.registry().block(world.stateHandle(x,y,z)).onInside(player);
        }
    }
    private static Motion clip(Box original,double x,double y,double z,List<Box> obstacles) {
        for (Box box:obstacles) y=box.clipY(original,y);Box moved=original.move(0,y,0);
        for (Box box:obstacles) x=box.clipX(moved,x);moved=moved.move(x,0,0);
        for (Box box:obstacles) z=box.clipZ(moved,z);
        return new Motion(moved.move(0,0,z),x,y,z,y);
    }
    private static Block blockAt(ClientWorld world,double x,double y,double z) {
        return world.registry().block(world.stateHandle((int)Math.floor(x),(int)Math.floor(y),(int)Math.floor(z)));
    }
    private static boolean onClimbable(ClientWorld world,PlayerState player) {
        if(player.noClip)return false;
        return blockAt(world,player.x,player.y,player.z).climbable();
    }
    private static boolean supported(ClientWorld world,Box box) { return !CollisionWorld.boxes(world,box).isEmpty(); }
    private static double approachZero(double value,double step) { return Math.abs(value)<=step?0:value-Math.copySign(step,value); }
}
