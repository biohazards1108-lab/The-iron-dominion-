(() => {
  "use strict";
  const feedback = document.getElementById("copy-feedback");
  document.querySelectorAll("[data-copy-target]").forEach(button => {
    button.addEventListener("click", async () => {
      const target = document.getElementById(button.dataset.copyTarget);
      const value = target?.textContent?.trim();
      if (!value || !feedback) return;
      try {
        if (navigator.clipboard && window.isSecureContext) {
          await navigator.clipboard.writeText(value);
        } else {
          const field = document.createElement("textarea");
          field.value = value;
          field.setAttribute("readonly", "");
          field.style.position = "fixed";
          field.style.opacity = "0";
          document.body.appendChild(field);
          field.select();
          const copied = document.execCommand("copy");
          field.remove();
          if (!copied) throw new Error("Copy command unavailable");
        }
        feedback.textContent = "Copied: " + value;
      } catch (_) {
        feedback.textContent = "Copy is unavailable in this browser. Press and hold the value above to copy it manually.";
      }
    });
  });
})();