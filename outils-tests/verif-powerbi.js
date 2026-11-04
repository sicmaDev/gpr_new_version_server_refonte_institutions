// Vérifie que le fichier Power BI contient exactement ce que la page dessine, graphique par graphique.
// Le « attendu » est calculé avec le VRAI code de la page : oldReportTreatment / newReportGlobalTreatment extraits de
// Global.js, et les rendus de rendu/graphiquesRapport.js (progression, anneau, empile100, courbes, colonnes).
// Usage : node verif-powerbi.js <email> [filtre JSON | null] [modele JSON | null]
const crypto = require("crypto"), fs = require("fs"), zlib = require("zlib"), vm = require("vm");
const FRONT = "C:/xampp/htdocs/GPR/REFONTE_GPR/sicmaDev-sicmaDev-gpr_new_version_client_refonte_institutions/src/pages/Rapports/";
const secret = Buffer.from(process.env.GPR_JWT_SECRET.trim(), "base64");
const b64 = (o) => Buffer.from(JSON.stringify(o)).toString("base64url");
const jeton = (email) => { const t = Math.floor(Date.now() / 1000), h = b64({ alg: "HS256" }), p = b64({ sub: email, iat: t, exp: t + 900 });
  return `${h}.${p}.${crypto.createHmac("sha256", secret).update(`${h}.${p}`).digest("base64url")}`; };
const [email = "dev@sicmagroup.com", filtreTxt = "null", modeleTxt = "null"] = process.argv.slice(2);
const filtre = JSON.parse(filtreTxt), modele = JSON.parse(modeleTxt);
const appel = async (chemin, corps, brut) => {
  const r = await fetch("http://localhost:8080/api/v1/" + chemin, { method: corps === undefined ? "GET" : "POST", headers: { Authorization: "Bearer " + jeton(email), "Content-Type": "application/json" }, body: corps === undefined ? undefined : JSON.stringify(corps) });
  if (!r.ok) throw new Error(chemin + " " + r.status);
  return brut ? Buffer.from(await r.arrayBuffer()) : r.json();
};

/* ── Code de la page ── */
const src = fs.readFileSync(FRONT + "Global.js", "utf8");
const extraire = (nom) => { const i = src.indexOf(`  const ${nom} = (`); let n = 0, j = src.indexOf("{", src.indexOf("=>", i));
  for (let k = j; k < src.length; k++) { if (src[k] === "{") n++; else if (src[k] === "}" && --n === 0) return src.slice(i, k + 2); } };
const rendu = fs.readFileSync(FRONT + "rendu/graphiquesRapport.js", "utf8").replace(/^import .*$/gm, "").replace(/^export /gm, "");
const couleurs = fs.readFileSync(FRONT + "rendu/couleursRapport.js", "utf8");
const AUTRES = /AUTRES = "([^"]+)"/.exec(couleurs)[1];
const ctx = { console, Chart: { register() {}, defaults: { plugins: { legend: { labels: { generateLabels: () => [] } } } } }, AUTRES, TEXTE: {} };
vm.createContext(ctx);
vm.runInContext(rendu + "\n" + extraire("newReportGlobalTreatment").replace("const newReportGlobalTreatment", "var newReportGlobalTreatment") + "\n" + extraire("oldReportTreatment").replace("const oldReportTreatment", "var oldReportTreatment")
  + "\nthis.f = { progression, anneau, empile100, courbes, colonnes, moisCourt, newReportGlobalTreatment, oldReportTreatment };", ctx);
const F = ctx.f;
const nombre = (v) => { const n = typeof v === "number" ? v : parseFloat(v); return Number.isFinite(n) ? n : 0; };

