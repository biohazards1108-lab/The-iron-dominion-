(function () {
  'use strict';

  const statusEl = document.getElementById('server-status');
  const playersEl = document.getElementById('players');
  const tpsEl = document.getElementById('tps');
  const source = 'api/server.json';
  const POLL_MS = 15000;
  const REQUEST_TIMEOUT_MS = 7000;

  if (!statusEl && !playersEl && !tpsEl) return;

  let requestController = null;
  let pollTimer = null;
  let requestInFlight = false;

  function finiteNumber(value) {
    const number = Number(value);
    return Number.isFinite(number) ? number : null;
  }

  function render(data) {
    const hasLiveData = data && data.live === true;
    const online = hasLiveData && data.online === true;
    const players = hasLiveData ? finiteNumber(data.players) : null;
    const maxPlayers = hasLiveData ? finiteNumber(data.maxPlayers) : null;
    const tps = hasLiveData ? finiteNumber(data.tps) : null;

    if (statusEl) {
      statusEl.textContent = hasLiveData ? (online ? 'ONLINE' : 'OFFLINE') : 'UNAVAILABLE';
      statusEl.classList.toggle('online', hasLiveData && online);
      statusEl.classList.toggle('offline', hasLiveData && !online);
    }

    if (playersEl) {
      playersEl.textContent = players === null
        ? '—'
        : (maxPlayers === null ? String(players) : `${players} / ${maxPlayers}`);
    }

    if (tpsEl) tpsEl.textContent = tps === null ? '—' : tps.toFixed(1);
  }

  function renderError() {
    render({ live: false });
  }

  async function fetchServer() {
    if (requestInFlight || document.visibilityState === 'hidden') return;
    requestInFlight = true;

    if (requestController) requestController.abort();
    requestController = new AbortController();
    const controller = requestController;
    const timeout = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

    try {
      const response = await fetch(`${source}?t=${Date.now()}`, {
        cache: 'no-store',
        headers: { Accept: 'application/json' },
        signal: controller.signal
      });

      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const data = await response.json();
      if (!data || typeof data !== 'object' || Array.isArray(data)) {
        throw new Error('Invalid status payload');
      }
      render(data);
    } catch (error) {
      if (error && error.name !== 'AbortError') {
        console.warn('Iron Dominion server status unavailable:', error);
        renderError();
      }
    } finally {
      window.clearTimeout(timeout);
      requestInFlight = false;
    }
  }

  function schedulePolling() {
    if (pollTimer !== null) window.clearInterval(pollTimer);
    pollTimer = window.setInterval(fetchServer, POLL_MS);
  }

  document.addEventListener('visibilitychange', function () {
    if (document.visibilityState === 'visible') fetchServer();
  });

  fetchServer();
  schedulePolling();
})();
