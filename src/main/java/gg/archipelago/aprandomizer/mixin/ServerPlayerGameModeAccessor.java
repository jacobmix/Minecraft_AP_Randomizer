package gg.archipelago.aprandomizer.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerPlayerGameMode.class)
public interface ServerPlayerGameModeAccessor {

    @Accessor("isDestroyingBlock")
    boolean isDestroyingBlock();

    @Accessor("destroyPos")
    BlockPos getDestroyPos();
}
