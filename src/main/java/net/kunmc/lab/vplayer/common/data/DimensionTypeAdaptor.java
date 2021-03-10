package net.kunmc.lab.vplayer.common.data;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.minecraft.world.dimension.DimensionType;

import java.io.IOException;

public class DimensionTypeAdaptor extends TypeAdapter<DimensionType> {
    @Override
    public void write(JsonWriter out, DimensionType value) throws IOException {
        out.beginObject();
        out.name("id").value(value.getId());
        out.endObject();
    }

    @Override
    public DimensionType read(JsonReader in) throws IOException {
        in.beginObject();
        in.nextName();
        int id = in.nextInt();
        in.endObject();
        return DimensionType.getById(id);
    }
}
