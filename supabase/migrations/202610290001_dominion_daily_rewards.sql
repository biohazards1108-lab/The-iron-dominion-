-- Daily website engagement points. These are website-only points, not in-game currency.
begin;

create table if not exists public.dominion_web_points (
  user_id uuid primary key references auth.users(id) on delete cascade,
  balance bigint not null default 0 check (balance >= 0),
  updated_at timestamptz not null default now()
);

create table if not exists public.dominion_daily_claims (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  claim_date date not null,
  points_awarded integer not null check (points_awarded > 0 and points_awarded <= 1000),
  created_at timestamptz not null default now(),
  unique (user_id, claim_date)
);

alter table public.dominion_web_points enable row level security;
alter table public.dominion_web_points force row level security;
alter table public.dominion_daily_claims enable row level security;
alter table public.dominion_daily_claims force row level security;

drop policy if exists dominion_web_points_select_own on public.dominion_web_points;
create policy dominion_web_points_select_own on public.dominion_web_points
  for select to authenticated using ((select auth.uid()) = user_id);
drop policy if exists dominion_daily_claims_select_own on public.dominion_daily_claims;
create policy dominion_daily_claims_select_own on public.dominion_daily_claims
  for select to authenticated using ((select auth.uid()) = user_id);
revoke all on public.dominion_web_points from anon, authenticated;
revoke all on public.dominion_daily_claims from anon, authenticated;
grant select on public.dominion_web_points to authenticated;
grant select on public.dominion_daily_claims to authenticated;

-- Atomic, idempotent claim: only a newly inserted claim can increase the balance.
create or replace function public.claim_dominion_daily_points(p_user_id uuid, p_claim_date date, p_points integer)
returns table (awarded boolean, new_balance bigint, claim_date date)
language plpgsql
security definer
set search_path = ''
as $$
declare
  inserted_id uuid;
  current_balance bigint;
begin
  if p_user_id is null or p_claim_date is null or p_points < 1 or p_points > 1000 then
    raise exception 'invalid_claim';
  end if;
  insert into public.dominion_web_points (user_id, balance)
    values (p_user_id, 0) on conflict (user_id) do nothing;
  insert into public.dominion_daily_claims (user_id, claim_date, points_awarded)
    values (p_user_id, p_claim_date, p_points)
    on conflict (user_id, claim_date) do nothing
    returning id into inserted_id;
  if inserted_id is not null then
    update public.dominion_web_points
      set balance = balance + p_points, updated_at = now()
      where user_id = p_user_id returning balance into current_balance;
    return query select true, current_balance, p_claim_date;
  else
    select balance into current_balance from public.dominion_web_points where user_id = p_user_id;
    return query select false, current_balance, p_claim_date;
  end if;
end;
$$;

revoke all on function public.claim_dominion_daily_points(uuid, date, integer) from public, anon, authenticated;
grant execute on function public.claim_dominion_daily_points(uuid, date, integer) to service_role;
commit;
