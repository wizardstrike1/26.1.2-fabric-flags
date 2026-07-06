package com.wizardstrike.flagquiz;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class FlagQuizMod implements ClientModInitializer {
    public static final String MOD_ID = "flagquiz";
    private static final int TICKS_BETWEEN_QUESTIONS = 20 * 30;

    private static final List<CountryFlag> FLAGS = List.of(
        new CountryFlag("France", flagTexture("france")),
        new CountryFlag("Germany", flagTexture("germany")),
        new CountryFlag("Italy", flagTexture("italy")),
        new CountryFlag("Japan", flagTexture("japan")),
        new CountryFlag("Ukraine", flagTexture("ukraine")),
        new CountryFlag("Netherlands", flagTexture("netherlands")),
        new CountryFlag("Poland", flagTexture("poland")),
        new CountryFlag("Austria", flagTexture("austria"))
    );

    private static boolean quizEnabled = false;
    private static int ticksUntilNextQuestion = TICKS_BETWEEN_QUESTIONS;

    @Override
    public void onInitializeClient() {
        registerCommand();
        registerTickLoop();
    }

    private static void registerCommand() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
            ClientCommands.literal("flagquiz").executes(context -> {
                quizEnabled = !quizEnabled;
                ticksUntilNextQuestion = TICKS_BETWEEN_QUESTIONS;

                Minecraft client = Minecraft.getInstance();
                if (client.player != null) {
                    client.player.sendSystemMessage(Component.literal(
                        quizEnabled ? "Flag quiz enabled. First question in 30 seconds." : "Flag quiz disabled."
                    ));
                }
                return 1;
            })
        ));
    }

    private static void registerTickLoop() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!quizEnabled || client.player == null || client.level == null) {
                return;
            }

            if (client.screen instanceof FlagQuizScreen) {
                return;
            }

            ticksUntilNextQuestion--;
            if (ticksUntilNextQuestion <= 0) {
                ticksUntilNextQuestion = TICKS_BETWEEN_QUESTIONS;
                client.setScreen(new FlagQuizScreen(randomFlag()));
            }
        });
    }

    private static CountryFlag randomFlag() {
        int index = ThreadLocalRandom.current().nextInt(FLAGS.size());
        return FLAGS.get(index);
    }

    private static Identifier flagTexture(String flagName) {
        return Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/flags/" + flagName + ".png");
    }
}
