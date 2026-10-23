(function () {
  const balanceEls = document.querySelectorAll('[data-balance], #balance');
  if (!balanceEls.length) return;

  // Player balances remain unavailable until authenticated server data exists.
  balanceEls.forEach((el) => {
    if (!el.textContent.trim() || el.textContent.trim() === '0' || el.textContent.trim() === '—') {
      el.textContent = 'Unavailable';
    }
  });
})();
