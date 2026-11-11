# Iron Dominion authentication backend plan

Status: planning and schema baseline only. **No credentials are collected and no sign-in provider is enabled by this repository change.**

## Decision

Use **Supabase Auth + PostgreSQL** with the existing static site hosted at GitHub Pages (https://theirondominion.com). Supabase provides the hosted authentication API and database; the static site calls the public Auth API using the Supabase JavaScript client. Keep GitHub as the source of truth for website code and use branches/PRs for changes.

Official references:
- Auth overview: https://supabase.com/docs/guides/auth
- Email/password: https://supabase.com/docs/guides/auth/passwords
- Microsoft/Azure provider setup: https://supabase.com/docs/guides/auth/social-login/auth-azure
- Row-level security: https://supabase.com/docs/guides/database/postgres/row-level-security

## Account experience

Offer two clearly labelled choices on the Dominion Gate:
1. **Iron Dominion account** — email + password, verified email, sign-in, sign-out, password reset/change, and account deletion request.
2. **Continue with Microsoft** — Supabase's Microsoft/Azure OAuth provider, configured with a Microsoft Entra app registration and exact callback/redirect URLs.

Important: Microsoft authentication proves control of a Microsoft identity. It does **not by itself prove Minecraft ownership, Xbox identity, gamer tag, or entitlement**. Do not label an account Minecraft-verified or grant server access based only on Microsoft sign-in. Any such claim requires a separate, documented verification flow against the appropriate Microsoft/Xbox/Minecraft service and user consent.

## Architecture

- Public frontend: existing static HTML/CSS/JS on GitHub Pages.
- Authentication: Supabase Auth; email confirmation enabled; password reset redirects restricted to the official site.
- Data: PostgreSQL public.profiles table, keyed to auth.users.id.
- Authorization: Row Level Security (RLS) on every user-owned table; a user can read/update only their own profile fields. Hidden buttons or frontend route checks are not authorization.
- Secrets: only the Supabase project URL and publishable/anon key may be included in browser code. Never expose the service_role key, database password, OAuth client secret, SMTP password, or management API token in HTML, JS, GitHub commits, or public Actions logs.
- Admin-only tasks: use a trusted server/Edge Function with secrets held in Supabase secrets, and verify caller authorization server-side. Do not add an admin panel as part of this phase.
- Sessions: use the supported Supabase client; do not invent localStorage token handling. Review session lifetime, refresh, logout, and multi-tab behavior before release.
- Hosting boundary: GitHub Pages is static hosting. Do not assume files under api/ or server/ execute there. Supabase handles Auth/database APIs; any custom privileged logic must run in a deployed server/Edge Function, not as a static file.

## Setup tasks that require project-owner action

1. Create a Supabase project in the owner's account and choose a region appropriate for the community.
2. Set the Auth Site URL to https://theirondominion.com; add only required exact redirect URLs for the production site and a separate local test origin. Avoid wildcard redirects.
3. Apply and review supabase/migrations/202610280001_profiles.sql in a test project first.
4. Configure email confirmation and password recovery. Set up a custom SMTP sender before public launch; the default email service has strict limits and is not intended as the production mail path.
5. Register a Microsoft Entra application. Configure supported personal Microsoft accounts as appropriate, the exact Supabase callback URL, and the matching provider credentials in the Supabase dashboard. Do not commit client secrets.
6. Configure provider scopes to the minimum required. Start with sign-in identity only; do not request Xbox/Minecraft scopes unless a separate verified feature needs them.
7. Add the project URL and publishable key to the frontend configuration only after the project exists. Publishable keys are not authorization; RLS remains mandatory.
8. Implement the UI integration in a separate PR after configuration is available. Until then the current pages must continue to say login/registration are unavailable and must not collect passwords.
9. Test on a non-production project: signup, duplicate email behavior, confirmation link, login, invalid credentials, reset flow, Microsoft cancel/success, redirect allowlist, logout, expired session, profile isolation between two users, RLS denials, mobile layouts, CSP, and no secrets in the built site.
10. Update privacy/terms to explain data collected, purpose, retention, deletion/contact method, identity-provider use, and account deletion before launch.

## Data model baseline

The migration creates a minimal private profile table and a trigger that creates a profile when Supabase Auth creates a user. It deliberately has no roles, ranks, currency, entitlements, or server privileges. Those must not be granted from browser-controlled metadata. Keep authorization decisions out of editable profile fields.

## Launch gates

Do not enable credential forms or claim accounts are live until all are true:
- Supabase project and production URL/redirects are configured.
- Email verification and recovery emails work through configured SMTP.
- Microsoft provider works with the exact registered callback.
- RLS tests show one user cannot read/change another user's profile.
- No service-role key or OAuth client secret appears in the repository, built assets, or client network requests.
- Security review, accessibility/mobile checks, and end-to-end tests pass.
- Privacy and terms are updated and the owner approves public launch.

## Rollback

If any test fails, leave the gate sealed and keep account pages informational. Revert the integration PR without deleting the Supabase project or user data; preserve a tested backup/migration plan before schema changes.