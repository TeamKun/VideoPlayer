package net.kunmc.lab.vplayer.mpv;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import org.apache.commons.lang3.ArrayUtils;

import java.util.concurrent.CompletableFuture;

import static net.kunmc.lab.vplayer.mpv.MpvLibrary.mpv_format.*;

public class MPlayerEventDispatchers {
    private final long handle;

    public MPlayerEventDispatchers(long handle) {
        this.handle = handle;
    }

    public final MGetPropertyEventDispatcher dispatcherPropertyGet = new MGetPropertyEventDispatcher();
    public final MSetPropertyEventDispatcher dispatcherPropertySet = new MSetPropertyEventDispatcher();
    public final MCommandEventDispatcher dispatcherCommand = new MCommandEventDispatcher();

    public class MGetPropertyEventDispatcher extends MEventDispatcher<Pointer> {
        private CompletableFuture<Pointer> getPropertyAsync(String name, int format) {
            long id = generateId();
            MPlayer.mpv.mpv_get_property_async(handle, id, name, format);
            return onRequest(id);
        }

        public CompletableFuture<Double> getPropertyAsyncDouble(String name) {
            return getPropertyAsync(name, MPV_FORMAT_DOUBLE).thenApply(p -> p.getDouble(0));
        }

        public CompletableFuture<Long> getPropertyAsyncLong(String name) {
            return getPropertyAsync(name, MPV_FORMAT_INT64).thenApply(p -> p.getLong(0));
        }

        public CompletableFuture<Boolean> getPropertyAsyncBoolean(String name) {
            return getPropertyAsync(name, MPV_FORMAT_FLAG).thenApply(p -> p.getInt(0) != 0);
        }
    }

    public class MSetPropertyEventDispatcher extends MEventDispatcher<Void> {
        private CompletableFuture<Void> setPropertyAsync(String name, int format, Pointer data) {
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

    public class MCommandEventDispatcher extends MEventDispatcher<Void> {
        private CompletableFuture<Void> commandAsyncRaw(String[] args) {
            long id = generateId();
            MPlayer.mpv.mpv_command_async(handle, id, args);
            return onRequest(id);
        }

        public CompletableFuture<Void> commandAsync(String... args) {
            return commandAsyncRaw(ArrayUtils.add(args, null));
        }
    }
}
