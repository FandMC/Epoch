package cn.fandmc.epoch.skia;

import cn.fandmc.epoch.render.GlPipeline;
import io.github.humbleui.skija.*;
import java.util.function.Consumer;

public final class SkiaLayer implements AutoCloseable {
    private final DirectContext context = DirectContext.makeGL();
    private final ColorSpace colorSpace = ColorSpace.getSRGB();
    private BackendRenderTarget target;
    private Surface surface;
    private int width, height;
    private final HudBackdrop backdrop = new HudBackdrop();
    public DirectContext context() { return context; }
    public HudBackdrop backdrop() { return backdrop; }

    public void render(int width, int height, int logicalWidth, int logicalHeight, Consumer<Canvas> draw) {
        if (width <= 0 || height <= 0 || logicalWidth <= 0 || logicalHeight <= 0) return;
        GlPipeline.beforeSkia();
        context.resetAll();
        if (surface == null || width != this.width || height != this.height) {
            if (surface != null) surface.close();
            if (target != null) target.close();
            target = BackendRenderTarget.makeGL(width, height, 0, 8, 0, FramebufferFormat.GR_GL_RGBA8);
            surface = Surface.wrapBackendRenderTarget(context, target, SurfaceOrigin.BOTTOM_LEFT, ColorType.RGBA_8888, colorSpace);
            if (surface == null) throw new IllegalStateException("Skija could not wrap GLFW framebuffer");
            this.width = width; this.height = height;
        }
        Canvas canvas = surface.getCanvas();
        // The OpenGL world pass writes into this external framebuffer every frame.
        surface.notifyContentWillChange(ContentChangeMode.RETAIN);
        backdrop.capture(context, surface, logicalWidth, logicalHeight);
        int save = canvas.getSaveCount();
        canvas.save();
        try {
            canvas.scale((float) width / logicalWidth, (float) height / logicalHeight);
            draw.accept(canvas);
        } finally { canvas.restoreToCount(save); }
        context.flushAndSubmit(surface, false);
    }

    @Override public void close() {
        backdrop.close();
        if (surface != null) surface.close();
        if (target != null) target.close();
        context.close();
        colorSpace.close();
    }
}
