-- Atomic per-account cooldown for issuing Minecraft link challenges.
-- The cooldown state is service-role-only and separate from player-visible records.
begin;

create table if not exists public.minecraft_link_cooldowns (
  user_id uuid primary key references auth.users(id) on delete cascade,
  last_requested_at timestamptz not null
);

alter table public.minecraft_link_cooldowns enable row level security;
alter table public.minecraft_link_cooldowns force row level security;
revoke all on public.minecraft_link_cooldowns from anon, authenticated;
grant all on public.minecraft_link_cooldowns to service_role;

create or replace function public.claim_minecraft_link_cooldown(p_user_id uuid)
returns integer
language plpgsql
security definer
set search_path = ''
as $$
declare
  reserved_user uuid;
  retry_seconds integer;
begin
  insert into public.minecraft_link_cooldowns (user_id, last_requested_at)
  values (p_user_id, now())
  on conflict (user_id) do update
    set last_requested_at = now()
    where public.minecraft_link_cooldowns.last_requested_at <= now() - interval '60 seconds'
  returning user_id into reserved_user;

  if reserved_user is not null then
    return 0;
  end if;

  select greatest(
    1,
    ceil(60 - extract(epoch from (now() - last_requested_at)))::integer
  )
  into retry_seconds
  from public.minecraft_link_cooldowns
  where user_id = p_user_id;

  return coalesce(retry_seconds, 60);
end;
$$;

revoke all on function public.claim_minecraft_link_cooldown(uuid) from public, anon, authenticated;
grant execute on function public.claim_minecraft_link_cooldown(uuid) to service_role;

commit;
