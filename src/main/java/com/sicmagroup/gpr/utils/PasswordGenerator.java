package com.sicmagroup.gpr.utils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mot de passe temporaire d'un compte : au moins une minuscule, une majuscule, un chiffre et un symbole
 * (il passe donc la règle de mot de passe de l'application), tiré au hasard avec un générateur sécurisé.
 * Les caractères qui se confondent à la lecture (O/0, l/1/I) sont exclus.
 */
public final class PasswordGenerator {

    public static final int DEFAULT_LENGTH = 10;

    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "!@#$%*-_";
    private static final String ALL = LOWER + UPPER + DIGITS + SYMBOLS;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordGenerator() {
    }

    public static String generate() {
        return generate(DEFAULT_LENGTH);
    }

    public static String generate(int length) {
        if (length < 4) {
            throw new IllegalArgumentException("Un mot de passe doit avoir au moins 4 caractères");
        }
        List<Character> chars = new ArrayList<>(length);
        chars.add(pick(LOWER));
        chars.add(pick(UPPER));
        chars.add(pick(DIGITS));
        chars.add(pick(SYMBOLS));
        while (chars.size() < length) {
            chars.add(pick(ALL));
        }
        Collections.shuffle(chars, RANDOM);
        StringBuilder sb = new StringBuilder(length);
        chars.forEach(sb::append);
        return sb.toString();
    }

    private static char pick(String from) {
        return from.charAt(RANDOM.nextInt(from.length()));
    }
}
