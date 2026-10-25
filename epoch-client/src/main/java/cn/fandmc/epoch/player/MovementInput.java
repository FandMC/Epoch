package cn.fandmc.epoch.player;

/** Logical controls sampled at the fixed simulation tick, independent of GLFW. */
public record MovementInput(float forward, float sideways, boolean jump, boolean sneak, boolean sprint) {
    public static final MovementInput NONE=new MovementInput(0,0,false,false,false);
}
