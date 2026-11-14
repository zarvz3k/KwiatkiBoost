package com.example.flowerboost.mixin;

import com.example.flowerboost.FlowerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GrassBlock.class)
public class GrassBlockMixin {

    @Unique
    private static boolean flowerboost$busy = false;

    @Inject(method = "performBonemeal", at = @At("RETURN"))
    private void flowerboost$repeat(ServerLevel level, RandomSource random, BlockPos pos,
                                    BlockState state, CallbackInfo ci) {
        if (flowerboost$busy) return; // zabezpieczenie przed nieskończoną pętlą
        flowerboost$busy = true;
        try {
            int attempts = 0;
            while (attempts < 200 && flowerboost$countFlowers(level, pos) < FlowerConfig.flowerCount) {
                ((GrassBlock) (Object) this).performBonemeal(level, random, pos, level.getBlockState(pos));
                attempts++;
            }
        } finally {
            flowerboost$busy = false;
        }
    }

    @Unique
    private static int flowerboost$countFlowers(ServerLevel level, BlockPos center) {
        int r = FlowerConfig.radius;
        int count = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    if (level.getBlockState(center.offset(dx, dy, dz)).is(BlockTags.SMALL_FLOWERS)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
