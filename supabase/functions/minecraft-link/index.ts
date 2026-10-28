import { createClient } from "https://esm.sh/@supabase/supabase-js@2.49.1";

const corsHeaders = {
  "Access-Control-Allow-Origin": "https://theirondominion.com",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type, x-dominion-server-token",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Vary": "Origin",
};

const json = (status: number, body: Record<string, unknown>) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json", "Cache-Control": "no-store" },
  });

async function sha256(value: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json(405, { error: "method_not_allowed" });
  if (req.headers.get("origin") && req.headers.get("origin") !== "https://theirondominion.com") {
    return json(403, { error: "origin_not_allowed" });
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const serverToken = Deno.env.get("DOMINION_SERVER_TOKEN");
  if (!supabaseUrl || !anonKey || !serviceKey || !serverToken) {
    return json(503, { error: "linking_service_not_configured" });
  }

  let body: Record<string, unknown>;
  try {
    body = await req.json();
  } catch {
    return json(400, { error: "invalid_json" });
  }
  const admin = createClient(supabaseUrl, serviceKey, { auth: { persistSession: false } });
  const action = body.action;

  if (action === "create" || action === "status") {
    const authHeader = req.headers.get("Authorization") || "";
    if (!authHeader.startsWith("Bearer ")) return json(401, { error: "sign_in_required" });
    const userClient = createClient(supabaseUrl, anonKey, {
      global: { headers: { Authorization: authHeader } },
      auth: { persistSession: false },
    });
    const { data: { user }, error: authError } = await userClient.auth.getUser();
    if (authError || !user) return json(401, { error: "invalid_session" });

    if (action === "create") {
      const rawCode = Array.from(crypto.getRandomValues(new Uint8Array(5)))
        .map((n) => "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"[n % 32]).join("");
      const codeHash = await sha256(rawCode);
      const now = new Date();
      const expiresAt = new Date(now.getTime() + 10 * 60 * 1000);
      const { error } = await admin.from("minecraft_link_challenges").insert({
        user_id: user.id,
        code_hash: codeHash,
        expires_at: expiresAt.toISOString(),
      });
      if (error) return json(500, { error: "challenge_create_failed" });
      return json(200, { code: rawCode, expiresAt: expiresAt.toISOString(), instructions: "Join Bedrock Dominion and type !link YOURCODE in chat." });
    }

    const { data: link, error: linkError } = await admin
      .from("minecraft_links")
      .select("platform,gamertag,player_uuid,verified_at")
      .eq("user_id", user.id)
      .maybeSingle();
    if (linkError) return json(500, { error: "link_status_failed" });
    const { data: challenge } = await admin
      .from("minecraft_link_challenges")
      .select("expires_at,consumed_at,linked_gamertag")
      .eq("user_id", user.id)
      .order("created_at", { ascending: false })
      .limit(1)
      .maybeSingle();
    return json(200, { linked: Boolean(link?.verified_at), link: link ?? null, challenge: challenge ?? null });
  }

  if (action === "claim") {
    const suppliedToken = req.headers.get("x-dominion-server-token") || "";
    if (suppliedToken.length < 32 || suppliedToken !== serverToken) {
      return json(401, { error: "server_authentication_failed" });
    }
    const code = String(body.code || "").toUpperCase().trim();
    const gamertag = String(body.gamertag || "").trim();
    const playerId = String(body.playerId || "").trim();
    if (!/^[A-HJ-NP-Z2-9]{8}$/.test(code) || !gamertag || gamertag.length > 64 || !playerId || playerId.length > 128) {
      return json(400, { error: "invalid_link_payload" });
    }

    const codeHash = await sha256(code);
    const { data: challenge, error: challengeError } = await admin
      .from("minecraft_link_challenges")
      .select("id,user_id,expires_at,consumed_at")
      .eq("code_hash", codeHash)
      .maybeSingle();
    if (challengeError || !challenge || challenge.consumed_at || new Date(challenge.expires_at).getTime() <= Date.now()) {
      return json(400, { error: "code_invalid_expired_or_used" });
    }

    // Atomically consume the one-time challenge before writing the trusted identity.
    const { data: consumed, error: consumeError } = await admin
      .from("minecraft_link_challenges")
      .update({ consumed_at: new Date().toISOString(), linked_gamertag: gamertag, linked_player_id: playerId })
      .eq("id", challenge.id)
      .is("consumed_at", null)
      .gt("expires_at", new Date().toISOString())
      .select("id")
      .maybeSingle();
    if (consumeError || !consumed) return json(409, { error: "code_already_claimed" });

    const { error: linkError } = await admin.from("minecraft_links").upsert({
      user_id: challenge.user_id,
      platform: "bedrock",
      gamertag,
      player_uuid: playerId,
      verified_at: new Date().toISOString(),
      verification_method: "bedrock-dedicated-server-one-time-code",
    }, { onConflict: "user_id" });
    if (linkError) {
      // Challenge remains consumed if persistence fails; user can generate a new code.
      return json(500, { error: "link_record_failed" });
    }
    return json(200, { linked: true, gamertag });
  }

  return json(400, { error: "unknown_action" });
});
