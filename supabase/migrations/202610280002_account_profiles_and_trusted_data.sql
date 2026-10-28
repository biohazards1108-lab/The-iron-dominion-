-- Iron Dominion account profiles and trusted player-data boundary.
-- Apply this migration in the Supabase SQL Editor before enabling profile editing.
begin;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  username text not null unique
    check (username ~ '^[a-z0-9_]{3,24}$'),
  display_name text not null default ''
    check (char_length(display_name) <= 40),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

comment on table public.profiles is
  'User-editable public profile fields only. Never store passwords, provider tokens, or trusted game/economy state here.';
comment on column public.profiles.username is
  'Unique public handle. Users may update their own handle subject to validation and uniqueness.';
comment on column public.profiles.display_name is
  'Optional user-editable display name. Not a verified Minecraft gamertag.';

alter table public.profiles enable row level security;
alter table public.profiles force row level security;

drop policy if exists "profiles_select_own" on public.profiles;
create policy "profiles_select_own"
  on public.profiles for select
  to authenticated
  using ((select auth.uid()) = id);

drop policy if exists "profiles_insert_own" on public.profiles;
create policy "profiles_insert_own"
  on public.profiles for insert
  to authenticated
  with check ((select auth.uid()) = id);

drop policy if exists "profiles_update_own" on public.profiles;
create policy "profiles_update_own"
  on public.profiles for update
  to authenticated
  using ((select auth.uid()) = id)
  with check ((select auth.uid()) = id);

create or replace function public.set_profile_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists profiles_set_updated_at on public.profiles;
create trigger profiles_set_updated_at
  before update on public.profiles
  for each row execute function public.set_profile_updated_at();

create or replace function public.create_profile_for_auth_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  generated_username text;
begin
  generated_username := 'member_' || lower(substr(replace(new.id::text, '-', ''), 1, 12));

  insert into public.profiles (id, username, display_name)
  values (new.id, generated_username, '')
  on conflict (id) do nothing;

  return new;
end;
$$;

drop trigger if exists on_auth_user_created_profile on auth.users;
create trigger on_auth_user_created_profile
  after insert on auth.users
  for each row execute function public.create_profile_for_auth_user();

-- These tables are intentionally not client-writable. A trusted server-side
-- verifier / game integration must be implemented before rows are created.
create table if not exists public.minecraft_links (
  user_id uuid primary key references auth.users(id) on delete cascade,
  platform text not null check (platform in ('bedrock', 'java')),
  gamertag text not null check (char_length(gamertag) between 1 and 64),
  player_uuid text,
  verified_at timestamptz not null,
  verification_method text not null,
  created_at timestamptz not null default now(),
  constraint minecraft_links_verification_method_nonempty
    check (char_length(verification_method) between 1 and 80)
);
comment on table public.minecraft_links is
  'Verified Minecraft identity records. Only trusted server-side verification may write these rows; OAuth login alone is not Minecraft ownership proof.';
alter table public.minecraft_links enable row level security;
alter table public.minecraft_links force row level security;
drop policy if exists "minecraft_links_select_own" on public.minecraft_links;
create policy "minecraft_links_select_own"
  on public.minecraft_links for select
  to authenticated
  using ((select auth.uid()) = user_id);
-- Deliberately no authenticated INSERT/UPDATE/DELETE policies for minecraft_links.

create table if not exists public.player_stats (
  user_id uuid primary key references auth.users(id) on delete cascade,
  server_rank text,
  vote_count integer not null default 0 check (vote_count >= 0),
  dominion_tokens bigint not null default 0 check (dominion_tokens >= 0),
  updated_at timestamptz not null default now()
);
comment on table public.player_stats is
  'Trusted game/server statistics. Only a service-role backend or verified integration should write these values.';
alter table public.player_stats enable row level security;
alter table public.player_stats force row level security;
drop policy if exists "player_stats_select_own" on public.player_stats;
create policy "player_stats_select_own"
  on public.player_stats for select
  to authenticated
  using ((select auth.uid()) = user_id);
-- Deliberately no authenticated INSERT/UPDATE/DELETE policies for player_stats.

commit;
