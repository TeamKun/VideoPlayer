package net.kunmc.lab.videoplayer.mpv;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class mpv_event extends Structure {
    public int event_id;
    public int error;
    public long reply_userdata;
    public Pointer data;

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("event_id", "error", "reply_userdata", "data");
    }
}
