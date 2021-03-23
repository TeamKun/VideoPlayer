package net.kunmc.lab.vplayer.client.video;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.kunmc.lab.vplayer.VideoPlayer;
import net.kunmc.lab.vplayer.common.model.Quad;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;

import static net.minecraft.client.Minecraft.ON_OSX;
import static org.lwjgl.opengl.GL11.*;

public class VRendererClient {
    private Framebuffer framebuffer;
    private ResourceLocation loadingTexture = new ResourceLocation(VideoPlayer.MODID, "textures/loading.png");
    private ResourceLocation gradientTexture = new ResourceLocation(VideoPlayer.MODID, "textures/gradient.png");

    public Framebuffer getFramebuffer() {
        return framebuffer;
    }

    public void render(MatrixStack stack, Quad quad) {
        ActiveRenderInfo activeRenderInfo = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vector3d view = activeRenderInfo.getPosition();

        framebuffer.bindRead();
        RenderSystem.disableCull();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuilder();
        bufferbuilder.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        stack.pushPose();
        stack.translate(-view.x, -view.y, -view.z);
        Matrix4f matrix = stack.last().pose();
        bufferbuilder.vertex(matrix, (float) quad.vertices[0].x, (float) quad.vertices[0].y, (float) quad.vertices[0].z).uv(0, 1).endVertex();    // left top
        bufferbuilder.vertex(matrix, (float) quad.vertices[1].x, (float) quad.vertices[1].y, (float) quad.vertices[1].z).uv(0, 0).endVertex();    // left bottom
        bufferbuilder.vertex(matrix, (float) quad.vertices[2].x, (float) quad.vertices[2].y, (float) quad.vertices[2].z).uv(1, 0).endVertex();    // right bottom
        bufferbuilder.vertex(matrix, (float) quad.vertices[3].x, (float) quad.vertices[3].y, (float) quad.vertices[3].z).uv(1, 1).endVertex();    // right top
        bufferbuilder.end();
        WorldVertexBufferUploader.end(bufferbuilder);
        stack.popPose();

        RenderSystem.enableCull();
        framebuffer.unbindRead();
    }

    public double getVolume(Quad quad) {
        ActiveRenderInfo activeRenderInfo = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vector3d view = activeRenderInfo.getPosition();

        Vector3d dir = new Vector3d(activeRenderInfo.getLookVector());
        double dot = dir.normalize().dot(quad.getCenter().subtract(view).normalize());

        GameSettings gameSettings = Minecraft.getInstance().options;
        double distance_min = quad.getNearestDistance(view);
        double distance = distance_min / (48.0 + 24.0 * (quad.getSize() - 1));
        double distance_clamped = MathHelper.clamp(distance, 0, 1);
        double distance_vol = Math.pow(1 - distance_clamped, 4) * MathHelper.clampedLerp(.5, 1, (1 + dot) / 2);
        return gameSettings.getSoundSourceVolume(SoundCategory.MASTER) * gameSettings.getSoundSourceVolume(SoundCategory.VOICE) * distance_vol;
    }

    public void destroy() {
        framebuffer.destroyBuffers();
    }

    public void initFbo(int _width, int _height) {
        framebuffer = new Framebuffer(_width, _height, true, ON_OSX);
        framebuffer.setClearColor(0.0F, 0.0F, 0.0F, 1.0F);
    }

    public void clear() {
        framebuffer.clear(ON_OSX);
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

        framebuffer.bindWrite(true);

        RenderSystem.enableTexture();

        {
            float w = framebuffer.width;
            float h = framebuffer.height;
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
            BufferBuilder bufferbuilder = tessellator.getBuilder();

            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();

            {
                manager.bind(gradientTexture);
                bufferbuilder.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX);

                float f = (System.currentTimeMillis() % 3000) / 3000f;
                float s = .1f;

                bufferbuilder.vertex(-.8, .8, 0).uv(nw0 * s - f, nh0).endVertex();    // left top
                bufferbuilder.vertex(-.8, -.8, 0).uv(nw0 * s - f, nh1).endVertex();   // left bottom
                bufferbuilder.vertex(.8, -.8, 0).uv(nw1 * s - f, nh1).endVertex();    // right bottom
                bufferbuilder.vertex(.8, .8, 0).uv(nw1 * s - f, nh0).endVertex();     // right top

                bufferbuilder.end();
                WorldVertexBufferUploader.end(bufferbuilder);
            }

            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL_ZERO, GL_SRC_COLOR);

            {
                manager.bind(loadingTexture);
                bufferbuilder.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX);

                bufferbuilder.vertex(-1, 1, 0).uv(nw0, nh0).endVertex();  // left top
                bufferbuilder.vertex(-1, -1, 0).uv(nw0, nh1).endVertex(); // left bottom
                bufferbuilder.vertex(1, -1, 0).uv(nw1, nh1).endVertex();  // right bottom
                bufferbuilder.vertex(1, 1, 0).uv(nw1, nh0).endVertex();   // right top

                bufferbuilder.end();
                WorldVertexBufferUploader.end(bufferbuilder);
            }

            RenderSystem.disableBlend();
        }

        framebuffer.unbindWrite();
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
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
    }
}
