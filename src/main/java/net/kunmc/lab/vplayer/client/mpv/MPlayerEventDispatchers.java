package net.kunmc.lab.vplayer.client.mpv;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import net.kunmc.lab.vplayer.common.util.EventDispatcher;
import net.kunmc.lab.vplayer.common.util.RepeatObservable;
import org.apache.commons.lang3.ArrayUtils;

import java.util.concurrent.CompletableFuture;

import static net.kunmc.lab.vplayer.client.mpv.MpvLibrary.mpv_format.*;

public class MPlayerEventDispatchers {
    private final long handle;

    public MPlayerEventDispatchers(long handle) {
        this.handle = handle;
    }

    public final MGetPropertyEventDispatcher dispatcherPropertyGet = new MGetPropertyEventDispatcher();
    public final MSetPropertyEventDispatcher dispatcherPropertySet = new MSetPropertyEventDispatcher();
    public final MCommandEventDispatcher dispatcherCommand = new MCommandEventDispatcher();
    public final MObservePropertyEventDispatcher dispatcherPropertyChange = new MObservePropertyEventDispatcher();

    public class MGetPropertyEventDispatcher extends EventDispatcher.CompletableEventDispatcher<Pointer> {
        private CompletableFuture<Pointer> getPropertyAsync(String name, int format) {
            if (handle == 0)
                return CompletableFuture.completedFuture(null);

            long id = generateId();
            MPlayer.mpv.mpv_get_property_async(handle, id, name, format);
            return onRequest(id);
        }

        public CompletableFuture<Double> getPropertyAsyncDouble(String name) {
            return getPropertyAsync(name, MPV_FORMAT_DOUBLE).thenApply(p -> p == null ? null : p.getDouble(0));
        }

        public CompletableFuture<Long> getPropertyAsyncLong(String name) {
            return getPropertyAsync(name, MPV_FORMAT_INT64).thenApply(p -> p == null ? null : p.getLong(0));
        }

        public CompletableFuture<Boolean> getPropertyAsyncBoolean(String name) {
            return getPropertyAsync(name, MPV_FORMAT_FLAG).thenApply(p -> p == null ? null : p.getInt(0) != 0);
        }
    }

    public class MSetPropertyEventDispatcher extends EventDispatcher.CompletableEventDispatcher<Void> {
        private CompletableFuture<Void> setPropertyAsync(String name, int format, Pointer data) {
            if (handle == 0)
                return CompletableFuture.completedFuture(null);

            long id = generateId();
            MPlayer.mpv.mpv_set_property_async(handle, id, name, format, data);
            return onRequest(id);
        }

        private DoubleByReference doubleRef = new DoubleByReference();
        private LongByReference longRef = new LongByReference();
        private IntByReference integerRef = new IntByReference();

        public CompletableFuture<Void> setPropertyAsyncDouble(String name, double data) {
            doubleRef.setValue(data);
            return setPropertyAsync(name, MPV_FORMAT_DOUBLE, doubleRef.getPointer());
        }

        public CompletableFuture<Void> setPropertyAsyncLong(String name, long data) {
            longRef.setValue(data);
            return setPropertyAsync(name, MPV_FORMAT_INT64, longRef.getPointer());
        }

        public CompletableFuture<Void> setPropertyAsyncBoolean(String name, boolean data) {
            integerRef.setValue(data ? 1 : 0);
            return setPropertyAsync(name, MPV_FORMAT_FLAG, integerRef.getPointer());
        }
    }

    public class MCommandEventDispatcher extends EventDispatcher.CompletableEventDispatcher<Void> {
        private CompletableFuture<Void> commandAsyncRaw(String[] args) {
            if (handle == 0)
                return CompletableFuture.completedFuture(null);

            long id = generateId();
            MPlayer.mpv.mpv_command_async(handle, id, args);
            return onRequest(id);
        }

        public CompletableFuture<Void> commandAsync(String... args) {
            return commandAsyncRaw(ArrayUtils.add(args, null));
        }
    }

    public class MObservePropertyEventDispatcher extends EventDispatcher.RepeatableEventDispatcher<Pointer> {
        private RepeatObservable<Pointer> observeAsync(String name, int format) {
            if (handle == 0)
                return new RepeatObservable<>();

            long id = generateId();
            MPlayer.mpv.mpv_observe_property(handle, id, name, format);
            return onRequest(id);
        }

        public RepeatObservable<Double> observeAsyncDouble(String name) {
            return observeAsync(name, MPV_FORMAT_DOUBLE).thenApply(p -> p == null ? null : p.getDouble(0));
        }

        public RepeatObservable<Long> observeAsyncLong(String name) {
            return observeAsync(name, MPV_FORMAT_INT64).thenApply(p -> p == null ? null : p.getLong(0));
        }

        public RepeatObservable<Boolean> observeAsyncBoolean(String name) {
            return observeAsync(name, MPV_FORMAT_FLAG).thenApply(p -> p == null ? null : p.getInt(0) != 0);
        }
    }
}
