package com.example.flowerboost;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class FlowerConfig {
    public static int flowerCount = 4; // ile kwiatków
    public static int radius = 3;      // zasięg (3 = obszar 7x7)

    private static final Path FILE =
            FabricLoader.getInstance().getConfigDir().resolve("flowerboost.properties");

    public static void load() {
        if (!Files.exists(FILE)) {
            save();
            return;
        }
        try (Reader r = Files.newBufferedReader(FILE)) {
            Properties p = new Properties();
            p.load(r);
            flowerCount = Integer.parseInt(p.getProperty("flowerCount", "4"));
            radius = Integer.parseInt(p.getProperty("radius", "3"));
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("flowerCount", String.valueOf(flowerCount));
        p.setProperty("radius", String.valueOf(radius));
        try (Writer w = Files.newBufferedWriter(FILE)) {
            p.store(w, "FlowerBoost config");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}