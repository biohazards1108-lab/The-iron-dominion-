// Public shop status renderer.
//
// The official Bedrock Dominion store is not live. Do not publish hard-coded
// catalog entries, prices, token balances, or pretend checkout is available.
// A real catalog must come from a verified server-side source after launch.
(function () {
  'use strict';

  const container = document.getElementById('shop-container');
  if (!container) return;

  const notice = document.createElement('p');
  notice.className = 'notice';
  notice.textContent = 'UNAVAILABLE — the official store and checkout are not enabled.';
  container.replaceChildren(notice);
})();
