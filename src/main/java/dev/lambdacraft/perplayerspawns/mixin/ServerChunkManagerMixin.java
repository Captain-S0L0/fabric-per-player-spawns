package dev.lambdacraft.perplayerspawns.mixin;

import dev.lambdacraft.perplayerspawns.access.*;
import dev.lambdacraft.perplayerspawns.util.PlayerDistanceMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.SpawnDensityCapper;
import net.minecraft.world.SpawnHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Iterator;


@Mixin (ServerChunkManager.class)
public class ServerChunkManagerMixin implements ServerChunkManagerMixinAccess {
	@Shadow @Final private ServerWorld world;

	@Shadow @Final public ThreadedAnvilChunkStorage threadedAnvilChunkStorage;

	private final PlayerDistanceMap playerDistanceMap = new PlayerDistanceMap();
	public PlayerDistanceMap getPlayerDistanceMap() { return playerDistanceMap; }

	@SuppressWarnings("UnresolvedMixinReference")
	@Redirect(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/SpawnHelper;setupSpawn(ILjava/lang/Iterable;Lnet/minecraft/world/SpawnHelper$ChunkSource;Lnet/minecraft/world/SpawnDensityCapper;)Lnet/minecraft/world/SpawnHelper$Info;"))
	private SpawnHelper.Info setupSpawning(int spawningChunkCount, Iterable<Entity> entities, SpawnHelper.ChunkSource chunkSource, SpawnDensityCapper sdc){

		/*
			Every all-chunks tick:
			1. Update distance map by adding all players
			2. Reset player's nearby mob counts
			3. Loop through all world's entities and add them to player's counts
	 	*/
		// update distance map
		playerDistanceMap.update(this.world.getPlayers(), ((TACSAccess) this.threadedAnvilChunkStorage).renderDistance());

		// calculate mob counts

		SpawnHelper.Info info = SpawnHelper.setupSpawn(spawningChunkCount, entities, chunkSource, sdc);
		Iterator<Entity> var5 = entities.iterator();
		out:
		while(true) {
			Entity entity;
			MobEntity mobEntity;
			do {
				if (!var5.hasNext()) break out;
				entity = var5.next();
				if (!(entity instanceof MobEntity)) break;
				mobEntity = (MobEntity)entity;
			}
			while(mobEntity.isPersistent() || mobEntity.cannotDespawn());

			SpawnGroup spawnGroup = entity.getType().getSpawnGroup();
			if (spawnGroup != SpawnGroup.MISC) {
				BlockPos blockPos = entity.getBlockPos();
				long l = ChunkPos.toLong(blockPos.getX() >> 4, blockPos.getZ() >> 4);
				chunkSource.query(l, (worldChunk) -> {
					// Find players in range of entity
					for (ServerPlayerEntity player : this.playerDistanceMap.getPlayersInRange(l)) {
						// Increment player's sighting of entity
						((InfoAccess)info).incrementPlayerMobCount(player, spawnGroup);
					}
				});
			}
		}

		((InfoAccess)info).setChunkManager(this);
		return info;
	}

}


