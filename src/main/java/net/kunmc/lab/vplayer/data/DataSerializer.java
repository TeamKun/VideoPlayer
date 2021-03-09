package net.kunmc.lab.vplayer.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.world.dimension.DimensionType;

import javax.annotation.Nullable;

public class DataSerializer {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(DimensionType.class, new DimensionTypeAdaptor())
            .create();

    public static String encode(Object object) {
        return gson.toJson(object);
    }

    @Nullable
    public static <T> T decode(String json, Class<T> classOfT) {
        try {
            return gson.fromJson(json, classOfT);
        } catch (Exception e) {
            return null;
        }
    }
}
