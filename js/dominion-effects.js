/* Iron Dominion visual effects. No tracking, cookies, or third-party requests. */
(() => {
  "use strict";
  const root = document.documentElement;
  const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const finePointer = window.matchMedia("(hover: hover) and (pointer: fine)").matches;

  if (finePointer) root.classList.add("dominion-cursor");

  if (reduceMotion) return;
  root.classList.add("dominion-transition-ready");

  // Fade out only for same-origin page navigation; leave downloads, anchors,
  // new tabs, external links, forms, and browser-native actions untouched.
  document.addEventListener("click", (event) => {
    const link = event.target.closest("a[href]");
    if (!link || event.defaultPrevented || event.button !== 0 ||
        event.metaKey || event.ctrlKey || event.shiftKey || event.altKey ||
        link.target === "_blank" || link.hasAttribute("download")) return;

    let destination;
    try { destination = new URL(link.href, window.location.href); }
    catch { return; }

    if (destination.origin !== window.location.origin ||
        destination.pathname === window.location.pathname &&
        destination.search === window.location.search ||
        destination.hash && destination.pathname === window.location.pathname &&
        destination.search === window.location.search) return;

    event.preventDefault();
    root.classList.add("dominion-transitioning");
    window.setTimeout(() => { window.location.assign(destination.href); }, 170);
  });
})();
