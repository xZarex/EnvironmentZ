package net.environmentz.temperature.rooms;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

public class AirBubble implements Serializable {
    // TODO: add world, air bubbles are world depended
    public int size;
    public String hash;
    public int x;
    public int y;
    public int z;
    public HashSet<String> blocks;
    public HashMap<String, HeatSource> heatSources;
    public int heat;
}