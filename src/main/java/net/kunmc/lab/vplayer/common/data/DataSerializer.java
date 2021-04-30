package net.kunmc.lab.vplayer.common.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class DataSerializer {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(new TypeToken<RegistryKey<World>>() {
            }.getType(), new DimensionTypeAdaptor())
            .registerTypeAdapter(Vector3d.class, new Vector3dTypeAdaptor())
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
