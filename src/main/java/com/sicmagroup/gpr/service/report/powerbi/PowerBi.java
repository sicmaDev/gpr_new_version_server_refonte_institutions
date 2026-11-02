package com.sicmagroup.gpr.service.report.powerbi;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

// Briques du format Power BI : tables saisies (Power Query), expressions de requête des visuels, visuels et pages.
public final class PowerBi {
	private PowerBi() {}

	public static Map<String, Object> m(Object... cv) {
		Map<String, Object> r = new LinkedHashMap<>();
		for (int i = 0; i < cv.length; i += 2) r.put((String) cv[i], cv[i + 1]);
		return r;
	}

	/* ── Tables saisies : les données sont écrites dans la requête Power Query (#table), sans source externe ── */
	public record Colonne(String nom, boolean texte, String format, String trierPar, boolean cachee) {
		public static Colonne texte(String nom) { return new Colonne(nom, true, null, null, false); }
		public static Colonne nombre(String nom) { return new Colonne(nom, false, null, null, false); }
		public Colonne format(String f) { return new Colonne(nom, texte, f, trierPar, cachee); }
		public Colonne trieePar(String c) { return new Colonne(nom, texte, format, c, cachee); }
		public Colonne masquee() { return new Colonne(nom, texte, format, trierPar, true); }
	}
	public record Mesure(String nom, String dax, String format) {}

	static String mTexte(Object v) {
		return v == null ? "null" : "\"" + String.valueOf(v).replace("\"", "\"\"").replace("\r", "").replace("\n", "#(lf)") + "\"";
	}
	static String mNombre(Object v) {
		if (v == null) return "null";
		double d = ((Number) v).doubleValue();
		return Double.isFinite(d) ? BigDecimal.valueOf(d).stripTrailingZeros().toPlainString() : "null";
	}

	public static Map<String, Object> table(String nom, List<Colonne> colonnes, List<List<Object>> lignes, List<Mesure> mesures) {
		StringBuilder type = new StringBuilder();
		for (Colonne c : colonnes) type.append(type.length() == 0 ? "" : ", ").append("#\"").append(c.nom().replace("\"", "\"\"")).append("\" = ").append(c.texte() ? "text" : "number");
		List<String> rangs = new ArrayList<>();
		for (List<Object> l : lignes) {
			List<String> v = new ArrayList<>();
			for (int i = 0; i < colonnes.size(); i++) v.add(colonnes.get(i).texte() ? mTexte(l.get(i)) : mNombre(l.get(i)));
			rangs.add("{" + String.join(", ", v) + "}");
		}
		List<String> expression = List.of("let", "    Source = #table(type table [" + type + "], {", "        " + String.join(",\n        ", rangs), "    })", "in", "    Source");
		List<Object> cols = new ArrayList<>();
		for (Colonne c : colonnes) {
			Map<String, Object> col = m("name", c.nom(), "dataType", c.texte() ? "string" : "double", "sourceColumn", c.nom(), "summarizeBy", c.texte() ? "none" : "sum");
			if (c.format() != null) col.put("formatString", c.format());
			if (c.trierPar() != null) col.put("sortByColumn", c.trierPar());
			if (c.cachee()) col.put("isHidden", true);
			cols.add(col);
		}
		Map<String, Object> t = m("name", nom, "columns", cols,
				"partitions", List.of(m("name", nom, "mode", "import", "source", m("type", "m", "expression", expression))));
		if (mesures != null && !mesures.isEmpty()) {
			List<Object> ms = new ArrayList<>();
			for (Mesure me : mesures) ms.add(m("name", me.nom(), "expression", me.dax(), "formatString", me.format()));
			t.put("measures", ms);
		}
		return t;
	}

	/* ── Requêtes des visuels ── */
	public static String lit(String texte) { return "'" + texte.replace("'", "''") + "'"; }
	public static Map<String, Object> litteral(String valeur) { return m("expr", m("Literal", m("Value", valeur))); }
	public static Map<String, Object> couleur(String hex) { return m("solid", m("color", litteral(lit(hex)))); }
	static Map<String, Object> source(String alias) { return m("SourceRef", m("Source", alias)); }
	static Map<String, Object> entite(String table) { return m("SourceRef", m("Entity", table)); }

