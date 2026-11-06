package com.sicmagroup.gpr.service.report.powerbi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Graphiques du rapport Global, préparés avec les MÊMES règles que la page (Global.js et rendu/graphiquesRapport.js) :
// oldReportTreatment (10 premiers + « Autres »), newReportGlobalTreatment (% par agence, 10 séries + « Autres »),
// puis le rendu : valeurs nulles masquées, tri décroissant avec « Autres » en dernier, séries et agences vides retirées.
// Aucun calcul nouveau : seulement la reprise de ce que la page affiche, pour que Power BI montre les mêmes chiffres.
public final class GraphiquesPowerBi {

	public static final String AUTRES = "#8A9EBB";
	public static final Map<String, String> SECTIONS = Map.of("denonciations", "#B45309", "suggestions", "#0F766E", "taux", "#5B4B8A", "delais", "#2C7A9B");
	public static final Map<String, String> SENS = Map.of("vert", "#2F855A", "orange", "#C05621", "rouge", "#C53030");

	public record Ligne(String categorie, String serie, double valeur, String couleur, int ordre) {}

	// Un graphique tel qu'affiché : section (onglet), sous-titre de la page, rendu, unité et lignes dessinées.
	public static class Graphique {
		public String cle, section, sousTitre, titre, rendu, accent;
		public boolean pct, demi;
		public final List<Ligne> lignes = new ArrayList<>();
		public Double valeur; // jauge
	}

	private final Map<String, Object> rapport;
	private final Map<String, Map<String, String>> couleursPage;
	private final String principale;

	public GraphiquesPowerBi(Map<String, Object> rapport, Map<String, Map<String, String>> couleursPage, String principale) {
		this.rapport = rapport;
		this.couleursPage = couleursPage == null ? Map.of() : couleursPage;
		this.principale = principale;
	}

	/* ── Lecture souple du JSON du rapport ── */
	@SuppressWarnings("unchecked")
	static Object chemin(Object racine, String... cles) {
		Object o = racine;
		for (String c : cles) {
			if (!(o instanceof Map)) return null;
			o = ((Map<String, Object>) o).get(c);
		}
		return o;
	}
	@SuppressWarnings("unchecked")
	static List<Object> liste(Object o) { return o instanceof List ? (List<Object>) o : new ArrayList<>(); }
	@SuppressWarnings("unchecked")
	static Map<String, Object> carte(Object o) { return o instanceof Map ? (Map<String, Object>) o : new LinkedHashMap<>(); }
	static String texte(Object o) { return o == null ? null : String.valueOf(o); }

	// nombre() de la page : parseFloat, 0 si ce n'est pas un nombre.
	static double nombre(Object v) {
		if (v instanceof Number n) return Double.isFinite(n.doubleValue()) ? n.doubleValue() : 0;
		if (v == null) return 0;
		Matcher m = Pattern.compile("^\\s*[-+]?(\\d+\\.?\\d*|\\.\\d+)([eE][-+]?\\d+)?").matcher(String.valueOf(v));
		return m.find() ? Double.parseDouble(m.group()) : 0;
	}
	// parseFloat(x.toLocaleString("en-US", { maximumFractionDigits: 2 })) : arrondi à 2 décimales.
	static double arrondi2(double v) { return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue(); }
	// (x).toFixed(1) : chiffre clé « Taux résolution » de la page.
	static double toFixed1(double v) { return Double.isFinite(v) ? new BigDecimal(v).setScale(1, RoundingMode.HALF_UP).doubleValue() : 0; }
	// (x).toFixed(2) puis parseFloat.
	static double toFixed2(double v) { return Double.isFinite(v) ? new BigDecimal(v).setScale(2, RoundingMode.HALF_UP).doubleValue() : 0; }
	static boolean estAutres(String l) { return l != null && l.trim().equalsIgnoreCase("autres"); }

