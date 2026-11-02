package com.sicmagroup.gpr.service.report.powerbi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicmagroup.gpr.service.report.carte.CarteAgence;

/**
 * Export Power BI du rapport Global : contenu du .pbit, sans base de données.
 * Écrit aussi target/powerbi-essai.pbit (agences localisées sur le Bénin) pour un contrôle dans Power BI Desktop.
 */
class RapportPowerBiTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @SuppressWarnings("unchecked")
    private static Map<String, Object> rapport() throws IOException {
        String json = """
            {"global":{"repartitionClaimDenunSuggest":{"labels":["Réclamations","Dénonciations","Suggestions"],"datas":[60,25,15],"backgroundColors":["#FFCE56","#ff6384","#36A2EB"]},
              "repartitionClaimDenuSuggestPerCanal":{"labels":["Email","site web"],"datas":[70,30],"backgroundColors":["rgba(10,20,30,0.5)","rgba(40,50,60,0.5)"]},
              "repartitionObjectByObj":{"labels":["objet 1"],"datas":[100],"backgroundColors":["#123456"]},
              "evolutionClaimDenunSuggestByYear":{"labels":["novembre","décembre"],"data":[{"label":"Réclamations","data":[1,2]},{"label":"Suggestions","data":[0,3]}]},
              "tauxResolution":0.25,
              "evolutionObjByYearAndAgence":{"labels":["Agence A","Agence B"],"datasets":[{"label":"2026","data":[3,7]}]}},
             "claimReport":{"basicStats":{"total":10,"statusAndValue":{"SAVED":4,"TREAT":6}},
              "repartitionClaimPerAgence":{"labels":["Agence A","Agence B"],"datas":[40,60],"backgroundColors":["#111111","#222222"]},
              "nbreClaimPerAgence":{"labels":["Agence A","Agence B"],"datas":[4,6],"backgroundColors":["#25AFBE"]},
              "repartitionClaimPerGender":{"labels":["FEMME","HOMME"],"datas":[50,50],"backgroundColors":["#FF99CC","#3399FF"]},
              "tauxClaimSatisfactionByMonth":{"labels":["Janv","Févr"],"datasets":[{"label":"Taux de satisfaction","data":[0,50]}]}},
             "denunReport":{"basicStats":{"total":5,"statusAndValue":{"SAVED":5}}},
             "suggestionReport":{"basicStats":{"total":3,"statusAndValue":{"SAVED":3}}},
             "statistic":{"claimStatsAndValue":[{"Nombre total de réclamations":10.0}],"denunStatsAndValue":[{"Nombre total de dénonciations":5}],
              "suggestStatsAndValue":[{"Nombre total de suggestions enregistrées":null},{"Nombre total de suggestions":null}]},
             "newVersionStat":{"AgencePerModalite":{"claims":{"Agence A":{"totals":4,"data":[{"name":"Email","nbre":3,"color":"#AA0000"},{"name":"site web","nbre":1,"color":"#00AA00"}]},
               "Agence B":{"totals":0,"data":[{"name":"Email","nbre":0,"color":"#AA0000"},{"name":"site web","nbre":0,"color":"#00AA00"}]}}}}}
            """;
        return JSON.readValue(json, Map.class);
    }

    private static List<CarteAgence> agences() {
        return List.of(
            CarteAgence.builder().id(1L).libelle("Agence A").latitude(6.37).longitude(2.39).reclamations(4).denonciations(2).suggestions(1).horsDelaiPct(10.0).build(),
            CarteAgence.builder().id(2L).libelle("Agence B").latitude(9.3).longitude(2.31).reclamations(6).denonciations(3).suggestions(2).horsDelaiPct(45.0).build(),
            CarteAgence.builder().id(3L).libelle("Agence C").reclamations(0).build());
    }

    private static Map<String, String> contenu(byte[] pbit) throws IOException {
        Map<String, String> r = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(pbit))) {
            for (ZipEntry e; (e = zip.getNextEntry()) != null; ) {
                byte[] b = zip.readAllBytes();
                boolean utf16 = !e.getName().contains("StaticResources") && !e.getName().endsWith(".xml");
                r.put(e.getName(), new String(b, utf16 ? StandardCharsets.UTF_16LE : StandardCharsets.UTF_8));
            }
        }
        return r;
    }

    private static DemandeExportPowerBi demande() {
        DemandeExportPowerBi d = new DemandeExportPowerBi();
        d.setCouleurPrincipale("#005081");
        d.setPaysCode("BJ");
        d.setCouleurs(Map.of("claimByAgencePieChartRef", Map.of("Agence A", "rgb(1,2,3)")));
        return d;
    }

    @Test
    void fichierCompletAvecPagesTablesEtCarte() throws IOException {
        byte[] pbit = new RapportPowerBi(rapport(), agences(), demande()).construire();
        Files.createDirectories(Path.of("target"));
        Files.write(Path.of("target", "powerbi-essai.pbit"), pbit);
        Map<String, String> f = contenu(pbit);

        assertThat(f).containsKeys("Version", "[Content_Types].xml", "DataModelSchema", "Report/Layout", "Settings", "Metadata",
                "Report/StaticResources/RegisteredResources/ThemeGPR.json");
        assertThat(f.keySet()).anyMatch(n -> n.startsWith("Report/StaticResources/RegisteredResources/fondcarte"));
        String layout = f.get("Report/Layout"), modele = f.get("DataModelSchema");
        for (String page : List.of("Vue d'ensemble", "Réclamations", "Dénonciations", "Suggestions", "Cartographie", "Informations"))
            assertThat(layout).contains("\"displayName\":\"" + page + "\"");
        for (String table : List.of("Graphiques", "Indicateurs", "Agences", "Tableau réclamations", "Tableau suggestions", "Informations"))
            assertThat(modele).contains("\"name\":\"" + table + "\"");
        // Couleur affichée par la page reprise ; suggestions sans valeur sous un filtre d'objet : mention de la page.
        assertThat(modele).contains("#010203").contains(RapportPowerBi.NON_APPLICABLE);
        // Pourcentages par agence comme la page : 3 / 4 = 75 %, agence sans plainte retirée.
        assertThat(modele).contains("{\\\"claimByCanalBarChartRef\\\", \\\"Agence A\\\", \\\"Email\\\", 75,").doesNotContain("\\\"claimByCanalBarChartRef\\\", \\\"Agence B\\\"");
    }

    @Test
    void graphiqueRetireDuModeleEtCarteRetiree() throws IOException {
        DemandeExportPowerBi d = demande();
        d.setModele(Map.of("claim", Map.of("claimByAgencePieChartRef", false), "global", Map.of("carteRef", false)));
        Map<String, String> f = contenu(new RapportPowerBi(rapport(), agences(), d).construire());
        assertThat(f.get("DataModelSchema")).doesNotContain("claimByAgencePieChartRef").contains("claimByAgenceBarChartRef");
        assertThat(f.get("Report/Layout")).doesNotContain("\"displayName\":\"Cartographie\"");
    }

    @Test
    void ongletsMasquesSelonLeTypeDePlainte() throws IOException {
        DemandeExportPowerBi d = demande();
        d.setSections(Map.of("global", false, "claim", true, "denun", false, "suggest", false));
        String layout = contenu(new RapportPowerBi(rapport(), agences(), d).construire()).get("Report/Layout");
        assertThat(layout).contains("\"displayName\":\"Réclamations\"").doesNotContain("\"displayName\":\"Dénonciations\"").doesNotContain("\"displayName\":\"Cartographie\"");
    }

    @Test
    void aucunNomDUtilisateurDansLesFiltres() throws IOException {
        DemandeExportPowerBi d = demande();
        d.setFiltresAffiches(List.of(Map.of("label", "Enregistré par", "value", "Jean Dupont, Awa Sow")));
        String modele = contenu(new RapportPowerBi(rapport(), agences(), d).construire()).get("DataModelSchema");
        assertThat(modele).doesNotContain("Jean Dupont").contains("utilisateur");
    }
}
