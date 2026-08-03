// Demo economy script: updates the displayed balance
(function(){
  const balEl = document.getElementById('balance');
  let balance = Math.floor(Math.random()*1000);
  function render(){
    balEl.textContent = balance.toLocaleString();
  }
  // small random changes to make the page feel alive
  setInterval(()=>{
    balance = Math.max(0, balance + Math.floor(Math.random()*200)-80);
    render();
  },7000);
  render();
})();
