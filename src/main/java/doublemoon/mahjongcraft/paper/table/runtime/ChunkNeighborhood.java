package doublemoon.mahjongcraft.paper.table;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

final class ChunkNeighborhood {
    private ChunkNeighborhood() {
    }

    static Set<ChunkKey> around(UUID worldId, int chunkX, int chunkZ) {
        Set<ChunkKey> keys = new HashSet<>();
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                keys.add(new ChunkKey(worldId, chunkX + offsetX, chunkZ + offsetZ));
            }
        }
        return keys;
    }

    record ChunkKey(UUID worldId, int chunkX, int chunkZ) {
    }
}