	// Un champ d'un visuel : colonne, colonne agrégée ou mesure, avec son nom de requête.
	public record Champ(String table, String nom, String type, int fonction) {
		public static Champ colonne(String t, String n) { return new Champ(t, n, "col", -1); }
		public static Champ mesure(String t, String n) { return new Champ(t, n, "mes", -1); }
		public static Champ somme(String t, String n) { return new Champ(t, n, "agg", 0); }
		public static Champ moyenne(String t, String n) { return new Champ(t, n, "agg", 1); }
		public static Champ min(String t, String n) { return new Champ(t, n, "agg", 3); }
		public String ref() {
			if (!type.equals("agg")) return table + "." + nom;
			return switch (fonction) { case 0 -> "Sum"; case 1 -> "Avg"; case 3 -> "Min"; default -> "Max"; } + "(" + table + "." + nom + ")";
		}
		Map<String, Object> expression(String alias) {
			Map<String, Object> e = m("Expression", source(alias), "Property", nom);
			return switch (type) {
				case "col" -> m("Column", e);
				case "mes" -> m("Measure", e);
				default -> m("Aggregation", m("Expression", m("Column", e), "Function", fonction));
			};
		}
		Map<String, Object> selection(String alias) {
			Map<String, Object> s = expression(alias);
			s.put("Name", ref());
			s.put("NativeReferenceName", nom);
			return s;
		}
		// Expression hors requête (mise en forme conditionnelle) : référence directe à la table.
		Map<String, Object> expressionEntite() {
			Map<String, Object> e = m("Expression", entite(table), "Property", nom);
			return switch (type) {
				case "col" -> m("Column", e);
				case "mes" -> m("Measure", e);
				default -> m("Aggregation", m("Expression", m("Column", e), "Function", fonction));
			};
		}
	}

	// Couleur de chaque point lue dans une colonne de la table (couleur affichée par la page).
	public static Map<String, Object> remplissageParChamp(String table, String colonne) {
		return m("properties", m("fill", m("solid", m("color", m("expr", Champ.min(table, colonne).expressionEntite())))),
				"selector", m("data", List.of(m("dataViewWildcard", m("matchingOption", 1)))));
	}
	// Couleur fixe d'une série (valeur de la colonne de légende).
	public static Map<String, Object> remplissageSerie(String table, String colonne, String valeur, String hex) {
		return m("properties", m("fill", couleur(hex)),
				"selector", m("data", List.of(m("scopeId", m("Comparison", m("ComparisonKind", 0,
						"Left", m("Column", m("Expression", entite(table), "Property", colonne)), "Right", m("Literal", m("Value", lit(valeur)))))))));
	}
	public static Map<String, Object> proprietes(Object... cv) { return m("properties", m(cv)); }

	// Filtre de visuel « colonne = valeur », masqué dans le volet des filtres.
	public static Map<String, Object> filtreEgal(String table, String colonne, String valeur) {
		return m("name", "f" + Integer.toHexString((table + colonne + valeur).hashCode()),
				"expression", m("Column", m("Expression", entite(table), "Property", colonne)),
				"filter", m("Version", 2, "From", List.of(m("Name", "f", "Entity", table, "Type", 0)),
						"Where", List.of(m("Condition", m("In", m("Expressions", List.of(m("Column", m("Expression", source("f"), "Property", colonne))),
								"Values", List.of(List.of(m("Literal", m("Value", lit(valeur)))))))))),
				"type", "Categorical", "howCreated", 1, "isHiddenInViewMode", true);
	}

	/* ── Visuels ── */
	public static final class Visuel {
		final String type;
		final Map<String, List<Champ>> projections = new LinkedHashMap<>();
		final List<Object> tri = new ArrayList<>();
		final Map<String, Object> objets = new LinkedHashMap<>();
		final Map<String, Object> objetsCadre = new LinkedHashMap<>();
		final List<Object> filtres = new ArrayList<>();
		double x, y, largeur, hauteur;

		public Visuel(String type) { this.type = type; }
		public Visuel champ(String role, Champ c) { projections.computeIfAbsent(role, k -> new ArrayList<>()).add(c); return this; }
		public Visuel triCroissant(Champ c) { tri.add(c); return this; }
		public Visuel triDecroissant(Champ c) { tri.add(m("desc", c)); return this; }
		@SuppressWarnings("unchecked")
		public Visuel objet(String nom, Map<String, Object> entree) { ((List<Object>) objets.computeIfAbsent(nom, k -> new ArrayList<>())).add(entree); return this; }
		public Visuel cadre(String nom, Map<String, Object> proprietes) { objetsCadre.put(nom, List.of(m("properties", proprietes))); return this; }
		public Visuel filtre(Map<String, Object> f) { filtres.add(f); return this; }
		public Visuel position(double x, double y, double l, double h) { this.x = x; this.y = y; this.largeur = l; this.hauteur = h; return this; }
		public Visuel titre(String texte) {
			return cadre("title", m("show", litteral("true"), "text", litteral(lit(texte)), "fontColor", couleur("#374151"), "fontSize", litteral("10D"), "bold", litteral("true")));
		}
		public double bas() { return y + hauteur; }

