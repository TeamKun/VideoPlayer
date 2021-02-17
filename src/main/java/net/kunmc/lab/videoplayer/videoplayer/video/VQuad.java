package net.kunmc.lab.videoplayer.videoplayer.video;

import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.Comparator;

public class VQuad {
    public final Vec3d[] vertices;

    public VQuad(Vec3d... verticesIn) {
        Validate.validIndex(verticesIn, 3, "VQuad needs 4 vertices");
        vertices = verticesIn;
    }

    public Vec3d getCenter() {
        return Arrays.stream(vertices).reduce((a, b) -> a.add(b).scale(.5)).orElse(Vec3d.ZERO);
    }

    public double getSize() {
        return Arrays.stream(vertices).findFirst().flatMap(p -> Arrays.stream(vertices).skip(1).map(p::distanceTo).min(Comparator.naturalOrder())).orElse(Double.MAX_VALUE);
    }

    public double getNearestDistance(Vec3d from) {
        return Arrays.stream(vertices).map(from::distanceTo).min(Comparator.naturalOrder()).orElse(Double.MAX_VALUE);
    }
}
