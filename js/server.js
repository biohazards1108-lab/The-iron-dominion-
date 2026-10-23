(function () {
  'use strict';
  // Live status stays unavailable until a verified integration exists.
  const statusEl = document.getElementById('server-status');
  const playersEl = document.getElementById('players');
  const tpsEl = document.getElementById('tps');
  if (statusEl) {
    statusEl.textContent = 'unavailable';
    statusEl.classList.remove('online', 'offline');
  }
  if (playersEl) playersEl.textContent = 'unavailable';
  if (tpsEl) tpsEl.textContent = 'unavailable';
})();
