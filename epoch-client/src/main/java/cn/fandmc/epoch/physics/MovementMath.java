package cn.fandmc.epoch.physics;

/** Historical movement uses a 16-bit angle lookup and float intermediates. */
public final class MovementMath {
    private static final float[] SINE = new float[1 << 16];
    static {
        for (int index=0;index<SINE.length;index++) SINE[index]=(float)Math.sin(index*Math.PI*2.0/65536.0);
    }
    private MovementMath() {}
    public static float sin(float radians) { return SINE[(int)(radians*10430.378f)&65535]; }
    public static float cos(float radians) { return SINE[(int)(radians*10430.378f+16384f)&65535]; }
}
