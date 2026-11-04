// Ajouter / modifier / supprimer sur les pages de configuration simples (intitulé + description), de façon visible. Aucune action au chargement.
window.__fermer = async () => { const o=document.querySelector("[role=dialog]"); if (o) { const b=__bouton(o, /^Annuler$/); if (b) await __cliquer(b, "Fermeture du formulaire (Annuler)"); } };
window.__ajouterV = async (nom, desc, titre) => { await __annonce(titre);
 await __cliquer(__bouton(document, /^Ajouter$/), "Clic sur « Ajouter »"); const d = await __dialogue();
 await __saisir(d.querySelector("input:not([type=hidden])"), nom, "Saisie de l'intitulé « " + nom + " »"); if (desc) await __saisir(d.querySelector("textarea"), desc, "Saisie de la description");
 await __cliquer(__bouton(d, /^Enregistrer/), "Clic sur « Enregistrer »"); const m = await __message(); await __p(1500); await __fermer(); return m; };
window.__modifierV = async (nom, nouveau, titre) => { await __annonce(titre); const l=__ligne(nom); if(!l) return "ligne absente";
 await __cliquer(l.querySelector("[data-testid=EditIcon]").closest("button"), "Clic sur « Modifier » (" + nom + ")"); const d = await __dialogue();
 await __saisir(d.querySelector("input:not([type=hidden])"), nouveau, "Nouvel intitulé « " + nouveau + " »");
 await __cliquer(__bouton(d, /^Modifier$/), "Clic sur « Modifier »"); const m = await __message(); await __p(1500); await __fermer(); return m; };
window.__supprimerV = async (nom, titre) => { await __annonce(titre); const l=__ligne(nom); if(!l) return "ligne absente";
 await __cliquer(l.querySelector("[data-testid=DeleteIcon]").closest("button"), "Clic sur « Supprimer » (" + nom + ")"); const d = await __dialogue();
 await __cliquer([...d.querySelectorAll("button")].filter(b=>/Supprimer/.test(b.textContent)).pop(), "Confirmation de la suppression");
 const m = await __message(); await __p(2000); await __fermer(); return m; };
true