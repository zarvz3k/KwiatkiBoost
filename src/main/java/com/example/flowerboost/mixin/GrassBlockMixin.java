package com.example.flowerboost.mixin;

import com.example.flowerboost.FlowerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(GrassBlock.class)
public class GrassBlockMixin {

    @Unique
    private static final Block[] FLOWERS = {
            Blocks.DANDELION, Blocks.POPPY, Blocks.BLUE_ORCHID, Blocks.ALLIUM,
            Blocks.AZURE_BLUET, Blocks.RED_TULIP, Blocks.ORANGE_TULIP,
            Blocks.WHITE_TULIP, Blocks.PINK_TULIP, Blocks.OXEYE_DAISY,
            Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY
    };

    @Inject(method = "performBonemeal", at = @At("HEAD"), cancellable = true)
    private void flowerboost$grow(ServerLevel level, RandomSource random, BlockPos pos,
                                  BlockState state, CallbackInfo ci) {
        int r = FlowerConfig.radius;
        List<BlockPos> spots = new ArrayList<>();

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos above = pos.offset(dx, dy + 1, dz);
                    if (level.getBlockState(above).isAir()
                            && level.getBlockState(above.below()).is(Blocks.GRASS_BLOCK)) {
                        spots.add(above);
                    }
                }
            }
        }

        Collections.shuffle(spots, new java.util.Random(random.nextLong()));

        int placed = 0;
        for (BlockPos spot : spots) {
            if (placed < FlowerConfig.flowerCount) {
                BlockState flower = FLOWERS[random.nextInt(FLOWERS.length)].defaultBlockState();
                if (flower.canSurvive(level, spot)) {
                    level.setBlock(spot, flower, Block.UPDATE_ALL);
                    placed++;
                }
            } else if (random.nextInt(2) == 0) {
                level.setBlock(spot, Blocks.SHORT_GRASS.defaultBlockState(), Block.UPDATE_ALL);
            }
        }

        ci.cancel();
    }
}
