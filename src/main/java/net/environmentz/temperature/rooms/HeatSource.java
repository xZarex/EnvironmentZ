package net.environmentz.temperature.rooms;

import java.io.Serializable;

public class HeatSource implements Serializable {
    int maxHeat;
    int currentHeat;
    boolean valid;
    String key;
    int x;
    int y;
    int z;
    int nextUpdate;
}
