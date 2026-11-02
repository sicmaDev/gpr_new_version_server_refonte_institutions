package com.sicmagroup.gpr.service.report.powerbi;

import static com.sicmagroup.gpr.service.report.powerbi.PowerBi.couleur;
import static com.sicmagroup.gpr.service.report.powerbi.PowerBi.litteral;
import static com.sicmagroup.gpr.service.report.powerbi.PowerBi.m;
import static com.sicmagroup.gpr.service.report.powerbi.PowerBi.proprietes;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.sicmagroup.gpr.service.report.carte.CarteAgence;
import com.sicmagroup.gpr.service.report.powerbi.GraphiquesPowerBi.Graphique;
import com.sicmagroup.gpr.service.report.powerbi.GraphiquesPowerBi.Ligne;
import com.sicmagroup.gpr.service.report.powerbi.PowerBi.Champ;
import com.sicmagroup.gpr.service.report.powerbi.PowerBi.Colonne;
import com.sicmagroup.gpr.service.report.powerbi.PowerBi.Mesure;
import com.sicmagroup.gpr.service.report.powerbi.PowerBi.Morceau;
import com.sicmagroup.gpr.service.report.powerbi.PowerBi.Visuel;

// Rapport Global en fichier Power BI : une page par onglet de la page GPR (vue d'ensemble, réclamations, dénonciations,
// suggestions), plus la cartographie et une page d'informations. Chaque graphique du modèle actif devient un visuel
// natif lu dans la table « Graphiques » (une ligne = un point dessiné par la page), filtré sur sa clé.
// Données agrégées uniquement : libellés d'agence, de canal, d'objet, de mois, de gravité ou de genre, et des chiffres.
public final class RapportPowerBi {

	private static final double LARGEUR = 1280, MARGE = 24, ECART = 16, UTILE = LARGEUR - 2 * MARGE, DEMI = (UTILE - ECART) / 2;
	private static final double HAUTEUR_MAX_PAGE = 6000;
	public static final double[] SEUILS_HORS_DELAI = { 15, 30 }; // mêmes seuils que la carte de la page (SEUILS_HORS_DELAI)
	private static final String CHIFFRE = "#1E293B", LIBELLE = "#64748B", TITRE = "#0F172A";
	static final String NON_APPLICABLE = "— non applicable avec un filtre par objet";

	private final Map<String, Object> rapport;
	private final List<CarteAgence> agences;
	private final DemandeExportPowerBi demande;
	private final String principale;
	private final FichierPbit fichier = new FichierPbit();
	private final AtomicInteger compteur = new AtomicInteger();
	private int ordrePage = 0;

	public RapportPowerBi(Map<String, Object> rapport, List<CarteAgence> agences, DemandeExportPowerBi demande) {
		this.rapport = rapport;
		this.agences = agences == null ? List.of() : agences;
		this.demande = demande;
		this.principale = GraphiquesPowerBi.hex(demande.getCouleurPrincipale()) != null ? GraphiquesPowerBi.hex(demande.getCouleurPrincipale()) : "#005081";
	}

	private boolean section(String s) {
		Map<String, Boolean> sec = demande.getSections();
		return sec == null || !Boolean.FALSE.equals(sec.get(s));
	}
	private boolean dansLeModele(String section, String cle) {
		Map<String, Map<String, Boolean>> mo = demande.getModele();
		if (mo == null || mo.get(section) == null) return true;
		return !Boolean.FALSE.equals(mo.get(section).get(cle));
	}

	/* ── Mise en page : visuels placés de haut en bas, pages « (suite) » si une partie est trop longue ── */
	private final class Mise {
		final String nom;
		final List<Visuel> visuels = new ArrayList<>();
		double y = MARGE;
		int suite = 0;
		Mise(String nom) { this.nom = nom; }
		void place(List<Visuel> bloc, double hauteur) {
			if (y + hauteur > HAUTEUR_MAX_PAGE && y > 200) { termine(); suite++; y = MARGE; }
			for (Visuel v : bloc) { v.position(v.x, y + v.y, v.largeur, v.hauteur); visuels.add(v); }
			y += hauteur + ECART;
		}
		void termine() {
			if (visuels.isEmpty()) return;
			fichier.page(PowerBi.page(suite == 0 ? nom : nom + " (suite " + suite + ")", ordrePage++, y + MARGE, new ArrayList<>(visuels), compteur));
			visuels.clear();
		}
	}

	private static Visuel enTete(String titre, String accent, String sous) {
		List<List<Morceau>> p = new ArrayList<>();
		p.add(List.of(new Morceau("▌ ", true, 18, accent), new Morceau(titre, true, 18, TITRE)));
		if (sous != null) p.add(List.of(new Morceau(sous, false, 10, LIBELLE)));
		return PowerBi.texte(p).position(MARGE, 0, UTILE, sous == null ? 44 : 64);
	}
	private static Visuel sousTitre(String texte, String accent) {
		return PowerBi.texte(List.of(List.of(new Morceau("▌ ", true, 11, accent), new Morceau(texte.toUpperCase(Locale.FRENCH), true, 11, "#374151")))).position(MARGE, 0, UTILE, 34);
	}

