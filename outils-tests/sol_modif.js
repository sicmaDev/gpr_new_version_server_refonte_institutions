window.__modifSolution = async (objetActuel, nouvelObjet, nouveauTexte, titre) => {
 await __annonce(titre);
 const l=[...document.querySelectorAll("tr")].find(t=>t.querySelector("td")?.innerText.trim()===objetActuel && /SOLUTION TEST/i.test(t.innerText)); if(!l) return "ligne absente";
 await __cliquer(l.querySelector("[data-testid=EditIcon]").closest("button"), "Clic sur « Modifier » (solution de " + objetActuel + ")");
 const d = await __dialogue();
 if (nouvelObjet) { await __ouvrirListe(d.querySelector("[role=button],[role=combobox]"), "Changement de l'objet pour « " + nouvelObjet + " »"); const o=[...document.querySelectorAll("[role=option]")].find(x=>x.textContent.trim()===nouvelObjet); await __montrer(o); o.click(); await __p(800); }
 if (nouveauTexte) await __saisir(d.querySelector("textarea"), nouveauTexte, "Nouveau texte de la solution");
 await __cliquer(__bouton(d, /^Modifier$/), "Clic sur « Modifier »");
 const m = await __message(); await __p(1500);
 const ouvert=document.querySelector("[role=dialog]"); if (ouvert) { const b=__bouton(ouvert, /^Annuler$/); if (b) await __cliquer(b, "Fermeture du formulaire (Annuler)"); }
 return m; };
true