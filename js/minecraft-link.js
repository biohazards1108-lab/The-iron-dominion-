import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const supabase = createClient(
  "https://dfdzjfsydaoovkxhilov.supabase.co",
  "sb_publishable_nc8UXBQPCXVhdwZxEWwCHg_le9zeAOC",
  { auth: { persistSession: true, autoRefreshToken: true, detectSessionInUrl: true, flowType: "pkce" } }
);
const $ = (selector) => document.querySelector(selector);
const message = $("#linkMessage");
const controls = $("#linkControls");
const codePanel = $("#linkCodePanel");
const setMessage = (text, kind = "info") => {
  message.textContent = text;
  message.dataset.kind = kind;
  message.hidden = false;
};
const invoke = async (action) => {
  const { data, error } = await supabase.functions.invoke("minecraft-link", { body: { action } });
  if (error) throw error;
  if (data?.error) throw new Error(data.error);
  return data;
};
const showStatus = async () => {
  const data = await invoke("status");
  if (data.linked && data.link) {
    $("#linkState").textContent = "Linked and verified in-game: " + data.link.gamertag + " (" + data.link.platform + ").";
    setMessage("Your Minecraft identity is linked.", "success");
  } else {
    $("#linkState").textContent = "No verified Minecraft identity linked yet.";
    setMessage("Generate a code, then enter it in Bedrock Dominion chat.", "info");
  }
  return data;
};
const init = async () => {
  const { data: { user }, error } = await supabase.auth.getUser();
  if (error || !user) {
    setMessage("Please sign in to your Iron Dominion account first.", "error");
    const a = document.createElement("a");
    a.className = "button";
    a.href = "login.html";
    a.textContent = "Sign in";
    message.after(a);
    return;
  }
  controls.hidden = false;
  try { await showStatus(); }
  catch { setMessage("The linking service is not deployed or configured yet. Your account is safe; please try again after the server integration is enabled.", "error"); }
};
$("#generateLinkCode").addEventListener("click", async () => {
  const button = $("#generateLinkCode");
  button.disabled = true;
  codePanel.hidden = true;
  try {
    const data = await invoke("create");
    $("#linkCode").textContent = data.code;
    $("#linkExpiry").textContent = "Expires at " + new Date(data.expiresAt).toLocaleTimeString([], { hour: "numeric", minute: "2-digit" }) + ".";
    codePanel.hidden = false;
    setMessage("Code created. Join the server and type !link " + data.code, "success");
  } catch {
    setMessage("Could not create a link code. The Edge Function and database migration must be deployed first.", "error");
  } finally { button.disabled = false; }
});
$("#refreshLinkStatus").addEventListener("click", async () => {
  const button = $("#refreshLinkStatus");
  button.disabled = true;
  try { await showStatus(); }
  catch { setMessage("Could not refresh link status. Check that the linking service is deployed.", "error"); }
  finally { button.disabled = false; }
});
$("#copyLinkCode").addEventListener("click", async () => {
  const code = $("#linkCode").textContent;
  try {
    await navigator.clipboard.writeText(code);
    setMessage("Code copied. Enter it in Bedrock Dominion chat.", "success");
  } catch {
    setMessage("Select and copy the code manually, then enter it in game.", "info");
  }
});
init();
