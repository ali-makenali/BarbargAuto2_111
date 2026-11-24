package ir.barbarg.auto.net

import ir.barbarg.auto.security.ServiceHash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * JSON-over-HTTP implementation retained from the supplied source.
 * Do not assume this matches SIPAAD's ASMX/SOAP contract until the official
 * WSDL/API document confirms the endpoint, verbs, field names and hash scheme.
 */
class UtcmsApi {
    companion object {
        const val FREIGHTER_BASE = "https://services2.sipaad.ir/ITMFreighter.asmx/"
        private const val CONNECT_TIMEOUT_MS = 20_000
        private const val READ_TIMEOUT_MS = 30_000
        private val allowedMethods = setOf("WS39_RegUserGenerateOTP", "WS40_RegUserAuthenticateOTP")
    }

    data class Account(val companyCode: String, val password: CharArray)

    data class ApiResult(
        val success: Boolean,
        val payload: JSONObject? = null,
        val errorMessage: String? = null,
        val httpCode: Int? = null
    )

    suspend fun generateOtp(account: Account, nationalCode: String, mobile: String): ApiResult {
        if (!isValidNationalCode(nationalCode)) return ApiResult(false, errorMessage = "کد ملی نامعتبر است")
        if (!isValidMobile(mobile)) return ApiResult(false, errorMessage = "شماره موبایل باید با 09 شروع شود و ۱۱ رقم باشد")
        val body = JSONObject().put("NaCode", nationalCode).put("MobileNo", mobile)
        return call("WS39_RegUserGenerateOTP", account, body)
    }

    suspend fun authenticateOtp(account: Account, idNo: Int, otp: String): ApiResult {
        if (idNo <= 0) return ApiResult(false, errorMessage = "شناسه نامعتبر است")
        if (otp.length !in 4..6 || otp.any { !it.isDigit() }) {
            return ApiResult(false, errorMessage = "کد یک‌بارمصرف باید ۴ تا ۶ رقم باشد")
        }
        val body = JSONObject().put("IDNo", idNo).put("OTPCode", otp)
        return call("WS40_RegUserAuthenticateOTP", account, body)
    }

    private suspend fun call(method: String, account: Account, fields: JSONObject): ApiResult =
        withContext(Dispatchers.IO) {
            if (method !in allowedMethods) {
                account.password.fill('\u0000')
                return@withContext ApiResult(false, errorMessage = "متد درخواست مجاز نیست")
            }
            var connection: HttpURLConnection? = null
            try {
                val hash = ServiceHash.create(account.companyCode, account.password)
                val requestBody = JSONObject(fields.toString())
                    .put("CompanyCode", account.companyCode)
                    .put("Salt", hash.salt)
                    .put("HashedValue", hash.hashedValue)
                    .toString()
                    .toByteArray(Charsets.UTF_8)

                val conn = (URL(FREIGHTER_BASE + method).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Connection", "close")
                }
                connection = conn
                conn.outputStream.use { it.write(requestBody) }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                    ?: throw IOException("پاسخ خالی از سرور (HTTP $code)")
                val raw = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val parsed = try {
                    JSONObject(raw)
                } catch (e: Exception) {
                    return@withContext ApiResult(false, errorMessage = "پاسخ JSON معتبر نیست", httpCode = code)
                }
                val data = parsed.opt("d")
                val payload = if (data is String && data.trim().startsWith("{")) {
                    try { JSONObject(data) } catch (_: Exception) { parsed }
                } else parsed

                if (code in 200..299) ApiResult(true, payload, httpCode = code)
                else ApiResult(false, payload, "خطای سرور (HTTP $code)", code)
            } catch (e: IOException) {
                ApiResult(false, errorMessage = "خطای شبکه: ${e.message}")
            } catch (e: Exception) {
                ApiResult(false, errorMessage = "خطای درخواست: ${e.message}")
            } finally {
                connection?.disconnect()
                account.password.fill('\u0000')
            }
        }

    private fun isValidNationalCode(code: String): Boolean {
        if (code.length != 10 || code.any { !it.isDigit() }) return false
        if (code.all { it == code[0] }) return false
        val check = code[9].digitToInt()
        val sum = (0..8).sumOf { index -> code[index].digitToInt() * (10 - index) }
        val remainder = sum % 11
        return (remainder < 2 && check == remainder) || (remainder >= 2 && check == 11 - remainder)
    }

    private fun isValidMobile(mobile: String): Boolean =
        mobile.length == 11 && mobile.startsWith("09") && mobile.all { it in '0'..'9' }
}
