// Server status updater — now fetches real data from /api/server.json with a simulated fallback
(function(){
  const statusEl = document.getElementById('server-status');
  const playersEl = document.getElementById('players');
  const tpsEl = document.getElementById('tps');

  // initial demo values
  let players = 0;
  let tps = 20.0;
  let online = true;

  // apply current values to the DOM
  function render(){
    statusEl.textContent = online ? 'ONLINE' : 'OFFLINE';
    statusEl.style.color = online ? '#22c55e' : '#ef4444';
    playersEl.textContent = players + ' / 100';
    tpsEl.textContent = tps;
  }

  // small random fluctuations for fallback simulation
  function randomTick(){
    players = Math.max(0, Math.min(100, players + Math.floor(Math.random()*5)-2));
    tps = Math.max(0, Math.min(20, Number((20 + (Math.random()*0.6-0.3)).toFixed(1))));
    online = Math.random() > 0.02; // mostly online
    render();
  }

  // Try fetching real server data from the repo's api endpoint
  async function fetchServer(){
    try{
      const res = await fetch('api/server.json', {cache: 'no-store'});
      if(!res.ok) throw new Error('non-OK response');
      const data = await res.json();
      // defensive parsing
      online = Boolean(data.online);
      players = Number.isFinite(Number(data.players)) ? Number(data.players) : players;
      // maxPlayers might be provided; if so, show players / maxPlayers
      const maxPlayers = Number.isFinite(Number(data.maxPlayers)) ? Number(data.maxPlayers) : 100;
      tps = Number.isFinite(Number(data.tps)) ? Number(data.tps) : tps;

      // render players with max when present
      playersEl.textContent = players + ' / ' + maxPlayers;
      render();
    }catch(err){
      // fetch failed — fall back to simulation for this tick
      randomTick();
    }
  }

  // initial seeding
  players = Math.floor(Math.random()*10);
  render();

  // Start by attempting to fetch immediately, then poll every 5s
  fetchServer();
  setInterval(fetchServer, 5000);
})();
