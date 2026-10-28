import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const supabase = createClient(
  "https://dfdzjfsydaoovkxhilov.supabase.co",
  "sb_publishable_nc8UXBQPCXVhdwZxEWwCHg_le9zeAOC",
  { auth: { persistSession: true, autoRefreshToken: true, detectSessionInUrl: true, flowType: "pkce" } }
);
const $ = (selector) => document.querySelector(selector);
const message = $("#dashboard-message");
const profileStatus = $("#profile-status");
const say = (text, kind = "info", target = message) => {
  if (!target) return;
  target.textContent = text;
  target.dataset.kind = kind;
  target.hidden = false;
};
const setText = (selector, value) => {
  const element = $(selector);
  if (element) element.textContent = value ?? "";
};
const profileErrorMessage = (error) => {
  const text = String(error?.message || "").toLowerCase();
  if (text.includes("profiles") && (text.includes("schema cache") || text.includes("does not exist") || text.includes("could not find"))) {
    return "Profile storage is prepared in GitHub but not yet installed in Supabase. The administrator must apply the account profile migration before profile editing is available.";
  }
  if (text.includes("duplicate key") || text.includes("profiles_username_key")) return "That username is already taken. Choose another username.";
  if (text.includes("check constraint") || text.includes("invalid input")) return "Check the username format and display-name length, then try again.";
  return "The profile could not be loaded or saved. Please try again later.";
};
const makeInitials = (name) => {
  const parts = String(name || "").trim().split(/\s+/).filter(Boolean);
  if (!parts.length) return "ID";
  return parts.slice(0, 2).map((part) => Array.from(part)[0]).join("").toUpperCase().slice(0, 2);
};
const renderProfile = (profile) => {
  const displayName = profile.display_name || profile.username || "Dominion Member";
  setText("#profile-username", profile.username || "");
  setText("#profile-display-name", profile.display_name || "");
  setText("#profile-display-preview", displayName);
  setText("#profile-handle-preview", "@" + (profile.username || "member"));
  setText("#profile-avatar", makeInitials(profile.display_name || profile.username));
  $("#compact-dashboard").checked = Boolean(profile.compact_dashboard);
  document.body.classList.toggle("compact-dashboard", Boolean(profile.compact_dashboard));
};
const setUnavailable = () => {
  setText("#minecraft-name", "UNAVAILABLE");
  setText("#minecraft-uuid", "Minecraft account not linked");
  setText("#minecraft-link-status", "Verification not configured");
  setText("#server-rank", "UNAVAILABLE");
  setText("#vote-count", "UNAVAILABLE");
  setText("#token-balance", "UNAVAILABLE");
  setText("#online-count", "UNAVAILABLE");
  setText("#server-status", "SERVER DATA UNAVAILABLE");
  setText("#iron-account-state", "Checking session…");
  setText("#mc-account-state", "Not linked");
  setText("#server-data-state", "Unavailable");
};
setUnavailable();

const { data, error } = await supabase.auth.getUser();
if (error || !data.user) {
  window.location.replace("login.html?next=dashboard");
} else {
  const user = data.user;
  setText("#member-email", user.email || "Signed in to your Iron Dominion account");
  setText("#iron-account-state", "Signed in");
  const signOutButton = $("#dash-signout");
  signOutButton?.addEventListener("click", async () => {
    signOutButton.disabled = true;
    const { error: signOutError } = await supabase.auth.signOut();
    if (signOutError) {
      signOutButton.disabled = false;
      say("Sign-out could not be completed. Please try again.", "error");
      return;
    }
    window.location.replace("login.html");
  });

  const defaultUsername = "member_" + user.id.replace(/-/g, "").slice(0, 12).toLowerCase();
  const { data: profile, error: profileError } = await supabase
    .from("profiles")
    .select("username,display_name,compact_dashboard")
    .eq("id", user.id)
    .maybeSingle();

  if (profileError) {
    $("#profile-save").disabled = true;
    say(profileErrorMessage(profileError), "error");
    renderProfile({ username: defaultUsername, display_name: "", compact_dashboard: false });
  } else {
    renderProfile(profile || { username: defaultUsername, display_name: "", compact_dashboard: false });
    $("#profile-form")?.addEventListener("submit", async (event) => {
      event.preventDefault();
      const form = $("#profile-form");
      if (!form.reportValidity()) return;
      const button = $("#profile-save");
      const username = $("#profile-username").value.trim().toLowerCase();
      const displayName = $("#profile-display-name").value.trim();
      if (!/^[a-z0-9_]{3,24}$/.test(username)) {
        say("Username must contain 3–24 letters, numbers, or underscores.", "error", profileStatus);
        return;
      }
      if (displayName.length > 40) {
        say("Display name must be 40 characters or fewer.", "error", profileStatus);
        return;
      }
      button.disabled = true;
      button.setAttribute("aria-busy", "true");
      const values = {
        id: user.id,
        username,
        display_name: displayName,
        compact_dashboard: $("#compact-dashboard").checked
      };
      try {
        const { data: saved, error: saveError } = await supabase
          .from("profiles")
          .upsert(values, { onConflict: "id" })
          .select("username,display_name,compact_dashboard")
          .single();
        if (saveError) throw saveError;
        renderProfile(saved);
        say("Your profile and dashboard preference have been saved.", "success", profileStatus);
      } catch (saveError) {
        say(profileErrorMessage(saveError), "error", profileStatus);
      } finally {
        button.disabled = false;
        button.removeAttribute("aria-busy");
      }
    });
  }

  const { data: link } = await supabase
    .from("minecraft_links")
    .select("platform,gamertag,player_uuid,verified_at")
    .eq("user_id", user.id)
    .maybeSingle();
  if (link?.verified_at && link?.gamertag) {
    setText("#minecraft-name", link.gamertag);
    setText("#minecraft-uuid", link.player_uuid || "Verified identity · UUID unavailable");
    setText("#minecraft-link-status", "Verified " + link.platform + " identity");
    setText("#mc-account-state", "Verified");
  } else {
    setText("#minecraft-name", "UNAVAILABLE");
    setText("#minecraft-uuid", "Minecraft ownership verification is not configured");
    setText("#minecraft-link-status", "Not linked");
    setText("#mc-account-state", "Not linked · verification pending");
  }

  const { data: stats } = await supabase
    .from("player_stats")
    .select("server_rank,vote_count,dominion_tokens")
    .eq("user_id", user.id)
    .maybeSingle();
  if (stats) {
    setText("#server-rank", stats.server_rank || "UNAVAILABLE");
    setText("#vote-count", Number.isInteger(stats.vote_count) ? String(stats.vote_count) : "UNAVAILABLE");
    setText("#token-balance", Number.isSafeInteger(Number(stats.dominion_tokens)) ? String(stats.dominion_tokens) : "UNAVAILABLE");
    setText("#server-data-state", "Trusted stats record available");
  } else {
    setText("#server-data-state", "Trusted server integration unavailable");
  }
  say("Your account session is active. Profile editing requires the Supabase profile migration. Minecraft ownership, rank, votes, and token balances are shown only when verified trusted records exist.");
}
