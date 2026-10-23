// The Bedrock Dominion shop is unavailable until an official catalog,
// authenticated checkout, and verified delivery process are implemented.
(function () {
  function renderShopStatus() {
    const container = document.getElementById('shop-container');
    if (!container) return;
    container.textContent = 'Unavailable';
    container.setAttribute('role', 'status');
    container.setAttribute('aria-live', 'polite');
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', renderShopStatus, { once: true });
  } else {
    renderShopStatus();
  }
})();
