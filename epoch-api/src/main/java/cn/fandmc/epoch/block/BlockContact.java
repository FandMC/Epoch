package cn.fandmc.epoch.block;

/** The minimal mutable motion surface exposed to inside-block callbacks. */
public interface BlockContact {
    void markInWeb();
    void scaleVelocity(double x, double y, double z);
}
