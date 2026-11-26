package ir.barbarg.auto

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import ir.barbarg.auto.data.FleetViewModel

class MainActivity : AppCompatActivity() {
    private val viewModel: FleetViewModel by viewModels()
    private lateinit var root: LinearLayout
    private lateinit var fleetList: LinearLayout
    private lateinit var fleetCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            textDirection = View.TEXT_DIRECTION_RTL
            setPadding(dp(20), dp(24), dp(20), dp(24))
        }
        scroll.addView(root)
        setContentView(scroll)
        showDashboard()
        viewModel.fleets.observe(this) {
            fleetCount.text = "تعداد ناوگان‌های ثبت‌شده: ${it.size}"
            if (root.tag == "fleet") renderFleetList(it)
        }
    }

    private fun showDashboard() {
        root.tag = "dashboard"
        root.removeAllViews()
        root.addView(label("دستیار باربرگ شهری", 26f))
        root.addView(label("داشبورد مدیریت ناوگان و خدمات باربرگ"))
        fleetCount = label("تعداد ناوگان‌های ثبت‌شده: ${viewModel.fleets.value.orEmpty().size}", 18f)
        root.addView(card("مدیریت ناوگان", "افزودن، مشاهده و حذف ناوگان‌های ذخیره‌شده"))
        root.addView(fleetCount)
        root.addView(Button(this).apply {
            text = "ورود به مدیریت ناوگان"
            setOnClickListener { showFleetManagement() }
        })
        root.addView(card("ثبت باربرگ", "اتصال عملیاتی تا زمان تطبیق و آزمون سرویس رسمی فعال نمی‌شود."))
        root.addView(Button(this).apply {
            text = "ثبت باربرگ (در انتظار اتصال سرویس)"
            isEnabled = false
        })
        root.addView(card("وضعیت سرویس", "قرارداد WSDL دریافت شده است؛ پیاده‌سازی و آزمون متد ثبت باربرگ هنوز انجام نشده است."))
        root.addView(label("نسخه ۲.۲.۰ — داشبورد و مدیریت ناوگان", 13f))
    }

    private fun showFleetManagement() {
        root.tag = "fleet"
        root.removeAllViews()
        root.addView(label("مدیریت ناوگان", 24f))
        root.addView(Button(this).apply {
            text = "بازگشت به داشبورد"
            setOnClickListener { showDashboard() }
        })
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val input = EditText(this).apply {
            hint = "نام ناوگان"
            setSingleLine(true)
            filters = arrayOf(android.text.InputFilter.LengthFilter(60))
        }
        row.addView(input, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(Button(this).apply {
            text = "افزودن"
            setOnClickListener {
                if (viewModel.addFleet(input.text.toString())) input.text.clear()
                else Toast.makeText(this@MainActivity, "نام ناوگان خالی، تکراری یا بیش از ۶۰ نویسه است.", Toast.LENGTH_SHORT).show()
            }
        })
        root.addView(row)
        fleetList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(fleetList)
        renderFleetList(viewModel.fleets.value.orEmpty())
    }

    private fun renderFleetList(fleets: List<String>) {
        if (!::fleetList.isInitialized || root.tag != "fleet") return
        fleetList.removeAllViews()
        if (fleets.isEmpty()) {
            fleetList.addView(label("هنوز ناوگانی اضافه نشده است."))
            return
        }
        fleets.forEachIndexed { index, name ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
            }
            item.addView(label("${index + 1}. $name"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            item.addView(Button(this).apply {
                text = "حذف"
                setOnClickListener { viewModel.removeFleet(name) }
            })
            fleetList.addView(item)
        }
    }

    private fun card(title: String, description: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(8), dp(14), dp(8))
        addView(label(title, 19f))
        addView(label(description, 14f))
    }

    private fun label(value: String, size: Float = 16f) = TextView(this).apply {
        text = value
        textSize = size
        gravity = Gravity.CENTER_VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        textDirection = View.TEXT_DIRECTION_RTL
        setPadding(0, dp(10), 0, dp(8))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
