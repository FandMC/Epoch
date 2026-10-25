package cn.fandmc.epoch.core;

import cn.fandmc.epoch.asset.AssetDirectory;
import cn.fandmc.epoch.block.BlockRegistry;
import cn.fandmc.epoch.config.ClientConfig;
import cn.fandmc.epoch.font.FontBook;
import cn.fandmc.epoch.gui.*;
import cn.fandmc.epoch.network.GameSession;
import cn.fandmc.epoch.network.StatusClient;
import cn.fandmc.epoch.network.packet.play.C01PacketChatMessage;
import cn.fandmc.epoch.network.packet.play.*;
import cn.fandmc.epoch.hud.HudRenderer;
import cn.fandmc.epoch.player.MovementInput;
import cn.fandmc.epoch.render.GlPipeline;
import cn.fandmc.epoch.render.WorldRenderer;
import cn.fandmc.epoch.skia.SkiaLayer;
import cn.fandmc.epoch.skia.SkiaGuiGraphics;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.Rect;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import java.nio.file.*;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;

/** Desktop lifecycle, game-thread event consumption and rendering. Invoked only by Epoch. */
public final class EpochClient {
    private final ClientConfig config;
    private long window;
    private int width,height,frameWidth,frameHeight;
    private boolean captured;
    private final ScreenManager screens=new ScreenManager(this::screenChanged);
    private double mouseX,mouseY;
    private String notice="正在读取本地资源…";
    private GameSession session;
    private GameState game;
    private BlockRegistry blocks;
    private WorldRenderer renderer;
    private FontBook fonts;
    private Paint paint;
    private GuiResources gui;
    private HudRenderer hud;
    private SkiaLayer skiaLayer;
    private boolean debugHud;
    private long shownWindowRevision;
    private CompletableFuture<StatusClient.Status> status;
    private String statusText="正在查询服务器…";
    private int frames,fps;
    private long ticks;
    private long lastJumpPress;
    private double frameSum,frameMax,averageFrameMillis,maximumFrameMillis;
    private String performanceText="",positionText="";
    private long lastMetrics;

    public EpochClient(ClientConfig config){this.config=config;width=config.width();height=config.height();}

