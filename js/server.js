(function () {
  const statusEl = document.getElementById('server-status');
  const playersEl = document.getElementById('players');
  const tpsEl = document.getElementById('tps');
  const source = 'api/server.json';

  if (!statusEl && !playersEl && !tpsEl) return;

  function render(data) {
    const hasLiveData = data && data.live === true;
    const online = hasLiveData && data.online === true;
    const players = hasLiveData && Number.isFinite(Number(data.players)) ? Number(data.players) : null;
    const maxPlayers = hasLiveData && Number.isFinite(Number(data.maxPlayers)) ? Number(data.maxPlayers) : null;
    const tps = hasLiveData && Number.isFinite(Number(data.tps)) ? Number(data.tps) : null;

    if (statusEl) {
      statusEl.textContent = hasLiveData ? (online ? 'ONLINE' : 'OFFLINE') : 'UNAVAILABLE';
      statusEl.classList.toggle('online', hasLiveData && online);
      statusEl.classList.toggle('offline', hasLiveData && !online);
    }
    if (playersEl) playersEl.textContent = players === null ? '—' : (maxPlayers === null ? String(players) : `${players} / ${maxPlayers}`);
    if (tpsEl) tpsEl.textContent = tps === null ? '—' : tps.toFixed(1);
  }

  function renderError() {
    render({ live: false });
  }

  async function fetchServer() {
    try {
      const response = await fetch(`${source}?t=${Date.now()}`, { cache: 'no-store' });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      render(await response.json());
    } catch (error) {
      console.warn('Iron Dominion server status unavailable:', error);
      renderError();
    }
  }

  fetchServer();
  window.setInterval(fetchServer, 15000);
})();
