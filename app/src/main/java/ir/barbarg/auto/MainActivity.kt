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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        localDb = LocalDatabase(this)
        credentialStore = ServiceCredentialStore(this)
        if (credentialStore.hasCredentials()) showLoginDialog() else showServiceSetupDialog()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val web = dashboard
                if (web != null && web.canGoBack()) web.goBack() else showLoginDialog()
            }
        })
    }

    private fun showServiceSetupDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(42, 12, 42, 0) }
        fun field(label: String, type: Int = InputType.TYPE_CLASS_TEXT) = EditText(this).apply {
            hint = label
            inputType = type
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            box.addView(this)
        }
        val company = field("شناسه فنی حساب وب‌سرویس UTCMS")
        val pass = field("رمز حساب سرویس UTCMS", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        val help = TextView(this).apply {
            text = "این مشخصات فقط یک‌بار برای اتصال امن برنامه به وب‌سرویس لازم است؛ در صفحه ورود راننده یا شرکت نمایش داده نمی‌شود."
            setPadding(0, 16, 0, 0)
        }
        box.addView(help)
        val dlg = AlertDialog.Builder(this).setTitle("تنظیم اتصال سرویس").setView(box)
            .setCancelable(false).setPositiveButton("ذخیره امن", null).create()
        dlg.setOnShowListener {
            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val code = company.text.toString().trim()
                val password = pass.text.toString().toCharArray()
                if (code.isBlank() || password.isEmpty()) {
                    help.text = "شناسه و رمز حساب سرویس الزامی است."
                    password.fill('\u0000')
                    return@setOnClickListener
                }
                try {
                    credentialStore.save(code, password)
                    dlg.dismiss()
                    showLoginDialog()
                } catch (e: Exception) {
                    help.text = "ذخیره امن انجام نشد: " + (e.message ?: "")
                } finally {
                    password.fill('\u0000')
                }
            }
        }
        dlg.show()
    }

    private fun showLoginDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(42, 12, 42, 0) }
        fun field(label: String, type: Int = InputType.TYPE_CLASS_TEXT) = EditText(this).apply {
            hint = label
            inputType = type
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            box.addView(this)
        }
        val national = field("کد ملی راننده", InputType.TYPE_CLASS_NUMBER)
        val mobile = field("شماره همراه ثبت‌شده در UTCMS", InputType.TYPE_CLASS_PHONE)
        val help = TextView(this).apply { text = "برای احراز هویت، کد ملی و شماره همراه ثبت‌شده را وارد کنید."; setPadding(0, 16, 0, 0) }
        box.addView(help)
        val dlg = AlertDialog.Builder(this).setTitle("ورود و احراز هویت").setView(box)
            .setCancelable(false).setNegativeButton("تنظیم اتصال") { _, _ -> showServiceSetupDialog() }
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
                    showServiceSetupDialog()
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
                        dlg.dismiss(); openDashboard()
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
        fun issueBol(json: String) {
            runOnUiThread {
                val position = currentGps()
                if (position == null) {
                    notifyPage(false, "موقعیت GPS تازه در دسترس نیست. GPS گوشی را روشن و مجوز مکان را فعال کنید.")
                    return@runOnUiThread
                }
                lifecycleScope.launch {
                    try {
                        val bol = JSONObject(json)
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

    private fun runGpsOperation(label: String, operation: suspend (JSONObject) -> UtcmsApi.ApiResult) {
        runOnUiThread {
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
