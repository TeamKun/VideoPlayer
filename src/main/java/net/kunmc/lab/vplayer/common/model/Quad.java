package net.kunmc.lab.vplayer.common.model;

import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.Comparator;

public class Quad {
    public final RegistryKey<World> dimension;
    public final Vector3d[] vertices;

    public Quad(RegistryKey<World> dimensionIn, Vector3d... verticesIn) {
        Validate.validIndex(verticesIn, 3, "VQuad needs 4 vertices");
        dimension = dimensionIn;
        vertices = verticesIn;
    }

    public Vector3d getCenter() {
        return Arrays.stream(vertices).reduce((a, b) -> a.add(b).scale(.5)).orElse(Vector3d.ZERO);
    }

    public double getSize() {
        return Arrays.stream(vertices).findFirst().flatMap(p -> Arrays.stream(vertices).skip(1).map(p::distanceTo).min(Comparator.naturalOrder())).orElse(Double.MAX_VALUE);
    }

    public double getNearestDistance(Vector3d from) {
        return Arrays.stream(vertices).map(from::distanceTo).min(Comparator.naturalOrder()).orElse(Double.MAX_VALUE);
    }
}
