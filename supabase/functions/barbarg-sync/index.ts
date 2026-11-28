import { createClient } from "npm:@supabase/supabase-js@2";
const cors = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "authorization, apikey, content-type", "Access-Control-Allow-Methods": "POST, OPTIONS" };
const tables: Record<string,string> = { vehicles:"vehicles", drivers:"drivers", cargo:"cargo_items", routes:"routes", bol:"bol_issuances" };
Deno.serve(async (req:Request) => {
 if(req.method==="OPTIONS") return new Response("ok",{headers:cors});
 if(req.method!=="POST") return Response.json({error:"method_not_allowed"},{status:405,headers:cors});
 const auth=req.headers.get("Authorization");
 if(!auth?.startsWith("Bearer ")) return Response.json({error:"unauthorized"},{status:401,headers:cors});
 try {
  const url=Deno.env.get("SUPABASE_URL"), key=Deno.env.get("SUPABASE_ANON_KEY")??Deno.env.get("SUPABASE_PUBLISHABLE_KEY");
  if(!url||!key) throw new Error("Missing Supabase environment");
  const sb=createClient(url,key,{global:{headers:{Authorization:auth}},auth:{persistSession:false,autoRefreshToken:false}});
  const {data:u,error:ue}=await sb.auth.getUser();
  if(ue||!u.user) return Response.json({error:"invalid_session"},{status:401,headers:cors});
  const body=await req.json(), entity=String(body.entity??""), action=String(body.action??""), table=tables[entity];
  if(!table||!["list","upsert","delete"].includes(action)) return Response.json({error:"invalid_operation"},{status:400,headers:cors});
  if(action==="list"){const {data,error}=await sb.from(table).select("*").order("updated_at",{ascending:false}).limit(500);if(error)throw error;return Response.json({data},{headers:cors});}
  if(action==="upsert"){
   const rec=body.record;if(!rec||typeof rec!=="object"||Array.isArray(rec))return Response.json({error:"invalid_record"},{status:400,headers:cors});
   const fields:Record<string,string[]>={vehicles:["local_key","plate_id","plate_type","vehicle_type","capacity","metadata"],drivers:["local_key","national_code","mobile","full_name","metadata"],cargo:["local_key","title","goods_code","metadata"],routes:["local_key","origin","destination","metadata"],bol:["local_trace_code","trace_code","status","request_payload","response_payload","error_code","error_message","issued_at"]};
   const clean:Record<string,unknown>={owner_id:u.user.id,updated_at:new Date().toISOString()};
   for(const k of fields[entity])if(k in rec)clean[k]=rec[k];
   const conflict=entity==="vehicles"?"owner_id,plate_id":entity==="drivers"?"owner_id,national_code":undefined;
   const q=conflict?sb.from(table).upsert(clean,{onConflict:conflict}).select().single():sb.from(table).insert(clean).select().single();
   const {data,error}=await q;if(error)throw error;return Response.json({data},{headers:cors});
  }
  const id=String(body.id??"");if(!id)return Response.json({error:"missing_id"},{status:400,headers:cors});
  const {error}=await sb.from(table).delete().eq("id",id);if(error)throw error;return Response.json({ok:true},{headers:cors});
 } catch(e){console.error("barbarg-sync",e);return Response.json({error:"request_failed"},{status:500,headers:cors});}
});