/* ── Les données de chaque graphique, comme dans le useEffect de Global.js ── */
function attendus(rap) {
  const g = rap.global, c = rap.claimReport, d = rap.denunReport, s = rap.suggestionReport, nv = rap.newVersionStat || {};
  const simple = (o, label) => ({ labels: o?.labels ?? [], datasets: [{ label, data: o?.datas ?? [], backgroundColor: o?.backgroundColors ?? [] }] });
  const evo = g.evolutionObjByYearAndAgence; let bag = { labels: [], datasets: [] };
  if (evo?.datasets?.length > 0 && evo?.labels?.length > 0) { const comb = evo.labels.map((l, i) => ({ label: l, data: evo.datasets[0].data[i] })).sort((a, b) => b.data - a.data);
    bag = { labels: comb.map((x) => x.label), datasets: [{ label: evo.datasets[0].label, data: comb.map((x) => x.data), backgroundColor: "#118DFF" }] }; }
  const N = F.newReportGlobalTreatment, O = F.oldReportTreatment;
  return {
    globalPieChartRef: ["anneau", simple(g.repartitionClaimDenunSuggest)],
    globalLineChartRef: ["courbes", { labels: g.evolutionClaimDenunSuggestByYear.labels ?? [], datasets: g.evolutionClaimDenunSuggestByYear.data }],
    globalByCanalPieChartRef: ["progression", O(g.repartitionClaimDenuSuggestPerCanal), { pct: true }],
    globalByCanalBarChartRef: ["empile", N(nv.GeneralPerAgence?.RSDModalite ?? [])],
    globalByObjetPieChartRef: ["progression", O(g.repartitionObjectByObj), { pct: true }],
    globalByObjetBarChartRef: ["chaleur", N(nv.GeneralPerAgence?.RSDObjet ?? [])],
    evolutionByAgenceByAnneeBarChartRef: ["progression", bag, { pct: false, couleur: "#000" }],
    claimByAgencePieChartRef: ["progression", O(c.repartitionClaimPerAgence), { pct: true }],
    claimByAgenceBarChartRef: ["progression", simple(c.nbreClaimPerAgence, "Réclamations"), { pct: false }],
    claimByCanalPieChartRef: ["progression", O(c.repartitionClaimPerCanal), { pct: true }],
    claimByCanalBarChartRef: ["empile", N(nv.AgencePerModalite?.claims ?? [])],
    claimByObjetPieChartRef: ["progression", O(c.repartitionClaimPerObjet), { pct: true }],
    claimByObjetBarChartRef: ["chaleur", N(nv.AgencePerObjet?.claims ?? [])],
    claimByGenderPieChartRef: ["anneau", simple(c.repartitionClaimPerGender)],
    claimByGenderBarChartRef: ["empile", N(nv.AgencePerGenre?.claims ?? [])],
    claimByGravitePieChartRef: ["anneau", simple(c.repartitionClaimPerObjRisque)],
    claimByGraviteBarChartRef: ["empile", N(nv.AgencePerGravity?.claims ?? [])],
    claimBySatisfactionPieChartRef: ["anneau", simple(c.repartitionClaimBySatisfaction)],
    tauxMensuelClaimByMonthByAgenceBarChartRef: ["progression", c.tauxClaimSatisfactionByMonthByAgence ?? [], { pct: true, couleur: "#000" }],
    tauxMensuelClaimByMonthBarChartRef: ["courbes", c.tauxClaimSatisfactionByMonth ?? [], { pct: true }],
    resolutionClaimDelaiByMonthBarChartRef: ["colonnes", c.nbreClaimTreatInDelaiOrNotByMonth ?? [], { couleur: "#000" }],
    resolutionClaimDelaiByMonthByAgenceBarChartRef: ["progression", c.nbreClaimTreatInDelaiOrNotByMonthByAgence ?? [], { pct: true, couleur: "#000" }],
    denunByAgencePieChartRef: ["progression", O(d.repartitionDenunPerAgence), { pct: true }],
    denunByAgenceBarChartRef: ["progression", simple(d.nbreDenunPerAgence, "Dénonciations"), { pct: false }],
    denunByCanalPieChartRef: ["progression", O(d.repartitionDenunPerCanal), { pct: true }],
    denunByCanalBarChartRef: ["empile", N(nv.AgencePerModalite?.denonciations ?? [])],
    denunByObjetPieChartRef: ["progression", O(d.repartitionDenunPerObjet), { pct: true }],
    denunByObjetBarChartRef: ["chaleur", N(nv.AgencePerObjet?.denonciations ?? [])],
    resolutionDenunDelaiByMonthBarChartRef: ["colonnes", c.nbreDenunTreatInDelaiOrNotByMonth ?? [], { couleur: "#000" }],
    resolutionDenunDelaiByMonthByAgenceBarChartRef: ["progression", c.nbreDenunTreatInDelaiOrNotByMonthByAgence ?? [], { pct: true, couleur: "#000" }],
    denunByGravitePieChartRef: ["anneau", simple(d.repartitionDenunPerObjRisque)],
    denunByGraviteBarChartRef: ["empile", N(nv.AgencePerGravity?.denonciations ?? [])],
    sugByAgencePieChartRef: ["progression", O(s.repartitionSuggestPerAgence), { pct: true }],
    sugByAgenceBarChartRef: ["progression", simple(s.nbreSuggestPerAgence, "Suggestions"), { pct: false }],
    sugByCanalPieChartRef: ["progression", O(s.repartitionSuggestPerCanal), { pct: true }],
    sugByCanalBarChartRef: ["empile", N(nv.AgencePerModalite?.suggestions ?? [])],
    sugByGenderPieChartRef: ["anneau", simple(s.repartitionSuggestPerGender)],
    sugByGenderBarChartRef: ["empile", N(nv.AgencePerGenre?.suggestions ?? [])],
  };
}
// Lignes dessinées (catégorie, série, valeur) à partir du rendu de la page.
function dessine(type, data, reglages = {}) {
  if (type === "chaleur") { // CarteChaleur
    const cols = (data?.datasets || []).filter((s) => (s.data || []).some((v) => nombre(v) > 0));
    const lignes = (data?.labels || []).map((n, a) => ({ n, a })).filter((r) => cols.some((s) => nombre(s.data[r.a]) > 0));
    return lignes.flatMap((r) => cols.map((s) => [r.n, s.label, nombre(s.data[r.a])]));
  }
  const fn = { progression: F.progression, anneau: F.anneau, empile: F.empile100, courbes: F.courbes, colonnes: F.colonnes }[type];
  const r = fn(data, reglages), dss = r.data.datasets.filter((d) => !d.rail), labels = r.data.labels;
  const mois = type === "courbes" || type === "colonnes" ? F.moisCourt : (x) => x;
  if (type === "empile") return labels.flatMap((l, i) => dss.map((d) => [l, d.label, nombre(d.data[i])]));
  if (type === "courbes") return dss.flatMap((d) => labels.map((l, i) => [mois(l), dss.length === 1 ? null : d.label, nombre(d.data[i])]));
  return labels.map((l, i) => [mois(l), null, nombre(dss[0].data[i])]);
}

