package com.sicmagroup.gpr.utils;

import java.util.*;

public class ColorUtils {
    public static List<RgbColor> generateColor(int total) {
        Set<RgbColor> uniqueColors = new HashSet<>();
        Random random = new Random();
        
        while (uniqueColors.size() < total) {
            int red = random.nextInt(256);   // 0 à 255 inclus
            int green = random.nextInt(256); // 0 à 255 inclus
            int blue = random.nextInt(256);  // 0 à 255 inclus
            
            // Crée une nouvelle couleur
            RgbColor color = new RgbColor(red, green, blue);
            
            // Exclure les couleurs noires et blanches
            if (!isBlackOrWhite(red, green, blue)) {
                uniqueColors.add(color);
            }
        }
        
        // Convertir le Set en List pour retourner le résultat
        return new ArrayList<>(uniqueColors);
    }
    
    private static boolean isBlackOrWhite(int red, int green, int blue) {
        return (red == 0 && green == 0 && blue == 0) || 
               (red == 255 && green == 255 && blue == 255);
    }
    
    // Classe représentant une couleur RGB
    public static class RgbColor {
        private final int red;
        private final int green;
        private final int blue;

        public RgbColor(int red, int green, int blue) {
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        public int getRed() {
            return red;
        }

        public int getGreen() {
            return green;
        }

        public int getBlue() {
            return blue;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            RgbColor rgbColor = (RgbColor) obj;
            return red == rgbColor.red && green == rgbColor.green && blue == rgbColor.blue;
        }

        @Override
        public int hashCode() {
            return Objects.hash(red, green, blue);
        }
    }
}
