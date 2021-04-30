package net.kunmc.lab.vplayer.common.data;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.minecraft.util.math.vector.Vector3d;

import java.io.IOException;

public class Vector3dTypeAdaptor extends TypeAdapter<Vector3d> {
    @Override
    public void write(JsonWriter out, Vector3d value) throws IOException {
        out.beginObject();
        out.name("x").value(value.x);
        out.name("y").value(value.y);
        out.name("z").value(value.z);
        out.endObject();
    }

    @Override
    public Vector3d read(JsonReader in) throws IOException {
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
        return new Vector3d(x, y, z);
    }
}
