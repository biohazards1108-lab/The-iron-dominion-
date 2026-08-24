(() => {
  const button = document.getElementById('copy-server-address');
  const value = document.getElementById('server-address-value');
  const feedback = document.getElementById('copy-feedback');
  if (!button || !value || !feedback) return;

  button.addEventListener('click', async () => {
    const address = value.textContent.trim();
    try {
      await navigator.clipboard.writeText(address);
      feedback.textContent = 'Copied!';
      window.setTimeout(() => { feedback.textContent = ''; }, 2200);
    } catch {
      feedback.textContent = 'Copy unavailable — select the address manually.';
      window.setTimeout(() => { feedback.textContent = ''; }, 3000);
    }
  });
})();
