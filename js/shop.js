// Shop system - handles crate purchases with Dominion Tokens
(function() {
  // Sample crates available in shop
  const SHOP_CRATES = {
    starter: {
      id: 'starter_crate',
      name: 'Starter Crate',
      description: 'Basic tools and resources',
      price: 100,
      items: ['Iron Pickaxe', 'Coal x64', 'Wood x32']
    },
    industrial: {
      id: 'industrial_crate',
      name: 'Industrial Crate',
      description: 'Machines and power components',
      price: 500,
      items: ['Electric Furnace', 'Engine', 'Cable x64']
    },
    legendary: {
      id: 'legendary_crate',
      name: 'Legendary Crate',
      description: 'Rare and exclusive items',
      price: 2000,
      items: ['Quantum Armor Helmet', 'MFSU', 'Singularity']
    }
  };

  // Get player's current token balance
  async function getPlayerBalance(playerName) {
    try {
      const res = await fetch(`api/player/${playerName}/balance`, { cache: 'no-store' });
      if (!res.ok) throw new Error('Failed to fetch balance');
      const data = await res.json();
      return data.balance || 0;
    } catch (err) {
      console.error('Error fetching balance:', err);
      return 0;
    }
  }

  // Process crate purchase
  async function purchaseCrate(playerName, crateId) {
    const crate = SHOP_CRATES[crateId];
    if (!crate) {
      return { success: false, message: 'Invalid crate ID' };
    }

    try {
      const res = await fetch('api/shop/purchase', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          playerName: playerName,
          crateId: crateId,
          crateName: crate.name,
          price: crate.price,
          items: crate.items,
          timestamp: new Date().toISOString()
        })
      });

      if (!res.ok) {
        const error = await res.json();
        return { success: false, message: error.message || 'Purchase failed' };
      }

      const result = await res.json();
      return { success: true, message: result.message, transaction: result.transactionId };
    } catch (err) {
      console.error('Purchase error:', err);
      return { success: false, message: 'Network error. Please try again.' };
    }
  }

  // Render shop UI
  function renderShop() {
    const shopContainer = document.getElementById('shop-container');
    if (!shopContainer) return;

    let html = '<div class="shop-grid">';
    for (const [key, crate] of Object.entries(SHOP_CRATES)) {
      html += `
        <div class="crate-card">
          <h3>${crate.name}</h3>
          <p class="description">${crate.description}</p>
          <p class="price">💰 ${crate.price} Dominion Tokens</p>
          <ul class="items">
            ${crate.items.map(item => `<li>✓ ${item}</li>`).join('')}
          </ul>
          <button onclick="window.purchaseCrate('${key}')">Buy Crate</button>
        </div>
      `;
    }
    html += '</div>';
    shopContainer.innerHTML = html;
  }

  // Expose to window for onclick handlers
  window.purchaseCrate = async function(crateId) {
    const playerName = prompt('Enter your in-game username:');
    if (!playerName) return;

    const button = event.target;
    button.disabled = true;
    button.textContent = 'Processing...';

    const result = await purchaseCrate(playerName, crateId);
    alert(result.message);

    if (result.success) {
      console.log('Transaction ID:', result.transaction);
      // Reload shop to show updated balance
      renderShop();
    }

    button.disabled = false;
    button.textContent = 'Buy Crate';
  };

  // Initialize shop on page load
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', renderShop);
  } else {
    renderShop();
  }
})();
