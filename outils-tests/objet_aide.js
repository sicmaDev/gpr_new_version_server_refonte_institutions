// Remplit le formulaire objet ouvert (ou l'ouvre) ; renvoie le message affiché. Aucune action au chargement.
window.__objet = async (o) => { const p=(ms)=>new Promise(r=>setTimeout(r,ms));
 let d=document.querySelector("[role=dialog]");
 if (!d) { [...document.querySelectorAll("button")].find(b=>b.offsetParent && b.textContent.trim()==="Ajouter").click(); for(let i=0;i<20 && !(d=document.querySelector("[role=dialog]"));i++) await p(800); await p(1000); }
 await __saisir(d.querySelector("input[placeholder^=Ex]"), o.nom); if (o.desc) await __saisir(d.querySelector("textarea"), o.desc);
 const ch = async (i, t) => { const b=d.querySelectorAll("[role=button],[role=combobox]")[i]; b.dispatchEvent(new MouseEvent("mousedown",{bubbles:true,button:0})); await p(1000); const opt=[...document.querySelectorAll("[role=option]")].find(x=>x.textContent.trim().toLowerCase().startsWith(t.toLowerCase())); if(opt){opt.click();} else document.querySelector("[role=listbox]")?.dispatchEvent(new KeyboardEvent("keydown",{key:"Escape",bubbles:true})); await p(800); return !!opt; };
 if (o.gravite) await ch(0, o.gravite); if (o.categorie) await ch(1, o.categorie);
 const n=d.querySelectorAll("input[type=number]"); if (o.delai !== undefined) await __saisir(n[0], String(o.delai)); if (o.pec !== undefined) await __saisir(n[1], String(o.pec));
 await p(800); const bt=[...d.querySelectorAll("button")].find(b=>/^Enregistrer|^Modifier$/.test(b.textContent.trim())); bt.click();
 const m = await __message(); await p(1500);
 const ouvert=document.querySelector("[role=dialog]"); const err = ouvert ? [...ouvert.querySelectorAll("p, .Mui-error, div")].filter(x=>x.children.length===0 && /requis|max|incorrect|invalide|entre|supérieur|inférieur|doit/i.test(x.textContent)).map(x=>x.textContent).slice(0,4) : [];
 if (ouvert && o.fermer !== false) [...ouvert.querySelectorAll("button")].find(b=>b.textContent.trim()==="Annuler")?.click();
 return JSON.stringify({message:m, erreurs:err}); };
true