/* ── Lecture du .pbit ── */
function dezipper(buf) {
  const fichiers = {}; let fin = buf.length - 22; while (buf.readUInt32LE(fin) !== 0x06054b50) fin--;
  let p = buf.readUInt32LE(fin + 16); const n = buf.readUInt16LE(fin + 10);
  for (let i = 0; i < n; i++) {
    const meth = buf.readUInt16LE(p + 10), taille = buf.readUInt32LE(p + 20), ln = buf.readUInt16LE(p + 28), le = buf.readUInt16LE(p + 30), lc = buf.readUInt16LE(p + 32), off = buf.readUInt32LE(p + 42);
    const nom = buf.slice(p + 46, p + 46 + ln).toString("utf8"), d = off + 30 + buf.readUInt16LE(off + 26) + buf.readUInt16LE(off + 28);
    const brut = buf.slice(d, d + taille); fichiers[nom] = meth === 8 ? zlib.inflateRawSync(brut) : brut; p += 46 + ln + le + lc;
  }
  return fichiers;
}
// #table de Power Query → lignes JavaScript.
function lignesM(expression) {
  const t = expression.join("\n"), i = t.indexOf("}), {") >= 0 ? t.indexOf("], {") : t.indexOf("], {"), corps = t.slice(i + 4, t.lastIndexOf("})"));
  const lignes = []; let k = 0;
  const valeur = () => { if (corps[k] === '"') { let s = ""; k++; for (;;) { if (corps[k] === '"') { if (corps[k + 1] === '"') { s += '"'; k += 2; continue; } k++; break; } s += corps[k++]; } return s.replace(/#\(lf\)/g, "\n"); }
    const m = /^(null|[-0-9.Ee+]+)/.exec(corps.slice(k)); k += m[0].length; return m[0] === "null" ? null : Number(m[0]); };
  while (k < corps.length) { if (corps[k] === "{") { k++; const l = []; for (;;) { while (corps[k] === " ") k++; l.push(valeur()); while (corps[k] === " ") k++; if (corps[k] === ",") { k++; continue; } k++; break; } lignes.push(l); } else k++; }
  return lignes;
}

(async () => {
  const rap = filtre ? await appel("report/filtered", JSON.parse(JSON.stringify(filtre))) : (await appel("report/global")).content;
  const pbit = await appel("report/powerbi", { filtre, sections: null, modele, couleurs: {}, couleurPrincipale: "#005081", filtresAffiches: [], paysCode: "BJ" }, true);
  const carte = await appel("report/carte", filtre);
  const z = dezipper(pbit), schema = JSON.parse(z["DataModelSchema"].toString("utf16le"));
  const table = (nom) => lignesM(schema.model.tables.find((t) => t.name === nom).partitions[0].source.expression);
  const graph = table("Graphiques"), ind = table("Indicateurs");
  let ok = 0, ko = 0; const pres = new Set(graph.map((l) => l[0]));
  const att = attendus(rap);
  for (const [cle, [type, data, reglages]] of Object.entries(att)) {
    const section = /^(claim|tauxMensuel|resolutionClaim)/.test(cle) ? "claim" : /^(denun|resolutionDenun)/.test(cle) ? "denun" : /^sug/.test(cle) ? "suggest" : "global";
    if (modele && modele[section] && modele[section][cle] === false) { if (pres.has(cle)) { ko++; console.log("✗", cle, "retiré du modèle mais présent"); } else ok++; continue; }
    const voulu = dessine(type, data, reglages).map((r) => JSON.stringify([r[0], r[1], +r[2].toFixed(9)]));
    const recu = graph.filter((l) => l[0] === cle).map((l) => JSON.stringify([l[1], l[2], +Number(l[3]).toFixed(9)]));
    if (JSON.stringify(voulu) === JSON.stringify(recu)) ok++;
    else { ko++; console.log("✗", cle, "\n   page    :", voulu.slice(0, 6).join(" "), voulu.length, "\n   PowerBI :", recu.slice(0, 6).join(" "), recu.length); }
  }
  // Chiffres clés, statuts et jauge
  const v = (code) => ind.find((l) => l[0] === code)?.[2];
  const kpi = [["KPI_RECLAMATIONS", rap.claimReport.basicStats.total], ["KPI_DENONCIATIONS", rap.denunReport.basicStats.total], ["KPI_SUGGESTIONS", rap.suggestionReport.basicStats.total],
    ["KPI_TAUX", +(parseFloat(rap.global.tauxResolution) * 100).toFixed(1)], ["TAUX_RESOLUTION", nombre(rap.global.tauxResolution) * 100]];
  for (const [sec, k] of [["CLAIM", "claimReport"], ["DENUN", "denunReport"], ["SUGGEST", "suggestionReport"]]) {
    kpi.push([sec + "_TOTAL", rap[k].basicStats.total]);
    for (const [st, val] of Object.entries(rap[k].basicStats.statusAndValue || {})) if (ind.some((l) => l[0] === sec + "_" + st)) kpi.push([sec + "_" + st, val ?? 0]);
  }
  for (const [code, val] of kpi) { if (v(code) !== undefined && Math.abs(v(code) - val) < 1e-9) ok++; else { ko++; console.log("✗", code, "page", val, "PowerBI", v(code)); } }
  // Tableaux récapitulatifs
  const recap = [["Tableau réclamations", "claimStatsAndValue", false], ["Tableau dénonciations", "denunStatsAndValue", false], ["Tableau suggestions", "suggestStatsAndValue", true]];
  for (const [nom, cle, premier] of recap) {
    if (!schema.model.tables.some((t) => t.name === nom)) continue;
    const attendu = (rap.statistic[cle] || []).flatMap((e) => (premier ? Object.entries(e).slice(0, 1) : Object.entries(e)))
      .map(([l, x]) => [l, x === null ? (premier ? "— non applicable avec un filtre par objet" : null) : String(x)]);
    const recu = table(nom).map((l) => [l[0].replace(/\u200B/g, ""), l[1]]);
    if (JSON.stringify(attendu) === JSON.stringify(recu)) ok++; else { ko++; console.log("✗", nom, "\n   page    :", JSON.stringify(attendu).slice(0, 300), "\n   PowerBI :", JSON.stringify(recu).slice(0, 300)); }
  }
  // Carte
  if (schema.model.tables.some((t) => t.name === "Agences")) {
    const ag = table("Agences"), att2 = (carte.agences || []).map((a) => [a.libelle, a.reclamations + a.denonciations + a.suggestions, a.horsDelaiPct]);
    const rec2 = ag.map((l) => [l[0], l[5], l[13]]);
    if (JSON.stringify(att2) === JSON.stringify(rec2)) ok++; else { ko++; console.log("✗ carte", JSON.stringify(att2).slice(0, 200), JSON.stringify(rec2).slice(0, 200)); }
  }
  // Aucune donnée personnelle : aucun nom de client, téléphone ou contenu de plainte dans le fichier.
  const tout = Object.entries(z).filter(([n]) => !n.endsWith(".png")).map(([n, b]) => (n.includes("StaticResources") || n.endsWith(".xml") ? b.toString("utf8") : b.toString("utf16le"))).join(" ");
  // (« Email » seul est un canal de dépôt : on cherche de vraies adresses et de vrais numéros.)
  for (const [nom, re] of [["adresse e-mail", /[\w.-]+@[\w-]+\.[a-z]{2,}/i], ["téléphone", /(?<![\d.])\+?\d{3}[ ]?\d{2}[ ]?\d{2}[ ]?\d{2}[ ]?\d{2}(?![\d.])/], ["champ client", /client_first_and_last_name|code_client|"content"/]]) {
    const m = re.exec(tout); if (m) { ko++; console.log("✗ donnée sensible trouvée :", nom, m[0]); }
  }
  console.log(`${email} filtre=${filtreTxt.slice(0, 80)} : ${ok} contrôles identiques, ${ko} écarts ; pages : ${JSON.parse(z["Report/Layout"].toString("utf16le")).sections.map((s) => s.displayName).join(" | ")}`);
  if (ko) process.exitCode = 1;
})().catch((e) => { console.error("ECHEC", e.message); process.exit(1); });
