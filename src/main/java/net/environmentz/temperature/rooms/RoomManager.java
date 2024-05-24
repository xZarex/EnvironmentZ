package net.environmentz.temperature.rooms;

import net.environmentz.EnvironmentzMain;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.world.World;

import java.util.*;

public class RoomManager {
    private static RoomManager instance = null;
    private ArrayList<AirBubbleFinder.AirBubble> airBubbles = new ArrayList<>();
    private static AirBubbleFinder finder = new AirBubbleFinder();
    //TODO: nextTick via HashMap for worlds
    private int nextTick = 10;

    RoomManager() {}

    public static RoomManager getInstance() {
        if (instance == null)
            instance = new RoomManager();
        return instance;
    }

    public void tick(World world)
    {
        nextTick--;
        //EnvironmentzMain.LOGGER.error("got tick of world {}", world.getRegistryKey().toString());
        if (nextTick <= 0 && world.getRegistryKey().toString().equals("ResourceKey[minecraft:dimension / minecraft:overworld]") && !world.isClient())
        {
            EnvironmentzMain.LOGGER.error("ticking world to check airbubbles");
            Set<AirBubbleFinder.AirBubble> toRemove = new HashSet<>();
            HashMap<String, Integer> invalidatedHeatSources = new HashMap<>();

            for (AirBubbleFinder.AirBubble existingBubble : airBubbles) {
                AirBubbleFinder.AirBubble newBubble = find(existingBubble.x, existingBubble.y, existingBubble.z, world, false);
                if (newBubble == null) {
                    toRemove.add(existingBubble);
                    continue;
                }

                if (!newBubble.hash.equals(existingBubble.hash)) {
                    invalidatedHeatSources.putAll(existingBubble.heatSources);
                    toRemove.add(existingBubble);
                }
            }

            for (AirBubbleFinder.AirBubble airBubble: toRemove) {
                airBubbles.remove(airBubble);
            }

            for (Map.Entry<String, Integer> entry : invalidatedHeatSources.entrySet()) {
                String[] keys = entry.getKey().split(",");
                int x = Integer.parseInt(keys[0]);
                int y = Integer.parseInt(keys[1]);
                int z = Integer.parseInt(keys[2]);
                addHeatSource(x, y, z, world, entry.getValue());
            }

            nextTick = 20*5;
        }
    }

    private AirBubbleFinder.AirBubble find(int x, int y, int z, World world, boolean addUnknown)
    {
        AirBubbleFinder.AirBubble airBubble = finder.getAirBubble(x, y, z, world);
        if (airBubble == null)
            return null;

        for (AirBubbleFinder.AirBubble existingBubble : airBubbles) {
            if (existingBubble.hash.equals(airBubble.hash))
                return existingBubble;
        }

        if (addUnknown)
            airBubbles.add(airBubble);
        return airBubble;
    }

    private AirBubbleFinder.AirBubble getAirBubble(int x, int y, int z) {
        for (AirBubbleFinder.AirBubble airBubble: airBubbles) {
            if (Distance.get(x, y, z, airBubble.x, airBubble.y, airBubble.z) <= 100) {
                if (airBubble.blocks != null) {
                    if (airBubble.blocks.contains(AirBubbleFinder.getKey(x, y, z))) {
                        return airBubble;
                    }
                }
            }
        }

        return null;
    }

    public int getHeat(int x, int y, int z, World world) {
        AirBubbleFinder.AirBubble airBubble = getAirBubble(x, y, z);
        if (airBubble != null) {
            return airBubble.heat;
        }

        return 0;
    }

    public void addHeatSource(int x, int y, int z, World world, int heat) {
        AirBubbleFinder.AirBubble airBubble = getAirBubble(x, y, z);
        if (airBubble == null)
            airBubble = find(x, y, z, world, true);

        if (airBubble != null) {
            String key = AirBubbleFinder.getKey(x, y, z);
            airBubble.heatSources.put(key, heat);
            airBubble.heat += heat;

            EnvironmentzMain.LOGGER.error("Added heatsource (key: {} heat: {}) to airbubble {}", key, heat, airBubble.hash);
        }
    }

    public void removeHeatSource(int x, int y, int z, World world) {
        AirBubbleFinder.AirBubble airBubble = getAirBubble(x, y, z);

        if (airBubble != null) {
            String key = AirBubbleFinder.getKey(x, y, z);
            if (airBubble.heatSources.containsKey(key)) {
                int heat = airBubble.heatSources.get(key);
                airBubble.heat -= heat;
            }
            airBubble.heatSources.remove(key);

            if (airBubble.heatSources.isEmpty()) {
                airBubbles.remove(airBubble);
                EnvironmentzMain.LOGGER.error("Deleted airbubble {}", airBubble.hash);
            }
            EnvironmentzMain.LOGGER.error("Remove heatsource (key: {}) from airbubble {}", key, airBubble.hash);
        }
    }
}
