package com.example.flowerboost;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class FlowerBoost implements ModInitializer {
    @Override
    public void onInitialize() {
        FlowerConfig.load();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("flowerboost")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("count")
                                .then(Commands.argument("n", IntegerArgumentType.integer(0, 50))
                                        .executes(ctx -> {
                                            FlowerConfig.flowerCount = IntegerArgumentType.getInteger(ctx, "n");
                                            FlowerConfig.save();
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("Kwiatków na bone meal: " + FlowerConfig.flowerCount), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("radius")
                                .then(Commands.argument("n", IntegerArgumentType.integer(1, 10))
                                        .executes(ctx -> {
                                            FlowerConfig.radius = IntegerArgumentType.getInteger(ctx, "n");
                                            FlowerConfig.save();
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("Zasięg: " + FlowerConfig.radius), true);
                                            return 1;
                                        })))));
    }
}
