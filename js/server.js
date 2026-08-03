// Simulated server status updater for demo
(function(){
  const statusEl = document.getElementById('server-status');
  const playersEl = document.getElementById('players');
  const tpsEl = document.getElementById('tps');

  // initial demo values
  let players = 0;
  let tps = 20.0;
  let online = true;

  function randomTick(){
    // simulate small fluctuations
    players = Math.max(0, Math.min(100, players + Math.floor(Math.random()*5)-2));
    tps = Math.max(0, Math.min(20, (20 + (Math.random()*0.6-0.3)).toFixed(1)));
    online = Math.random() > 0.02; // mostly online

    statusEl.textContent = online ? 'ONLINE' : 'OFFLINE';
    statusEl.style.color = online ? '#22c55e' : '#ef4444';
    playersEl.textContent = players + ' / 100';
    tpsEl.textContent = tps;
  }

  // start with some players for demo
  players = Math.floor(Math.random()*10);
  randomTick();
  setInterval(randomTick, 5000);
})();