	/* ── Indicateurs (cartes chiffrées) ── */
	private final List<List<Object>> indicateurs = new ArrayList<>();
	private Visuel indicateur(String code, String libelle, Double valeur, boolean pct, String accent) {
		indicateurs.add(Arrays.asList(code, libelle, valeur, (double) indicateurs.size() + 1));
		Visuel v = new Visuel("card").champ("Values", Champ.mesure("Indicateurs", pct ? "Indicateur (%)" : "Indicateur"))
				.filtre(PowerBi.filtreEgal("Indicateurs", "Code", code))
				.objet("labels", proprietes("color", couleur(CHIFFRE), "fontSize", litteral("20D")))
				.objet("categoryLabels", proprietes("show", litteral("false")));
		v.titre(libelle);
		v.cadre("title", m("show", litteral("true"), "text", litteral(PowerBi.lit(libelle)), "fontColor", couleur(accent), "fontSize", litteral("9D"), "bold", litteral("true")));
		return v;
	}
	private List<Visuel> rangee(List<Visuel> cartes, double hauteur) {
		double l = (UTILE - ECART * (cartes.size() - 1)) / cartes.size();
		for (int i = 0; i < cartes.size(); i++) cartes.get(i).position(MARGE + i * (l + ECART), 0, l, hauteur);
		return cartes;
	}

	/* ── Graphiques ── */
	private final List<List<Object>> lignesGraphiques = new ArrayList<>();

	private static double hauteur(Graphique g) {
		long categories = g.lignes.stream().map(Ligne::categorie).distinct().count();
		return switch (g.rendu) {
			case "progression" -> Math.max(150, 52 + categories * 26 + 56);
			case "anneau" -> 340;
			case "empile" -> Math.max(220, 90 + categories * 30 + 50);
			case "chaleur" -> Math.max(240, 110 + categories * 24);
			case "courbes" -> 400;
			case "colonnes" -> 360;
			default -> 300;
		};
	}

