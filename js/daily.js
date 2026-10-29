import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const supabase = createClient(
  "https://dfdzjfsydaoovkxhilov.supabase.co",
  "sb_publishable_nc8UXBQPCXVhdwZxEWwCHg_le9zeAOC",
  { auth: { persistSession: true, autoRefreshToken: true, detectSessionInUrl: true, flowType: "pkce" } }
);
const $ = (selector) => document.querySelector(selector);
const claimButton = $("#daily-claim");
const refreshButton = $("#daily-refresh");
const message = $("#daily-message");

function announce(text, kind = "info") {
  message.textContent = text;
  message.dataset.kind = kind;
  message.hidden = false;
}
async function invoke(action) {
  const { data, error } = await supabase.functions.invoke("dominion-daily-reward", { body: { action } });
  if (error) throw error;
  if (data?.error) throw new Error(data.error);
  return data;
}
function render(data) {
  $("#daily-balance").textContent = Number(data.balance ?? 0).toLocaleString();
  $("#daily-date").textContent = "Daily reset: 00:00 UTC · " + (data.claimDate || "date unavailable");
  $("#daily-claim-state").textContent = data.claimedToday || data.alreadyClaimed ? "Claimed today" : "Ready to claim";
  claimButton.disabled = Boolean(data.claimedToday || data.alreadyClaimed);
  claimButton.textContent = claimButton.disabled ? "Today's reward claimed" : "Claim 25 website points";
}
async function loadStatus() {
  refreshButton.disabled = true;
  claimButton.disabled = true;
  try {
    const { data: { user }, error } = await supabase.auth.getUser();
    if (error || !user) {
      $("#daily-balance").textContent = "Sign in required";
      $("#daily-claim-state").textContent = "Not signed in";
      claimButton.textContent = "Sign in to claim";
      claimButton.disabled = false;
      claimButton.onclick = () => window.location.assign("login.html?next=daily");
      announce("Sign in to your Iron Dominion account to claim your daily website points.");
      return;
    }
    const status = await invoke("status");
    render(status);
    announce(status.claimedToday ? "Your daily website points have already been claimed for today." : "Your daily reward is ready.");
  } catch {
    $("#daily-balance").textContent = "Unavailable";
    $("#daily-claim-state").textContent = "Could not load";
    claimButton.textContent = "Try again";
    claimButton.disabled = false;
    announce("The daily reward service is not deployed or configured yet. No points were added. Please try again later.", "error");
  } finally {
    refreshButton.disabled = false;
  }
}
claimButton.addEventListener("click", async () => {
  claimButton.disabled = true;
  refreshButton.disabled = true;
  try {
    const { data: { user }, error } = await supabase.auth.getUser();
    if (error || !user) {
      window.location.assign("login.html?next=daily");
      return;
    }
    const result = await invoke("claim");
    render({ ...result, claimedToday: true });
    announce(result.awarded ? "Daily reward claimed: 25 website points. These are not in-game currency." : "You have already claimed today's reward. Your balance has not been increased again.");
  } catch {
    announce("Your claim could not be confirmed. Refresh the status before trying again; duplicate claims are blocked by the server.", "error");
  } finally {
    refreshButton.disabled = false;
    await loadStatus();
  }
});
refreshButton.addEventListener("click", loadStatus);
loadStatus();
