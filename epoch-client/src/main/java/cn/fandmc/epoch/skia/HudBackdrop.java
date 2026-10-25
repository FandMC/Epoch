package cn.fandmc.epoch.skia;

import io.github.humbleui.skija.*;
import io.github.humbleui.types.*;

/** One shared GPU blur of the scene before any UI, at 3/8 resolution. */
public final class HudBackdrop implements AutoCloseable {
    private Surface small, blurred;
    private Image snapshot;
    private final Paint paint = new Paint();
    private ImageFilter filter;
    private int sourceWidth, sourceHeight, logicalWidth, logicalHeight, frame;

    public void capture(DirectContext context, Surface source, int logicalWidth, int logicalHeight) {
        boolean resized = sourceWidth != source.getWidth() || sourceHeight != source.getHeight()
                || this.logicalWidth != logicalWidth || this.logicalHeight != logicalHeight;
        if (resized) {
            release();
            sourceWidth = source.getWidth(); sourceHeight = source.getHeight();
            this.logicalWidth = logicalWidth; this.logicalHeight = logicalHeight;
            int w = Math.max(1, (sourceWidth * 3 + 7) / 8), h = Math.max(1, (sourceHeight * 3 + 7) / 8);
            small = Surface.makeRenderTarget(context, false, ImageInfo.makeN32Premul(w, h));
            blurred = Surface.makeRenderTarget(context, false, ImageInfo.makeN32Premul(w, h));
            if (small == null || blurred == null) throw new IllegalStateException("Cannot create HUD backdrop");
            filter = ImageFilter.makeBlur(7f * w / logicalWidth, 7f * h / logicalHeight, FilterTileMode.CLAMP);
        }
        if (!resized && (frame++ & 1) != 0) return;
        if (snapshot != null) { snapshot.close(); snapshot = null; }
        Rect bounds = Rect.makeWH(small.getWidth(), small.getHeight());
        try (Image scene = source.makeImageSnapshot()) {
            small.getCanvas().clear(0);
            small.getCanvas().drawImageRect(scene, Rect.makeWH(sourceWidth, sourceHeight), bounds, SamplingMode.LINEAR, null, true);
        }
        try (Image scene = small.makeImageSnapshot()) {
            blurred.getCanvas().clear(0);
            paint.setImageFilter(filter);
            blurred.getCanvas().drawImageRect(scene, bounds, bounds, SamplingMode.LINEAR, paint, true);
            paint.setImageFilter(null);
        }
        snapshot = blurred.makeImageSnapshot();
    }

    public void draw(Canvas canvas, float x, float y, float width, float height, float radius) {
        if (snapshot == null) return;
        canvas.save();
        try {
            canvas.clipRRect(RRect.makeXYWH(x, y, width, height, radius), true);
            // The clip is now in device space; nested UI scaling must not move the scene sample.
            canvas.resetMatrix();
            canvas.drawImageRect(snapshot, Rect.makeWH(snapshot.getWidth(), snapshot.getHeight()),
                    Rect.makeWH(sourceWidth, sourceHeight), SamplingMode.LINEAR, null, true);
        } finally { canvas.restore(); }
    }

    private void release() {
        if (snapshot != null) { snapshot.close(); snapshot = null; }
        if (small != null) { small.close(); small = null; }
        if (blurred != null) { blurred.close(); blurred = null; }
        if (filter != null) { filter.close(); filter = null; }
    }
    @Override public void close() { release(); paint.close(); }
}
