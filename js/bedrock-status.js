(() => {
  "use strict";
  // Live polling is paused until the new Akliz-assigned Geyser port is verified.
  const host = "the-iron-Dominion.g.akliz.net";
  const statusNodes = [...document.querySelectorAll("[data-bedrock-status]")];
  const playersNodes = [...document.querySelectorAll("[data-bedrock-players]")];
  const maxNodes = [...document.querySelectorAll("[data-bedrock-max]")];
  const versionNodes = [...document.querySelectorAll("[data-bedrock-version]")];
  const checkedNodes = [...document.querySelectorAll("[data-bedrock-checked]")];
  const setText = (nodes, value) => nodes.forEach(node => { node.textContent = value; });
  setText(statusNodes, "PORT VERIFICATION PENDING");
  setText(playersNodes, "unavailable");
  setText(maxNodes, "unavailable");
  setText(versionNodes, "unavailable");
  setText(checkedNodes, "Live checks will resume after the Akliz-assigned Geyser port is confirmed for " + host + ".");
})();