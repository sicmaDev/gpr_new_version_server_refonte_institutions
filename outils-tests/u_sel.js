window.__choisir = async (idx, motif, texte) => { const d=document.querySelector("[role=dialog]");
 const ctl=d.querySelectorAll("[role=combobox],[aria-haspopup=listbox]")[idx]; await __ouvrirListe(ctl, texte);
 const opts=[...document.querySelectorAll("[role=listbox] [role=option]")].filter(o=>!o.getAttribute("aria-disabled"));
 const o=opts.find(o=>motif.test(o.innerText)) || opts[0]; const t=o.innerText; await __cliquer(o,"Choix : "+t); await __p(600); return t; };
window.__erreursChamps = () => [...document.querySelector("[role=dialog]").querySelectorAll("div")].filter(x=>x.children.length===0 && /requis|invalide/i.test(x.innerText)).map(e=>e.parentElement.querySelector("label").innerText+" : "+e.innerText);
true