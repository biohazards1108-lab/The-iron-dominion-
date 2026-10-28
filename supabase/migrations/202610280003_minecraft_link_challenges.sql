-- One-time challenge records for linking a signed-in website account to a player
-- who proves presence on the actual Bedrock Dedicated Server.
begin;

create table if not exists public.minecraft_link_challenges (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  code_hash text not null unique check (char_length(code_hash) = 64),
  created_at timestamptz not null default now(),
  expires_at timestamptz not null,
  consumed_at timestamptz,
  linked_gamertag text,
  linked_player_id text,
  constraint minecraft_link_challenge_expiry check (expires_at > created_at)
);

create index if not exists minecraft_link_challenges_user_created_idx
  on public.minecraft_link_challenges (user_id, created_at desc);

create unique index if not exists minecraft_links_player_uuid_unique
  on public.minecraft_links (player_uuid)
  where player_uuid is not null and player_uuid <> '';

alter table public.minecraft_link_challenges enable row level security;
alter table public.minecraft_link_challenges force row level security;
-- No client policies: only the Edge Function service-role client may access challenges.
revoke all on public.minecraft_link_challenges from anon, authenticated;
grant all on public.minecraft_link_challenges to service_role;

commit;
