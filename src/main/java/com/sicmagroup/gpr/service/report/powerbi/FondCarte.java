package com.sicmagroup.gpr.service.report.powerbi;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import com.fasterxml.jackson.databind.ObjectMapper;

// Fond de la carte Power BI : contours des départements du Bénin (geoBoundaries, domaine public) dessinés dans une image,
// en coordonnées longitude / latitude directes, pour se superposer au nuage de points dont les axes ont les mêmes bornes.
// Aucun appel réseau : le fichier Power BI reste lisible hors connexion.
public final class FondCarte {
	private FondCarte() {}

	public record Cadre(double lonMin, double lonMax, double latMin, double latMax) {}

	@SuppressWarnings("unchecked")
	public static byte[] benin(Cadre c, int largeur, int hauteur) {
		Map<String, Object> fond;
		try (InputStream in = FondCarte.class.getResourceAsStream("/powerbi/fond-benin.json")) {
			fond = new ObjectMapper().readValue(in, Map.class);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		BufferedImage img = new BufferedImage(largeur, hauteur, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(0xEEF2F6));
		g.fillRect(0, 0, largeur, hauteur);
		for (Object d : (List<Object>) fond.get("departements")) {
			Path2D.Double p = new Path2D.Double(Path2D.WIND_EVEN_ODD);
			for (Object anneau : (List<Object>) ((Map<String, Object>) d).get("anneaux")) {
				boolean debut = true;
				for (Object pt : (List<Object>) anneau) {
					List<Number> xy = (List<Number>) pt;
					double x = (xy.get(0).doubleValue() - c.lonMin()) / (c.lonMax() - c.lonMin()) * largeur;
					double y = (c.latMax() - xy.get(1).doubleValue()) / (c.latMax() - c.latMin()) * hauteur;
					if (debut) p.moveTo(x, y); else p.lineTo(x, y);
					debut = false;
				}
				p.closePath();
			}
			g.setColor(Color.WHITE);
			g.fill(p);
			g.setColor(new Color(0xCBD5E1));
			g.setStroke(new BasicStroke(1.4f));
			g.draw(p);
		}
		g.dispose();
		ByteArrayOutputStream sortie = new ByteArrayOutputStream();
		try {
			ImageIO.write(img, "png", sortie);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return sortie.toByteArray();
	}
}
