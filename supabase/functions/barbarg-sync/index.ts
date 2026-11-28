import { createClient } from "npm:@supabase/supabase-js@2";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const tables: Record<string, string> = {
  vehicles: "vehicles",
  drivers: "drivers",
  cargo: "cargo_items",
  routes: "routes",
  bol: "bol_issuances",
};

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (req.method !== "POST") return Response.json({ error: "method_not_allowed" }, { status: 405, headers: cors });

  const auth = req.headers.get("Authorization");
  if (!auth?.startsWith("Bearer ")) {
    return Response.json({ error: "unauthorized" }, { status: 401, headers: cors });
  }

  try {
    const url = Deno.env.get("SUPABASE_URL");
    const key = Deno.env.get("SUPABASE_ANON_KEY") ?? Deno.env.get("SUPABASE_PUBLISHABLE_KEY");
    if (!url || !key) throw new Error("Supabase function environment is incomplete");

    const supabase = createClient(url, key, {
      global: { headers: { Authorization: auth } },
      auth: { persistSession: false, autoRefreshToken: false },
    });

    const { data: userData, error: userError } = await supabase.auth.getUser();
    if (userError || !userData.user) {
      return Response.json({ error: "invalid_session" }, { status: 401, headers: cors });
    }

    const body = await req.json();
    const entity = String(body.entity ?? "");
    const action = String(body.action ?? "");
    const table = tables[entity];
    if (!table || !["list", "upsert", "delete"].includes(action)) {
      return Response.json({ error: "invalid_operation" }, { status: 400, headers: cors });
    }

    if (action === "list") {
      const { data, error } = await supabase.from(table).select("*").order("updated_at", { ascending: false }).limit(500);
      if (error) throw error;
      return Response.json({ data }, { headers: cors });
    }

    if (action === "upsert") {
      const record = body.record;
      if (!record || typeof record !== "object" || Array.isArray(record)) {
        return Response.json({ error: "invalid_record" }, { status: 400, headers: cors });
      }
      const allowed = entity === "vehicles"
        ? ["local_key", "plate_id", "plate_type", "vehicle_type", "capacity", "metadata"]
        : entity === "drivers"
        ? ["local_key", "national_code", "mobile", "full_name", "metadata"]
        : entity === "cargo"
        ? ["local_key", "title", "goods_code", "metadata"]
        : entity === "routes"
        ? ["local_key", "origin", "destination", "metadata"]
        : ["local_trace_code", "trace_code", "status", "request_payload", "response_payload", "error_code", "error_message", "issued_at"];
      const clean: Record<string, unknown> = {};
      for (const field of allowed) if (field in record) clean[field] = record[field];
      clean.owner_id = userData.user.id;
      clean.updated_at = new Date().toISOString();
      const conflict = entity === "vehicles" ? "owner_id,plate_id"
        : entity === "drivers" ? "owner_id,national_code"
        : entity === "cargo" ? "owner_id,local_key"
        : entity === "routes" ? "owner_id,local_key"
        : undefined;
      const query = conflict
        ? supabase.from(table).upsert(clean, { onConflict: conflict }).select().single()
        : supabase.from(table).insert(clean).select().single();
      const { data, error } = await query;
      if (error) throw error;
      return Response.json({ data }, { headers: cors });
    }

    const recordId = String(body.id ?? "");
    if (!recordId) return Response.json({ error: "missing_id" }, { status: 400, headers: cors });
    const { error } = await supabase.from(table).delete().eq("id", recordId);
    if (error) throw error;
    return Response.json({ ok: true }, { headers: cors });
  } catch (error) {
    console.error("barbarg-sync error", error);
    return Response.json({ error: "request_failed" }, { status: 500, headers: cors });
  }
});
