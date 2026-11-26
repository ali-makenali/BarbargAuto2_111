package ir.barbarg.auto

import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

/**
 * Opens the offline-first dashboard bundled with the APK.
 * Dashboard data is kept in WebView DOM storage on this device.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var dashboard: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dashboard = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = false
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val target = request.url
                    // Only pages bundled inside this APK may be opened.
                    return !(target.scheme == "file" &&
                        target.path?.startsWith("/android_asset/") == true)
                }
            }

            setBackgroundColor(android.graphics.Color.WHITE)
        }

        setContentView(dashboard)
        dashboard.loadUrl("file:///android_asset/index.html")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (dashboard.canGoBack()) {
                    dashboard.goBack()
                } else {
                    finish()
                }
            }
        })
    }

    override fun onDestroy() {
        dashboard.stopLoading()
        dashboard.destroy()
        super.onDestroy()
    }
}
