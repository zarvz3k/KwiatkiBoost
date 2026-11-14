package com.example.flowerboost;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

public class FlowerBoost implements ModInitializer {
    @Override
    public void onInitialize() {
        FlowerConfig.load();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("flowerboost")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.literal("count")
                                .then(CommandManager.argument("n", IntegerArgumentType.integer(0, 50))
                                        .executes(ctx -> {
                                            FlowerConfig.flowerCount = IntegerArgumentType.getInteger(ctx, "n");
                                            FlowerConfig.save();
                                            ctx.getSource().sendFeedback(
                                                    () -> Text.literal("Kwiatków na bone meal: " + FlowerConfig.flowerCount), true);
                                            return 1;
                                        })))
                        .then(CommandManager.literal("radius")
                                .then(CommandManager.argument("n", IntegerArgumentType.integer(1, 10))
                                        .executes(ctx -> {
                                            FlowerConfig.radius = IntegerArgumentType.getInteger(ctx, "n");
                                            FlowerConfig.save();
                                            ctx.getSource().sendFeedback(
                                                    () -> Text.literal("Zasięg: " + FlowerConfig.radius), true);
                                            return 1;
                                        })))));
    }
}