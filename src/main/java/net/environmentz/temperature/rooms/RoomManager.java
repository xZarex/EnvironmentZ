package net.environmentz.temperature.rooms;

import net.environmentz.EnvironmentzMain;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class RoomManager {
    private static RoomManager instance = null;
    private HashMap<String,ArrayList<AirBubble>> worldAirBubbles = new HashMap<>();
    private static AirBubbleFinder finder = new AirBubbleFinder();
    private HashMap<String, Integer> nextTickWorld = new HashMap<>();
    private MinecraftServer server;

    RoomManager(MinecraftServer server) {
        this.server = server;
        load("environmentz.roommanager", worldAirBubbles);
    }

    public static RoomManager getInstance(MinecraftServer server) {
        if (instance == null)
            instance = new RoomManager(server);
        return instance;
    }

    /// File Management
    public void save(String filename, HashMap<String, ArrayList<AirBubble>> hashMap)
    {
        Path path = server.getSavePath(WorldSavePath.ROOT);
        Path configPath = path.resolve( filename ).toAbsolutePath();
        try {
            Files.deleteIfExists(configPath);
            Files.createFile(configPath);
            ObjectOutputStream oos = null;
            FileOutputStream fout = null;
            try {
                File file = new File(configPath.toString());
                fout = new FileOutputStream(file);
                oos = new ObjectOutputStream(fout);

                oos.writeObject(hashMap);
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if(oos != null){
                    oos.close();
                }
            }

        } catch (Exception e) {
            EnvironmentzMain.LOGGER.error("Could not save data file! "+e.toString());
        }
    }

    public void load(String filename, HashMap<String, ArrayList<AirBubble>> hashMap)
    {
        Path path = server.getSavePath(WorldSavePath.ROOT);
        Path configPath = path.resolve( filename ).toAbsolutePath();
        try {
            ObjectInputStream objectinputstream = null;
            try {
                File file = new File(configPath.toString());
                FileInputStream streamIn = new FileInputStream(file);
                objectinputstream = new ObjectInputStream(streamIn);
                hashMap.putAll ((HashMap<String, ArrayList<AirBubble>>)objectinputstream.readObject());
            } catch (Exception e) {
                EnvironmentzMain.LOGGER.warn("Could not load data file, maybe there are no airbubbles" + e.toString());
            } finally {
                if(objectinputstream != null){
                    objectinputstream .close();
                }
            }
        } catch (Exception ex2) {
            EnvironmentzMain.LOGGER.warn("Could not load data file, maybe there are no airbubbles" + ex2.toString());
        }
    }


    // Logic

    public void tick(World world)
    {
        String worldName = world.getRegistryKey().toString();
        if (!nextTickWorld.containsKey(worldName)) {
            nextTickWorld.put(worldName, 100);
        }

        int currentTick = nextTickWorld.get(worldName) - 1;
        nextTickWorld.put(worldName, currentTick);

        if (currentTick <= 0 && !world.isClient() && worldAirBubbles.containsKey(worldName))
        {
            boolean needsSave = false;
            Set<AirBubble> toRemove = new HashSet<>();
            HashMap<String, HeatSource> invalidatedHeatSources = new HashMap<>();

            for (AirBubble existingBubble : worldAirBubbles.get(worldName)) {
                AirBubble newBubble = find(existingBubble.x, existingBubble.y, existingBubble.z, world, false);
                if (newBubble == null) {
                    toRemove.add(existingBubble);
                    needsSave = true;
                    continue;
                }

                if (!newBubble.hash.equals(existingBubble.hash)) {
                    invalidatedHeatSources.putAll(existingBubble.heatSources);
                    toRemove.add(existingBubble);
                    needsSave = true;
                } else {
                    int heat = 0;
                    Set<String> heatSourceToRemove = new HashSet<>();

                    // Our airbubble didn't change; we can validate heatsources here and adapt their heat
                    for (Map.Entry<String, HeatSource> heatSource : existingBubble.heatSources.entrySet()) {
                        heatSource.getValue().nextUpdate -= 1;
                        if (heatSource.getValue().nextUpdate <= 0) {
                            heatSource.getValue().nextUpdate = 10;
                            // if our chunk is loaded we can validate the heatsource
                            if (world.isChunkLoaded(ChunkSectionPos.getSectionCoord(heatSource.getValue().x), ChunkSectionPos.getSectionCoord(heatSource.getValue().z))) {
                                BlockState blockState = world.getBlockState(new BlockPos(heatSource.getValue().x, heatSource.getValue().y, heatSource.getValue().z));
                                if (blockState.getBlock() instanceof AbstractFurnaceBlock) {
                                    if (blockState.get(AbstractFurnaceBlock.LIT)) {
                                        heatSource.getValue().valid = true;
                                        if (heatSource.getValue().currentHeat < heatSource.getValue().maxHeat) {
                                            needsSave = true;
                                            heatSource.getValue().currentHeat += 1;
                                        }

                                        heat += heatSource.getValue().currentHeat;
                                    } else {
                                        heatSource.getValue().valid = false;
                                        if (heatSource.getValue().currentHeat > 0) {
                                            heatSource.getValue().currentHeat -= 1;
                                            heat += heatSource.getValue().currentHeat;
                                            needsSave = true;
                                        } else {
                                            heatSourceToRemove.add(heatSource.getKey());
                                        }
                                    }
                                } else {
                                    heatSource.getValue().valid = false;
                                    if (heatSource.getValue().currentHeat > 0) {
                                        heatSource.getValue().currentHeat -= 1;
                                        heat += heatSource.getValue().currentHeat;
                                    } else {
                                        heatSourceToRemove.add(heatSource.getKey());
                                    }
                                    needsSave = true;
                                }
                            } else {
                                heat += heatSource.getValue().currentHeat;
                            }
                        } else {
                            heat += heatSource.getValue().currentHeat;
                        }
                    }

                    for (String heatSource : heatSourceToRemove) {
                        existingBubble.heatSources.remove(heatSource);
                        needsSave = true;
                    }

                    existingBubble.heat = heat;
                }
            }

            for (AirBubble airBubble: toRemove) {
                worldAirBubbles.get(worldName).remove(airBubble);
            }

            for (Map.Entry<String, HeatSource> entry : invalidatedHeatSources.entrySet()) {
                addHeatSource(entry.getValue().x, entry.getValue().y, entry.getValue().z, world, entry.getValue().maxHeat, entry.getValue().currentHeat);
            }


            nextTickWorld.put(worldName, 20*5);
            if (needsSave)
                save("environmentz.roommanager", worldAirBubbles);
        }
    }

    private AirBubble find(int x, int y, int z, World world, boolean addUnknown)
    {
        String worldName = world.getRegistryKey().toString();
        AirBubble airBubble = finder.getAirBubble(x, y, z, world);
        if (airBubble == null)
            return null;

        if (worldAirBubbles.containsKey(worldName)) {
            for (AirBubble existingBubble : worldAirBubbles.get(worldName)) {
                if (existingBubble.hash.equals(airBubble.hash))
                    return existingBubble;
            }

        }

        if (addUnknown) {
            if (!worldAirBubbles.containsKey(worldName))
                worldAirBubbles.put(worldName, new ArrayList<AirBubble>());

            worldAirBubbles.get(worldName).add(airBubble);
        }
        return airBubble;
    }

    private AirBubble getAirBubble(int x, int y, int z, World world) {
        String worldName = world.getRegistryKey().toString();

        if (worldAirBubbles.containsKey(worldName)) {
            for (AirBubble airBubble: worldAirBubbles.get(worldName)) {
                if (Distance.get(x, y, z, airBubble.x, airBubble.y, airBubble.z) <= 100) {
                    if (airBubble.blocks != null) {
                        if (airBubble.blocks.contains(AirBubbleFinder.getKey(x, y, z))) {
                            return airBubble;
                        }
                    }
                }
            }
        }


        return null;
    }

    public int getHeat(int x, int y, int z, World world) {
        AirBubble airBubble = getAirBubble(x, y, z, world);
        if (airBubble != null) {
            return airBubble.heat;
        }

        return 0;
    }

    public void addHeatSource(int x, int y, int z, World world, int maxHeat, int currentHeat) {
        AirBubble airBubble = getAirBubble(x, y, z, world);
        if (airBubble == null)
            airBubble = find(x, y, z, world, true);

        if (airBubble != null) {
            String key = AirBubbleFinder.getKey(x, y, z);

            airBubble.heatSources.remove(key);

            HeatSource heatSource = new HeatSource();
            heatSource.currentHeat = currentHeat;
            heatSource.maxHeat = maxHeat;
            heatSource.key = key;
            heatSource.x = x;
            heatSource.y = y;
            heatSource.z = z;
            heatSource.valid = true;
            heatSource.nextUpdate = 10;

            airBubble.heatSources.put(key, heatSource);
            airBubble.heat += currentHeat;

            save("environmentz.roommanager", worldAirBubbles);
        }
    }
}
