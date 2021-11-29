package dev.lambdacraft.perplayerspawns.mixin;

import dev.lambdacraft.perplayerspawns.access.InfoAccess;
import dev.lambdacraft.perplayerspawns.access.ServerChunkManagerMixinAccess;
import dev.lambdacraft.perplayerspawns.util.PlayerMobCountMap;
import dev.lambdacraft.perplayerspawns.util.PlayerDistanceMap;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpawnHelper.Info.class)
public class WeirdInfoAkaGravityAndMobCapCheckingMixin implements InfoAccess {

    private final PlayerMobCountMap playerMobCountMap = new PlayerMobCountMap();
    public void incrementPlayerMobCount(ServerPlayerEntity playerEntity, SpawnGroup spawnGroup) { this.playerMobCountMap.incrementPlayerMobCount(playerEntity, spawnGroup); }

    private PlayerDistanceMap playerDistanceMap;
    public void setChunkManager(ServerChunkManagerMixinAccess chunkManager) {
        this.playerDistanceMap = chunkManager.getPlayerDistanceMap();
    }
    public boolean isBelowChunkCap(SpawnGroup spawnGroup, ChunkPos cp) {
        // Compute if mobs should be spawned between all players in range of chunk
        int cap = spawnGroup.getCapacity();
        for (ServerPlayerEntity player : playerDistanceMap.getPlayersInRange(cp.toLong())) {
            int mobCountNearPlayer = playerMobCountMap.getPlayerMobCount(player, spawnGroup);
            if(cap <= mobCountNearPlayer) return false;
        }
        return true;
    }


    @Inject(method = "run", at = @At("HEAD"))
    private void addSpawnedMobToMap(MobEntity entity, Chunk chunk, CallbackInfo callbackInfo){
        for (ServerPlayerEntity player : playerDistanceMap.getPlayersInRange(chunk.getPos().toLong())) {
            // Increment player's sighting of entity
            incrementPlayerMobCount(player, entity.getType().getSpawnGroup());
        }
    }

}