	private Visuel visuel(Graphique g) {
		for (Ligne l : g.lignes) lignesGraphiques.add(Arrays.asList(g.cle, l.categorie(), l.serie(), l.valeur(), l.couleur(), (double) l.ordre()));
		Champ cat = Champ.colonne("Graphiques", "Catégorie"), ser = Champ.colonne("Graphiques", "Série"), ord = Champ.min("Graphiques", "Ordre");
		Champ val = Champ.mesure("Graphiques", g.pct ? "Pourcentage" : "Nombre");
		Visuel v;
		switch (g.rendu) {
			case "progression" -> {
				double haut = g.lignes.stream().mapToDouble(Ligne::valeur).max().orElse(0), pas = g.pct || haut <= 200 ? 10 : 50;
				double fin = g.pct ? Math.min(100, (Math.floor(haut / 10) + 1) * 10) : (Math.floor(haut / pas) + 1) * pas;
				v = new Visuel("barChart").champ("Category", cat).champ("Y", val).champ("Tooltips", ord).triCroissant(ord)
						.objet("dataPoint", PowerBi.remplissageParChamp("Graphiques", "Couleur"))
						.objet("labels", proprietes("show", litteral("true"), "color", couleur("#374151")))
						.objet("valueAxis", proprietes("start", litteral("0D"), "end", litteral(fin + "D"), "showAxisTitle", litteral("false")))
						.objet("categoryAxis", proprietes("showAxisTitle", litteral("false")));
			}
			case "anneau" -> v = new Visuel("donutChart").champ("Category", cat).champ("Y", val).champ("Tooltips", ord).triCroissant(ord)
					.objet("dataPoint", PowerBi.remplissageParChamp("Graphiques", "Couleur"))
					.objet("legend", proprietes("show", litteral("true"), "position", litteral("'Right'")))
					.objet("labels", proprietes("show", litteral("true"), "labelStyle", litteral("'Data value'")));
			case "empile" -> {
				v = new Visuel("hundredPercentStackedBarChart").champ("Category", cat).champ("Series", ser).champ("Y", val).champ("Tooltips", ord).triCroissant(ord)
						.objet("legend", proprietes("show", litteral("true"), "position", litteral("'Top'")))
						.objet("labels", proprietes("show", litteral("true")))
						.objet("valueAxis", proprietes("showAxisTitle", litteral("false")))
						.objet("categoryAxis", proprietes("showAxisTitle", litteral("false")));
				couleursSeries(v, g);
			}
			case "chaleur" -> v = new Visuel("pivotTable").champ("Rows", cat).champ("Columns", ser).champ("Values", val)
					.objet("values", m("properties", m("backColor", m("solid", m("color", m("expr", m("FillRule", m("Input", val.expressionEntite(),
							"FillRule", m("linearGradient2", m("min", m("color", m("Literal", m("Value", PowerBi.lit("#F8FAFC")))),
									"max", m("color", m("Literal", m("Value", PowerBi.lit(g.accent)))),
									"nullColoringStrategy", m("strategy", m("Literal", m("Value", "'asZero'"))))))))))),
							"selector", m("data", List.of(m("dataViewWildcard", m("matchingOption", 1))), "metadata", val.ref())))
					.objet("values", m("properties", m("fontColor", m("solid", m("color", m("expr", m("FillRule", m("Input", val.expressionEntite(),
							"FillRule", m("linearGradient3", m("min", m("color", m("Literal", m("Value", "'#1F2937'"))),
									"mid", m("color", m("Literal", m("Value", "'#1F2937'"))),
									"max", m("color", m("Literal", m("Value", "'#FFFFFF'"))),
									"nullColoringStrategy", m("strategy", m("Literal", m("Value", "'asZero'"))))))))))),
							"selector", m("data", List.of(m("dataViewWildcard", m("matchingOption", 1))), "metadata", val.ref())))
					.objet("subTotals", proprietes("rowSubtotals", litteral("false"), "columnSubtotals", litteral("false")));
			case "courbes" -> {
				boolean seule = g.lignes.stream().allMatch(l -> l.serie() == null);
				v = new Visuel("lineChart").champ("Category", cat).champ("Y", val).champ("Tooltips", ord).triCroissant(ord)
						.objet("lineStyles", proprietes("showMarker", litteral("true")))
						.objet("legend", proprietes("show", litteral(seule ? "false" : "true"), "position", litteral("'Bottom'")))
						.objet("labels", proprietes("show", litteral(seule ? "true" : "false")))
						.objet("categoryAxis", proprietes("showAxisTitle", litteral("false")))
						.objet("valueAxis", g.pct ? proprietes("start", litteral("0D"), "end", litteral("100D"), "showAxisTitle", litteral("false"))
								: proprietes("start", litteral("0D"), "showAxisTitle", litteral("false")));
				if (seule) {
					String c = g.lignes.isEmpty() ? g.accent : g.lignes.get(0).couleur();
					v.objet("dataPoint", proprietes("fill", couleur(c)));
				} else {
					v.champ("Series", ser);
					couleursSeries(v, g);
				}
			}
			case "colonnes" -> v = new Visuel("columnChart").champ("Category", cat).champ("Y", val).champ("Tooltips", ord).triCroissant(ord)
					.objet("dataPoint", proprietes("fill", couleur(g.accent)))
					.objet("labels", proprietes("show", litteral("true")))
					.objet("valueAxis", proprietes("start", litteral("0D"), "end", litteral("100D"), "showAxisTitle", litteral("false")))
					.objet("categoryAxis", proprietes("showAxisTitle", litteral("false")));
			default -> { // jauge du taux de résolution : rouge 0–30, orange 30–80, vert 80–100 (proportions de la jauge de la page)
				double t = g.valeur == null ? 0 : g.valeur;
				String c = t < 30 ? GraphiquesPowerBi.SENS.get("rouge") : t < 80 ? GraphiquesPowerBi.SENS.get("orange") : GraphiquesPowerBi.SENS.get("vert");
				v = new Visuel("gauge").champ("Y", Champ.mesure("Indicateurs", "Indicateur (%)"))
						.filtre(PowerBi.filtreEgal("Indicateurs", "Code", "TAUX_RESOLUTION"))
						.objet("axis", proprietes("min", litteral("0D"), "max", litteral("100D")))
						.objet("dataPoint", proprietes("fill", couleur(c), "target", couleur("#374151")));
			}
		}
		if (!"jauge".equals(g.rendu)) v.filtre(PowerBi.filtreEgal("Graphiques", "Graphique", g.cle));
		v.titre(g.titre);
		v.cadre("background", m("show", litteral("true"), "color", couleur("#FFFFFF"), "transparency", litteral("0D")));
		v.cadre("border", m("show", litteral("true"), "color", couleur("#E2E8F0"), "radius", litteral("8D")));
		v.cadre("divider", m("show", litteral("true"), "color", couleur(g.accent), "width", litteral("3D")));
		return v;
	}

	private static void couleursSeries(Visuel v, Graphique g) {
		Map<String, String> vues = new LinkedHashMap<>();
		for (Ligne l : g.lignes) if (l.serie() != null) vues.putIfAbsent(l.serie(), l.couleur());
		vues.forEach((s, c) -> v.objet("dataPoint", PowerBi.remplissageSerie("Graphiques", "Série", s, c)));
	}