	// pleine() : rgba(r,g,b,0.5) → même teinte opaque ; renvoyée en #RRGGBB pour Power BI.
	static String hex(Object c) {
		if (c == null) return null;
		String s = String.valueOf(c).trim();
		Matcher m = Pattern.compile("^rgba?\\(\\s*([\\d.]+)\\s*,\\s*([\\d.]+)\\s*,\\s*([\\d.]+)", Pattern.CASE_INSENSITIVE).matcher(s);
		if (m.find()) return String.format("#%02X%02X%02X", (int) Double.parseDouble(m.group(1)), (int) Double.parseDouble(m.group(2)), (int) Double.parseDouble(m.group(3)));
		if (s.matches("#[0-9a-fA-F]{6}")) return s.toUpperCase(Locale.ROOT);
		if (s.matches("#[0-9a-fA-F]{3}")) return ("#" + s.charAt(1) + s.charAt(1) + s.charAt(2) + s.charAt(2) + s.charAt(3) + s.charAt(3)).toUpperCase(Locale.ROOT);
		return null;
	}

	// Mois abrégés de la page (Nov, Déc, Jan…).
	private static final Map<String, String> MOIS = Map.ofEntries(Map.entry("janvier", "Jan"), Map.entry("janv", "Jan"), Map.entry("fevrier", "Fév"),
			Map.entry("fevr", "Fév"), Map.entry("mars", "Mars"), Map.entry("avril", "Avr"), Map.entry("mai", "Mai"), Map.entry("juin", "Juin"),
			Map.entry("juillet", "Juil"), Map.entry("aout", "Août"), Map.entry("septembre", "Sept"), Map.entry("octobre", "Oct"),
			Map.entry("novembre", "Nov"), Map.entry("decembre", "Déc"));
	static String moisCourt(String l) {
		if (l == null) return null;
		String[] mots = l.trim().split("\\s+");
		String cle = Normalizer.normalize(mots[0], Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
		if (!MOIS.containsKey(cle)) return l;
		mots[0] = MOIS.get(cle);
		return String.join(" ", mots);
	}

	/* ── Données « Chart.js » préparées comme la page ── */
	record Serie(String label, List<Object> data, Object couleur) {}
	record Donnees(List<String> labels, List<Serie> series) {}

	// { labels, datas, backgroundColors } tel quel (anneaux genre, gravité, satisfaction, type ; barres « nombre par agence »).
	static Donnees simple(Object info, String label) {
		List<String> labels = new ArrayList<>();
		liste(chemin(info, "labels")).forEach(l -> labels.add(texte(l)));
		return new Donnees(labels, List.of(new Serie(label, liste(chemin(info, "datas")), chemin(info, "backgroundColors"))));
	}

	// { labels, datasets | data : [{ label, data, backgroundColor }] } tel quel.
	static Donnees multi(Object info, String cleSeries) {
		List<String> labels = new ArrayList<>();
		liste(chemin(info, "labels")).forEach(l -> labels.add(texte(l)));
		List<Serie> series = new ArrayList<>();
		for (Object s : liste(chemin(info, cleSeries))) {
			Object bg = chemin(s, "backgroundColor");
			series.add(new Serie(texte(chemin(s, "label")), liste(chemin(s, "data")), bg != null ? bg : chemin(s, "borderColor")));
		}
		return new Donnees(labels, series);
	}

	// oldReportTreatment(info, 10)
	static Donnees oldReportTreatment(Object info) {
		if (info == null) return new Donnees(new ArrayList<>(), List.of(new Serie(null, new ArrayList<>(), new ArrayList<>())));
		List<Object> datas = liste(chemin(info, "datas")), labels = liste(chemin(info, "labels")), bg = liste(chemin(info, "backgroundColors"));
		record Item(String label, Object couleur, double data) {}
		List<Item> items = new ArrayList<>();
		for (int i = 0; i < datas.size(); i++) items.add(new Item(i < labels.size() ? texte(labels.get(i)) : null, i < bg.size() ? bg.get(i) : null, nombre(datas.get(i))));
		items.sort(Comparator.comparingDouble(Item::data).reversed()); // tri stable, comme Array.sort
		List<String> l = new ArrayList<>();
		List<Object> d = new ArrayList<>(), c = new ArrayList<>();
		double autres = 0;
		for (int j = 0; j < items.size(); j++) {
			if (j < 10) { l.add(items.get(j).label()); d.add(items.get(j).data()); c.add(items.get(j).couleur()); }
			else autres += items.get(j).data();
		}
		if ((long) autres > 0) { l.add("Autres"); d.add(arrondi2(autres)); c.add("rgb(10,114,153)"); }
		return new Donnees(l, List.of(new Serie(null, d, c)));
	}

	// newReportGlobalTreatment(data) : % de chaque modalité dans les plaintes de l'agence, séries au-delà de 10 dans « Autres ».
	static Donnees newReportGlobalTreatment(Object data) {
		List<String> labels = new ArrayList<>();
		Map<String, List<Object>> valeurs = new LinkedHashMap<>();
		Map<String, Object> couleurs = new LinkedHashMap<>();
		for (Map.Entry<String, Object> agence : carte(data).entrySet()) {
			labels.add(agence.getKey());
			Object totals = chemin(agence.getValue(), "totals");
			double total = "0".equals(texte(totals)) || nombre(totals) == 0 && "0.0".equals(texte(totals)) ? 1 : nombre(totals);
			for (Object a : liste(chemin(agence.getValue(), "data"))) {
				String nom = texte(chemin(a, "name"));
				double pc = toFixed2(nombre(chemin(a, "nbre")) / total * 100);
				valeurs.computeIfAbsent(nom, k -> { couleurs.put(k, chemin(a, "color")); return new ArrayList<>(); }).add(pc);
			}
		}
		List<Serie> series = new ArrayList<>();
		List<Object> autres = null;
		int i = 0;
		for (Map.Entry<String, List<Object>> s : valeurs.entrySet()) {
			if (i++ > 9) {
				if (autres == null) autres = new ArrayList<>(s.getValue());
				else for (int j = 0; j < autres.size(); j++)
					autres.set(j, arrondi2(nombre(autres.get(j))) + (j < s.getValue().size() ? arrondi2(nombre(s.getValue().get(j))) : Double.NaN));
			} else series.add(new Serie(s.getKey(), s.getValue(), couleurs.get(s.getKey())));
		}
		if (autres != null) series.add(new Serie("Autres", autres, AUTRES));
		return new Donnees(labels, series);
	}

	/* ── Rendus (graphiquesRapport.js) ── */
	private String couleurPage(String cle, String libelle) {
		Map<String, String> m = couleursPage.get(cle);
		return m == null ? null : hex(m.get(libelle));
	}
	private static Object couleurA(Object bg, int i) { return bg instanceof List<?> l ? (i < l.size() ? l.get(i) : null) : bg; }

	private void progression(Graphique g, Donnees d, String couleurFixe) {
		Serie s = d.series().isEmpty() ? null : d.series().get(0);
		record Row(String l, double v, Object c, int i) {}
		List<Row> rows = new ArrayList<>();
		for (int i = 0; s != null && i < d.labels().size(); i++) {
			double v = i < s.data().size() ? nombre(s.data().get(i)) : 0;
			if (v > 0) rows.add(new Row(d.labels().get(i), v, couleurA(s.couleur(), i), i));
		}
		rows.sort(Comparator.comparing((Row r) -> estAutres(r.l())).thenComparing(Comparator.comparingDouble(Row::v).reversed()));
		int ordre = 0;
		for (Row r : rows) {
			String c = estAutres(r.l()) ? AUTRES : couleurFixe != null ? couleurFixe : first(couleurPage(g.cle, r.l()), hex(r.c()), hex(couleurA(s.couleur(), 0)), principale);
			g.lignes.add(new Ligne(r.l(), null, r.v(), c, ++ordre));
		}
	}

	private void anneau(Graphique g, Donnees d, Function<String, String> couleurs) {
		Serie s = d.series().get(0);
		int ordre = 0;
		for (int i = 0; i < d.labels().size(); i++) {
			double v = i < s.data().size() ? nombre(s.data().get(i)) : 0;
			if (v <= 0) continue;
			String l = d.labels().get(i);
			String c = estAutres(l) ? AUTRES : first(couleurs != null ? couleurs.apply(l) : null, couleurPage(g.cle, l), hex(couleurA(s.couleur(), i)), principale);
			g.lignes.add(new Ligne(l, null, v, c, ++ordre));
		}
	}

	private void empile(Graphique g, Donnees d) {
		List<Serie> series = d.series().stream().filter(s -> s.data().stream().anyMatch(v -> nombre(v) > 0)).toList();
		int ordre = 0;
		for (int a = 0; a < d.labels().size(); a++) {
			final int ia = a;
			if (series.stream().noneMatch(s -> ia < s.data().size() && nombre(s.data().get(ia)) > 0)) continue;
			ordre++;
			for (Serie s : series) {
				double v = a < s.data().size() ? nombre(s.data().get(a)) : 0;
				String c = estAutres(s.label()) ? AUTRES : first(couleurPage(g.cle, s.label()), hex(couleurA(s.couleur(), 0)), principale);
				g.lignes.add(new Ligne(d.labels().get(a), s.label(), v, c, ordre));
			}
		}
	}

	private void chaleur(Graphique g, Donnees d) {
		List<Serie> cols = d.series().stream().filter(s -> s.data().stream().anyMatch(v -> nombre(v) > 0)).toList();
		int ordre = 0;
		for (int a = 0; a < d.labels().size(); a++) {
			final int ia = a;
			if (cols.stream().noneMatch(s -> ia < s.data().size() && nombre(s.data().get(ia)) > 0)) continue;
			ordre++;
			for (Serie s : cols) g.lignes.add(new Ligne(d.labels().get(a), s.label(), a < s.data().size() ? nombre(s.data().get(a)) : 0, g.accent, ordre));
		}
	}

	private void courbes(Graphique g, Donnees d, Function<String, String> couleurs) {
		boolean seule = d.series().size() == 1;
		for (Serie s : d.series()) {
			String c = first(couleurs != null ? couleurs.apply(s.label()) : null, couleurPage(g.cle, s.label()), hex(couleurA(s.couleur(), 0)), "#64748B");
			for (int i = 0; i < d.labels().size(); i++)
				g.lignes.add(new Ligne(moisCourt(d.labels().get(i)), seule ? null : s.label(), i < s.data().size() ? nombre(s.data().get(i)) : 0, c, i + 1));
		}
	}

	private void colonnes(Graphique g, Donnees d) {
		Serie s = d.series().isEmpty() ? null : d.series().get(0);
		for (int i = 0; i < d.labels().size(); i++)
			g.lignes.add(new Ligne(moisCourt(d.labels().get(i)), null, s != null && i < s.data().size() ? nombre(s.data().get(i)) : 0, g.accent, i + 1));
	}

	@SafeVarargs
	private static <T> T first(T... v) { for (T x : v) if (x != null) return x; return null; }

	private Graphique nouveau(String section, String cle, String sousTitre, String titre, String rendu, boolean pct, boolean demi, String accent) {
		Graphique g = new Graphique();
		g.section = section; g.cle = cle; g.sousTitre = sousTitre; g.titre = titre; g.rendu = rendu; g.pct = pct; g.demi = demi; g.accent = accent;
		return g;
	}

	private String couleurDuType(String label) {
		String l = label == null ? "" : label.toLowerCase(Locale.ROOT);
		if (l.matches(".*(claim|r[ée]clam).*")) return principale;
		if (l.matches(".*d[ée]non.*")) return SECTIONS.get("denonciations");
		if (l.contains("sugg")) return SECTIONS.get("suggestions");
		return null;
	}
	private static String couleurDeSatisfaction(String label) {
		String l = label == null ? "" : label.toLowerCase(Locale.ROOT);
		return l.contains("partiel") ? SENS.get("orange") : l.contains("non") || l.contains("insatisf") ? SENS.get("rouge") : l.contains("satisf") ? SENS.get("vert") : null;
	}

	// Tous les graphiques de la page, dans l'ordre d'affichage (onglet par onglet).
	public List<Graphique> tous() {
		Object glob = rapport.get("global"), rec = rapport.get("claimReport"), den = rapport.get("denunReport"), sug = rapport.get("suggestionReport");
		Object nv = rapport.get("newVersionStat");
		String reclam = principale, denon = SECTIONS.get("denonciations"), sugg = SECTIONS.get("suggestions"), taux = SECTIONS.get("taux"), delais = SECTIONS.get("delais");
		List<Graphique> l = new ArrayList<>();
		Graphique g;

		// Vue d'ensemble
		g = nouveau("global", "globalPieChartRef", null, "Répartition des réclamations, dénonciations et suggestions (%)", "anneau", true, true, principale);
		anneau(g, simple(chemin(glob, "repartitionClaimDenunSuggest"), null), this::couleurDuType); l.add(g);
		g = nouveau("global", "globalLineChartRef", null, "Glissement annuel des RSD", "colonnesGroupees", false, true, principale);
		courbes(g, multi(chemin(glob, "evolutionClaimDenunSuggestByYear"), "data"), this::couleurDuType); l.add(g);
		g = nouveau("global", "globalByCanalPieChartRef", null, "Répartition des réclamations, dénonciations, suggestions par modalité de dépôt (%)", "progression", true, false, principale);
		progression(g, oldReportTreatment(chemin(glob, "repartitionClaimDenuSuggestPerCanal")), null); l.add(g);
		g = nouveau("global", "globalByCanalBarChartRef", null, "Répartition des RSD par modalité de dépôt et par agence (%)", "empile", true, false, principale);
		empile(g, newReportGlobalTreatment(chemin(nv, "GeneralPerAgence", "RSDModalite"))); l.add(g);
		g = nouveau("global", "globalByObjetPieChartRef", null, "Répartition des réclamations, dénonciations par objets (%)", "progression", true, false, principale);
		progression(g, oldReportTreatment(chemin(glob, "repartitionObjectByObj")), null); l.add(g);
		g = nouveau("global", "globalByObjetBarChartRef", null, "Répartition des Réclamations et Dénonciations par objets et par agence (%)", "chaleur", true, false, principale);
		chaleur(g, newReportGlobalTreatment(chemin(nv, "GeneralPerAgence", "RSDObjet"))); l.add(g);
		g = nouveau("global", "resolutionPieChartRef", null, "Taux de résolution des plaintes", "jauge", true, false, principale);
		g.valeur = chemin(glob, "tauxResolution") == null ? 0 : nombre(chemin(glob, "tauxResolution")) * 100; l.add(g);
		g = nouveau("global", "evolutionByAgenceByAnneeBarChartRef", null, "Evolution annuelle des réclamations, dénonciations, suggestions par agence", "progression", false, false, principale);
		progression(g, evolutionParAgence(chemin(glob, "evolutionObjByYearAndAgence")), principale); l.add(g);

		// Réclamations
		g = nouveau("claim", "claimByAgencePieChartRef", "Par agence", "Répartition des réclamations par agence (%)", "progression", true, true, reclam);
		progression(g, oldReportTreatment(chemin(rec, "repartitionClaimPerAgence")), null); l.add(g);
		g = nouveau("claim", "claimByAgenceBarChartRef", "Par agence", "Nombre de réclamations par Agence", "progression", false, true, reclam);
		progression(g, simple(chemin(rec, "nbreClaimPerAgence"), "Réclamations"), null); l.add(g);
		g = nouveau("claim", "claimByCanalPieChartRef", "Par modalité de dépôt", "Répartition des réclamations par modalité de dépôt (%)", "progression", true, true, reclam);
		progression(g, oldReportTreatment(chemin(rec, "repartitionClaimPerCanal")), null); l.add(g);
		g = nouveau("claim", "claimByCanalBarChartRef", "Par modalité de dépôt", "Répartition des réclamations par modalité de dépôt et par agence (%)", "empile", true, true, reclam);
		empile(g, newReportGlobalTreatment(chemin(nv, "AgencePerModalite", "claims"))); l.add(g);
		g = nouveau("claim", "claimByObjetPieChartRef", "Par objet", "Répartition des objets des réclamations (%)", "progression", true, false, reclam);
		progression(g, oldReportTreatment(chemin(rec, "repartitionClaimPerObjet")), null); l.add(g);
		g = nouveau("claim", "claimByObjetBarChartRef", "Par objet", "Répartition des réclamations par objet par agence (%)", "chaleur", true, false, reclam);
		chaleur(g, newReportGlobalTreatment(chemin(nv, "AgencePerObjet", "claims"))); l.add(g);
		g = nouveau("claim", "claimByGenderPieChartRef", "Par genre", "Répartition des réclamations par genre (%)", "anneau", true, true, reclam);
		anneau(g, simple(chemin(rec, "repartitionClaimPerGender"), null), null); l.add(g);
		g = nouveau("claim", "claimByGenderBarChartRef", "Par genre", "Répartition des réclamations par genre par agence (%)", "empile", true, true, reclam);
		empile(g, newReportGlobalTreatment(chemin(nv, "AgencePerGenre", "claims"))); l.add(g);
		g = nouveau("claim", "claimByGravitePieChartRef", "Par niveau de gravité", "Répartition des réclamations par niveau de gravité (%)", "anneau", true, true, reclam);
		anneau(g, simple(chemin(rec, "repartitionClaimPerObjRisque"), null), null); l.add(g);
		g = nouveau("claim", "claimByGraviteBarChartRef", "Par niveau de gravité", "Répartition des réclamations par gravité de dépôt par agence (%)", "empile", true, true, reclam);
		empile(g, newReportGlobalTreatment(chemin(nv, "AgencePerGravity", "claims"))); l.add(g);
		g = nouveau("claim", "claimBySatisfactionPieChartRef", "Satisfaction des réclamants", "Répartition de la satisfaction des réclamants (%)", "anneau", true, true, taux);
		anneau(g, simple(chemin(rec, "repartitionClaimBySatisfaction"), null), GraphiquesPowerBi::couleurDeSatisfaction); l.add(g);
		g = nouveau("claim", "tauxMensuelClaimByMonthByAgenceBarChartRef", "Satisfaction des réclamants", "Taux de satisfaction des Réclamations par agence (%)", "progression", true, true, taux);
		progression(g, multi(chemin(rec, "tauxClaimSatisfactionByMonthByAgence"), "datasets"), taux); l.add(g);
		g = nouveau("claim", "tauxMensuelClaimByMonthBarChartRef", "Satisfaction des réclamants", "Taux de satisfaction mensuel des Réclamations (%)", "courbes", true, false, taux);
		courbes(g, multi(chemin(rec, "tauxClaimSatisfactionByMonth"), "datasets"), x -> taux); l.add(g);
		g = nouveau("claim", "resolutionClaimDelaiByMonthBarChartRef", "Délai de résolution", "Respect du délai de résolution des Réclamations par mois (%)", "colonnes", true, false, delais);
		colonnes(g, multi(chemin(rec, "nbreClaimTreatInDelaiOrNotByMonth"), "datasets")); l.add(g);
		g = nouveau("claim", "resolutionClaimDelaiByMonthByAgenceBarChartRef", "Délai de résolution", "Respect du délai de résolution des Réclamations par agence (%)", "progression", true, false, delais);
		progression(g, multi(chemin(rec, "nbreClaimTreatInDelaiOrNotByMonthByAgence"), "datasets"), delais); l.add(g);

		// Dénonciations
		g = nouveau("denun", "denunByAgencePieChartRef", "Par agence", "Répartition des dénonciations par agence (%)", "progression", true, true, denon);
		progression(g, oldReportTreatment(chemin(den, "repartitionDenunPerAgence")), null); l.add(g);
		g = nouveau("denun", "denunByAgenceBarChartRef", "Par agence", "Nombre de dénonciations par Agence", "progression", false, true, denon);
		progression(g, simple(chemin(den, "nbreDenunPerAgence"), "Dénonciations"), null); l.add(g);
		g = nouveau("denun", "denunByCanalPieChartRef", "Par modalité de dépôt", "Répartition des modalités de dépôt des dénonciations (%)", "progression", true, true, denon);
		progression(g, oldReportTreatment(chemin(den, "repartitionDenunPerCanal")), null); l.add(g);
		g = nouveau("denun", "denunByCanalBarChartRef", "Par modalité de dépôt", "Répartition des dénonciations par modalité de dépôt et par agence (%)", "empile", true, true, denon);
		empile(g, newReportGlobalTreatment(chemin(nv, "AgencePerModalite", "denonciations"))); l.add(g);
		g = nouveau("denun", "denunByObjetPieChartRef", "Par objet", "Répartition des objets des dénonciations (%)", "progression", true, false, denon);
		progression(g, oldReportTreatment(chemin(den, "repartitionDenunPerObjet")), null); l.add(g);
		g = nouveau("denun", "denunByObjetBarChartRef", "Par objet", "Répartition des dénonciations par objet par agence (%)", "chaleur", true, false, denon);
		chaleur(g, newReportGlobalTreatment(chemin(nv, "AgencePerObjet", "denonciations"))); l.add(g);
		g = nouveau("denun", "resolutionDenunDelaiByMonthBarChartRef", "Délai de résolution", "Respect du délai de résolution des Dénonciations par mois (%)", "colonnes", true, false, delais);
		colonnes(g, multi(chemin(rec, "nbreDenunTreatInDelaiOrNotByMonth"), "datasets")); l.add(g);
		g = nouveau("denun", "resolutionDenunDelaiByMonthByAgenceBarChartRef", "Délai de résolution", "Respect du délai de résolution des Dénonciations par agence (%)", "progression", true, false, delais);
		progression(g, multi(chemin(rec, "nbreDenunTreatInDelaiOrNotByMonthByAgence"), "datasets"), delais); l.add(g);
		g = nouveau("denun", "denunByGravitePieChartRef", "Par niveau de gravité", "Répartition des dénonciations par niveau de gravité (%)", "anneau", true, true, denon);
		anneau(g, simple(chemin(den, "repartitionDenunPerObjRisque"), null), null); l.add(g);
		g = nouveau("denun", "denunByGraviteBarChartRef", "Par niveau de gravité", "Répartition des dénonciations par gravité de dépôt par agence (%)", "empile", true, true, denon);
		empile(g, newReportGlobalTreatment(chemin(nv, "AgencePerGravity", "denonciations"))); l.add(g);

		// Suggestions
		g = nouveau("suggest", "sugByAgencePieChartRef", "Par agence", "Répartition des suggestions par agence (%)", "progression", true, true, sugg);
		progression(g, oldReportTreatment(chemin(sug, "repartitionSuggestPerAgence")), null); l.add(g);
		g = nouveau("suggest", "sugByAgenceBarChartRef", "Par agence", "Nombre de suggestions par Agence", "progression", false, true, sugg);
		progression(g, simple(chemin(sug, "nbreSuggestPerAgence"), "Suggestions"), null); l.add(g);
		g = nouveau("suggest", "sugByCanalPieChartRef", "Par modalité de dépôt", "Répartition des modalités de dépôt des suggestions (%)", "progression", true, true, sugg);
		progression(g, oldReportTreatment(chemin(sug, "repartitionSuggestPerCanal")), null); l.add(g);
		g = nouveau("suggest", "sugByCanalBarChartRef", "Par modalité de dépôt", "Répartition des suggestions par modalité de dépôt et par agence (%)", "empile", true, true, sugg);
		empile(g, newReportGlobalTreatment(chemin(nv, "AgencePerModalite", "suggestions"))); l.add(g);
		g = nouveau("suggest", "sugByGenderPieChartRef", "Par genre", "Répartition des suggestions par genre (%)", "anneau", true, true, sugg);
		anneau(g, simple(chemin(sug, "repartitionSuggestPerGender"), null), null); l.add(g);
		g = nouveau("suggest", "sugByGenderBarChartRef", "Par genre", "Répartition des suggestions par genre par agence (%)", "empile", true, true, sugg);
		empile(g, newReportGlobalTreatment(chemin(nv, "AgencePerGenre", "suggestions"))); l.add(g);
		return l;
	}

	// Évolution annuelle par agence : la page garde la première série, triée par valeur décroissante.
	private static Donnees evolutionParAgence(Object info) {
		List<Object> series = liste(chemin(info, "datasets")), labels = liste(chemin(info, "labels"));
		if (series.isEmpty() || labels.isEmpty()) return new Donnees(new ArrayList<>(), List.of(new Serie(null, new ArrayList<>(), null)));
		Object premiere = series.get(0);
		List<Object> data = liste(chemin(premiere, "data"));
		List<String> l = new ArrayList<>();
		labels.forEach(x -> l.add(texte(x)));
		return new Donnees(l, List.of(new Serie(texte(chemin(premiere, "label")), data, "#118DFF")));
	}
}
