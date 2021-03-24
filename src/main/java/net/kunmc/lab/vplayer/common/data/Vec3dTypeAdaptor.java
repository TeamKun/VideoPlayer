package net.kunmc.lab.vplayer.common.data;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.minecraft.util.math.Vec3d;

import java.io.IOException;

public class Vec3dTypeAdaptor extends TypeAdapter<Vec3d> {
    @Override
    public void write(JsonWriter out, Vec3d value) throws IOException {
        out.beginObject();
        out.name("x").value(value.x);
        out.name("y").value(value.y);
        out.name("z").value(value.z);
        out.endObject();
    }

    @Override
    public Vec3d read(JsonReader in) throws IOException {
        double x = 0, y = 0, z = 0;
        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "x":
                    x = in.nextDouble();
                    break;
                case "y":
                    y = in.nextDouble();
                    break;
                case "z":
                    z = in.nextDouble();
                    break;
            }
        }
        in.endObject();
        return new Vec3d(x, y, z);
    }
}
