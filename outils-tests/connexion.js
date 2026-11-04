// Remplit le formulaire de connexion comme un utilisateur (événements React) puis clique « Se connecter ».
// Usage : node connexion.js <email> ; le mot de passe est lu dans la variable GPR_TEST_MDP (jamais écrit dans un fichier).
const [, , email] = process.argv;
const mdp = process.env.GPR_TEST_MDP;
const expr = `(async () => {
  window.dispatchEvent(new MouseEvent("mousemove"));
  const poser = (el, v) => { const s = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "value").set; s.call(el, v); el.dispatchEvent(new Event("input", { bubbles: true })); el.dispatchEvent(new Event("change", { bubbles: true })); };
  const e = document.querySelector("input[type=email], input[name=email]"), p = document.querySelector("input[type=password]");
  if (!e || !p) return "formulaire introuvable";
  poser(e, ${JSON.stringify(email)}); poser(p, ${JSON.stringify(mdp)});
  await new Promise((r) => setTimeout(r, 300));
  const b = [...document.querySelectorAll("button")].find((x) => x.textContent.trim() === "Se connecter");
  b.click();
  return "envoyé";
})()`;
fetch("http://127.0.0.1:9400/eval", { method: "POST", body: expr }).then((r) => r.text()).then(console.log);
