package cn.fandmc.epoch.gui;

/** Navigation is owned by the game thread. Null selects gameplay without a screen. */
public interface ScreenHost {
    Screen current();
    void show(Screen screen);
    /** A stale screen cannot close its replacement. */
    void close(Screen requester);
}
