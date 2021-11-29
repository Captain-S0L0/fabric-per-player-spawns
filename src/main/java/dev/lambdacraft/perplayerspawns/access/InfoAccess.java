package dev.lambdacraft.perplayerspawns.access;

import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;

public interface InfoAccess {
    void setChunkManager(ServerChunkManagerMixinAccess chunkManager);
    void incrementPlayerMobCount(ServerPlayerEntity playerEntity, SpawnGroup spawnGroup);
    boolean isBelowChunkCap(SpawnGroup group, ChunkPos cp);
}
