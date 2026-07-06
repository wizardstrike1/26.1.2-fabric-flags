package com.wizardstrike.flagquiz;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;

public final class FlagQuizMod implements ClientModInitializer {
    public static final String MOD_ID = "flagquiz";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MIN_INTERVAL_SECONDS = 5;
    private static final int MAX_INTERVAL_SECONDS = 600;

    private static List<CountryFlag> countries = List.of();
    private static final Map<String, CountryProgress> countryProgress = new HashMap<>();
    private static int totalCorrectAnswers = 0;
    private static int totalWrongAnswers = 0;

    private static boolean quizEnabled = false;
    private static int intervalSeconds = 30;
    private static int ticksUntilNextQuestion = 20 * 30;
    private static Path statsFilePath;

    @Override
    public void onInitializeClient() {
        countries = loadCountries();
        statsFilePath = Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("flagquiz_stats.json");
        loadStats();
        registerCommand();
        registerTickLoop();
    }

    public static void handleAnswer(CountryFlag countryFlag, String guess, Minecraft client) {
        if (client.player == null) {
            return;
        }

        CountryProgress progress = countryProgress.computeIfAbsent(countryFlag.code(), code -> new CountryProgress());
        boolean correct = countryFlag.matchesGuess(guess);

        if (correct) {
            progress.correct++;
            totalCorrectAnswers++;
            client.player.sendSystemMessage(Component.literal(
                "Correct! " + countryFlag.countryName()
                    + " | This flag: " + progress.correct + " right, " + progress.wrong + " wrong"
                    + " | Overall: " + totalCorrectAnswers + " right, " + totalWrongAnswers + " wrong"
            ));
        } else {
            progress.wrong++;
            totalWrongAnswers++;
            client.player.sendSystemMessage(Component.literal(
                "Wrong. Correct answer: " + countryFlag.countryName()
                    + " | This flag: " + progress.correct + " right, " + progress.wrong + " wrong"
                    + " | Overall: " + totalCorrectAnswers + " right, " + totalWrongAnswers + " wrong"
            ));
        }

        saveStats();
    }

