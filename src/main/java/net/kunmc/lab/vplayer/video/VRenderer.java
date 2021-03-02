package net.kunmc.lab.vplayer.video;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.kunmc.lab.vplayer.model.Quad;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import static net.minecraft.client.Minecraft.IS_RUNNING_ON_MAC;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glClearColor;

public class VRenderer {
    private Framebuffer framebuffer;

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
        framebuffer.setFramebufferColor(0.0F, 1.0F, 0.0F, 1.0F);
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
