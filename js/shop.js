// Public shop catalog renderer.
//
// Security rule: this browser script never sends a token balance, price,
// item list, admin key, or purchase request. Balance-changing operations are
// trusted-server operations and require the authenticated Minecraft bridge.
(function () {
  const SHOP_CRATES = Object.freeze([
    {
      id: 'starter_crate',
      name: 'Starter Crate',
      description: 'Basic tools and resources.',
      price: 100,
      items: ['Iron Pickaxe', 'Coal x64', 'Wood x32']
    },
    {
      id: 'industrial_crate',
      name: 'Industrial Crate',
      description: 'Machines and power components.',
      price: 500,
      items: ['Electric Furnace', 'Engine', 'Cable x64']
    },
    {
      id: 'legendary_crate',
      name: 'Legendary Crate',
      description: 'Rare and exclusive items.',
      price: 2000,
      items: ['Quantum Armor Helmet', 'MFSU', 'Singularity']
    }
  ]);

  function escapeHtml(value) {
    return String(value)
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#039;');
  }

  function renderShop() {
    const container = document.getElementById('shop-container');
    if (!container) return;

    const cards = SHOP_CRATES.map(crate => `
      <article class="crate-card">
        <h3>${escapeHtml(crate.name)}</h3>
        <p class="description">${escapeHtml(crate.description)}</p>
        <p class="price">◆ ${crate.price.toLocaleString()} Dominion Tokens</p>
        <ul class="items">
          ${crate.items.map(item => `<li>✓ ${escapeHtml(item)}</li>`).join('')}
        </ul>
        <button type="button" disabled aria-disabled="true" title="Authenticated checkout is not enabled yet">
          Checkout Coming Soon
        </button>
      </article>
    `).join('');

    container.innerHTML = `<div class="shop-grid">${cards}</div>`;
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', renderShop, { once: true });
  } else {
    renderShop();
  }
})();
