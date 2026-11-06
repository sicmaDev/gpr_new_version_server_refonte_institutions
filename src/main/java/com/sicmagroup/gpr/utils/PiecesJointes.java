package com.sicmagroup.gpr.utils;

import java.util.regex.Pattern;

import org.springframework.web.multipart.MultipartFile;

/** Règles communes aux pièces jointes des réclamations, dénonciations et suggestions. */
public final class PiecesJointes {

    private static final int MAX_FILES_COUNT = 5;
    private static final int MAX_AUDIOS_COUNT = 3;
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 Mo par fichier ou audio
    private static final long MAX_TOTAL_ATTACHMENTS_SIZE = 100L * 1024 * 1024; // 100 Mo cumulés

    private static final Pattern EXTENSION_AUTORISEE = Pattern.compile(
            "\\.(pdf|docx?|xlsx?|jpe?g|png|ogg|mp3|wav|m4a|aac|opus|mp4|webm|mov|avi)$", Pattern.CASE_INSENSITIVE);

    private PiecesJointes() {
    }

    public static void valider(MultipartFile[] files, MultipartFile[] audios) throws Exception {
        int filesCount = files != null ? files.length : 0;
        int audiosCount = audios != null ? audios.length : 0;

        if (filesCount > MAX_FILES_COUNT) {
            throw new Exception("Vous ne pouvez pas joindre plus de " + MAX_FILES_COUNT + " fichiers.");
        }
        if (audiosCount > MAX_AUDIOS_COUNT) {
            throw new Exception("Vous ne pouvez pas joindre plus de " + MAX_AUDIOS_COUNT + " enregistrements audio.");
        }

        long totalSize = 0;
        if (files != null) {
            for (MultipartFile file : files) {
                String nom = file.getOriginalFilename();
                if (nom == null || !EXTENSION_AUTORISEE.matcher(nom).find()) {
                    throw new Exception("Le fichier \"" + nom
                            + "\" n'est pas d'un type autorisé (PDF, Word, Excel, image, audio ou vidéo).");
                }
                if (file.getSize() > MAX_FILE_SIZE) {
                    throw new Exception("Le fichier \"" + nom + "\" dépasse la taille maximale autorisée ("
                            + (MAX_FILE_SIZE / (1024 * 1024)) + " Mo).");
                }
                totalSize += file.getSize();
            }
        }
        if (audios != null) {
            for (MultipartFile audio : audios) {
                if (audio.getSize() > MAX_FILE_SIZE) {
                    throw new Exception("Un enregistrement audio dépasse la taille maximale autorisée ("
                            + (MAX_FILE_SIZE / (1024 * 1024)) + " Mo).");
                }
                totalSize += audio.getSize();
            }
        }
        if (totalSize > MAX_TOTAL_ATTACHMENTS_SIZE) {
            throw new Exception("La taille totale des pièces jointes dépasse la limite autorisée ("
                    + (MAX_TOTAL_ATTACHMENTS_SIZE / (1024 * 1024)) + " Mo).");
        }
    }
}
