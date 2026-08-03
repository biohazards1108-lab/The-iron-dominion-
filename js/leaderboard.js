// Populate leaderboard with demo entries
(function(){
  const table = document.getElementById('leaderboard-table');

  // sample demo data
  const sample = [
    {rank:'🥇', name:'IronLord', wealth:'12,432'},
    {rank:'🥈', name:'ForgeMaster', wealth:'9,210'},
    {rank:'🥉', name:'SteelSmith', wealth:'7,005'},
    {rank:'4', name:'Wasteland', wealth:'5,900'},
    {rank:'5', name:'Miner42', wealth:'4,450'}
  ];

  // remove all rows except header
  while(table.rows.length > 1) table.deleteRow(1);

  sample.forEach(entry=>{
    const r = table.insertRow(-1);
    const c1 = r.insertCell(0);
    const c2 = r.insertCell(1);
    const c3 = r.insertCell(2);
    c1.textContent = entry.rank;
    c2.textContent = entry.name;
    c3.textContent = entry.wealth;
  });
})();
