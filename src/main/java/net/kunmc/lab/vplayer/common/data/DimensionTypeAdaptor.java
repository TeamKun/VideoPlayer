package net.kunmc.lab.vplayer.common.data;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.io.IOException;

public class DimensionTypeAdaptor extends TypeAdapter<RegistryKey<World>> {
    @Override
    public void write(JsonWriter out, RegistryKey<World> value) throws IOException {
        out.beginObject();
        out.name("id").value(value.getRegistryName());
        out.endObject();
    }

    @Override
    public RegistryKey<World> read(JsonReader in) throws IOException {
        in.beginObject();
        in.nextName();
        int id = in.nextInt();
        in.endObject();
        return DimensionType.getById(id);
    }
}
