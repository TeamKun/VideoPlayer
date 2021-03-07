package net.kunmc.lab.vplayer.mpv;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class MEventDispatcher<T> {
    private AtomicLong lastId = new AtomicLong();
    private Map<Long, CompletableFuture<T>> futures = new ConcurrentHashMap<>();

    protected long generateId() {
        return lastId.getAndIncrement();
    }

    protected CompletableFuture<T> onRequest(long id) {
        CompletableFuture<T> future = new CompletableFuture<>();
        futures.put(id, future);
        return future;
    }

    public void onReply(long id, T data) {
        CompletableFuture<T> future = futures.remove(id);
        if (future != null)
            future.complete(data);
    }
}
