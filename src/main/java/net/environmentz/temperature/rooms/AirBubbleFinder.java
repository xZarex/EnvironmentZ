package net.environmentz.temperature.rooms;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.security.MessageDigest;
import java.util.*;

public class AirBubbleFinder {
    public class AirBubble {
        // TODO: add world, air bubbles are world depended
        public int size;
        public String hash;
        public int x;
        public int y;
        public int z;
        public Set<String> blocks;
        public HashMap<String, Integer> heatSources;
        public int heat;
    }

    private static final int MAX_AIR_BUBBLE_SIZE = 100 * 100 * 100;

    public AirBubble getAirBubble(int x, int y, int z, World grid) {
        Set<String> visited = new HashSet<>();
        Set<String> airBubble = new HashSet<>();

        if (!exploreAirBubble(x, y, z, grid, visited, airBubble, x, y, z))
            return null;

        AirBubble ret = new AirBubbleFinder.AirBubble();
        ret.size = airBubble.size();
        ret.hash = generateHash(airBubble);
        ret.x = x;
        ret.y = y;
        ret.z = z;
        ret.blocks = airBubble;
        ret.heatSources = new HashMap<>();
        return ret;
    }

    private static String generateHash(Set<String> airBubble) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            List<String> sortedBubble = new ArrayList<>(airBubble);
            Collections.sort(sortedBubble);
            for (String coord : sortedBubble) {
                digest.update(coord.getBytes());
            }
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            // Fallback
            List<String> sortedBubble = new ArrayList<>(airBubble);
            Collections.sort(sortedBubble);
            return Integer.toString(Arrays.hashCode(sortedBubble.toArray()));
        }
    }



    private static boolean exploreAirBubble(int startX, int startY, int startZ, World grid, Set<String> visited, Set<String> airBubble, int x, int y, int z) {
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{x, y, z});
        boolean isFirstIteration = true;

        while (!queue.isEmpty()) {
            int[] pos = queue.poll();
            x = pos[0];
            y = pos[1];
            z = pos[2];

            if (Distance.isGreater(x, y, z, startX, startY, startZ, 100))
                return false;

            String key = getKey(x, y, z);
            if (!isFirstIteration && (visited.contains(key) || !isValidCell(x, y, z, grid)))
                continue;

            isFirstIteration = false;

            visited.add(key);

            airBubble.add(key);

            if (airBubble.size() > MAX_AIR_BUBBLE_SIZE)
                return false;

            int[][] adjacentCells = {
                    {-1, 0, 0},  // Left
                    {1, 0, 0},   // Right
                    {0, -1, 0},  // Behind
                    {0, 1, 0},   // In front
                    {0, 0, -1},  // Below
                    {0, 0, 1}    // Above
            };

            for (int[] offset : adjacentCells) {
                int nx = x + offset[0];
                int ny = y + offset[1];
                int nz = z + offset[2];
                queue.add(new int[]{nx, ny, nz});
            }
        }

        return true;
    }

    private static boolean isValidCell(int x, int y, int z, World grid) {
        return grid.getBlockState(new BlockPos(x, y, z)).isAir();
    }

    public static String getKey(int x, int y, int z) {
        return x + "," + y + "," + z;
    }
}
