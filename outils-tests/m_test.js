window.__testerOption = async (valeur, nom) => { const r={};
 await __choixSelect(__champ(/chiffrement/),valeur,"Option « "+nom+" »"); r.port=__champ(/port/).value;
 r.enregistrement=await __enregistrer("Enregistrer avec « "+nom+" » (mot de passe laissé vide)"); await __p(2000);
 const bt=[...document.querySelector(".card-panel").querySelectorAll("button")].find(b=>b.innerText.trim()==="Tester"); await __cliquer(bt,"Clic sur « Tester »"); const d=await __dialogue(); await __p(800);
 const ch=[...d.querySelectorAll("input,textarea")].filter(x=>x.type!=="hidden");
 await __saisir(ch[0],"test.gpr.auto@example.com","Destinataire de test (adresse réservée, n'arrive chez personne)"); await __saisir(ch[1],"Test GPR "+nom,"Objet"); await __saisir(ch[2],"Essai automatique","Message");
 const env=[...d.querySelectorAll("button")].filter(b=>/Envoyer|Tester/.test(b.innerText)).pop(); await __cliquer(env,"Clic sur « Envoyer »");
 for(let i=0;i<60;i++){ await __p(1000); const t=[...document.querySelectorAll("div")].find(x=>x.children.length<4 && /Super - Mail|Oups - Mail/.test(x.textContent) && !x.closest("#__bandeau_test")); if(t){ r.envoi=t.innerText.split(String.fromCharCode(10))[0]; await __montrer(t,"Résultat : "+r.envoi); break; } }
 return r; };
true