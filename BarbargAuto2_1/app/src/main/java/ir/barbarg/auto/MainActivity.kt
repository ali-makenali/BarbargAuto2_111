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
    private lateinit var fleetList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            textDirection = View.TEXT_DIRECTION_RTL
            setPadding(dp(20), dp(24), dp(20), dp(24))
        }
        scroll.addView(content)
        setContentView(scroll)

        content.addView(label(getString(ir.barbarg.auto.R.string.title_main), 25f))
        content.addView(label(getString(ir.barbarg.auto.R.string.subtitle_main)))

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
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

        val status = label(getString(ir.barbarg.auto.R.string.status_line))
        status.setPadding(0, dp(20), 0, dp(8))
        content.addView(status)

        viewModel.fleets.observe(this) { renderFleets(it) }
    }

    private fun renderFleets(fleets: List<String>) {
        fleetList.removeAllViews()
        if (fleets.isEmpty()) {
            fleetList.addView(label(getString(ir.barbarg.auto.R.string.empty_fleets)))
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