    public void run() throws Exception {
        Files.createDirectories(Path.of("run/logs"));
        FileHandler log=new FileHandler("run/logs/epoch-%g.log",2_000_000,3,true);
        log.setEncoding("UTF-8");log.setFormatter(new SimpleFormatter());
        Logger.getLogger("").addHandler(log);
        GLFWErrorCallback errors=GLFWErrorCallback.createPrint(System.err);
        glfwSetErrorCallback(errors);
        try{
            if(!glfwInit())throw new IllegalStateException("GLFW initialization failed");
            glfwDefaultWindowHints();
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3);glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,3);
            glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_CORE_PROFILE);
            glfwWindowHint(GLFW_STENCIL_BITS,8);glfwWindowHint(GLFW_DEPTH_BITS,24);
            glfwWindowHint(GLFW_VISIBLE,GLFW_FALSE);
            window=glfwCreateWindow(width,height,"Epoch",0,0);
            if(window==0)throw new IllegalStateException("Could not create the Epoch window");
            glfwMakeContextCurrent(window);GL.createCapabilities();glfwSwapInterval(config.vsync()?1:0);
            System.out.printf("Epoch | Java %s | OpenGL %s | %s%n",System.getProperty("java.version"),glGetString(GL_VERSION),glGetString(GL_RENDERER));
            installCallbacks();
            int[] fw={0},fh={0};glfwGetFramebufferSize(window,fw,fh);frameWidth=fw[0];frameHeight=fh[0];
            blocks=BlockRegistry.load();game=new GameState(blocks);game.world.modernLadderCollision(config.modernLadderCollision());
            try(SkiaLayer skia=new SkiaLayer();FontBook book=new FontBook();Paint sharedPaint=new Paint();
                WorldRenderer worldRenderer=new WorldRenderer(new AssetDirectory(Path.of(config.assetDirectory())),blocks);
                GuiResources guiResources=new GuiResources(new AssetDirectory(Path.of(config.assetDirectory())))){
                fonts=book;paint=sharedPaint;renderer=worldRenderer;gui=guiResources;skiaLayer=skia;hud=new HudRenderer(game,gui);
                Files.write(Path.of("run/logs/unresolved-models.txt"),renderer.unresolved());
                notice="本地资源已加载";screens.resize(width,height);showMainMenu();refreshStatus();glfwShowWindow(window);
                long last=System.nanoTime(),lastFps=last;double accumulator=0;
                while(!glfwWindowShouldClose(window)){
                    glfwPollEvents();long now=System.nanoTime();double elapsed=(now-last)/1e9;
                    accumulator+=Math.min(.25,elapsed);last=now;
                    frameSum+=elapsed*1000;frameMax=Math.max(frameMax,elapsed*1000);
                    updateStatus();
                    if(session!=null){
                        try{game.drain(session);}catch(IOException e){notice=e.getMessage();disconnect();}
                        if(session!=null&&session.closed()){notice=session.failure()==null?"已断开连接":session.failure();disconnect();}
                    }
                    syncScreen();
                    while(accumulator>=.05){tick();accumulator-=.05;ticks++;}
                    if(frameWidth>0&&frameHeight>0){
                        if(session!=null)gui.prepare(game.inventory.visibleItems(),skia.context());
                        GlPipeline.beginFrame(frameWidth,frameHeight);
                        if(session!=null)renderer.render(game.world,game.player,frameWidth,frameHeight,config.viewDistance(),ticks,(float)(accumulator/.05));
                        float tickDelta=(float)(accumulator/.05);
                        skia.render(frameWidth,frameHeight,width,height,c->draw(c,tickDelta));
                        int error=glGetError();if(error!=GL_NO_ERROR)throw new IllegalStateException("OpenGL error 0x"+Integer.toHexString(error));
                        glfwSwapBuffers(window);frames++;
                    }else glfwWaitEventsTimeout(.05);
                    if(now-lastFps>=1_000_000_000L){
                        fps=(int)(frames*1e9/(now-lastFps));averageFrameMillis=frameSum/Math.max(1,frames);maximumFrameMillis=frameMax;
                        frames=0;frameSum=frameMax=0;lastFps=now;
                        performanceText=String.format("Epoch / %d FPS / %.1f ms avg / %.1f ms max / %d draws / %d mesh queued",fps,averageFrameMillis,maximumFrameMillis,renderer.drawCalls(),renderer.pendingMeshes());
                        if(session!=null&&now-lastMetrics>=5_000_000_000L){
                            lastMetrics=now;System.getLogger(EpochClient.class.getName()).log(System.Logger.Level.INFO,
                                    String.format("Frame avg=%.2fms max=%.2fms fps=%d chunks=%d draws=%d vertices=%d meshPending=%d lastMesh=%.2fms xyz=%.3f,%.3f,%.3f ground=%s corrections=%d",averageFrameMillis,maximumFrameMillis,fps,game.world.chunkCount(),renderer.drawCalls(),renderer.vertices(),renderer.pendingMeshes(),renderer.lastMeshMillis(),game.player.x,game.player.y,game.player.z,game.player.onGround,game.corrections));
                        }
                    }
                }
            }finally{disconnect();screens.show(null);}
        }finally{
            if(status!=null)status.cancel(true);
            if(window!=0){Callbacks.glfwFreeCallbacks(window);glfwDestroyWindow(window);window=0;GL.setCapabilities(null);}
            glfwTerminate();glfwSetErrorCallback(null);errors.free();Logger.getLogger("").removeHandler(log);log.close();
        }
    }

    private void installCallbacks(){
        glfwSetWindowSizeCallback(window,(w,x,y)->{width=x;height=y;screens.resize(x,y);});
        glfwSetFramebufferSizeCallback(window,(w,x,y)->{frameWidth=x;frameHeight=y;});
        glfwSetWindowFocusCallback(window,(w,focused)->{
            screens.focusChanged(focused);
            if(!focused){
                capture(false);
                if(session!=null&&game.player.positioned&&screens.current()==null)screens.show(new PauseScreen(this::returnToMenu));
            }
        });
        glfwSetCursorPosCallback(window,(w,x,y)->{
            if(captured&&screens.current()==null&&game!=null&&game.player.positioned&&!game.deathScreenVisible()){game.player.yaw+=(float)((x-mouseX)*.12);game.player.pitch=Math.clamp(game.player.pitch+(float)((y-mouseY)*.12),-90,90);}
            mouseX=x;mouseY=y;
            screens.mouseMoved(x,y);
        });
        glfwSetMouseButtonCallback(window,(w,button,action,mods)->{
            if(action==GLFW_RELEASE){screens.mouseReleased(mouseX,mouseY,button,mods);return;}
            if(action!=GLFW_PRESS||screens.mousePressed(mouseX,mouseY,button,mods))return;
            if(button==GLFW_MOUSE_BUTTON_LEFT&&session!=null&&game.player.positioned&&!game.deathScreenVisible())capture(true);
        });
        glfwSetScrollCallback(window,(w,x,y)->{if(!screens.mouseScrolled(mouseX,mouseY,x,y)&&session!=null&&captured&&y!=0)selectHotbar(Math.floorMod(game.inventory.selected()-(y>0?1:-1),9));});
        glfwSetKeyCallback(window,(w,key,scan,action,mods)->{
            KeyInput input=new KeyInput(key,scan,mods,action==GLFW_REPEAT);
            if(action==GLFW_RELEASE){screens.keyReleased(input);return;}
            if(action!=GLFW_PRESS&&action!=GLFW_REPEAT)return;
            if(screens.keyPressed(input))return;
            if(action==GLFW_PRESS&&session!=null&&game.player.positioned&&!game.deathScreenVisible()){
                if(key==GLFW_KEY_E){openInventory();return;}
                if(key>=GLFW_KEY_1&&key<=GLFW_KEY_9){selectHotbar(key-GLFW_KEY_1);return;}
                if(key==GLFW_KEY_F3){debugHud=!debugHud;return;}
            }
            if(key==GLFW_KEY_ESCAPE&&action==GLFW_PRESS&&session!=null){screens.show(new PauseScreen(this::returnToMenu));return;}
            if(key==GLFW_KEY_SPACE&&action==GLFW_PRESS&&session!=null&&captured&&game.player.allowFlying&&!game.player.noClip){
                long now=System.nanoTime();
                if(now-lastJumpPress<350_000_000L){
                    game.player.flying=!game.player.flying;
                    try{sendFlying();}catch(IOException e){notice=e.getMessage();disconnect();}
                    lastJumpPress=0;
                }else lastJumpPress=now;
            }
            if(key==GLFW_KEY_T&&action==GLFW_PRESS&&session!=null&&game.player.positioned&&!game.deathScreenVisible())screens.show(new ChatScreen(this::sendChat));
        });
        glfwSetCharCallback(window,(w,cp)->screens.charTyped(cp));
    }
    private void refreshStatus(){
        if(status!=null&&!status.isDone())return;
        statusText="正在查询 "+config.host()+":"+config.port();
        status=new CompletableFuture<>();var result=status;
        Thread.ofVirtual().name("epoch-server-status").start(()->{try{result.complete(new StatusClient().query(config.host(),config.port()));}catch(Exception e){result.completeExceptionally(e);}});
    }
    private void updateStatus(){if(status!=null&&status.isDone()){try{var value=status.join();statusText=value.version()+" · "+value.online()+"/"+value.maximum()+" · "+value.pingMillis()+" ms";}catch(Exception e){statusText="服务器查询失败: "+e.getCause().getMessage();}status=null;}}
    private void connect(){
        if(session!=null)return;
        try{game=new GameState(blocks);game.world.modernLadderCollision(config.modernLadderCollision());renderer.reset();lastJumpPress=0;session=new GameSession(config,blocks);gui.mapping(session.items());hud=new HudRenderer(game,gui);shownWindowRevision=game.windowRevision;session.start();notice="正在连接 "+config.host()+":"+config.port();screens.show(new ConnectionScreen(()->notice,this::returnToMenu));}
        catch(IOException e){notice=e.getMessage();}
    }
    private void disconnect(){capture(false);if(session!=null){session.close();session=null;}showMainMenu();}
    private void returnToMenu(){notice="已返回主菜单";disconnect();}
    public ScreenHost screens(){return screens;}
    private void showMainMenu(){
        if(!(screens.current() instanceof MainMenuScreen))screens.show(new MainMenuScreen(
                config.host()+":"+config.port()+"   /   "+config.username(),()->statusText,()->notice,
                this::connect,this::refreshStatus,()->glfwSetWindowShouldClose(window,true)));
    }
    private void sendChat(String message){
        try{if(session!=null)session.send(new C01PacketChatMessage(message));}
        catch(IOException e){notice=e.getMessage();disconnect();}
    }
    private void requestRespawn(){
        try{if(session!=null&&screens.current() instanceof DeathScreen death&&death.canRespawn())game.requestRespawn(session);}
        catch(IOException e){notice=e.getMessage();disconnect();}
    }
    private void selectHotbar(int slot){
        if(session==null||slot==game.inventory.selected())return;
        try{session.queueTickAction(new C09PacketHeldItemChange(slot));game.inventory.selected(slot);}
        catch(IOException e){notice=e.getMessage();disconnect();}
    }
    private void openInventory(){
        try{session.queueTickAction(new C16PacketClientStatus(C16PacketClientStatus.Action.OPEN_INVENTORY));showInventory();}
        catch(IOException e){notice=e.getMessage();disconnect();}
    }
    private void showInventory(){
        int id=game.inventory.windowId();long generation=game.windowRevision;GameState owner=game;GameSession connection=session;
        screens.show(new InventoryScreen(game.inventory,gui,this::clickInventory,()->{
            if(session!=connection||game!=owner||game.inventory.windowId()!=id||game.windowRevision!=generation)return;
            try{session.queueTickAction(new C0DPacketCloseWindow(id));game.inventory.close();}
            catch(IOException e){notice=e.getMessage();disconnect();}
        }));
    }
    private void clickInventory(int slot,int button,boolean shift){
        if(session==null||game.inventory.awaitingResync())return;
        var inventory=game.inventory;
        if(shift&&!inventory.canQuickMove(slot))return;
        try{
            var clicked=shift?inventory.quickMove(slot):inventory.click(slot,button);
            session.queueTickAction(new C0EPacketClickWindow(inventory.windowId(),slot,button,inventory.nextAction(),shift?1:0,session.items().encode(clicked)));
        }catch(IOException e){notice=e.getMessage();disconnect();}
    }
    private void syncScreen(){
        if(session==null)return;
        Screen current=screens.current();
        if(shownWindowRevision!=game.windowRevision){
            shownWindowRevision=game.windowRevision;
            if(game.inventory.windowId()!=0&&!game.deathScreenVisible()){showInventory();current=screens.current();}
            else if(current instanceof InventoryScreen){screens.show(null);current=null;}
        }
        if(game.deathScreenVisible()){
            if(!(current instanceof DeathScreen))screens.show(new DeathScreen(game::lifeState,this::requestRespawn,this::returnToMenu));
        }else if(game.player.positioned){
            if(current instanceof DeathScreen||current instanceof ConnectionScreen)screens.show(null);
        }else if(!(current instanceof ConnectionScreen))screens.show(new ConnectionScreen(()->"正在接收世界数据…",this::returnToMenu));
    }
    private void screenChanged(Screen current){
        lastJumpPress=0;
        capture(current==null&&session!=null&&game!=null&&game.player.positioned&&!game.deathScreenVisible()
                &&window!=0&&glfwGetWindowAttrib(window,GLFW_FOCUSED)==GLFW_TRUE);
    }
    private void capture(boolean enabled){
        if(window==0||captured==enabled)return;
        captured=enabled;glfwSetInputMode(window,GLFW_CURSOR,enabled?GLFW_CURSOR_DISABLED:GLFW_CURSOR_NORMAL);
        if(glfwRawMouseMotionSupported())glfwSetInputMode(window,GLFW_RAW_MOUSE_MOTION,enabled?GLFW_TRUE:GLFW_FALSE);
        double[] x={0},y={0};glfwGetCursorPos(window,x,y);mouseX=x[0];mouseY=y[0];
    }
    private void tick(){
        screens.tick();
        if(game!=null)game.tick();
        if(session!=null&&session.playing()&&game.player.positioned){
            boolean active=captured&&screens.current()==null&&!game.deathScreenVisible()&&glfwGetWindowAttrib(window,GLFW_FOCUSED)==GLFW_TRUE;
            MovementInput input=active?new MovementInput((down(GLFW_KEY_W)?1:0)-(down(GLFW_KEY_S)?1:0),
                    (down(GLFW_KEY_A)?1:0)-(down(GLFW_KEY_D)?1:0),down(GLFW_KEY_SPACE),down(GLFW_KEY_LEFT_SHIFT),down(GLFW_KEY_LEFT_CONTROL)):MovementInput.NONE;
            game.movement.tick(game.world,game.player,input);
            try{
                game.movementSender.sendTick(session,game.entityId,game.player);
            }catch(IOException e){notice=e.getMessage();disconnect();}
            positionText=String.format("XYZ %.2f / %.2f / %.2f / %s / corrections %d",game.player.x,game.player.y,game.player.z,game.player.onGround?"ground":game.player.flying?"flying":"air",game.corrections);
        }
    }
    private boolean down(int key){return glfwGetKey(window,key)==GLFW_PRESS;}
    private void sendFlying()throws IOException{
        int flags=(game.player.abilityFlags&~2)|(game.player.flying?2:0);
        session.queueTickAction(new C13PacketPlayerAbilities(flags,game.player.flySpeed,game.player.walkSpeed));
    }
    private void draw(Canvas c,float tickDelta){
        Screen current=screens.current();
        SkiaGuiGraphics graphics=new SkiaGuiGraphics(c,fonts,paint,skiaLayer.backdrop(),gui);
        c.save();
        try{
            if(session!=null&&(current==null||current.showsHud())){
                hud.render(graphics,width,height,tickDelta,current==null&&down(GLFW_KEY_TAB),current==null,
                        debugHud,performanceText,positionText);
            }
            screens.render(graphics,mouseX,mouseY,tickDelta);
        }finally{c.restore();}
    }
}
