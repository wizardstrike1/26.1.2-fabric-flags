package com.wizardstrike.flagquiz;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Locale;

public record CountryFlag(String code, String countryName, Identifier texture, List<String> acceptedAnswers) {
    public boolean matchesGuess(String guess) {
        String normalizedGuess = normalize(guess);
        if (normalizedGuess.isEmpty()) {
            return false;
        }

        for (String answer : this.acceptedAnswers) {
            if (normalize(answer).equals(normalizedGuess)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String input) {
        String normalized = input.toLowerCase(Locale.ROOT)
            .replace("'", "")
            .replace(".", "")
            .replace("-", " ")
            .replace(",", " ")
            .replace("(", " ")
            .replace(")", " ")
            .replace("’", "")
            .replace("`", "")
            .replace("the ", " ");

        return normalized.replaceAll("\\s+", " ").trim();
    }
}
