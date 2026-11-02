package com.sicmagroup.gpr.service.report.powerbi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

// Assemble un modèle Power BI (.pbit) : modèle de données (DataModelSchema), pages (Report/Layout), thème et images.
// Format interne repris à l'identique d'un fichier enregistré par Power BI Desktop 2.159 (octobre 2026) :
// textes en UTF-16 LE sans BOM, [Content_Types].xml en UTF-8 avec BOM. Ce format n'est pas documenté par Microsoft
// pour la génération : une version future de Power BI Desktop pourrait demander des ajustements.
public final class FichierPbit {

	private static final ObjectMapper JSON = new ObjectMapper();
	public static final String VERSION_DESKTOP = "2.159.883.0";

	private final List<Object> tables = new ArrayList<>();
	private final List<Object> pages = new ArrayList<>();
	private final Map<String, byte[]> images = new LinkedHashMap<>();
	private Object theme;

	public void table(Map<String, Object> table) { tables.add(table); }
	public void page(Map<String, Object> page) { pages.add(page); }
	public void theme(Object theme) { this.theme = theme; }
	public void image(String nom, byte[] png) { images.put(nom, png); }

	static String json(Object o) {
		try {
			return JSON.writeValueAsString(o);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	public byte[] octets() {
		Map<String, Object> modele = PowerBi.m("name", UUID.randomUUID().toString(), "compatibilityLevel", 1567, "model", PowerBi.m(
				"culture", "fr-FR",
				"dataAccessOptions", PowerBi.m("legacyRedirects", true, "returnErrorValuesAsNull", true),
				"defaultPowerBIDataSourceVersion", "powerBI_V3",
				"sourceQueryCulture", "fr-FR",
				"tables", tables,
				"annotations", List.of(PowerBi.m("name", "__PBI_TimeIntelligenceEnabled", "value", "0"), PowerBi.m("name", "PBIDesktopVersion", "value", VERSION_DESKTOP + " (Main)"))));

		List<Object> ressources = new ArrayList<>();
		ressources.add(PowerBi.m("type", 201, "path", "ThemeGPR.json", "name", "ThemeGPR"));
		images.keySet().forEach(n -> ressources.add(PowerBi.m("type", 100, "path", n, "name", n)));
		Map<String, Object> versionTheme = PowerBi.m("visual", "2.6.0", "report", "3.1.0", "page", "2.3.0");
		Map<String, Object> layout = PowerBi.m(
				"id", 0,
				"resourcePackages", List.of(PowerBi.m("resourcePackage", PowerBi.m("name", "RegisteredResources", "type", 1, "items", ressources, "disabled", false))),
				"sections", pages,
				"config", json(PowerBi.m("version", "5.70",
						"themeCollection", PowerBi.m("customTheme", PowerBi.m("name", "ThemeGPR", "type", 1, "version", versionTheme)),
						"activeSectionIndex", 0, "defaultDrillFilterOtherVisuals", true,
						"settings", PowerBi.m("useNewFilterPaneExperience", true, "allowChangeFilterTypes", true, "useStylableVisualContainerHeader", true,
								"exportDataMode", 1, "useDefaultAggregateDisplayName", true))),
				"layoutOptimization", 0);
		Map<String, Object> diagramme = PowerBi.m("version", "1.1.0", "diagrams", List.of(PowerBi.m("ordinal", 0, "scrollPosition", PowerBi.m("x", 0, "y", 0),
				"nodes", List.of(), "name", "Toutes les tables", "zoomValue", 100, "pinKeyFieldsToTop", false, "showExtraHeaderInfo", false,
				"hideKeyFieldsWhenCollapsed", false, "tablesLocked", false)), "selectedDiagram", "Toutes les tables", "defaultDiagram", "Toutes les tables");
		String types = "<?xml version=\"1.0\" encoding=\"utf-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
				+ "<Default Extension=\"json\" ContentType=\"\" /><Default Extension=\"png\" ContentType=\"\" /><Override PartName=\"/Version\" ContentType=\"\" />"
				+ "<Override PartName=\"/DataModelSchema\" ContentType=\"\" /><Override PartName=\"/DiagramLayout\" ContentType=\"\" />"
				+ "<Override PartName=\"/Report/Layout\" ContentType=\"\" /><Override PartName=\"/Settings\" ContentType=\"application/json\" />"
				+ "<Override PartName=\"/Metadata\" ContentType=\"application/json\" /></Types>";

		ByteArrayOutputStream sortie = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(sortie, StandardCharsets.UTF_8)) {
			ecrire(zip, "Version", utf16("1.28"));
			ecrire(zip, "[Content_Types].xml", avecBom(types));
			ecrire(zip, "DataModelSchema", utf16(json(modele)));
			ecrire(zip, "DiagramLayout", utf16(json(diagramme)));
			ecrire(zip, "Report/Layout", utf16(json(layout)));
			ecrire(zip, "Settings", utf16(json(PowerBi.m("Version", 4, "ReportSettings", PowerBi.m(),
					"QueriesSettings", PowerBi.m("TypeDetectionEnabled", true, "RelationshipImportEnabled", true, "Version", VERSION_DESKTOP)))));
			ecrire(zip, "Metadata", utf16(json(PowerBi.m("Version", 5, "AutoCreatedRelationships", List.of(), "CreatedFrom", "Cloud", "CreatedFromRelease", "2026.10"))));
			ecrire(zip, "Report/StaticResources/RegisteredResources/ThemeGPR.json", json(theme).getBytes(StandardCharsets.UTF_8));
			for (Map.Entry<String, byte[]> i : images.entrySet()) ecrire(zip, "Report/StaticResources/RegisteredResources/" + i.getKey(), i.getValue());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return sortie.toByteArray();
	}

	private static byte[] utf16(String s) { return s.getBytes(StandardCharsets.UTF_16LE); }
	private static byte[] avecBom(String s) {
		byte[] t = s.getBytes(StandardCharsets.UTF_8), r = new byte[t.length + 3];
		r[0] = (byte) 0xEF; r[1] = (byte) 0xBB; r[2] = (byte) 0xBF;
		System.arraycopy(t, 0, r, 3, t.length);
		return r;
	}
	private static void ecrire(ZipOutputStream zip, String nom, byte[] contenu) throws IOException {
		zip.putNextEntry(new ZipEntry(nom));
		zip.write(contenu);
		zip.closeEntry();
	}
}
