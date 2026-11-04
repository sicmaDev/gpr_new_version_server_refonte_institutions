window.__solution = async (objet, texte, titre) => {
 let d = document.querySelector("[role=dialog]"); if (!d) { await __cliquer(__bouton(document, /^Ajouter$/), "Clic sur « Ajouter »"); d = await __dialogue(); }
 await __annonce(titre);
 await __ouvrirListe(d.querySelector("[role=button],[role=combobox]"), "Choix de l'objet « " + objet + " »");
 const o=[...document.querySelectorAll("[role=option]")].find(x=>x.textContent.trim()===objet); if (!o) return "objet absent"; await __montrer(o); o.click(); await __p(800);
 await __saisir(d.querySelector("textarea"), texte, "Saisie de la solution");
 await __cliquer(__bouton(d, /^Enregistrer/), "Clic sur « Enregistrer »");
 const m = await __message(); await __p(1500);
 const ouvert = document.querySelector("[role=dialog]"); if (ouvert) { const b=__bouton(ouvert, /^Annuler$/); if (b) await __cliquer(b, "Fermeture du formulaire (Annuler)"); }
 return m; };
true