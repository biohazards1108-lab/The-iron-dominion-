import { world, system } from "@minecraft/server";
import { HttpHeader, HttpRequest, HttpRequestMethod, http } from "@minecraft/server-net";
import { secrets, variables } from "@minecraft/server-admin";

const endpoint = String(variables.get("dominionLinkEndpoint") || "");
const serverToken = secrets.get("dominionServerToken");
const CHAT_PREFIX = "!link ";

function tell(player, message) {
  try { player.sendMessage("§6[Iron Dominion]§r " + message); } catch {}
}

async function submitLink(player, code) {
  if (!endpoint.startsWith("https://") || !serverToken) {
    tell(player, "Website linking is not configured on this server yet. Please contact an administrator.");
    return;
  }
  const req = new HttpRequest(endpoint);
  req.method = HttpRequestMethod.Post;
  req.headers = [
    new HttpHeader("Content-Type", "application/json"),
    new HttpHeader("x-dominion-server-token", serverToken),
  ];
  req.body = JSON.stringify({
    action: "claim",
    code,
    gamertag: player.name,
    playerId: player.id,
  });
  try {
    const response = await http.request(req);
    if (response.status >= 200 && response.status < 300) {
      tell(player, "Your Minecraft identity has been linked to your Iron Dominion website account. Refresh your player dashboard.");
    } else {
      tell(player, "That link code was invalid, expired, or already used. Generate a new code on the website and try again.");
    }
  } catch {
    tell(player, "The linking service could not be reached. Please try again later.");
  }
}

world.beforeEvents.chatSend.subscribe((event) => {
  const message = event.message.trim();
  if (!message.toLowerCase().startsWith(CHAT_PREFIX)) return;
  event.cancel = true;
  const code = message.slice(CHAT_PREFIX.length).trim().toUpperCase();
  const player = event.sender;
  if (!/^[A-HJ-NP-Z2-9]{8}$/.test(code)) {
    tell(player, "Use the code from your website dashboard like this: !link ABCD2345");
    return;
  }
  // Defer HTTP work out of the before-event restricted execution context.
  system.run(() => { void submitLink(player, code); });
});

world.afterEvents.playerSpawn.subscribe(({ player, initialSpawn }) => {
  if (initialSpawn) tell(player, "To link your website account, sign in at theirondominion.com, generate a Minecraft link code, then type !link CODE in chat.");
});
