package net.kunmc.lab.vplayer.client.video;

public interface VEventHandlerClient {
    void onResize(int width, int height);
    void onBeforeRender();
    void onLoaded();
}
