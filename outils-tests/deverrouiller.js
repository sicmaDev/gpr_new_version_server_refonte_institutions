const mdp = process.env.GPR_TEST_MDP;
const expr = `(async () => {
  const p = document.querySelector("input[type=password]"); if (!p) return "pas de champ";
  const s = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "value").set; s.call(p, ${JSON.stringify(mdp)});
  p.dispatchEvent(new Event("input", { bubbles: true })); p.dispatchEvent(new Event("change", { bubbles: true }));
  await new Promise(r => setTimeout(r, 300));
  [...document.querySelectorAll("button")].find(b => b.textContent.trim() === "Déverrouiller").click();
  return "envoyé";
})()`;
fetch("http://127.0.0.1:9400/eval", { method: "POST", body: expr }).then(r => r.text()).then(console.log);
