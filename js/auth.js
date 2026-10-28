import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL = "https://dfdzjfsydaoovkxhilov.supabase.co";
const SUPABASE_PUBLISHABLE_KEY = "sb_publishable_nc8UXBQPCXVhdwZxEWwCHg_le9zeAOC";
const supabase = createClient(SUPABASE_URL, SUPABASE_PUBLISHABLE_KEY, {
  auth: {
    persistSession: true,
    autoRefreshToken: true,
    detectSessionInUrl: true,
    flowType: "pkce"
  }
});

const message = document.querySelector("#authMessage");
const showMessage = (text, kind = "info") => {
  if (!message) return;
  message.textContent = text;
  message.dataset.kind = kind;
  message.hidden = false;
};
const formValue = (form, name) => String(new FormData(form).get(name) || "");
const setBusy = (form, busy) => {
  const button = form?.querySelector('button[type="submit"]');
  if (button) {
    button.disabled = busy;
    button.setAttribute("aria-busy", String(busy));
  }
};
// Keep provider errors generic so the UI does not reveal whether an email is registered.
const friendlyError = () => "We couldn't complete that request. Check your details, confirm your email if needed, and try again.";

const signupForm = document.querySelector("#signupForm");
signupForm?.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (!signupForm.reportValidity()) return;
  const email = formValue(signupForm, "email").trim();
  const password = formValue(signupForm, "password");
  const confirm = formValue(signupForm, "confirmPassword");
  if (password.length < 10) return showMessage("Choose a password with at least 10 characters.", "error");
  if (password !== confirm) return showMessage("The passwords don't match.", "error");
  setBusy(signupForm, true);
  try {
    const { data, error } = await supabase.auth.signUp({
      email,
      password,
      options: { emailRedirectTo: new URL("login.html", location.origin).href }
    });
    if (error) throw error;
    if (data.session) {
      showMessage("Your account was created and you're signed in.", "success");
      window.location.assign("login.html");
    } else {
      signupForm.reset();
      showMessage("Account request received. Check your email for a confirmation link before signing in.", "success");
    }
  } catch (error) {
    showMessage(friendlyError(error), "error");
  } finally {
    setBusy(signupForm, false);
  }
});

const loginForm = document.querySelector("#loginForm");
const microsoftButtons = document.querySelectorAll("[data-microsoft-signin]");
microsoftButtons.forEach((button) => {
  button.addEventListener("click", async () => {
    button.disabled = true;
    button.setAttribute("aria-busy", "true");
    showMessage("Connecting securely to Microsoft…", "info");
    try {
      const { error } = await supabase.auth.signInWithOAuth({
        provider: "azure",
        options: {
          scopes: "email",
          redirectTo: new URL("login.html", location.origin).href
        }
      });
      if (error) throw error;
    } catch (error) {
      showMessage(friendlyError(error), "error");
      button.disabled = false;
      button.removeAttribute("aria-busy");
    }
  });
});
loginForm?.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (!loginForm.reportValidity()) return;
  setBusy(loginForm, true);
  try {
    const { error } = await supabase.auth.signInWithPassword({
      email: formValue(loginForm, "email").trim(),
      password: formValue(loginForm, "password")
    });
    if (error) throw error;
    showMessage("Signed in successfully.", "success");
    // Only allow the known internal dashboard destination; never trust arbitrary redirect URLs.
    if (new URLSearchParams(location.search).get("next") === "dashboard") {
      window.location.assign("dashboard.html");
      return;
    }
    await refreshSessionUI();
  } catch (error) {
    showMessage(friendlyError(error), "error");
  } finally {
    setBusy(loginForm, false);
  }
});

const resetForm = document.querySelector("#resetForm");
document.querySelector("#showReset")?.addEventListener("click", (event) => {
  event.preventDefault();
  if (!resetForm) return;
  resetForm.hidden = !resetForm.hidden;
  if (!resetForm.hidden) document.querySelector("#resetEmail")?.focus();
});
resetForm?.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (!resetForm.reportValidity()) return;
  setBusy(resetForm, true);
  try {
    const { error } = await supabase.auth.resetPasswordForEmail(formValue(resetForm, "email").trim(), {
      redirectTo: new URL("login.html?mode=reset", location.origin).href
    });
    if (error) throw error;
    showMessage("If an account matches that email, a password-reset link will be sent.", "success");
  } catch (error) {
    showMessage(friendlyError(error), "error");
  } finally {
    setBusy(resetForm, false);
  }
});

const updateForm = document.querySelector("#updatePasswordForm");
updateForm?.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (!updateForm.reportValidity()) return;
  const password = formValue(updateForm, "password");
  if (password.length < 10) return showMessage("Choose a password with at least 10 characters.", "error");
  setBusy(updateForm, true);
  try {
    const { error } = await supabase.auth.updateUser({ password });
    if (error) throw error;
    updateForm.hidden = true;
    history.replaceState({}, document.title, location.pathname);
    showMessage("Password updated. You can continue using your account.", "success");
    await refreshSessionUI();
  } catch (error) {
    showMessage(friendlyError(error), "error");
  } finally {
    setBusy(updateForm, false);
  }
});

document.querySelector("#signOutButton")?.addEventListener("click", async () => {
  const { error } = await supabase.auth.signOut();
  if (error) showMessage(friendlyError(error), "error");
  else {
    showMessage("You've signed out.", "success");
    await refreshSessionUI();
  }
});

async function refreshSessionUI() {
  const { data: { user } } = await supabase.auth.getUser();
  const panel = document.querySelector("#signedInPanel");
  const text = document.querySelector("#signedInText");
  if (panel) panel.hidden = !user;
  if (text && user) text.textContent = "Signed in as " + (user.email || "your Iron Dominion account") + ".";
  const isResetMode = new URLSearchParams(location.search).get("mode") === "reset";
  if (loginForm) loginForm.hidden = Boolean(user) || isResetMode;
  microsoftButtons.forEach((button) => {
    button.hidden = Boolean(user) || isResetMode;
    button.disabled = false;
    button.removeAttribute("aria-busy");
  });
  const resetLink = document.querySelector("#showReset");
  if (resetLink) resetLink.hidden = Boolean(user) || isResetMode;
  if (user && resetForm) resetForm.hidden = true;
}
supabase.auth.onAuthStateChange((event) => {
  if (event === "PASSWORD_RECOVERY") {
    if (updateForm) updateForm.hidden = false;
    if (loginForm) loginForm.hidden = true;
    if (resetForm) resetForm.hidden = true;
    showMessage("Choose a new password for your account.", "info");
  }
});
if (new URLSearchParams(location.search).get("mode") === "reset") {
  if (updateForm) updateForm.hidden = false;
  if (loginForm) loginForm.hidden = true;
}
refreshSessionUI().catch(() => showMessage("Account status couldn't be loaded. Please refresh and try again.", "error"));
