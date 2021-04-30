package net.kunmc.lab.vplayer.common.data;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.io.IOException;

public class DimensionTypeAdaptor extends TypeAdapter<RegistryKey<World>> {
    @Override
    public void write(JsonWriter out, RegistryKey<World> value) throws IOException {
        if (!Registry.DIMENSION_REGISTRY.location().equals(value.getRegistryName())) {
            out.nullValue();
            return;
        }
        out.beginObject();
        if (World.OVERWORLD.equals(value)) {
            out.name("id").value(0);
        } else if (World.NETHER.equals(value)) {
            out.name("id").value(-1);
        } else if (World.END.equals(value)) {
            out.name("id").value(1);
        }
        out.name("name").value(value.location().toString());
        out.endObject();
    }

    @Override
    public RegistryKey<World> read(JsonReader in) throws IOException {
        in.beginObject();
        String name = null;
        int id = 0;
        while (in.hasNext()) {
            String type = in.nextName();
            switch (type) {
                case "name":
                    name = in.nextString();
                    break;
                case "id":
                    id = in.nextInt();
                    break;
            }
        }
        in.endObject();
        if (name == null) {
            switch (id) {
                default:
                case 0:
                    return World.OVERWORLD;
                case -1:
                    return World.NETHER;
                case 1:
                    return World.END;
            }
        }
        return RegistryKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(name));
    }
}
