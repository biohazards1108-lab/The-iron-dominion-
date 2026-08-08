(function () {
  const balanceEls = document.querySelectorAll('[data-balance], #balance');
  if (!balanceEls.length) return;

  // Live player balances require authenticated server data. Never invent a balance in the UI.
  balanceEls.forEach((el) => {
    if (!el.textContent.trim() || el.textContent.trim() === '0') {
      el.textContent = '—';
    }
  });
})();
