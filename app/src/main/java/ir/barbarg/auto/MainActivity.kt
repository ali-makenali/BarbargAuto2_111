package ir.barbarg.auto

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import ir.barbarg.auto.BuildConfig
import ir.barbarg.auto.data.LocalDatabase
import ir.barbarg.auto.security.ServiceCredentialStore
import ir.barbarg.auto.net.UtcmsApi
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private val api = UtcmsApi()
    private lateinit var localDb: LocalDatabase
    private lateinit var credentialStore: ServiceCredentialStore
    private var loginNationalCode = ""
    private var loginMobile = ""
    private var loginAccountType = "driver"
    private var companyCode = ""
    private var servicePassword = charArrayOf()
    private var pendingIdNo = 0
    private var dashboard: WebView? = null
    private var lastTraceCode = ""
    private var serviceAuthenticated = false
    private var pendingBolJson: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        localDb = LocalDatabase(this)
        credentialStore = ServiceCredentialStore(this)
        provisionServiceCredentials()
        lastTraceCode = localDb.latestIssuedTrace()
        // ورود به خود اپلیکیشن آزاد است؛ احراز هویت UTCMS فقط هنگام اولین عملیات صدور انجام می‌شود.
        openDashboard()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val web = dashboard
                if (web != null && web.canGoBack()) web.goBack() else finish()
            }
        })
    }

    private fun provisionServiceCredentials(): Boolean {
        val company = BuildConfig.UTCMS_COMPANY_CODE.trim()
        val password = BuildConfig.UTCMS_SERVICE_PASSWORD.toCharArray()
        if (company.isBlank() || password.isEmpty()) {
            password.fill('\u0000')
            return false
        }
        return try {
            if (!credentialStore.hasCredentials()) credentialStore.save(company, password)
            true
        } catch (_: Exception) {
            false
        } finally {
            password.fill('\u0000')
        }
    }

    private fun serviceSetupUnavailable() {
        notifyPage(false, "اتصال به سامانه باربرگ در دسترس نیست. لطفاً بعداً دوباره تلاش کنید یا با پشتیبانی تماس بگیرید.")
    }

    private fun showLoginDialog(prefillNational: String = "", prefillMobile: String = "") {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(42, 12, 42, 0) }
        fun field(label: String, type: Int = InputType.TYPE_CLASS_TEXT) = EditText(this).apply {
            hint = label
            inputType = type
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            box.addView(this)
        }
        val national = field("کد ملی راننده", InputType.TYPE_CLASS_NUMBER).apply { setText(prefillNational) }
        val mobile = field("شماره همراه ثبت‌شده در UTCMS", InputType.TYPE_CLASS_PHONE).apply { setText(prefillMobile) }
        val help = TextView(this).apply { text = "برای احراز هویت، کد ملی و شماره همراه ثبت‌شده را وارد کنید."; setPadding(0, 16, 0, 0) }
        box.addView(help)
        val dlg = AlertDialog.Builder(this).setTitle("ورود و احراز هویت").setView(box)
            .setCancelable(false)
            .setPositiveButton("دریافت کد تأیید", null).create()
        dlg.setOnShowListener {
            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val n = national.text.toString().trim()
                val m = mobile.text.toString().trim()
                if (n.isBlank() || m.isBlank()) {
                    help.text = "کد ملی و شماره همراه الزامی است."
                    return@setOnClickListener
                }
                val credentials = credentialStore.load()
                if (credentials == null) {
                    dlg.dismiss()
                    serviceSetupUnavailable()
                    return@setOnClickListener
                }
                loginAccountType = "driver"
                dlg.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                help.text = "در حال درخواست کد یک‌بارمصرف از UTCMS…"
                lifecycleScope.launch {
                    val secret = credentials.password.copyOf()
                    val result = api.generateOtp(UtcmsApi.Account(credentials.companyCode, secret.copyOf()), n, m)
                    val nested = try { result.value?.let { JSONObject(it) } } catch (_: Exception) { null }
                    val id = result.payload?.optInt("IDNo", -1)?.takeIf { it > 0 }
                        ?: nested?.optInt("IDNo", -1)?.takeIf { it > 0 }
                        ?: result.value?.trim()?.toIntOrNull()
                        ?: result.payload?.optString("d")?.toIntOrNull()
                        ?: 0
                    if (result.success && id > 0) {
                        companyCode = credentials.companyCode
                        servicePassword.fill('\u0000')
                        servicePassword = secret.copyOf()
                        pendingIdNo = id
                        loginNationalCode = n
                        loginMobile = m
                        secret.fill('\u0000')
                        credentials.password.fill('\u0000')
                        dlg.dismiss()
                        showOtpDialog()
                    } else {
                        secret.fill('\u0000')
                        credentials.password.fill('\u0000')
                        dlg.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                        help.text = result.errorMessage ?: ("پاسخ سرویس: " + (result.value ?: result.payload))
                    }
                }
            }
        }
        dlg.show()
    }

    private fun showOtpDialog() {
        val input = EditText(this).apply { hint = "کد یک‌بارمصرف ۴ رقمی"; inputType = InputType.TYPE_CLASS_NUMBER }
        val help = TextView(this).apply { text = "کد ارسال‌شده به شماره ثبت‌شده در UTCMS را وارد کنید." }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(42, 8, 42, 0); addView(input); addView(help) }
        val dlg = AlertDialog.Builder(this).setTitle("تأیید کد یک‌بارمصرف").setView(box).setCancelable(false)
            .setNegativeButton("بازگشت") { _, _ -> showLoginDialog() }.setPositiveButton("تأیید و ورود", null).create()
        dlg.setOnShowListener {
            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val otp = input.text.toString().trim()
                if (otp.length != 4) { help.text = "کد باید دقیقاً ۴ رقم باشد."; return@setOnClickListener }
                dlg.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                help.text = "در حال تأیید هویت در UTCMS…"
                lifecycleScope.launch {
                    val result = api.authenticateOtp(UtcmsApi.Account(companyCode, servicePassword.copyOf()), pendingIdNo, otp)
                    if (result.success) {
                        localDb.saveVerifiedProfile(loginNationalCode, loginAccountType, loginMobile)
                        dlg.dismiss()
                        serviceAuthenticated = true
                        val queued = pendingBolJson
                        pendingBolJson = null
                        (dashboard?.let { UtcmsBridge() })?.loadRequiredBaseInfo()
                        if (queued != null) (dashboard?.let { UtcmsBridge() })?.submitBol(queued)
                    }
                    else {
                        dlg.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                        help.text = result.errorMessage ?: ("احراز هویت ناموفق بود: " + (result.value ?: ""))
                    }
                }
            }
        }
        dlg.show()
    }

    private fun openDashboard() {
        val web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = false
            settings.allowFileAccessFromFileURLs = false
            settings.allowUniversalAccessFromFileURLs = false
            addJavascriptInterface(UtcmsBridge(), "BarbargNative")
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String) =
                    !url.startsWith("file:///android_asset/")
            }
        }
        dashboard = web
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
    }

    private inner class UtcmsBridge {
        @JavascriptInterface
        fun saveLocalRecord(type: String, key: String, json: String): Boolean =
            try { localDb.put(type, key, JSONObject(json)) } catch (_: Exception) { false }

        @JavascriptInterface
        fun listLocalRecords(type: String): String =
            try { localDb.all(type).toString() } catch (_: Exception) { "[]" }

        @JavascriptInterface
        fun deleteLocalRecord(type: String, key: String): Boolean =
            try { localDb.delete(type, key) } catch (_: Exception) { false }

        @JavascriptInterface
        fun connectService(nationalCode: String, mobile: String) {
            runOnUiThread {
                if (nationalCode.isBlank() || mobile.isBlank()) {
                    notifyPage(false, "ابتدا کد ملی و شماره همراه راننده را در بخش راننده ثبت کنید.")
                    return@runOnUiThread
                }
                if (serviceAuthenticated && localDb.isProfileVerified(nationalCode, mobile)) {
                    loadRequiredBaseInfo()
                    return@runOnUiThread
                }
                val credentials = credentialStore.load()
                if (credentials == null) {
                    serviceSetupUnavailable()
                    return@runOnUiThread
                }
                if (localDb.isProfileVerified(nationalCode, mobile)) {
                    companyCode = credentials.companyCode
                    servicePassword.fill('\u0000')
                    servicePassword = credentials.password.copyOf()
                    credentials.password.fill('\u0000')
                    loginNationalCode = nationalCode
                    loginMobile = mobile
                    serviceAuthenticated = true
                    loadRequiredBaseInfo()
                } else {
                    credentials.password.fill('\u0000')
                    showLoginDialog(nationalCode, mobile)
                }
            }
        }

        @JavascriptInterface
        fun getBolHistory(): String =
            try { localDb.bolHistory().toString() } catch (_: Exception) { "[]" }

        @JavascriptInterface
        fun loadBaseInfo(kind: String) {
            runOnUiThread {
                if (!serviceAuthenticated) {
                    notifyBaseInfo(kind, false, "ابتدا احراز هویت سرویس را از طریق درخواست صدور انجام دهید.")
                    return@runOnUiThread
                }
                val secret = servicePassword.copyOf()
                lifecycleScope.launch {
                    val result = api.getBaseInfo(UtcmsApi.Account(companyCode, secret), kind)
                    if (result.success) {
                        val data = result.value ?: result.payload?.toString() ?: "[]"
                        localDb.put("baseinfo", kind, JSONObject().put("raw", data))
                        notifyBaseInfo(kind, true, data)
                    } else {
                        val cached = localDb.get("baseinfo", kind)?.optString("raw")
                        if (!cached.isNullOrBlank()) {
                            notifyBaseInfo(kind, true, cached)
                        } else {
                            notifyBaseInfo(kind, false, result.errorMessage ?: result.value ?: "دریافت اطلاعات پایه ناموفق بود")
                        }
                    }
                }
            }
        }

        fun loadRequiredBaseInfo() {
            listOf("goodTypes", "loadingTypes", "packingTypes", "goods", "provinceCities")
                .forEach { loadBaseInfo(it) }
        }

        @JavascriptInterface
        fun issueBol(json: String) {
            runOnUiThread {
                pendingBolJson = json
                val bol = try { JSONObject(json) } catch (_: Exception) { JSONObject() }
                val national = bol.optString("DriverNationalCode")
                val mobile = bol.optString("DriverMobile")
                if (serviceAuthenticated) {
                    val queued = pendingBolJson
                    if (national.isNotBlank() && mobile.isNotBlank() &&
                        localDb.isProfileVerified(national, mobile)) {
                        pendingBolJson = null
                        if (queued != null) submitBol(queued)
                    } else {
                        showLoginDialog(national, mobile)
                    }
                    return@runOnUiThread
                }
                val credentials = credentialStore.load()
                if (credentials == null) {
                    serviceSetupUnavailable()
                    return@runOnUiThread
                }
                if (national.isNotBlank() && mobile.isNotBlank() &&
                    localDb.isProfileVerified(national, mobile)) {
                    companyCode = credentials.companyCode
                    servicePassword.fill('\u0000')
                    servicePassword = credentials.password.copyOf()
                    credentials.password.fill('\u0000')
                    loginNationalCode = national
                    loginMobile = mobile
                    serviceAuthenticated = true
                    loadRequiredBaseInfo()
                    val queued = pendingBolJson
                    pendingBolJson = null
                    if (queued != null) submitBol(queued)
                } else {
                    credentials.password.fill('\u0000')
                    showLoginDialog(national, mobile)
                }
            }
        }

        fun submitBol(json: String) {
            runOnUiThread {
                val position = currentGps()
                if (position == null) {
                    notifyPage(false, "موقعیت GPS تازه در دسترس نیست. GPS گوشی را روشن و مجوز مکان را فعال کنید.")
                    return@runOnUiThread
                }
                lifecycleScope.launch {
                    try {
                        val bol = JSONObject(json)
                        // DriverMobile is used locally to bind OTP verification to this driver;
                        // it is not a member of the official BOLCnt payload.
                        bol.remove("DriverMobile")
                        val result = api.insertBol(
                            UtcmsApi.Account(companyCode, servicePassword.copyOf()),
                            bol,
                            (System.currentTimeMillis() / 1000L).toInt(),
                            gpsJson(position)
                        )
                        val trace = result.value?.trim()?.takeIf { it.isNotBlank() && !it.startsWith("{") }
                            ?: result.payload?.optString("TraceCode")?.takeIf { it.isNotBlank() }
                        if (result.success && !trace.isNullOrBlank()) {
                            lastTraceCode = trace
                            localDb.saveBol(trace, bol, result.payload ?: JSONObject().put("value", result.value ?: ""), "ISSUED")
                            notifyPage(true, "باربرگ صادر شد. کد رهگیری: " + trace, trace)
                        } else {
                            val failure = JSONObject()
                                .put("errorCode", result.errorCode ?: JSONObject.NULL)
                                .put("errorMessage", result.errorMessage ?: "درخواست صدور رد شد")
                                .put("httpCode", result.httpCode ?: JSONObject.NULL)
                                .put("raw", result.value ?: result.payload?.toString() ?: "")
                            localDb.saveBolFailure(bol, failure)
                            notifyPage(false, result.errorMessage ?: ("پاسخ صدور: " + (result.value ?: result.payload)))
                        }
                    } catch (e: Exception) {
                        notifyPage(false, "اطلاعات باربرگ قابل پردازش نیست: " + (e.message ?: ""))
                    }
                }
            }
        }

        @JavascriptInterface
        fun startTransport() = runGpsOperation("شروع حمل") { gps ->
            api.startBol(UtcmsApi.Account(companyCode, servicePassword.copyOf()), lastTraceCode,
                (System.currentTimeMillis() / 1000L).toInt(), gps)
        }

        @JavascriptInterface
        fun sendTrack() = runGpsOperation("ثبت موقعیت") { gps ->
            api.insertBolTrack(UtcmsApi.Account(companyCode, servicePassword.copyOf()), lastTraceCode,
                (System.currentTimeMillis() / 1000L).toInt(), gps)
        }

        @JavascriptInterface
        fun endTransport(receiptNo: Int, receiptTime: Int) = runGpsOperation("پایان حمل") { gps ->
            api.endBol(UtcmsApi.Account(companyCode, servicePassword.copyOf()), lastTraceCode,
                receiptNo, receiptTime, (System.currentTimeMillis() / 1000L).toInt(), gps)
        }
    }

    private fun ensureServiceCredentials(): Boolean {
        if (companyCode.isNotBlank() && servicePassword.isNotEmpty()) return true
        val credentials = try { credentialStore.load() } catch (_: Exception) { null }
        if (credentials == null) {
            serviceSetupUnavailable()
            return false
        }
        companyCode = credentials.companyCode
        servicePassword.fill('\u0000')
        servicePassword = credentials.password.copyOf()
        credentials.password.fill('\u0000')
        return true
    }

    private fun runGpsOperation(label: String, operation: suspend (JSONObject) -> UtcmsApi.ApiResult) {
        runOnUiThread {
            if (!ensureServiceCredentials()) return@runOnUiThread
            if (lastTraceCode.isBlank()) {
                notifyPage(false, "ابتدا باید باربرگ صادر شود و کد رهگیری دریافت شود.")
                return@runOnUiThread
            }
            val position = currentGps()
            if (position == null) {
                notifyPage(false, "GPS تازه در دسترس نیست؛ مکان گوشی را فعال کنید.")
                return@runOnUiThread
            }
            lifecycleScope.launch {
                val result = operation(gpsJson(position))
                if (result.success) notifyPage(true, label + " با موفقیت در UTCMS ثبت شد.", lastTraceCode)
                else notifyPage(false, result.errorMessage ?: (label + " ناموفق بود؛ کد خطا: " + result.errorCode))
            }
        }
    }

    private fun currentGps(): Location? {
        val fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 501)
            return null
        }
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val candidates = manager.getProviders(true).mapNotNull { provider ->
            try { manager.getLastKnownLocation(provider) } catch (_: SecurityException) { null }
        }
        val best = candidates.maxByOrNull { it.time } ?: return null
        if (System.currentTimeMillis() - best.time > 120_000L) return null
        return best
    }

    private fun gpsJson(location: Location) = JSONObject()
        .put("Latitude", location.latitude)
        .put("Longitude", location.longitude)
        .put("Altitude", location.altitude)
        .put("Bearing", location.bearing.toDouble())
        .put("NumberOfSatellite", 0)
        .put("PDOP", 0)
        .put("GPSSpeed", (location.speed * 3.6f).toInt())
        .put("GPSMaxSpeed", 0)
        .put("GPSTotalTraveledDistance", 0)

    private fun notifyBaseInfo(kind: String, success: Boolean, data: String) {
        val payload = JSONObject().put("kind", kind).put("success", success).put("data", data)
        dashboard?.post {
            dashboard?.evaluateJavascript(
                "window.onBaseInfo && window.onBaseInfo(" + JSONObject.quote(payload.toString()) + ")",
                null
            )
        }
    }

    private fun notifyPage(success: Boolean, message: String, trace: String = "") {
        val payload = JSONObject().put("success", success).put("message", message).put("traceCode", trace)
        dashboard?.post {
            dashboard?.evaluateJavascript(
                "window.onNativeResult && window.onNativeResult(" + JSONObject.quote(payload.toString()) + ")",
                null
            )
        }
    }

    override fun onDestroy() {
        servicePassword.fill('\u0000')
        if (::localDb.isInitialized) localDb.close()
        dashboard?.stopLoading()
        dashboard?.removeJavascriptInterface("BarbargNative")
        dashboard?.destroy()
        super.onDestroy()
    }
}
