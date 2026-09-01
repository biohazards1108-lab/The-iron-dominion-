(function () {
  'use strict';

  const statusEl = document.getElementById('server-status');
  const playersEl = document.getElementById('players');
  const tpsEl = document.getElementById('tps');

  // The official site is hosted on GitHub Pages, so use a browser-safe public
  // status service instead of a serverless /api route that GitHub Pages cannot run.
  const SERVER_HOST = 'the-iron-dominion-dev.g.akliz.net';
  const STATUS_URL = `https://api.mcstatus.io/v2/status/java/${encodeURIComponent(SERVER_HOST)}?query=false&timeout=5`;
  const POLL_MS = 60000;
  const REQUEST_TIMEOUT_MS = 8000;

  if (!statusEl && !playersEl && !tpsEl) return;

  let pollTimer = null;
  let requestInFlight = false;
  let requestController = null;

  function finiteNumber(value) {
    const number = Number(value);
    return Number.isFinite(number) ? number : null;
  }

  function render(data) {
    const online = data && data.online === true;
    const players = online ? finiteNumber(data.players && data.players.online) : null;
    const maxPlayers = online ? finiteNumber(data.players && data.players.max) : null;
    const tps = online ? finiteNumber(data.tps) : null;

    if (statusEl) {
      statusEl.textContent = online ? 'ONLINE' : 'OFFLINE';
      statusEl.classList.toggle('online', online);
      statusEl.classList.toggle('offline', !online);
    }

    if (playersEl) {
      playersEl.textContent = players === null
        ? '0'
        : (maxPlayers === null ? String(players) : `${players} / ${maxPlayers}`);
    }

    if (tpsEl) tpsEl.textContent = tps === null ? '—' : tps.toFixed(1);
  }

  function renderUnavailable() {
    if (statusEl) {
      statusEl.textContent = 'UNAVAILABLE';
      statusEl.classList.remove('online', 'offline');
    }
    if (playersEl) playersEl.textContent = '—';
    if (tpsEl) tpsEl.textContent = '—';
  }

  async function fetchServer() {
    if (requestInFlight || document.visibilityState === 'hidden') return;
    requestInFlight = true;

    if (requestController) requestController.abort();
    requestController = new AbortController();
    const controller = requestController;
    const timeout = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

    try {
      const response = await fetch(`${STATUS_URL}&t=${Date.now()}`, {
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
        console.warn('Iron Dominion live server status unavailable:', error);
        renderUnavailable();
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
