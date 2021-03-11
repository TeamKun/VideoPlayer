package net.kunmc.lab.vplayer.client.video;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.kunmc.lab.vplayer.VideoPlayer;
import net.kunmc.lab.vplayer.common.model.Quad;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import static net.minecraft.client.Minecraft.IS_RUNNING_ON_MAC;
import static org.lwjgl.opengl.GL11.*;

public class VRendererClient {
    private Framebuffer framebuffer;
    private ResourceLocation loadingTexture = new ResourceLocation(VideoPlayer.MODID, "textures/loading.png");
    private ResourceLocation gradientTexture = new ResourceLocation(VideoPlayer.MODID, "textures/gradient.png");

    public Framebuffer getFramebuffer() {
        return framebuffer;
    }

    public void render(MatrixStack stack, Quad quad) {
        ActiveRenderInfo activeRenderInfo = Minecraft.getInstance().gameRenderer.getActiveRenderInfo();
        Vec3d view = activeRenderInfo.getProjectedView();

        framebuffer.bindFramebufferTexture();
        RenderSystem.disableCull();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        stack.push();
        stack.translate(-view.x, -view.y, -view.z);
        Matrix4f matrix = stack.getLast().getMatrix();
        bufferbuilder.pos(matrix, (float) quad.vertices[0].x, (float) quad.vertices[0].y, (float) quad.vertices[0].z).tex(0, 1).endVertex();    // left top
        bufferbuilder.pos(matrix, (float) quad.vertices[1].x, (float) quad.vertices[1].y, (float) quad.vertices[1].z).tex(0, 0).endVertex();    // left bottom
        bufferbuilder.pos(matrix, (float) quad.vertices[2].x, (float) quad.vertices[2].y, (float) quad.vertices[2].z).tex(1, 0).endVertex();    // right bottom
        bufferbuilder.pos(matrix, (float) quad.vertices[3].x, (float) quad.vertices[3].y, (float) quad.vertices[3].z).tex(1, 1).endVertex();    // right top
        bufferbuilder.finishDrawing();
        WorldVertexBufferUploader.draw(bufferbuilder);
        stack.pop();

        RenderSystem.enableCull();
        framebuffer.unbindFramebufferTexture();
    }

    public double getVolume(Quad quad) {
        ActiveRenderInfo activeRenderInfo = Minecraft.getInstance().gameRenderer.getActiveRenderInfo();
        Vec3d view = activeRenderInfo.getProjectedView();

        Vec3d dir = new Vec3d(activeRenderInfo.getViewVector());
        double dot = dir.normalize().dotProduct(quad.getCenter().subtract(view).normalize());

        GameSettings gameSettings = Minecraft.getInstance().gameSettings;
        double distance_min = quad.getNearestDistance(view);
        double distance = distance_min / (48.0 + 24.0 * (quad.getSize() - 1));
        double distance_clamped = MathHelper.clamp(distance, 0, 1);
        double distance_vol = Math.pow(1 - distance_clamped, 4) * MathHelper.clampedLerp(.5, 1, (1 + dot) / 2);
        return gameSettings.getSoundLevel(SoundCategory.MASTER) * gameSettings.getSoundLevel(SoundCategory.VOICE) * distance_vol;
    }

    public void destroy() {
        framebuffer.deleteFramebuffer();
    }

    public void initFbo(int _width, int _height) {
        framebuffer = new Framebuffer(_width, _height, true, IS_RUNNING_ON_MAC);
        framebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 1.0F);
    }

    public void clear() {
        framebuffer.framebufferClear(IS_RUNNING_ON_MAC);
    }

    public void initFrame() {
        glPushAttrib(GL_TRANSFORM_BIT);

        RenderSystem.matrixMode(GL_PROJECTION);
        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();
        RenderSystem.ortho(-1.0, 1.0, -1.0, 1.0, 1.0, -1.0);
        RenderSystem.matrixMode(GL_MODELVIEW);
        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();

        framebuffer.bindFramebuffer(true);

        RenderSystem.enableTexture();

        {
            float w = framebuffer.framebufferWidth;
            float h = framebuffer.framebufferHeight;
            float margin = .5f;

            float m = Math.min(w, h);
            float fw = w / m * margin;
            float fh = h / m * margin;
            float nw0 = (m / w - 1) - fw;
            float nw1 = (m / w - 1) + fw + w / m;
            float nh0 = (m / h - 1) - fh;
            float nh1 = (m / h - 1) + fh + h / m;

            TextureManager manager = Minecraft.getInstance().getTextureManager();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();

            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();

            {
                manager.bindTexture(gradientTexture);
                bufferbuilder.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX);

                float f = (System.currentTimeMillis() % 3000) / 3000f;
                float s = .1f;

                bufferbuilder.pos(-.8, .8, 0).tex(nw0 * s - f, nh0).endVertex();    // left top
                bufferbuilder.pos(-.8, -.8, 0).tex(nw0 * s - f, nh1).endVertex();   // left bottom
                bufferbuilder.pos(.8, -.8, 0).tex(nw1 * s - f, nh1).endVertex();    // right bottom
                bufferbuilder.pos(.8, .8, 0).tex(nw1 * s - f, nh0).endVertex();     // right top

                bufferbuilder.finishDrawing();
                WorldVertexBufferUploader.draw(bufferbuilder);
            }

            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL_ZERO, GL_SRC_COLOR);

            {
                manager.bindTexture(loadingTexture);
                bufferbuilder.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX);

                bufferbuilder.pos(-1, 1, 0).tex(nw0, nh0).endVertex();  // left top
                bufferbuilder.pos(-1, -1, 0).tex(nw0, nh1).endVertex(); // left bottom
                bufferbuilder.pos(1, -1, 0).tex(nw1, nh1).endVertex();  // right bottom
                bufferbuilder.pos(1, 1, 0).tex(nw1, nh0).endVertex();   // right top

                bufferbuilder.finishDrawing();
                WorldVertexBufferUploader.draw(bufferbuilder);
            }

            RenderSystem.disableBlend();
        }

        framebuffer.unbindFramebuffer();
        //Minecraft.getInstance().getFramebuffer().bindFramebuffer(true);

        RenderSystem.matrixMode(GL_MODELVIEW);
        RenderSystem.popMatrix();
        RenderSystem.matrixMode(GL_PROJECTION);
        RenderSystem.popMatrix();

        glPopAttrib();
    }

    public void updateFbo(int _width, int _height) {
        framebuffer.resize(_width, _height, true);
    }

    public static void beginRenderFrame() {
        RenderSystem.pushLightingAttributes();
        RenderSystem.pushTextureAttributes();
        RenderSystem.pushMatrix();
    }

    public static void endRenderFrame() {
        RenderSystem.popMatrix();
        RenderSystem.popAttributes();
        RenderSystem.popAttributes();

        glClearColor(0, 0, 0, 0);
        Minecraft.getInstance().getFramebuffer().bindFramebuffer(true);
    }
}
