package net.kunmc.lab.vplayer.video;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.kunmc.lab.vplayer.VideoPlayer;
import net.kunmc.lab.vplayer.model.Quad;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import static net.minecraft.client.Minecraft.IS_RUNNING_ON_MAC;
import static org.lwjgl.opengl.GL11.*;

public class VRenderer {
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
        bufferbuilder.pos(matrix, (float) quad.vertices[0].x, (float) quad.vertices[0].y, (float) quad.vertices[0].z).tex(0, 1).endVertex();
        bufferbuilder.pos(matrix, (float) quad.vertices[1].x, (float) quad.vertices[1].y, (float) quad.vertices[1].z).tex(1, 1).endVertex();
        bufferbuilder.pos(matrix, (float) quad.vertices[2].x, (float) quad.vertices[2].y, (float) quad.vertices[2].z).tex(1, 0).endVertex();
        bufferbuilder.pos(matrix, (float) quad.vertices[3].x, (float) quad.vertices[3].y, (float) quad.vertices[3].z).tex(0, 0).endVertex();
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
        RenderSystem.disableBlend();

        {
            Minecraft.getInstance().getTextureManager().bindTexture(gradientTexture);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            bufferbuilder.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX);

            float f = (System.currentTimeMillis() % 1000) / 1000f;

            bufferbuilder.pos(-1, 1, 0).tex(f + 0, 0).endVertex();
            bufferbuilder.pos(-1, -1, 0).tex(f + 0, 1).endVertex();
            bufferbuilder.pos(1, -1, 0).tex(f + 1, 1).endVertex();
            bufferbuilder.pos(1, 1, 0).tex(f + 1, 0).endVertex();

            bufferbuilder.finishDrawing();
            WorldVertexBufferUploader.draw(bufferbuilder);
        }

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL_ZERO, GL_SRC_COLOR);

        {
            Minecraft.getInstance().getTextureManager().bindTexture(loadingTexture);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            bufferbuilder.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX);

            bufferbuilder.pos(-1, 1, 0).tex(0, 0).endVertex();
            bufferbuilder.pos(-1, -1, 0).tex(0, 1).endVertex();
            bufferbuilder.pos(1, -1, 0).tex(1, 1).endVertex();
            bufferbuilder.pos(1, 1, 0).tex(1, 0).endVertex();

            bufferbuilder.finishDrawing();
            WorldVertexBufferUploader.draw(bufferbuilder);
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
