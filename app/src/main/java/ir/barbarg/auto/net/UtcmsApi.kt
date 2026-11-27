package ir.barbarg.auto.net

import ir.barbarg.auto.security.ServiceHash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * UTCMS.BOL.WS.V3.0 JSON/REST client.
 *
 * ITMFreighter.asmx handles user registration/OTP.
 * ITMBOL.asmx handles BOL issue, start, tracking and end operations.
 * Server-side licence, vehicle, driver and GPS checks remain authoritative.
 */
class UtcmsApi {
    companion object {
        const val FREIGHTER_BASE = "https://services2.sipaad.ir/ITMFreighter.asmx/"
        const val BOL_BASE = "https://services2.sipaad.ir/ITMBOL.asmx/"

        private const val CONNECT_TIMEOUT_MS = 20_000
        private const val READ_TIMEOUT_MS = 30_000

        private val freighterMethods = setOf(
            "WS39_RegUserGenerateOTP",
            "WS40_RegUserAuthenticateOTP"
        )
        private val bolMethods = setOf(
            "WS01_InsertBOL",
            "WS03_StartBOL",
            "WS04_EndBOL",
            "WS06_InsertBOLTrack",
            "WS31_GetBaseInfoGoodType",
            "WS32_GetBaseInfoLoadingType",
            "WS33_GetBaseInfoPackingType",
            "WS34_GetBaseInfoGood",
            "WS43_GetBaseInfoProvinceCity"
        )

        private val baseInfoMethods = mapOf(
            "goodTypes" to "WS31_GetBaseInfoGoodType",
            "loadingTypes" to "WS32_GetBaseInfoLoadingType",
            "packingTypes" to "WS33_GetBaseInfoPackingType",
            "goods" to "WS34_GetBaseInfoGood",
            "provinceCities" to "WS43_GetBaseInfoProvinceCity"
        )

        private val requiredBolFields = listOf(
            "PlaqueID",
            "PlaqueSN",
            "PlaqueType",
            "DriverNationalCode",
            "OWNERNATIONALID",
            "SenderType",
            "SenderName",
            "RecieverType",
            "RecieverName",
            "SerialNo",
            "IssuerNaCode",
            "IssueDate",
            "LoadingPlaceAddress",
            "OffLoadingPlaceAddress",
            "Goods",
            "TotalAmountPayment", "Freightage", "InsuranceCosts"
        )
    }

    data class Account(val companyCode: String, val password: CharArray)

    data class ApiResult(
        val success: Boolean,
        val payload: JSONObject? = null,
        val value: String? = null,
        val errorMessage: String? = null,
        val errorCode: Int? = null,
        val httpCode: Int? = null
    )

    /** Downloads official UTCMS reference lists; IDs are never fabricated in the app. */
    suspend fun getBaseInfo(account: Account, kind: String): ApiResult {
        val method = baseInfoMethods[kind]
            ?: return reject(account, "نوع اطلاعات پایه ناشناخته است")
        return call(method, account, JSONObject())
    }

    /** WS39: NaCode is a string and MobileNo is a 64-bit integer. */
    suspend fun generateOtp(
        account: Account,
        nationalCode: String,
        mobile: String
    ): ApiResult {
        if (!isValidNationalCode(nationalCode)) {
            return reject(account, "کد ملی راننده باید ۱۰ رقم معتبر باشد")
        }
        val mobileNumber = mobile.toLongOrNull()
        if (mobile.length != 11 || !mobile.startsWith("09") || mobileNumber == null) {
            return reject(account, "شماره موبایل باید ۱۱ رقم و با 09 شروع شود")
        }

        val body = JSONObject()
            .put("NaCode", nationalCode)
            .put("MobileNo", mobileNumber)
        return call("WS39_RegUserGenerateOTP", account, body)
    }

    /** WS40: OTPCode is an integer; the V3 guide specifies four digits. */
    suspend fun authenticateOtp(
        account: Account,
        idNo: Int,
        otp: String
    ): ApiResult {
        val otpNumber = otp.toIntOrNull()
        if (idNo <= 0) return reject(account, "شناسه کاربر نامعتبر است")
        if (otp.length != 4 || otpNumber == null) {
            return reject(account, "کد یک‌بارمصرف باید دقیقاً ۴ رقم باشد")
        }

        val body = JSONObject()
            .put("IDNo", idNo)
            .put("OTPCode", otpNumber)
        return call("WS40_RegUserAuthenticateOTP", account, body)
    }

