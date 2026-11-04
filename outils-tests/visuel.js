// Aides visibles : bandeau d'annonce, surlignage orange, saisie lettre par lettre, clic après une pause. Aucune action au chargement.
window.__p = (ms) => new Promise(r => setTimeout(r, ms));
window.__annonce = async (texte) => {
  let b = document.getElementById("__bandeau_test");
  if (!b) { b = document.createElement("div"); b.id = "__bandeau_test";
    b.style.cssText = "position:fixed;top:0;left:50%;transform:translateX(-50%);z-index:2147483647;background:#111827;color:#fff;padding:10px 22px;border-radius:0 0 12px 12px;font:600 15px Arial;box-shadow:0 4px 14px rgba(0,0,0,.35);border:3px solid #f97316;border-top:none;max-width:80vw;text-align:center;pointer-events:none";
    document.body.appendChild(b); }
  b.textContent = "TEST AUTOMATIQUE  •  " + texte; await __p(1500); };
window.__montrer = async (el) => { if (!el) return; el.scrollIntoView({ block: "center", behavior: "smooth" }); await __p(500);
  const ancien = el.style.outline; el.style.outline = "4px solid #f97316"; el.style.outlineOffset = "2px"; await __p(1200); setTimeout(() => { el.style.outline = ancien; }, 2500); };
window.__cliquer = async (el, texte) => { if (texte) await __annonce(texte); await __montrer(el); el.click(); await __p(1200); };
window.__saisir = async (el, v, texte) => { if (texte) await __annonce(texte); await __montrer(el);
  const proto = el.tagName === "TEXTAREA" ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype; const set = Object.getOwnPropertyDescriptor(proto, "value").set;
  set.call(el, ""); el.dispatchEvent(new Event("input", { bubbles: true }));
  const txt = String(v); for (let i = 6; i < txt.length + 6; i += 6) { set.call(el, txt.slice(0, i)); el.dispatchEvent(new Event("input", { bubbles: true })); await __p(40); }
  await __p(700); };
window.__ouvrirListe = async (ctl, texte) => { if (texte) await __annonce(texte); await __montrer(ctl); ctl.dispatchEvent(new MouseEvent("mousedown", { bubbles: true, button: 0 })); await __p(1500); };
window.__message = async () => { for (let i = 0; i < 20; i++) { await __p(600); const t = [...document.querySelectorAll("div")].filter(x => x.id !== "__bandeau_test" && !x.closest("#__bandeau_test") && x.children.length < 6 && /Bravo|Erreur|existe|mpossible|ne peut|Attention|Super|Oups|dernier|intervient|invalide|refus/.test(x.textContent)).pop(); if (t) { await __annonce("Message affiché : " + t.textContent.replace("×", "")); return t.textContent; } } await __annonce("Aucun message affiché"); return ""; };
window.__ligne = (nom) => [...document.querySelectorAll("tr")].find(t => t.querySelector("td")?.innerText.trim() === nom);
window.__dialogue = async () => { let d; for (let i = 0; i < 25 && !(d = document.querySelector("[role=dialog]")); i++) await __p(800); await __p(1000); return d; };
window.__bouton = (racine, re) => [...racine.querySelectorAll("button")].find(b => b.offsetParent && re.test(b.textContent.trim()));
if (!window.__eveil) window.__eveil = setInterval(() => window.dispatchEvent(new MouseEvent("mousemove")), 20000);
document.documentElement.style.zoom = "0.62";
true