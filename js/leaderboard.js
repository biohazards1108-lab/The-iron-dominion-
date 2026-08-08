(function () {
  const table = document.getElementById('leaderboard-table');
  if (!table) return;

  const body = table.tBodies[0] || table.createTBody();
  while (body.rows.length) body.deleteRow(0);

  const row = body.insertRow();
  const cell = row.insertCell();
  cell.colSpan = 3;
  cell.className = 'empty-state';
  cell.textContent = 'Leaderboard data will appear here when the server API is connected.';
})();
