package net.kunmc.lab.vplayer.common.util;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public abstract class EventDispatcher<T> {
    private AtomicLong lastId = new AtomicLong();

    protected long generateId() {
        return lastId.getAndIncrement();
    }

    public abstract void onReply(long id, T data);

    public static class CompletableEventDispatcher<T> extends EventDispatcher<T> {
        private Map<Long, CompletableFuture<T>> futures = new ConcurrentHashMap<>();

        protected CompletableFuture<T> onRequest(long id) {
            CompletableFuture<T> future = new CompletableFuture<>();
            futures.put(id, future);
            return future;
        }

        @Override
        public void onReply(long id, T data) {
            CompletableFuture<T> future = futures.remove(id);
            if (future != null)
                future.complete(data);
        }
    }

    public static class RepeatableEventDispatcher<T> extends EventDispatcher<T> {
        private Map<Long, RepeatObservable<T>> futures = new ConcurrentHashMap<>();

        protected RepeatObservable<T> onRequest(long id) {
            RepeatObservable<T> future = new RepeatObservable<>();
            futures.put(id, future);
            return future;
        }

        @Override
        public void onReply(long id, T data) {
            RepeatObservable<T> future = futures.get(id);
            if (future != null)
                future.fire(data);
        }
    }
}
