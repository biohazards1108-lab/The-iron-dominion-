(() => {
  "use strict";
  const HOST = "the-iron-dominion-bedrock.g.akliz.net";
  const PORT = 15300;
  const ENDPOINT = "https://api.mcsrvstat.us/bedrock/3/" + HOST + ":" + PORT;
  const statusNodes = [...document.querySelectorAll("[data-bedrock-status]")];
  const playersNodes = [...document.querySelectorAll("[data-bedrock-players]")];
  const maxNodes = [...document.querySelectorAll("[data-bedrock-max]")];
  const versionNodes = [...document.querySelectorAll("[data-bedrock-version]")];
  const checkedNodes = [...document.querySelectorAll("[data-bedrock-checked]")];
  const setText = (nodes, value) => nodes.forEach(node => { node.textContent = value; });
  const safeValue = (value, fallback = "unavailable") =>
    typeof value === "string" && value.trim() ? value.trim().slice(0, 120) : fallback;

  async function refresh() {
    setText(statusNodes, "CHECKING…");
    try {
      const response = await fetch(ENDPOINT, {
        method: "GET",
        headers: { "Accept": "application/json" },
        cache: "no-store",
        signal: AbortSignal.timeout(12000)
      });
      if (!response.ok) throw new Error("Status provider returned " + response.status);
      const data = await response.json();
      if (typeof data.online !== "boolean") throw new Error("Status provider response was incomplete");

      setText(statusNodes, data.online ? "ONLINE" : "OFFLINE");
      statusNodes.forEach(node => {
        node.dataset.state = data.online ? "online" : "offline";
        node.setAttribute("aria-label", "Bedrock server status: " + (data.online ? "online" : "offline"));
      });
      setText(playersNodes, data.online && Number.isFinite(data.players?.online) ? String(data.players.online) : (data.online ? "unavailable" : "—"));
      setText(maxNodes, data.online && Number.isFinite(data.players?.max) ? String(data.players.max) : (data.online ? "unavailable" : "—"));
      setText(versionNodes, data.online ? safeValue(data.version) : "—");
      const now = new Date().toLocaleString();
      setText(checkedNodes, now + " (status provider cache may be up to 5 minutes old)");
    } catch (_) {
      setText(statusNodes, "CHECK UNAVAILABLE");
      statusNodes.forEach(node => {
        node.dataset.state = "unavailable";
        node.setAttribute("aria-label", "Bedrock server status check unavailable");
      });
      setText(playersNodes, "unavailable");
      setText(maxNodes, "unavailable");
      setText(versionNodes, "unavailable");
      setText(checkedNodes, "Could not reach the external status provider. This does not prove the server is offline.");
    }
  }

  refresh();
  // The upstream provider caches responses; poll no more frequently than every five minutes.
  window.setInterval(refresh, 5 * 60 * 1000);
})();