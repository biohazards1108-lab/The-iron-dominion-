(function () {
  const statusEl = document.getElementById('server-status');
  const playersEl = document.getElementById('players');
  const tpsEl = document.getElementById('tps');
  const source = 'api/server.json';

  if (!statusEl && !playersEl && !tpsEl) return;

  function render(data) {
    const online = data.online === true;
    const players = Number.isFinite(Number(data.players)) ? Number(data.players) : 0;
    const maxPlayers = Number.isFinite(Number(data.maxPlayers)) ? Number(data.maxPlayers) : 0;
    const tps = Number.isFinite(Number(data.tps)) ? Number(data.tps) : null;

    if (statusEl) {
      statusEl.textContent = online ? 'ONLINE' : 'OFFLINE';
      statusEl.classList.toggle('online', online);
      statusEl.classList.toggle('offline', !online);
    }
    if (playersEl) playersEl.textContent = maxPlayers ? `${players} / ${maxPlayers}` : String(players);
    if (tpsEl) tpsEl.textContent = tps === null ? '—' : tps.toFixed(1);
  }

  function renderError() {
    render({ online: false, players: 0, maxPlayers: 0, tps: null });
  }

  async function fetchServer() {
    try {
      const response = await fetch(`${source}?t=${Date.now()}`, { cache: 'no-store' });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const data = await response.json();
      render(data);
    } catch (error) {
      console.warn('Iron Dominion server status unavailable:', error);
      renderError();
    }
  }

  fetchServer();
  window.setInterval(fetchServer, 15000);
})();