    /**
     * WS01_InsertBOL. The BOLCnt object must contain the documented
     * mandatory fields and official base-data identifiers.
     */
    suspend fun insertBol(
        account: Account,
        bol: JSONObject,
        insertTime: Int,
        insertPosition: JSONObject
    ): ApiResult {
        if (insertTime <= 0) {
            return reject(account, "زمان ثبت سند باید Unix timestamp معتبر باشد")
        }
        val missing = requiredBolFields.filter { field ->
            !bol.has(field) || (bol.isNull(field) && field != "FreighterID") ||
                (bol.opt(field) is String && bol.optString(field).isBlank())
        }
        if (missing.isNotEmpty()) {
            return reject(
                account,
                "فیلدهای ضروری سند کامل نیست: " + missing.joinToString(", ")
            )
        }
        val goods = bol.optJSONObject("Goods")?.optJSONArray("BOLGoodCnt")
            ?: bol.optJSONArray("Goods")
        if ((goods?.length() ?: 0) == 0) {
            return reject(account, "حداقل یک ردیف کالا باید ثبت شود")
        }
        if (!hasGpsCoordinates(insertPosition)) {
            return reject(account, "موقعیت GPS معتبر برای ثبت سند لازم است")
        }

        val body = JSONObject()
            .put("bol", JSONObject(bol.toString()))
            .put("InsertTime", insertTime)
            .put("InsertPosition", JSONObject(insertPosition.toString()))
        return call("WS01_InsertBOL", account, body)
    }

    /** WS03_StartBOL: starts transport for a server-issued trace code. */
    suspend fun startBol(
        account: Account,
        traceCode: String,
        startTime: Int,
        startPosition: JSONObject
    ): ApiResult {
        if (traceCode.isBlank()) return reject(account, "کد رهگیری باربرگ وارد نشده است")
        if (startTime <= 0) return reject(account, "زمان شروع حمل معتبر نیست")
        if (!hasGpsCoordinates(startPosition)) {
            return reject(account, "موقعیت GPS معتبر برای شروع حمل لازم است")
        }

        val body = JSONObject()
            .put("BOLTraceCode", traceCode)
            .put("StartTime", startTime)
            .put("StartPosition", JSONObject(startPosition.toString()))
        return call("WS03_StartBOL", account, body)
    }

    /** WS06_InsertBOLTrack: sends a driver GPS point during an active transport. */
    suspend fun insertBolTrack(
        account: Account,
        traceCode: String,
        eventTime: Int,
        eventPosition: JSONObject
    ): ApiResult {
        if (traceCode.isBlank()) return reject(account, "کد رهگیری باربرگ وارد نشده است")
        if (eventTime <= 0) return reject(account, "زمان ثبت موقعیت معتبر نیست")
        if (!hasGpsCoordinates(eventPosition)) {
            return reject(account, "مختصات GPS معتبر نیست")
        }

        // UTCMS.BOL.WS guide defines WS06 arguments as BOLTraceCode,
        // EventTime and EventPosition (GPSCnt); it does not accept BOLPositions.
        val body = JSONObject()
            .put("BOLTraceCode", traceCode)
            .put("EventTime", eventTime)
            .put("EventPosition", JSONObject(eventPosition.toString()))
        return call("WS06_InsertBOLTrack", account, body)
    }

    /** WS04_EndBOL: ends an active transport with receipt details and final GPS. */
    suspend fun endBol(
        account: Account,
        traceCode: String,
        receiptNumber: Int,
        receiptTime: Int,
        endTime: Int,
        endPosition: JSONObject
    ): ApiResult {
        if (traceCode.isBlank()) return reject(account, "کد رهگیری باربرگ وارد نشده است")
        if (receiptNumber <= 0 || receiptTime <= 0 || endTime <= 0) {
            return reject(account, "شماره رسید و زمان‌های پایان حمل باید معتبر باشند")
        }
        if (!hasGpsCoordinates(endPosition)) {
            return reject(account, "موقعیت GPS معتبر برای پایان حمل لازم است")
        }

        val body = JSONObject()
            .put("BOLTraceCode", traceCode)
            .put("ReciptNo", receiptNumber)
            .put("ReciptTime", receiptTime)
            .put("EndTime", endTime)
            .put("EndPosition", JSONObject(endPosition.toString()))
        return call("WS04_EndBOL", account, body)
    }

