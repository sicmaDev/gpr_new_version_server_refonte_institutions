// Session Edge invisible gardée ouverte sur l'interface (:3000, branchée au serveur :8080 / base gpr_sicma_verif).
// Commandes sur http://127.0.0.1:9400 :
//   GET  /login?email=..       ouvre l'application connectée sous ce compte (jeton signé localement)
//   GET  /goto?hash=/rapports/global
//   POST /eval                 corps = expression JS (peut être async), renvoie la valeur
//   GET  /shot?nom=x[&plein=1] capture PNG dans shots/
//   GET  /journal[&vider=1]    erreurs console, exceptions, requêtes échouées (>= 400)
//   GET  /stop
const fs = require("fs"), path = require("path"), http = require("http"), crypto = require("crypto"), { spawn, spawnSync } = require("child_process");
const WebSocket = require(process.env.WS_MODULE);
const SHOTS = path.join(__dirname, "shots"), TELECHARGEMENTS = path.join(__dirname, "telechargements");
fs.mkdirSync(SHOTS, { recursive: true }); fs.mkdirSync(TELECHARGEMENTS, { recursive: true });

const secret = Buffer.from(process.env.GPR_JWT_SECRET.trim(), "base64");
const b64 = (o) => Buffer.from(JSON.stringify(o)).toString("base64url");
const jeton = (email) => { const t = Math.floor(Date.now() / 1000), h = b64({ alg: "HS256" }), p = b64({ sub: email, iat: t, exp: t + 8 * 3600 });
  return `${h}.${p}.${crypto.createHmac("sha256", secret).update(`${h}.${p}`).digest("base64url")}`; };

const EDGE = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";
// Edge déjà ouvert (pilote relancé) : on s'y rattache sans le relancer ni le fermer
const dejaOuvert = spawnSync("curl", ["-s", "-m", "3", "http://127.0.0.1:9333/json/version"]).stdout?.length > 0;
const edge = dejaOuvert ? { pid: 0 } : spawn(EDGE, ["--remote-debugging-port=9333", "--remote-debugging-address=127.0.0.1","--user-data-dir=" + path.join(__dirname, "edge-cdp"), "--window-size=1400,1000", "--no-first-run", "--disable-background-timer-throttling", "--disable-renderer-backgrounding", "--disable-backgrounding-occluded-windows", "--disable-features=IntensiveWakeUpThrottling,CalculateNativeWinOcclusion", "about:blank"], { stdio: "ignore" });
const pause = (ms) => new Promise((r) => setTimeout(r, ms));
const arreterEdge = () => { if (!edge.pid) return; try { spawnSync("taskkill", ["/PID", String(edge.pid), "/T", "/F"]); } catch {} };

let s = null; const journal = [];
async function cible() {
  try { const l = await (await fetch("http://127.0.0.1:9333/json/list")).json(); const t = l.find((x) => x.type === "page" && x.url.includes("localhost:3000")); if (t) return t; } catch {}
  for (let i = 0; i < 240; i++) { try { const r = await fetch("http://127.0.0.1:9333/json/new?about:blank", { method: "PUT" }); return await r.json(); } catch { await pause(500); } }
  throw new Error("Edge injoignable");
}
function session(wsUrl) {
  const ws = new WebSocket(wsUrl, { perMessageDeflate: false, maxPayload: 512 * 1024 * 1024 });
  let id = 0; const attente = new Map(), ecoute = [];
  ws.on("message", (m) => { const d = JSON.parse(m); if (d.id && attente.has(d.id)) { const { ok, ko } = attente.get(d.id); attente.delete(d.id); d.error ? ko(new Error(JSON.stringify(d.error))) : ok(d.result); } else ecoute.forEach((f) => f(d)); });
  const envoi = (method, params = {}) => new Promise((ok, ko) => { const n = ++id; attente.set(n, { ok, ko }); ws.send(JSON.stringify({ id: n, method, params })); });
  return new Promise((ok) => ws.on("open", () => ok({ envoi, ecoute, fermer: () => ws.close() })));
}
// Chaque évaluation passe par une connexion neuve à l'onglet : une connexion figée ne bloque plus le pilote
let cibleWs = null;
const evaluerFrais = (expr) => new Promise((ok, ko) => {
  const ws = new WebSocket(cibleWs, { perMessageDeflate: false, maxPayload: 512 * 1024 * 1024 });
  const fin = setTimeout(() => { try { ws.terminate(); } catch {} ko(new Error("délai dépassé (900 s)")); }, 900000);
  ws.on("open", () => ws.send(JSON.stringify({ id: 1, method: "Runtime.evaluate", params: { expression: expr, awaitPromise: true, returnByValue: true } })));
  ws.on("message", (m) => { const d = JSON.parse(m); if (d.id !== 1) return; clearTimeout(fin); ws.close();
    if (d.error) return ko(new Error(JSON.stringify(d.error))); const r = d.result;
    ok(r.exceptionDetails ? { exception: r.exceptionDetails.exception?.description || r.exceptionDetails.text } : r.result?.value); });
  ws.on("error", (e) => { clearTimeout(fin); ko(e); });
});
const evaluer = (expr) => evaluerFrais(expr);
const evaluerAncien = async (expr) => { const r = await Promise.race([s.envoi("Runtime.evaluate", { expression: expr, awaitPromise: true, returnByValue: true }), pause(120000).then(() => { throw new Error("délai dépassé (120 s)"); })]);
  if (r.exceptionDetails) return { exception: r.exceptionDetails.exception?.description || r.exceptionDetails.text }; return r.result?.value; };

