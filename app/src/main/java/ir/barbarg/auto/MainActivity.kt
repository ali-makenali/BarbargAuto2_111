package ir.barbarg.auto
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ir.barbarg.auto.net.UtcmsApi
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
 private val api = UtcmsApi()
 private var companyCode = ""
 private var servicePassword = charArrayOf()
 private var pendingIdNo = 0
 private var dashboard: WebView? = null

 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  showLoginDialog()
  onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
   override fun handleOnBackPressed() {
    val web = dashboard
    if (web != null && web.canGoBack()) web.goBack() else showLoginDialog()
   }
  })
 }

 private fun showLoginDialog() {
  val box = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(42,12,42,0) }
  fun field(label:String, type:Int=InputType.TYPE_CLASS_TEXT) = EditText(this).apply {
   hint=label; inputType=type
   layoutParams=LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)
   box.addView(this)
  }
  val company=field("کد شرکت / CompanyCode")
  val pass=field("رمز عبور سرویس",InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
  val national=field("کد ملی / شناسه ملی",InputType.TYPE_CLASS_NUMBER)
  val mobile=field("شماره موبایل ثبت‌شده در UTCMS",InputType.TYPE_CLASS_PHONE)
  val help=TextView(this).apply { text="کد شرکت، رمز سرویس، کد ملی و موبایل ثبت‌شده الزامی است."; setPadding(0,16,0,0) }
  box.addView(help)
  val dlg=AlertDialog.Builder(this).setTitle("ورود و احراز هویت UTCMS").setView(box)
   .setCancelable(false).setNegativeButton("خروج"){_,_->finish()}
   .setPositiveButton("دریافت کد تأیید",null).create()
  dlg.setOnShowListener {
   dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
    val c=company.text.toString().trim(); val p=pass.text.toString()
    val n=national.text.toString().trim(); val m=mobile.text.toString().trim()
    if(c.isBlank()||p.isBlank()||n.isBlank()||m.isBlank()){help.text="همه فیلدها الزامی هستند.";return@setOnClickListener}
    dlg.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled=false
    help.text="در حال اتصال به UTCMS و درخواست کد یک‌بارمصرف…"
    lifecycleScope.launch {
     val secret=p.toCharArray()
     val result=api.generateOtp(UtcmsApi.Account(c,secret.copyOf()),n,m)
     val id=result.payload?.optInt("IDNo",0) ?: result.value?.trim()?.toIntOrNull() ?: 0
     if(result.success && id>0) {
      companyCode=c; servicePassword.fill('\u0000'); servicePassword=secret.copyOf()
      pendingIdNo=id; secret.fill('\u0000'); dlg.dismiss(); showOtpDialog()
     } else {
      secret.fill('\u0000'); dlg.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled=true
      help.text=result.errorMessage ?: ("شناسه IDNo از پاسخ سرویس استخراج نشد: "+(result.value ?: result.payload))
     }
    }
   }
  }
  dlg.show()
 }

 private fun showOtpDialog() {
  val input=EditText(this).apply { hint="کد یک‌بارمصرف ۴ رقمی"; inputType=InputType.TYPE_CLASS_NUMBER }
  val help=TextView(this).apply { text="کد ارسال‌شده به شماره ثبت‌شده در UTCMS را وارد کنید." }
  val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(42,8,42,0); addView(input); addView(help) }
  val dlg=AlertDialog.Builder(this).setTitle("تأیید کد یک‌بارمصرف").setView(box).setCancelable(false)
   .setNegativeButton("بازگشت"){_,_->showLoginDialog()}.setPositiveButton("تأیید و ورود",null).create()
  dlg.setOnShowListener {
   dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
    val otp=input.text.toString().trim()
    if(otp.length!=4){help.text="کد باید دقیقاً ۴ رقم باشد.";return@setOnClickListener}
    dlg.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled=false
    help.text="در حال تأیید هویت در UTCMS…"
    lifecycleScope.launch {
     val result=api.authenticateOtp(UtcmsApi.Account(companyCode,servicePassword.copyOf()),pendingIdNo,otp)
     if(result.success){dlg.dismiss();openDashboard()}
     else {dlg.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled=true;help.text=result.errorMessage ?: "احراز هویت ناموفق بود"}
    }
   }
  }
  dlg.show()
 }

 private fun openDashboard() {
  val web=WebView(this).apply {
   settings.javaScriptEnabled=true; settings.domStorageEnabled=true; settings.allowFileAccess=true
   settings.allowContentAccess=false; settings.allowFileAccessFromFileURLs=false
   settings.allowUniversalAccessFromFileURLs=false
   webViewClient=object:WebViewClient(){
    override fun shouldOverrideUrlLoading(view:WebView,url:String)=!url.startsWith("file:///android_asset/")
   }
  }
  dashboard=web; setContentView(web); web.loadUrl("file:///android_asset/index.html")
 }

 override fun onDestroy() {
  servicePassword.fill('\u0000'); dashboard?.stopLoading(); dashboard?.destroy(); super.onDestroy()
 }
}