		@SuppressWarnings("unchecked")
		Map<String, Object> conteneur(int z, AtomicInteger compteur) {
			Map<String, Object> sv = m("visualType", type);
			if (!projections.isEmpty()) {
				Map<String, String> alias = new LinkedHashMap<>();
				List<Object> from = new ArrayList<>(), select = new ArrayList<>();
				Map<String, Object> proj = new LinkedHashMap<>();
				List<String> vus = new ArrayList<>();
				for (Map.Entry<String, List<Champ>> p : projections.entrySet()) {
					List<Object> refs = new ArrayList<>();
					for (Champ c : p.getValue()) {
						String a = alias.computeIfAbsent(c.table(), t -> { String n = "t" + alias.size(); from.add(m("Name", n, "Entity", t, "Type", 0)); return n; });
						refs.add(p.getKey().equals("Category") || p.getKey().equals("Rows") ? m("queryRef", c.ref(), "active", true) : m("queryRef", c.ref()));
						if (!vus.contains(c.ref())) { vus.add(c.ref()); select.add(c.selection(a)); }
					}
					proj.put(p.getKey(), refs);
				}
				Map<String, Object> requete = m("Version", 2, "From", from, "Select", select);
				if (!tri.isEmpty()) {
					List<Object> ordre = new ArrayList<>();
					for (Object t : tri) {
						boolean desc = t instanceof Map;
						Champ c = desc ? (Champ) ((Map<String, Object>) t).get("desc") : (Champ) t;
						ordre.add(m("Direction", desc ? 2 : 1, "Expression", c.expression(alias.get(c.table()))));
					}
					requete.put("OrderBy", ordre);
				}
				sv.put("projections", proj);
				sv.put("prototypeQuery", requete);
				if (!tri.isEmpty()) sv.put("hasDefaultSort", true);
			}
			sv.put("drillFilterOtherVisuals", true);
			if (!objets.isEmpty()) sv.put("objects", objets);
			if (!objetsCadre.isEmpty()) sv.put("vcObjects", objetsCadre);
			Map<String, Object> cfg = m("name", String.format("v%019d", compteur.incrementAndGet()),
					"layouts", List.of(m("id", 0, "position", m("x", x, "y", y, "z", z, "width", largeur, "height", hauteur, "tabOrder", z))),
					"singleVisual", sv);
			return m("x", x, "y", y, "z", z, "width", largeur, "height", hauteur, "config", FichierPbit.json(cfg), "filters", FichierPbit.json(filtres));
		}
	}

	// Zone de texte : paragraphes faits de morceaux (texte, gras, taille, couleur).
	public record Morceau(String texte, boolean gras, int taille, String couleur) {}
	public static Visuel texte(List<List<Morceau>> paragraphes) {
		List<Object> ps = new ArrayList<>();
		for (List<Morceau> p : paragraphes) {
			List<Object> runs = new ArrayList<>();
			for (Morceau mo : p) runs.add(m("value", mo.texte(), "textStyle", m("fontWeight", mo.gras() ? "bold" : "normal", "fontSize", mo.taille() + "pt", "color", mo.couleur())));
			ps.add(m("textRuns", runs));
		}
		return new Visuel("textbox").objet("general", m("properties", m("paragraphs", ps)));
	}

	public static Map<String, Object> page(String nom, int ordre, double hauteur, List<Visuel> visuels, AtomicInteger compteur) {
		List<Object> conteneurs = new ArrayList<>();
		int z = 0;
		for (Visuel v : visuels) conteneurs.add(v.conteneur(z += 1000, compteur));
		String config = FichierPbit.json(m("objects", m("background", List.of(m("properties", m("color", couleur("#F8FAFC"), "transparency", litteral("0D")))),
				"outspace", List.of(m("properties", m("color", couleur("#F8FAFC"), "transparency", litteral("0D")))))));
		return m("name", String.format("ReportSection%04d", ordre), "displayName", nom, "filters", "[]", "ordinal", ordre, "visualContainers", conteneurs,
				"config", config, "displayOption", 2, "width", 1280, "height", (int) Math.ceil(hauteur));
	}
}
