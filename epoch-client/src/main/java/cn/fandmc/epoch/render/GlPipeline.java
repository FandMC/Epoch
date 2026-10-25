package cn.fandmc.epoch.render;

import static org.lwjgl.opengl.GL33C.*;

public final class GlPipeline {
    private GlPipeline() {}

    public static void beginFrame(int width, int height) {
        // Epoch 独占上下文：每个 pass 显式声明所需状态，避免昂贵且易遗漏的全局快照。
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, width, height);
        glDisable(GL_SCISSOR_TEST);
        glDisable(GL_STENCIL_TEST);
        glDisable(GL_BLEND);
        glDisable(GL_FRAMEBUFFER_SRGB);
        glDisable(GL_CULL_FACE);
        glDisable(GL_POLYGON_OFFSET_FILL);
        glDisable(GL_RASTERIZER_DISCARD);
        glColorMask(true, true, true, true);
        glDepthMask(true);
        glDepthFunc(GL_LEQUAL);
        glEnable(GL_DEPTH_TEST);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glActiveTexture(GL_TEXTURE0);
        glBindSampler(0, 0);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
        glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
        glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
        glClearColor(0.52f, 0.72f, 0.89f, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
    }

    public static void beforeSkia() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDisable(GL_FRAMEBUFFER_SRGB);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
        glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
    }
}
