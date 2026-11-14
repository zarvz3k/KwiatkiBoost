package com.example.flowerboost.mixin;

import com.example.flowerboost.FlowerConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.GrassBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
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

    @Inject(method = "grow", at = @At("HEAD"), cancellable = true)
    private void flowerboost$grow(ServerWorld world, Random random, BlockPos pos,
                                  BlockState state, CallbackInfo ci) {
        int r = FlowerConfig.radius;
        List<BlockPos> spots = new ArrayList<>();

        // szukamy wolnych miejsc nad blokami trawy w okolicy
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos above = pos.add(dx, dy + 1, dz);
                    if (world.getBlockState(above).isAir()
                            && world.getBlockState(above.down()).isOf(Blocks.GRASS_BLOCK)) {
                        spots.add(above);
                    }
                }
            }
        }

        Collections.shuffle(spots, new java.util.Random(random.nextLong()));

        int placed = 0;
        for (BlockPos spot : spots) {
            if (placed < FlowerConfig.flowerCount) {
                BlockState flower = FLOWERS[random.nextInt(FLOWERS.length)].getDefaultState();
                if (flower.canPlaceAt(world, spot)) {
                    world.setBlockState(spot, flower, Block.NOTIFY_ALL);
                    placed++;
                }
            } else if (random.nextInt(2) == 0) {
                world.setBlockState(spot, Blocks.SHORT_GRASS.getDefaultState(), Block.NOTIFY_ALL);
            }
        }

        ci.cancel(); // wyłącza standardowe, losowe zachowanie bone meala na trawie
    }
}
