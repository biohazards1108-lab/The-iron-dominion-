import { createClient } from "https://esm.sh/@supabase/supabase-js@2.49.1";

const corsHeaders = {
  "Access-Control-Allow-Origin": "https://theirondominion.com",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Vary": "Origin",
};
const json = (status: number, body: Record<string, unknown>) => new Response(JSON.stringify(body), {
  status, headers: { ...corsHeaders, "Content-Type": "application/json", "Cache-Control": "no-store" },
});

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json(405, { error: "method_not_allowed" });
  if (req.headers.get("origin") !== "https://theirondominion.com") return json(403, { error: "origin_not_allowed" });

  const url = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!url || !anonKey || !serviceKey) return json(503, { error: "service_not_configured" });

  const auth = req.headers.get("Authorization") || "";
  if (!auth.startsWith("Bearer ")) return json(401, { error: "sign_in_required" });
  const userClient = createClient(url, anonKey, { global: { headers: { Authorization: auth } }, auth: { persistSession: false } });
  const { data: { user }, error: userError } = await userClient.auth.getUser();
  if (userError || !user) return json(401, { error: "invalid_session" });

  let body: Record<string, unknown>;
  try { body = await req.json(); } catch { return json(400, { error: "invalid_json" }); }
  const action = body.action;
  const admin = createClient(url, serviceKey, { auth: { persistSession: false } });
  const today = new Date().toISOString().slice(0, 10);

  if (action === "status") {
    const [{ data: points, error: pointsError }, { data: claim, error: claimError }] = await Promise.all([
      admin.from("dominion_web_points").select("balance").eq("user_id", user.id).maybeSingle(),
      admin.from("dominion_daily_claims").select("claim_date,points_awarded,created_at").eq("user_id", user.id).eq("claim_date", today).maybeSingle(),
    ]);
    if (pointsError || claimError) return json(500, { error: "status_lookup_failed" });
    return json(200, { balance: points?.balance ?? 0, claimedToday: Boolean(claim), claim, claimDate: today, timezone: "UTC", pointsPerClaim: 25, currencyType: "website-only" });
  }

  if (action === "claim") {
    const { data, error } = await admin.rpc("claim_dominion_daily_points", { p_user_id: user.id, p_claim_date: today, p_points: 25 });
    if (error || !Array.isArray(data) || !data.length) return json(500, { error: "claim_failed" });
    const result = data[0] as { awarded: boolean; new_balance: number | string; claim_date: string };
    return json(200, { awarded: Boolean(result.awarded), balance: Number(result.new_balance), claimDate: result.claim_date, points: result.awarded ? 25 : 0, alreadyClaimed: !result.awarded, timezone: "UTC", currencyType: "website-only" });
  }
  return json(400, { error: "unknown_action" });
});
