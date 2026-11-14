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
    public static int radius = 3;      // zasięg
    public static int waitTicks = 4;   // czekanie na efekt bone meala (klient)
    public static int stepTicks = 1;   // ticki między zmianą slotu a postawieniem (klient)

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
            waitTicks = Integer.parseInt(p.getProperty("waitTicks", "4"));
            stepTicks = Integer.parseInt(p.getProperty("stepTicks", "1"));
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("flowerCount", String.valueOf(flowerCount));
        p.setProperty("radius", String.valueOf(radius));
        p.setProperty("waitTicks", String.valueOf(waitTicks));
        p.setProperty("stepTicks", String.valueOf(stepTicks));
        try (Writer w = Files.newBufferedWriter(FILE)) {
            p.store(w, "FlowerBoost config");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
