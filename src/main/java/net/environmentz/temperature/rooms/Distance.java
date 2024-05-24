package net.environmentz.temperature.rooms;

public class Distance {
    public static double get(double x1, double y1, double z1, double x2, double y2, double z2)
    {
        double deltaX = x2 - x1;
        double deltaY = y2 - y1;
        double deltaZ = z2 - z1;
        double distanceSquared = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
        return Math.sqrt(distanceSquared);
    }

    public static boolean isGreater(double x1, double y1, double z1, double x2, double y2, double z2, int maxDistance) {
        return get(x1, y1, z1, x2, y2, z2) > maxDistance;
    }

}
