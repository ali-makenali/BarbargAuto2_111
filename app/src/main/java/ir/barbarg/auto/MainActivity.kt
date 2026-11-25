package ir.barbarg.auto

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
    private lateinit var fleetList: LinearLayout
    private lateinit var fleetCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this).apply {
            fillViewport = true
            setBackgroundColor(Color.rgb(246, 248, 247))
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            textDirection = View.TEXT_DIRECTION_RTL
            setPadding(dp(18), dp(24), dp(18), dp(32))
        }
        scroll.addView(content)
        setContentView(scroll)

        content.addView(label("داشبورد", 27f, Color.rgb(25, 75, 64)))
        content.addView(label("دستیار باربرگ شهری", 15f, Color.DKGRAY))

        val statusCard = card()
        statusCard.addView(label("وضعیت سامانه", 18f, Color.rgb(25, 75, 64)))
        statusCard.addView(label(
            "اتصال به سامانه مرکزی هنوز فعال نشده است. برای فعال‌سازی، قرارداد رسمی وب‌سرویس و دسترسی مجاز باید بررسی و تأیید شود.",
            14f,
            Color.DKGRAY
        ))
        content.addView(statusCard, verticalParams())

        val stats = card()
        stats.addView(label("ناوگان‌های ثبت‌شده در این دستگاه", 14f, Color.DKGRAY))
        fleetCount = label("۰", 28f, Color.rgb(25, 75, 64))
        stats.addView(fleetCount)
        content.addView(stats, verticalParams())

        content.addView(label("مدیریت ناوگان", 20f, Color.rgb(25, 75, 64)))

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL
        }
        val input = EditText(this).apply {
            hint = getString(ir.barbarg.auto.R.string.hint_fleet)
            setSingleLine(true)
            filters = arrayOf(android.text.InputFilter.LengthFilter(60))
        }
        val add = Button(this).apply {
            text = getString(ir.barbarg.auto.R.string.btn_add)
            setOnClickListener {
                if (viewModel.addFleet(input.text.toString())) {
                    input.text.clear()
                } else {
                    Toast.makeText(this@MainActivity, "نام ناوگان خالی، تکراری یا بیش از ۶۰ نویسه است.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        row.addView(input, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(add)
        content.addView(row)

        fleetList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(fleetList)

        content.addView(label(
            "توجه: در این نسخه، مدیریت ناوگان به‌صورت محلی انجام می‌شود؛ ثبت باربرگ و ارسال درخواست به سامانه هنوز فعال نیست.",
            12f,
            Color.GRAY
        ))

        viewModel.fleets.observe(this) { renderFleets(it) }
    }

    private fun renderFleets(fleets: List<String>) {
        fleetCount.text = fleets.size.toString()
        fleetList.removeAllViews()
        if (fleets.isEmpty()) {
            val empty = card()
            empty.addView(label(getString(ir.barbarg.auto.R.string.empty_fleets), 14f, Color.GRAY))
            fleetList.addView(empty, verticalParams())
            return
        }
        fleets.forEachIndexed { index, name ->
            val item = card().apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            item.addView(
                label("${index + 1}. $name", 15f, Color.DKGRAY),
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            )
            item.addView(Button(this).apply {
                text = "حذف"
                setOnClickListener { viewModel.removeFleet(name) }
            })
            fleetList.addView(item, verticalParams())
        }
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(dp(16), dp(12), dp(16), dp(12))
        background = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = dp(14).toFloat()
            setStroke(dp(1), Color.rgb(229, 235, 232))
        }
    }

    private fun verticalParams() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = dp(12) }

    private fun label(value: String, size: Float = 16f, color: Int = Color.DKGRAY) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        gravity = Gravity.CENTER_VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        textDirection = View.TEXT_DIRECTION_RTL
        setPadding(0, dp(5), 0, dp(5))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
