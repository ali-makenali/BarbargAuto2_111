# Supabase backend

- Project ref: `ufejrknhkwrtvfesxbbp`.
- Schema migration: `supabase/migrations/20261128000000_barbarg_core_data_schema.sql` (applied to the linked project).
- Edge Function: `barbarg-sync` (deployed, JWT verification enabled).
- All exposed data tables use owner-scoped Row Level Security. The function forwards the caller JWT and uses the caller-scoped Data API client; it never uses a service-role key.
- UTCMS credentials are intentionally not stored in these tables or in Supabase client configuration.

## Important integration prerequisite
The Android app currently authenticates drivers directly with UTCMS OTP and does not create a Supabase Auth session. The sync function therefore returns HTTP 401 until a separate Supabase Auth identity/session is established and its access token is sent as `Authorization: Bearer <JWT>`. Do not replace this with a hard-coded shared secret in the APK. Map the UTCMS-verified driver identity to the Supabase user only after a trusted server-side identity-linking flow is implemented.