	// Place les graphiques d'une section, groupés par sous-titre ; deux demi-largeurs consécutives partagent une rangée.
	private void graphiques(Mise mise, List<Graphique> liste, String accent) {
		String sousTitre = null;
		for (int i = 0; i < liste.size(); i++) {
			Graphique g = liste.get(i);
			if (g.sousTitre != null && !g.sousTitre.equals(sousTitre)) {
				sousTitre = g.sousTitre;
				mise.place(List.of(sousTitre(sousTitre, accent)), 34);
			}
			Graphique suivant = i + 1 < liste.size() ? liste.get(i + 1) : null;
			if (g.demi && suivant != null && suivant.demi && java.util.Objects.equals(suivant.sousTitre, g.sousTitre)) {
				double h = Math.max(hauteur(g), hauteur(suivant));
				mise.place(List.of(visuel(g).position(MARGE, 0, DEMI, h), visuel(suivant).position(MARGE + DEMI + ECART, 0, DEMI, h)), h);
				i++;
			} else {
				double h = hauteur(g);
				mise.place(List.of(visuel(g).position(MARGE, 0, g.demi ? DEMI : UTILE, h)), h);
			}
		}
	}

	/* ── Tableaux récapitulatifs ── */
	// Valeur comme la page l'affiche (nombre JavaScript : 30.0 → « 30 »).
	static String commeLaPage(Object v) {
		if (v == null) return null;
		if (v instanceof Number n) {
			double d = n.doubleValue();
			return d == Math.rint(d) && Math.abs(d) < 1e15 ? String.valueOf((long) d) : BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
		}
		return String.valueOf(v);
	}

	private void tableau(Mise mise, String table, String cleStat, String accent, boolean suggestions) {
		List<List<Object>> lignes = new ArrayList<>();
		Set<String> vus = new LinkedHashSet<>();
		for (Object element : GraphiquesPowerBi.liste(GraphiquesPowerBi.chemin(rapport, "statistic", cleStat))) {
			Map<String, Object> e = GraphiquesPowerBi.carte(element);
			List<Map.Entry<String, Object>> entrees = new ArrayList<>(e.entrySet());
			// La page des suggestions n'affiche que la première entrée de chaque élément (boucle « for ... in » avec return).
			if (suggestions && entrees.size() > 1) entrees = entrees.subList(0, 1);
			for (Map.Entry<String, Object> en : entrees) {
				String libelle = en.getKey();
				while (!vus.add(libelle)) libelle += "​"; // libellés uniques (tri par la colonne Ordre)
				String valeur = commeLaPage(en.getValue());
				if (valeur == null && suggestions) valeur = NON_APPLICABLE;
				lignes.add(Arrays.asList(libelle, valeur, (double) lignes.size() + 1));
			}
		}
		fichier.table(PowerBi.table(table, List.of(Colonne.texte("Libellé").trieePar("Ordre"), Colonne.texte("Valeur"), Colonne.nombre("Ordre").masquee()), lignes, null));
		mise.place(List.of(sousTitre("Tableau récapitulatif", accent)), 34);
		double h = Math.max(120, 50 + lignes.size() * 26);
		Visuel v = new Visuel("tableEx").champ("Values", Champ.colonne(table, "Libellé")).champ("Values", Champ.colonne(table, "Valeur"))
				.triCroissant(Champ.colonne(table, "Libellé"))
				.objet("columnHeaders", proprietes("fontColor", couleur("#FFFFFF"), "backColor", couleur(accent)))
				.position(MARGE, 0, UTILE, h);
		v.cadre("border", m("show", litteral("true"), "color", couleur("#E2E8F0"), "radius", litteral("8D")));
		mise.place(List.of(v), h);
	}

	/* ── Onglets ── */
	private List<Graphique> graphiquesDe(List<Graphique> tous, String section) {
		return tous.stream().filter(g -> g.section.equals(section) && dansLeModele(section, g.cle)).toList();
	}
	private static Double total(Object section) {
		Object t = GraphiquesPowerBi.chemin(section, "basicStats", "total");
		return t == null ? null : GraphiquesPowerBi.nombre(t);
	}
	private static Double statut(Object section, String s) {
		return GraphiquesPowerBi.nombre(GraphiquesPowerBi.chemin(section, "basicStats", "statusAndValue", s));
	}