    private static void registerCommand() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
            ClientCommands.literal("flagquiz")
                .executes(context -> {
                    setQuizEnabled(!quizEnabled);
                    return 1;
                })
                .then(ClientCommands.literal("on").executes(context -> {
                    setQuizEnabled(true);
                    return 1;
                }))
                .then(ClientCommands.literal("off").executes(context -> {
                    setQuizEnabled(false);
                    return 1;
                }))
                .then(ClientCommands.literal("timer")
                    .executes(context -> {
                        sendClientMessage("Flag quiz timer is " + intervalSeconds + " seconds.");
                        return 1;
                    })
                    .then(ClientCommands.argument("seconds", IntegerArgumentType.integer(MIN_INTERVAL_SECONDS, MAX_INTERVAL_SECONDS))
                        .executes(context -> {
                            int seconds = getInteger(context, "seconds");
                            intervalSeconds = seconds;
                            resetTimer();
                            sendClientMessage("Flag quiz timer set to " + seconds + " seconds.");
                            return 1;
                        })))
                .then(ClientCommands.literal("stats").executes(context -> {
                    sendClientMessage("Overall stats: " + totalCorrectAnswers + " right, " + totalWrongAnswers + " wrong.");
                    return 1;
                }))
        ));
    }

    private static void registerTickLoop() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!quizEnabled || client.player == null || client.level == null || countries.isEmpty()) {
                return;
            }

            if (client.screen instanceof FlagQuizScreen) {
                return;
            }

            ticksUntilNextQuestion--;
            if (ticksUntilNextQuestion <= 0) {
                resetTimer();
                client.setScreen(new FlagQuizScreen(pickWeightedCountry()));
            }
        });
    }

    private static CountryFlag pickWeightedCountry() {
        double totalWeight = 0.0;
        for (CountryFlag country : countries) {
            totalWeight += weightFor(country.code());
        }

        double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
        for (CountryFlag country : countries) {
            roll -= weightFor(country.code());
            if (roll <= 0.0) {
                return country;
            }
        }

        return countries.get(countries.size() - 1);
    }

    private static double weightFor(String countryCode) {
        CountryProgress progress = countryProgress.get(countryCode);
        if (progress == null) {
            return 1.0;
        }

        double ratio = (progress.wrong + 1.0) / (progress.correct + 1.0);
        return Math.max(0.2, Math.min(8.0, Math.pow(ratio, 1.35)));
    }

    private static void setQuizEnabled(boolean enabled) {
        quizEnabled = enabled;
        resetTimer();

        if (quizEnabled) {
            sendClientMessage("Flag quiz enabled (" + countries.size() + " countries). First question in " + intervalSeconds + " seconds.");
        } else {
            sendClientMessage("Flag quiz disabled.");
        }
    }

    private static void resetTimer() {
        ticksUntilNextQuestion = intervalSeconds * 20;
    }

    private static void sendClientMessage(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(message));
        }
    }

    private static List<CountryFlag> loadCountries() {
        try (InputStream input = FlagQuizMod.class.getResourceAsStream("/assets/flagquiz/countries.json")) {
            if (input == null) {
                throw new IllegalStateException("Missing countries.json resource");
            }

            Type listType = new TypeToken<List<CountryDefinition>>() {}.getType();
            try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                List<CountryDefinition> definitions = GSON.fromJson(reader, listType);
                List<CountryFlag> loaded = new ArrayList<>();
                for (CountryDefinition definition : definitions) {
                    List<String> acceptedAnswers = definition.accepted;
                    if (acceptedAnswers == null || acceptedAnswers.isEmpty()) {
                        acceptedAnswers = List.of(definition.name);
                    }

                    Identifier texture = Identifier.fromNamespaceAndPath(
                        MOD_ID,
                        "textures/gui/flags/" + definition.code.toLowerCase(Locale.ROOT) + ".png"
                    );
                    loaded.add(new CountryFlag(
                        definition.code.toUpperCase(Locale.ROOT),
                        definition.name,
                        texture,
                        acceptedAnswers
                    ));
                }
                return List.copyOf(loaded);
            }
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load country dataset", exception);
        }
    }

    private static void loadStats() {
        countryProgress.clear();
        totalCorrectAnswers = 0;
        totalWrongAnswers = 0;

        if (statsFilePath == null || !Files.exists(statsFilePath)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(statsFilePath, StandardCharsets.UTF_8)) {
            StatsFile file = GSON.fromJson(reader, StatsFile.class);
            if (file == null) {
                return;
            }

            totalCorrectAnswers = Math.max(0, file.totalCorrect);
            totalWrongAnswers = Math.max(0, file.totalWrong);

            if (file.byCountry != null) {
                for (Map.Entry<String, CountryProgress> entry : file.byCountry.entrySet()) {
                    CountryProgress progress = entry.getValue();
                    if (progress != null) {
                        progress.correct = Math.max(0, progress.correct);
                        progress.wrong = Math.max(0, progress.wrong);
                        countryProgress.put(entry.getKey().toUpperCase(Locale.ROOT), progress);
                    }
                }
            }
        } catch (IOException exception) {
            System.err.println("[flagquiz] Failed to load stats: " + exception.getMessage());
        }
    }

    private static void saveStats() {
        if (statsFilePath == null) {
            return;
        }

        StatsFile file = new StatsFile();
        file.totalCorrect = totalCorrectAnswers;
        file.totalWrong = totalWrongAnswers;
        file.byCountry = new HashMap<>(countryProgress);

        try {
            Files.createDirectories(statsFilePath.getParent());
            try (Writer writer = Files.newBufferedWriter(statsFilePath, StandardCharsets.UTF_8)) {
                GSON.toJson(file, writer);
            }
        } catch (IOException exception) {
            System.err.println("[flagquiz] Failed to save stats: " + exception.getMessage());
        }
    }

    private static final class CountryDefinition {
        String code;
        String name;
        List<String> accepted;
    }

    private static final class CountryProgress {
        int correct;
        int wrong;
    }

    private static final class StatsFile {
        int totalCorrect;
        int totalWrong;
        Map<String, CountryProgress> byCountry;
    }
}