async function demarrer() {
  const t = await cible(); cibleWs = t.webSocketDebuggerUrl; s = await session(t.webSocketDebuggerUrl);
  await s.envoi("Page.enable"); await s.envoi("Runtime.enable"); await s.envoi("Network.enable");
  await s.envoi("Page.addScriptToEvaluateOnNewDocument", { source: "window.requestAnimationFrame = (cb) => setTimeout(() => cb(performance.now()), 16); window.cancelAnimationFrame = (id) => clearTimeout(id);" });
  await s.envoi("Emulation.setFocusEmulationEnabled", { enabled: true }).catch(() => {});
  await s.envoi("Emulation.clearDeviceMetricsOverride").catch(() => {});
  await s.envoi("Browser.setDownloadBehavior", { behavior: "allow", downloadPath: TELECHARGEMENTS }).catch(() => {});
  const urls = new Map();
  s.ecoute.push((d) => {
    const h = new Date().toTimeString().slice(0, 8);
    if (d.method === "Runtime.consoleAPICalled" && ["error", "warning"].includes(d.params.type))
      journal.push(`${h} console.${d.params.type}: ` + d.params.args.map((a) => a.value ?? a.description ?? "").join(" ").slice(0, 400));
    if (d.method === "Runtime.exceptionThrown") journal.push(`${h} EXCEPTION: ` + (d.params.exceptionDetails.exception?.description || d.params.exceptionDetails.text).slice(0, 600));
    if (d.method === "Network.requestWillBeSent") urls.set(d.params.requestId, d.params.request.method + " " + d.params.request.url);
    if (d.method === "Network.responseReceived" && d.params.response.status >= 400) journal.push(`${h} HTTP ${d.params.response.status}: ${urls.get(d.params.requestId) || d.params.response.url}`);
    if (d.method === "Network.loadingFailed" && !d.params.canceled) journal.push(`${h} ÉCHEC réseau (${d.params.errorText}): ${urls.get(d.params.requestId)}`);
    // Boîte de dialogue du navigateur (alert, confirm, quitter la page) : notée puis acceptée, sinon la page reste bloquée.
    if (d.method === "Page.javascriptDialogOpening") {
      journal.push(`${h} DIALOGUE ${d.params.type}: ${d.params.message}`);
      s.envoi("Page.handleJavaScriptDialog", { accept: true }).catch(() => {});
    }
  });
}

async function login(email) {
  await s.envoi("Page.navigate", { url: "http://localhost:3000/" }); await pause(2500);
  await evaluer(`sessionStorage.clear(); localStorage.clear(); const j = ${JSON.stringify(JSON.stringify(jeton(email)))}; sessionStorage.setItem("token", j); localStorage.setItem("token", j); sessionStorage.setItem("logged", "1"); true`);
  await s.envoi("Page.navigate", { url: "http://localhost:3000/" }); await pause(8000);
  return evaluer("location.href");
}
async function capture(nom, plein) {
  let h = 1000;
  if (plein) {
    const interne = await evaluer(`(() => { let d = 0; for (const e of document.querySelectorAll("*")) { const x = e.scrollHeight - e.clientHeight; if (x > d && getComputedStyle(e).overflowY !== "visible") d = x; } return Math.ceil(d); })()`) || 0;
    h = Math.min(1000 + interne, 12000);
    await s.envoi("Emulation.setDeviceMetricsOverride", { width: 1400, height: h, deviceScaleFactor: 1, mobile: false }); await pause(4000);
  }
  const r = await s.envoi("Page.captureScreenshot", { format: "png" });
  const f = path.join(SHOTS, nom + ".png"); fs.writeFileSync(f, Buffer.from(r.data, "base64"));
  if (plein) await s.envoi("Emulation.clearDeviceMetricsOverride").catch(() => {});
  return f;
}

http.createServer(async (req, res) => {
  const u = new URL(req.url, "http://x"); let corps = "";
  req.on("data", (c) => (corps += c));
  req.on("end", async () => {
    let out;
    try {
      if (u.pathname === "/login") out = await login(u.searchParams.get("email"));
      else if (u.pathname === "/goto") { await evaluer(`location.hash = ${JSON.stringify(u.searchParams.get("hash"))}; true`); await pause(+(u.searchParams.get("attente") || 6000)); out = await evaluer("location.href"); }
      else if (u.pathname === "/eval") out = await evaluer(corps);
      else if (u.pathname === "/shot") out = await capture(u.searchParams.get("nom"), u.searchParams.get("plein"));
      else if (u.pathname === "/journal") { out = journal.slice(); if (u.searchParams.get("vider")) journal.length = 0; }
      else if (u.pathname === "/stop") { res.end("arrêt"); arreterEdge(); process.exit(0); }
      else out = "commande inconnue";
    } catch (e) { out = { erreur: String(e) }; }
    res.writeHead(200, { "Content-Type": "application/json; charset=utf-8" }); res.end(JSON.stringify(out, null, 1));
  });
}).listen(9400, "127.0.0.1", async () => { await demarrer(); console.log("PRET"); });
process.on("exit", arreterEdge);