    private suspend fun call(
        method: String,
        account: Account,
        fields: JSONObject
    ): ApiResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val baseUrl = when (method) {
                in freighterMethods -> FREIGHTER_BASE
                in bolMethods -> BOL_BASE
                else -> return@withContext ApiResult(
                    success = false,
                    errorMessage = "متد وب‌سرویس مجاز نیست"
                )
            }

            val hash = ServiceHash.create(account.companyCode, account.password)
            val requestBody = JSONObject(fields.toString())
                .put("CompanyCode", account.companyCode)
                .put("Salt", hash.salt)
                .put("HashedValue", hash.hashedValue)
                .toString()
                .toByteArray(Charsets.UTF_8)

            val conn = (URL(baseUrl + method).openConnection() as HttpURLConnection).apply {
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
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            parseResponse(raw, code)
        } catch (e: IOException) {
            ApiResult(false, errorMessage = "خطای شبکه: " + (e.message ?: "نامشخص"))
        } catch (e: Exception) {
            ApiResult(false, errorMessage = "خطای درخواست: " + (e.message ?: "نامشخص"))
        } finally {
            connection?.disconnect()
            account.password.fill('\u0000')
        }
    }

    /**
     * ASMX wraps JSON results in "d". Error objects may be returned with
     * HTTP 200, so inspect ErrCode rather than relying on HTTP status alone.
     */
    private fun parseResponse(raw: String, httpCode: Int): ApiResult {
        val envelope = try {
            JSONObject(raw.trim().removePrefix("\uFEFF"))
        } catch (_: Exception) {
            return ApiResult(
                success = false,
                errorMessage = if (httpCode in 200..299) {
                    "پاسخ JSON معتبر از سرویس دریافت نشد"
                } else {
                    "خطای سرویس (HTTP " + httpCode + ")"
                },
                httpCode = httpCode
            )
        }

        val wrappedValue = envelope.opt("d")
        val nestedObject = when (wrappedValue) {
            is JSONObject -> wrappedValue
            is String -> {
                val text = wrappedValue.trim()
                if (text.startsWith("{")) {
                    try { JSONObject(text) } catch (_: Exception) { null }
                } else null
            }
            else -> null
        }
        val payload = nestedObject ?: envelope
        val errorCode = if (payload.has("ErrCode")) {
            payload.optInt("ErrCode", -1)
        } else null
        val errorDescription = payload.optString("ErrDesc").takeIf { it.isNotBlank() }
        val value = when (wrappedValue) {
            null, JSONObject.NULL -> null
            is JSONObject -> wrappedValue.toString()
            else -> wrappedValue.toString()
        }

        if (httpCode !in 200..299) {
            return ApiResult(
                success = false,
                payload = payload,
                value = value,
                errorMessage = errorDescription ?: ("خطای سرویس (HTTP " + httpCode + ")"),
                errorCode = errorCode,
                httpCode = httpCode
            )
        }

        if (errorCode != null && errorCode != 0) {
            return ApiResult(
                success = false,
                payload = payload,
                value = value,
                errorMessage = errorDescription
                    ?: ("درخواست توسط سرویس رد شد (کد " + errorCode + ")"),
                errorCode = errorCode,
                httpCode = httpCode
            )
        }

        return ApiResult(
            success = true,
            payload = payload,
            value = value,
            httpCode = httpCode
        )
    }

    private fun reject(account: Account, message: String): ApiResult {
        account.password.fill('\u0000')
        return ApiResult(success = false, errorMessage = message)
    }

    private fun hasGpsCoordinates(position: JSONObject): Boolean {
        if (!position.has("Latitude") || !position.has("Longitude")) return false
        val latitude = position.optDouble("Latitude", Double.NaN)
        val longitude = position.optDouble("Longitude", Double.NaN)
        return latitude.isFinite() && longitude.isFinite() &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    private fun isValidNationalCode(code: String): Boolean {
        if (code.any { it !in '0'..'9' }) return false
        // Driver login uses a natural person's 10-digit Iranian national code.
        // Eleven-digit legal-entity identifiers must not be accepted as driver IDs.
        if (code.length != 10 || code.all { it == code[0] }) return false

        val check = code[9].digitToInt()
        val sum = (0..8).sumOf { index -> code[index].digitToInt() * (10 - index) }
        val remainder = sum % 11
        return (remainder < 2 && check == remainder) ||
            (remainder >= 2 && check == 11 - remainder)
    }
}
