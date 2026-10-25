package cn.fandmc.epoch.gui;

/** GLFW-compatible key/modifier tokens, with no dependency on a native library. */
public record KeyInput(int key,int scanCode,int modifiers,boolean repeat) {
    public static final int SPACE=32,ESCAPE=256,ENTER=257,TAB=258,BACKSPACE=259,
            DELETE=261,RIGHT=262,LEFT=263,DOWN=264,UP=265,HOME=268,END=269,KEYPAD_ENTER=335;
    public boolean shift() { return (modifiers&1)!=0; }
    public boolean control() { return (modifiers&2)!=0; }
    public boolean alt() { return (modifiers&4)!=0; }
}