	private void onglet(String nom, String section, String accent, String nomTotal, Object donnees, String[][] statuts, List<Graphique> tous, String table, String cleStat) {
		Mise mise = new Mise(nom);
		double total = total(donnees) == null ? 0 : total(donnees);
		mise.place(List.of(enTete(nom, accent, null)), 44);
		List<Visuel> cartes = new ArrayList<>();
		cartes.add(indicateur(section.toUpperCase(Locale.ROOT) + "_TOTAL", nomTotal + (total > 1 ? "s" : "") + " au total", total, false, accent));
		for (String[] s : statuts) cartes.add(indicateur(section.toUpperCase(Locale.ROOT) + "_" + s[0], s[1], statut(donnees, s[0]), false, s[2]));
		if (cartes.size() > 6) {
			mise.place(rangee(cartes.subList(0, 5), 86), 86);
			mise.place(rangee(cartes.subList(5, cartes.size()), 86), 86);
		} else mise.place(rangee(cartes, 86), 86);
		graphiques(mise, graphiquesDe(tous, section), accent);
		tableau(mise, table, cleStat, accent, "suggest".equals(section));
		mise.termine();
	}

	/* ── Cartographie ── */
	private static long volume(CarteAgence a) { return a.getReclamations() + a.getDenonciations() + a.getSuggestions(); }
	private static boolean localisee(CarteAgence a) { return a.getLatitude() != null && a.getLongitude() != null; }
	static String couleurHorsDelai(Double p) {
		if (p == null) return "#94A3B8";
		return p < SEUILS_HORS_DELAI[0] ? GraphiquesPowerBi.SENS.get("vert") : p <= SEUILS_HORS_DELAI[1] ? GraphiquesPowerBi.SENS.get("orange") : GraphiquesPowerBi.SENS.get("rouge");
	}
	private static String pourcent(Double p) {
		if (p == null) return "n.d.";
		DecimalFormat f = new DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(Locale.FRANCE));
		return f.format(p) + " %";
	}

	private void cartographie() {
		Mise mise = new Mise("Cartographie");
		List<CarteAgence> loc = agences.stream().filter(RapportPowerBi::localisee).toList();
		List<CarteAgence> nonLoc = agences.stream().filter(a -> !localisee(a)).toList();
		List<List<Object>> lignes = new ArrayList<>();
		for (CarteAgence a : agences) lignes.add(Arrays.asList(a.getLibelle(), a.getVille(), a.getQuartier(), a.getLatitude(), a.getLongitude(), (double) volume(a),
				(double) a.getReclamations(), (double) a.getReclamationsTraitees(), (double) a.getReclamationsNonTraitees(),
				(double) a.getDenonciations(), (double) a.getDenonciationsTraitees(), (double) a.getDenonciationsNonTraitees(), (double) a.getSuggestions(),
				a.getHorsDelaiPct(), pourcent(a.getHorsDelaiPct()), a.getSatisfactionPct(), couleurHorsDelai(a.getHorsDelaiPct()), localisee(a) ? "Oui" : "Non"));
		fichier.table(PowerBi.table("Agences", List.of(Colonne.texte("Agence"), Colonne.texte("Ville"), Colonne.texte("Quartier"),
				Colonne.nombre("Latitude"), Colonne.nombre("Longitude"), Colonne.nombre("Volume").format("#,0"),
				Colonne.nombre("Réclamations").format("#,0"), Colonne.nombre("Réclamations traitées").format("#,0"), Colonne.nombre("Réclamations non traitées").format("#,0"),
				Colonne.nombre("Dénonciations").format("#,0"), Colonne.nombre("Dénonciations traitées").format("#,0"), Colonne.nombre("Dénonciations non traitées").format("#,0"),
				Colonne.nombre("Suggestions").format("#,0"), Colonne.nombre("Part hors délai").format("#,0.#\" %\""), Colonne.texte("% hors délai"),
				Colonne.nombre("Satisfaction").format("#,0.#\" %\""), Colonne.texte("Couleur").masquee(), Colonne.texte("Localisée")), lignes, null));

		mise.place(List.of(enTete("Cartographie des plaintes par agence", principale, "Taille : volume total (réclamations + dénonciations + suggestions). Couleur : part traitée hors délai (réclamations + dénonciations).")), 64);
		double hCarte = 560, lCarte = 760;
		List<Visuel> bloc = new ArrayList<>();
		if (loc.isEmpty()) {
			bloc.add(PowerBi.texte(List.of(List.of(new Morceau("Aucune agence n'est encore localisée.", true, 11, GraphiquesPowerBi.SENS.get("orange"))),
					List.of(new Morceau("Renseignez la latitude et la longitude dans Configurations, Points de service. Les chiffres de chaque agence restent dans le classement ci-contre.", false, 10, "#374151"))))
					.position(MARGE, 0, lCarte, 120));
		} else {
			bloc.add(nuage(loc, lCarte, hCarte));
		}
		Champ ag = Champ.colonne("Agences", "Agence");
		Visuel classement = new Visuel("tableEx").champ("Values", ag).champ("Values", Champ.somme("Agences", "Volume")).champ("Values", Champ.colonne("Agences", "% hors délai"))
				.triDecroissant(Champ.somme("Agences", "Volume"))
				.objet("columnHeaders", proprietes("fontColor", couleur("#FFFFFF"), "backColor", couleur(principale)))
				.objet("values", m("properties", m("fontColor", m("solid", m("color", m("expr", Champ.min("Agences", "Couleur").expressionEntite())))),
						"selector", m("metadata", "Agences.% hors délai")))
				.objet("total", proprietes("totals", litteral("false")))
				.position(MARGE + lCarte + ECART, 0, UTILE - lCarte - ECART, hCarte);
		classement.titre("Classement des agences");
		bloc.add(classement);
		mise.place(bloc, hCarte);

		List<List<Morceau>> legende = new ArrayList<>();
		legende.add(List.of(new Morceau("Couleur : part traitée hors délai   ", true, 10, "#374151"),
				new Morceau("● ", true, 12, GraphiquesPowerBi.SENS.get("vert")), new Morceau("< 15 %    ", false, 10, "#374151"),
				new Morceau("● ", true, 12, GraphiquesPowerBi.SENS.get("orange")), new Morceau("15–30 %    ", false, 10, "#374151"),
				new Morceau("● ", true, 12, GraphiquesPowerBi.SENS.get("rouge")), new Morceau("> 30 %    ", false, 10, "#374151"),
				new Morceau("● ", true, 12, "#94A3B8"), new Morceau("n.d. (aucune plainte traitée)", false, 10, "#374151")));
		if ("BJ".equalsIgnoreCase(demande.getPaysCode()) && !loc.isEmpty())
			legende.add(List.of(new Morceau("Fond : contours des départements du Bénin (geoBoundaries, domaine public). Position des points approximative sur le fond.", false, 9, LIBELLE)));
		if (!nonLoc.isEmpty())
			legende.add(List.of(new Morceau(nonLoc.size() + (nonLoc.size() > 1 ? " agences non localisées" : " agence non localisée") + " : ", true, 10, GraphiquesPowerBi.SENS.get("orange")),
					new Morceau(String.join(", ", nonLoc.stream().map(CarteAgence::getLibelle).toList()) + ". Coordonnées à renseigner dans Configurations, Points de service.", false, 10, "#374151")));
		mise.place(List.of(PowerBi.texte(legende).position(MARGE, 0, UTILE, 40 + 22 * legende.size())), 40 + 22 * legende.size());
		mise.termine();
	}

	private Visuel nuage(List<CarteAgence> loc, double largeur, double hauteur) {
		double lonMin = loc.stream().mapToDouble(CarteAgence::getLongitude).min().getAsDouble(), lonMax = loc.stream().mapToDouble(CarteAgence::getLongitude).max().getAsDouble();
		double latMin = loc.stream().mapToDouble(CarteAgence::getLatitude).min().getAsDouble(), latMax = loc.stream().mapToDouble(CarteAgence::getLatitude).max().getAsDouble();
		// Cadrage sur les agences avec une marge, puis élargi pour garder les proportions (1° de longitude = cos(latitude) km relatifs).
		double marge = Math.max(0.3, Math.max(lonMax - lonMin, latMax - latMin) * 0.15);
		lonMin -= marge; lonMax += marge; latMin -= marge; latMax += marge;
		double cos = Math.cos(Math.toRadians((latMin + latMax) / 2)), zoneL = largeur - 20, zoneH = hauteur - 60;
		double lonPx = (lonMax - lonMin) * cos / zoneL, latPx = (latMax - latMin) / zoneH;
		if (lonPx > latPx) { double c = (latMin + latMax) / 2, d = lonPx * zoneH / 2; latMin = c - d; latMax = c + d; }
		else { double c = (lonMin + lonMax) / 2, d = latPx * zoneL / cos / 2; lonMin = c - d; lonMax = c + d; }

		Visuel v = new Visuel("scatterChart").champ("Category", Champ.colonne("Agences", "Agence"))
				.champ("X", Champ.moyenne("Agences", "Longitude")).champ("Y", Champ.moyenne("Agences", "Latitude")).champ("Size", Champ.somme("Agences", "Volume"))
				.champ("Tooltips", Champ.somme("Agences", "Réclamations")).champ("Tooltips", Champ.somme("Agences", "Réclamations traitées"))
				.champ("Tooltips", Champ.somme("Agences", "Réclamations non traitées")).champ("Tooltips", Champ.somme("Agences", "Dénonciations"))
				.champ("Tooltips", Champ.somme("Agences", "Dénonciations traitées")).champ("Tooltips", Champ.somme("Agences", "Dénonciations non traitées"))
				.champ("Tooltips", Champ.somme("Agences", "Suggestions")).champ("Tooltips", Champ.moyenne("Agences", "Part hors délai"))
				.champ("Tooltips", Champ.moyenne("Agences", "Satisfaction"))
				.filtre(PowerBi.filtreEgal("Agences", "Localisée", "Oui"))
				.objet("dataPoint", PowerBi.remplissageParChamp("Agences", "Couleur"))
				.objet("categoryLabels", proprietes("show", litteral("true"), "color", couleur("#374151")))
				.objet("categoryAxis", proprietes("show", litteral("false"), "start", litteral(lonMin + "D"), "end", litteral(lonMax + "D"), "gridlineShow", litteral("false")))
				.objet("valueAxis", proprietes("show", litteral("false"), "start", litteral(latMin + "D"), "end", litteral(latMax + "D"), "gridlineShow", litteral("false")))
				.objet("bubbles", proprietes("bubbleSize", litteral("10L")))
				.position(MARGE, 0, largeur, hauteur);
		if ("BJ".equalsIgnoreCase(demande.getPaysCode())) {
			String nom = "fondcarte" + Integer.toHexString(Arrays.hashCode(new double[] { lonMin, lonMax, latMin, latMax })) + ".png";
			fichier.image(nom, FondCarte.benin(new FondCarte.Cadre(lonMin, lonMax, latMin, latMax), 1200, (int) Math.round(1200 * zoneH / zoneL)));
			v.objet("plotArea", proprietes("transparency", litteral("0D"), "image", m("image", m("name", litteral(PowerBi.lit(nom)),
					"url", m("expr", m("ResourcePackageItem", m("PackageName", "RegisteredResources", "PackageType", 1, "ItemName", nom))),
					"scaling", litteral("'Fill'")))));
		}
		v.titre("Cartographie des plaintes par agence");
		v.cadre("border", m("show", litteral("true"), "color", couleur("#E2E8F0"), "radius", litteral("8D")));
		return v;
	}

	/* ── Informations ── */
	private void informations() {
		Mise mise = new Mise("Informations");
		List<List<Object>> lignes = new ArrayList<>();
		String date = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm"));
		lignes.add(Arrays.asList("Exporté le", date, 1d));
		List<Map<String, String>> filtres = demande.getFiltresAffiches() == null ? List.of() : demande.getFiltresAffiches();
		if (filtres.isEmpty()) lignes.add(Arrays.asList("Filtres", "Aucun : rapport complet du périmètre de l'utilisateur", 2d));
		for (Map<String, String> f : filtres) {
			String libelle = f.get("label"), valeur = f.get("value");
			// Pas de noms d'utilisateurs dans le fichier : seulement leur nombre.
			if (libelle != null && libelle.toLowerCase(Locale.ROOT).startsWith("enregistr")) {
				int n = demande.getFiltre() != null && demande.getFiltre().getSavedBy() != null ? demande.getFiltre().getSavedBy().size() : 0;
				valeur = n + (n > 1 ? " utilisateurs sélectionnés" : " utilisateur sélectionné");
			}
			lignes.add(Arrays.asList("Filtre : " + libelle, valeur, (double) lignes.size() + 1));
		}
		lignes.add(Arrays.asList("Contenu", "Chiffres agrégés du rapport Global GPR (comptages, taux, délais par agence, mois, type). Aucune donnée personnelle.", (double) lignes.size() + 1));
		lignes.add(Arrays.asList("Périmètre", "Celui de l'utilisateur qui a exporté (mêmes règles que la page Rapport Global).", (double) lignes.size() + 1));
		fichier.table(PowerBi.table("Informations", List.of(Colonne.texte("Rubrique").trieePar("Ordre"), Colonne.texte("Valeur"), Colonne.nombre("Ordre").masquee()), lignes, null));
		mise.place(List.of(enTete("Informations sur l'export", principale, null)), 44);
		double h = 60 + lignes.size() * 30;
		Visuel v = new Visuel("tableEx").champ("Values", Champ.colonne("Informations", "Rubrique")).champ("Values", Champ.colonne("Informations", "Valeur"))
				.triCroissant(Champ.colonne("Informations", "Rubrique"))
				.objet("columnHeaders", proprietes("fontColor", couleur("#FFFFFF"), "backColor", couleur(principale)))
				.position(MARGE, 0, UTILE, h);
		mise.place(List.of(v), h);
		mise.termine();
	}

	public byte[] construire() {
		Object glob = rapport.get("global"), rec = rapport.get("claimReport"), den = rapport.get("denunReport"), sug = rapport.get("suggestionReport");
		List<Graphique> tous = new GraphiquesPowerBi(rapport, demande.getCouleurs(), principale).tous();
		String denon = GraphiquesPowerBi.SECTIONS.get("denonciations"), sugg = GraphiquesPowerBi.SECTIONS.get("suggestions"), taux = GraphiquesPowerBi.SECTIONS.get("taux");
		Map<String, String> sens = GraphiquesPowerBi.SENS;

		// Vue d'ensemble : les 4 chiffres clés (toujours affichés en tête de la page GPR), puis les graphiques globaux.
		Mise vue = new Mise("Vue d'ensemble");
		vue.place(List.of(enTete("Rapport global GPR", principale, "Export Power BI du " + ZonedDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"))
				+ ". Chiffres agrégés, filtres et modèle de rapport appliqués (voir la page Informations).")), 64);
		Object tauxResolution = GraphiquesPowerBi.chemin(glob, "tauxResolution");
		vue.place(rangee(new ArrayList<>(List.of(
				indicateur("KPI_RECLAMATIONS", "Réclamations", total(rec), false, principale),
				indicateur("KPI_DENONCIATIONS", "Dénonciations", total(den), false, denon),
				indicateur("KPI_SUGGESTIONS", "Suggestions", total(sug), false, sugg),
				indicateur("KPI_TAUX", "Taux résolution", tauxResolution == null ? null : GraphiquesPowerBi.toFixed1(GraphiquesPowerBi.nombre(tauxResolution) * 100), true, taux))), 86), 86);
		// Valeur de la jauge (même taux, non arrondi), lue par le visuel « gauge ».
		indicateurs.add(Arrays.asList("TAUX_RESOLUTION", "Taux de résolution des plaintes", tauxResolution == null ? 0d : GraphiquesPowerBi.nombre(tauxResolution) * 100, (double) indicateurs.size() + 1));
		if (section("global")) graphiques(vue, graphiquesDe(tous, "global"), principale);
		vue.termine();

		if (section("claim")) onglet("Réclamations", "claim", principale, "réclamation", rec, new String[][] {
				{ "SAVED", "À traiter", denon }, { "AFFECTED", "Affectée", GraphiquesPowerBi.SECTIONS.get("delais") }, { "DESAPPROUVED", "Désapprouvée", sens.get("rouge") },
				{ "TREAT", "Traitée", sugg }, { "SATISFIED", "Satisfait", sens.get("vert") }, { "PARTIAL_SATISFIED", "Partiellement satisfait", sens.get("orange") },
				{ "UNSATISFIED", "Non satisfait", sens.get("rouge") }, { "LITIGATION", "Contentieux", taux }, { "CLASSED", "Classée", LIBELLE } },
				tous, "Tableau réclamations", "claimStatsAndValue");
		if (section("denun")) onglet("Dénonciations", "denun", denon, "dénonciation", den, new String[][] {
				{ "SAVED", "À traiter", denon }, { "AFFECTED", "Affectée", GraphiquesPowerBi.SECTIONS.get("delais") }, { "TREAT", "Traitée", sugg } },
				tous, "Tableau dénonciations", "denunStatsAndValue");
		if (section("suggest")) onglet("Suggestions", "suggest", sugg, "suggestion", sug, new String[][] {
				{ "SAVED", "À traiter", denon }, { "ACCEPTED", "Pris en compte", sens.get("vert") }, { "UNACCEPTED", "Non pris en compte", sens.get("rouge") } },
				tous, "Tableau suggestions", "suggestStatsAndValue");
		if (section("global") && dansLeModele("global", "carteRef")) cartographie();
		informations();

		fichier.table(PowerBi.table("Indicateurs", List.of(Colonne.texte("Code"), Colonne.texte("Libellé"), Colonne.nombre("Valeur"), Colonne.nombre("Ordre").masquee()),
				indicateurs, List.of(new Mesure("Indicateur", "SUM(Indicateurs[Valeur])", "#,0"), new Mesure("Indicateur (%)", "SUM(Indicateurs[Valeur])", "#,0.0\" %\""))));
		fichier.table(PowerBi.table("Graphiques", List.of(Colonne.texte("Graphique"), Colonne.texte("Catégorie"), Colonne.texte("Série"), Colonne.nombre("Valeur"),
				Colonne.texte("Couleur"), Colonne.nombre("Ordre")), lignesGraphiques,
				List.of(new Mesure("Nombre", "SUM(Graphiques[Valeur])", "#,0.###"), new Mesure("Pourcentage", "SUM(Graphiques[Valeur])", "#,0.#\" %\""))));
		fichier.theme(theme());
		return fichier.octets();
	}

	private Map<String, Object> theme() {
		return m("name", "Thème GPR",
				"dataColors", List.of(principale, GraphiquesPowerBi.SECTIONS.get("denonciations"), GraphiquesPowerBi.SECTIONS.get("suggestions"), GraphiquesPowerBi.SECTIONS.get("taux"),
						GraphiquesPowerBi.SECTIONS.get("delais"), GraphiquesPowerBi.SENS.get("vert"), GraphiquesPowerBi.SENS.get("orange"), GraphiquesPowerBi.SENS.get("rouge")),
				"foreground", CHIFFRE, "foregroundNeutralSecondary", LIBELLE, "background", "#FFFFFF", "tableAccent", principale);
	}
}
