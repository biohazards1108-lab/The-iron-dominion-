import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const supabase = createClient(
  "https://dfdzjfsydaoovkxhilov.supabase.co",
  "sb_publishable_nc8UXBQPCXVhdwZxEWwCHg_le9zeAOC",
  { auth: { persistSession: true, autoRefreshToken: true, detectSessionInUrl: true, flowType: "pkce" } }
);

const $ = (selector) => document.querySelector(selector);
const message = $("#dashboard-message");
const say = (text) => {
  message.textContent = text;
  message.hidden = false;
};
const setText = (selector, value) => {
  const element = $(selector);
  if (element) element.textContent = value ?? "";
};

// Intentionally leave server-derived values blank until trusted integrations exist.
setText("#minecraft-name", "");
setText("#minecraft-uuid", "");
setText("#minecraft-link-status", "");
setText("#server-rank", "");
setText("#vote-count", "");
setText("#token-balance", "");
setText("#online-count", "");
setText("#server-status", "");
setText("#iron-account-state", "");
setText("#mc-account-state", "");
setText("#server-data-state", "");

const { data, error } = await supabase.auth.getUser();
if (error || !data.user) {
  window.location.replace("login.html?next=dashboard");
} else {
  setText("#member-email", data.user.email || "");
  setText("#iron-account-state", "Signed in");
  $("#dash-signout")?.addEventListener("click", async () => {
    const button = $("#dash-signout");
    button.disabled = true;
    const { error: signOutError } = await supabase.auth.signOut();
    if (signOutError) {
      button.disabled = false;
      say("Sign-out could not be completed. Please try again.");
      return;
    }
    window.location.replace("login.html");
  });
  say("Your Iron Dominion account is connected. Minecraft linking, vote totals, rank, token balance, and live server counts will appear when their trusted data sources are connected.");
}
