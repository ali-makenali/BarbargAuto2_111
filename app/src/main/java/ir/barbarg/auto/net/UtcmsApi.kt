package ir.barbarg.auto.net
import ir.barbarg.auto.security.ServiceHash
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
class UtcmsApi {
 companion object { const val FREIGHTER="https://services2.sipaad.ir/ITMFreighter.asmx/" }
 data class Account(val companyCode:String,val password:CharArray)
 fun generateOtp(a:Account,nationalCode:String,mobile:Long)=call("WS39_RegUserGenerateOTP",a,JSONObject().put("NaCode",nationalCode).put("MobileNo",mobile))
 fun authenticateOtp(a:Account,idNo:Int,otp:Int)=call("WS40_RegUserAuthenticateOTP",a,JSONObject().put("IDNo",idNo).put("OTPCode",otp))
 private fun call(method:String,a:Account,fields:JSONObject):JSONObject {
  val h=ServiceHash.create(a.companyCode,a.password)
  fields.put("CompanyCode",a.companyCode).put("Salt",h.salt).put("HashedValue",h.hashedValue)
  val c=URL(FREIGHTER+method).openConnection() as HttpURLConnection
  try {
   c.requestMethod="POST"; c.connectTimeout=20000; c.readTimeout=30000; c.doOutput=true
   c.setRequestProperty("Content-Type","application/json; charset=utf-8"); c.setRequestProperty("Accept","application/json")
   c.outputStream.use { it.write(fields.toString().toByteArray(Charsets.UTF_8)) }
   val s=if(c.responseCode in 200..299)c.inputStream else c.errorStream
   val o=JSONObject(s.bufferedReader(Charsets.UTF_8).use{it.readText()})
   val d=o.opt("d"); return if(d is String && d.trim().startsWith("{")) JSONObject(d) else o
  } finally { c.disconnect() }
 }
}
