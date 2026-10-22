(function () {
  'use strict';
  // Do not query the retired Tekkit host or display its status as Bedrock Dominion.
  // Live status will be enabled after the new server address and status integration are verified.
  const statusEl = document.getElementById('server-status');
  const playersEl = document.getElementById('players');
  const tpsEl = document.getElementById('tps');
  if (statusEl) {
    statusEl.textContent = 'IN DEVELOPMENT';
    statusEl.classList.remove('online', 'offline');
  }
  if (playersEl) playersEl.textContent = 'Coming soon';
  if (tpsEl) tpsEl.textContent = '—';
})();
