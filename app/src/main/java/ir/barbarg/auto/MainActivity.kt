package ir.barbarg.auto

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import ir.barbarg.auto.data.FleetViewModel
import ir.barbarg.auto.data.FreightDraftStore

class MainActivity : AppCompatActivity() {
    private val viewModel: FleetViewModel by viewModels()
    private lateinit var fleetList: LinearLayout
    private lateinit var fleetCount: TextView
    private lateinit var fleetSpinner: Spinner
    private lateinit var goodsContainer: LinearLayout
    private val goodsRows = mutableListOf<GoodsRow>()

    private data class GoodsRow(val name: EditText, val type: EditText, val packing: EditText, val quantity: EditText, val weight: EditText)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this).apply { fillViewport = true; setBackgroundColor(Color.rgb(246, 248, 247)) }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            textDirection = View.TEXT_DIRECTION_RTL
            setPadding(dp(18), dp(24), dp(18), dp(32))
        }
        scroll.addView(content)
        setContentView(scroll)

        content.addView(label("داشبورد", 27f, GREEN))
        content.addView(label("دستیار باربرگ شهری — نسخه ۲.۳", 15f))
        val status = card()
        status.addView(label("وضعیت سامانه مرکزی", 18f, GREEN))
        status.addView(label("اتصال و ثبت واقعی باربرگ هنوز غیرفعال است. فرم زیر برای تکمیل و ذخیره پیش‌نویس محلی آماده شده است.", 14f))
        content.addView(status, verticalParams())

        val stats = card()
        stats.addView(label("ناوگان‌های ثبت‌شده در این دستگاه", 14f))
        fleetCount = label("۰", 28f, GREEN)
        stats.addView(fleetCount)
        content.addView(stats, verticalParams())

        content.addView(label("مدیریت ناوگان", 20f, GREEN))
        val fleetRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        val fleetInput = edit("نام ناوگان")
        fleetRow.addView(fleetInput, LinearLayout.LayoutParams(0, -2, 1f))
        fleetRow.addView(Button(this).apply {
            text = "افزودن"
            setOnClickListener {
                if (viewModel.addFleet(fleetInput.text.toString())) fleetInput.text.clear()
                else toast("نام ناوگان خالی، تکراری یا بیش از ۶۰ نویسه است.")
            }
        })
        content.addView(fleetRow)
        fleetList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(fleetList)

        content.addView(label("فرم پیش‌نویس باربرگ", 23f, GREEN))
        content.addView(label("اطلاعات در این بخش فقط روی همین دستگاه به‌صورت پیش‌نویس ذخیره می‌شود و به سامانه مرکزی ارسال نمی‌شود.", 13f, Color.GRAY))
        val draft = FreightDraftStore(this)
        val restored = draft.load()
        val inputs = linkedMapOf<String, EditText>()

        section(content, "۱. خودرو و راننده")
        fleetSpinner = Spinner(this)
        content.addView(label("ناوگان", 14f))
        content.addView(fleetSpinner, verticalParams())
        inputs["plate"] = addField(content, "شماره پلاک")
        inputs["plateSerial"] = addField(content, "سریال پلاک", true)
        inputs["plateType"] = addField(content, "نوع پلاک (IRI / KSH / QSM ...)")
        inputs["driverCode"] = addField(content, "کد ملی راننده", true)
        inputs["secondDriverCode"] = addField(content, "کد ملی راننده دوم (اختیاری)", true)
        inputs["ownerNationalId"] = addField(content, "شناسه ملی مالک", true)

        section(content, "۲. فرستنده")
        addTypeSpinner(content, "نوع فرستنده", listOf("حقیقی", "حقوقی"))
        inputs["senderName"] = addField(content, "نام فرستنده")
        inputs["senderLastName"] = addField(content, "نام خانوادگی فرستنده")
        inputs["senderNationalId"] = addField(content, "کد / شناسه ملی فرستنده", true)
        inputs["senderMobile"] = addField(content, "موبایل فرستنده", true)
        inputs["senderPhone"] = addField(content, "تلفن فرستنده", true)
        inputs["senderPostal"] = addField(content, "کد پستی فرستنده", true)
        inputs["senderCityCode"] = addField(content, "کد شهر فرستنده")
        inputs["senderAddress"] = addField(content, "نشانی فرستنده")

        section(content, "۳. گیرنده")
        addTypeSpinner(content, "نوع گیرنده", listOf("حقیقی", "حقوقی"))
        inputs["receiverName"] = addField(content, "نام گیرنده")
        inputs["receiverLastName"] = addField(content, "نام خانوادگی گیرنده")
        inputs["receiverNationalId"] = addField(content, "کد / شناسه ملی گیرنده", true)
        inputs["receiverMobile"] = addField(content, "موبایل گیرنده", true)
        inputs["receiverPhone"] = addField(content, "تلفن گیرنده", true)
        inputs["receiverPostal"] = addField(content, "کد پستی گیرنده", true)
        inputs["receiverCityCode"] = addField(content, "کد شهر گیرنده")
        inputs["receiverAddress"] = addField(content, "نشانی گیرنده")

        section(content, "۴. بارگیری و تخلیه")
        inputs["loadingPostal"] = addField(content, "کد پستی محل بارگیری", true)
        inputs["loadingCityCode"] = addField(content, "کد شهر محل بارگیری")
        inputs["loadingAddress"] = addField(content, "نشانی محل بارگیری")
        inputs["unloadingPostal"] = addField(content, "کد پستی محل تخلیه", true)
        inputs["unloadingCityCode"] = addField(content, "کد شهر محل تخلیه")
        inputs["unloadingAddress"] = addField(content, "نشانی محل تخلیه")

        section(content, "۵. کالاها")
        content.addView(label("اطلاعات کالا فعلاً محلی ثبت می‌شود؛ نگاشت نهایی به BOLGoodCnt بعد از تأیید قرارداد رسمی انجام خواهد شد.", 13f, Color.GRAY))
        goodsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(goodsContainer)
        content.addView(Button(this).apply { text = "افزودن ردیف کالا"; setOnClickListener { addGoodsRow() } }, verticalParams())
        addGoodsRow()

        section(content, "۶. مبالغ و توضیحات")
        inputs["freightage"] = addField(content, "کرایه (ریال)", true)
        inputs["preFreightage"] = addField(content, "پیش‌کرایه (ریال)", true)
        inputs["freightageTax"] = addField(content, "مالیات کرایه (ریال)", true)
        inputs["companyCommission"] = addField(content, "کمیسیون شرکت (ریال)", true)
        inputs["serviceCost"] = addField(content, "هزینه خدمات (ریال)", true)
        inputs["totalPayment"] = addField(content, "مبلغ کل پرداختی (ریال)", true)
        inputs["insurance"] = addField(content, "هزینه بیمه (ریال)", true)
        inputs["serialNo"] = addField(content, "شماره سریال سند (در صورت مشخص بودن)")
        inputs["description"] = addField(content, "توضیحات")

        section(content, "۷. موقعیت مکانی (اختیاری)")
        content.addView(label("مختصات را فقط در صورت داشتن منبع معتبر وارد کن. این نسخه GPS را خودکار ردیابی یا ارسال نمی‌کند.", 13f, Color.GRAY))
        inputs["originLat"] = addField(content, "عرض جغرافیایی مبدأ", true)
        inputs["originLon"] = addField(content, "طول جغرافیایی مبدأ", true)
        inputs["destinationLat"] = addField(content, "عرض جغرافیایی مقصد", true)
        inputs["destinationLon"] = addField(content, "طول جغرافیایی مقصد", true)

        restored.forEach { (key, value) -> inputs[key]?.setText(value) }
        content.addView(Button(this).apply {
            text = "ذخیره پیش‌نویس روی گوشی"
            setOnClickListener {
                val required = listOf("plate", "plateSerial", "plateType", "driverCode", "ownerNationalId", "loadingAddress", "unloadingAddress")
                if (required.any { inputs[it]?.text.isNullOrBlank() }) {
                    toast("فیلدهای ضروری خودرو، راننده، مالک و نشانی بارگیری/تخلیه را تکمیل کن.")
                    return@setOnClickListener
                }
                val safe = inputs.filterKeys { it !in SENSITIVE_KEYS }.mapValues { it.value.text.toString().trim() }.toMutableMap()
                safe["fleet"] = fleetSpinner.selectedItem?.toString().orEmpty()
                safe["goodsCount"] = goodsRows.size.toString()
                draft.save(safe)
                toast("پیش‌نویس محلی ذخیره شد. اطلاعات هویتی و تماس ذخیره نشدند.")
            }
        }, verticalParams())
        content.addView(Button(this).apply {
            text = "پاک کردن پیش‌نویس ذخیره‌شده"
            setOnClickListener { draft.clear(); toast("پیش‌نویس ذخیره‌شده پاک شد.") }
        }, verticalParams())
        content.addView(label("برای حفظ حریم خصوصی، کدهای ملی، شماره‌های تماس و اطلاعات هویتی در پیش‌نویس ذخیره نمی‌شوند.", 12f, Color.GRAY))

        viewModel.fleets.observe(this) { renderFleets(it) }
    }

    private fun addGoodsRow() {
        val box = card()
        val name = edit("نام کالا")
        val type = edit("کد نوع کالا")
        val packing = edit("کد نوع بسته‌بندی")
        val quantity = edit("تعداد بسته", true)
        val weight = edit("وزن / مقدار (واحد را در توضیحات بنویس)", true)
        listOf(name, type, packing, quantity, weight).forEach(box::addView)
        box.addView(Button(this).apply {
            text = "حذف این ردیف"
            setOnClickListener { goodsRows.removeAll { it.name === name }; goodsContainer.removeView(box) }
        })
        goodsRows.add(GoodsRow(name, type, packing, quantity, weight))
        goodsContainer.addView(box, verticalParams())
    }

    private fun addTypeSpinner(parent: LinearLayout, title: String, values: List<String>) {
        parent.addView(label(title, 14f))
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, values)
        parent.addView(spinner, verticalParams())
    }

    private fun addField(parent: LinearLayout, hint: String, numeric: Boolean = false): EditText {
        val field = edit(hint, numeric)
        parent.addView(field, verticalParams())
        return field
    }

    private fun edit(hint: String, numeric: Boolean = false) = EditText(this).apply {
        this.hint = hint
        minLines = 1
        maxLines = if (hint.contains("نشانی") || hint == "توضیحات") 3 else 1
        textDirection = View.TEXT_DIRECTION_RTL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        if (numeric) inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        filters = arrayOf(InputFilter.LengthFilter(180))
        setPadding(dp(12), dp(8), dp(12), dp(8))
    }

    private fun section(parent: LinearLayout, title: String) { parent.addView(label(title, 18f, GREEN)) }

    private fun renderFleets(fleets: List<String>) {
        fleetCount.text = fleets.size.toString()
        fleetList.removeAllViews()
        if (fleets.isEmpty()) fleetList.addView(label("هنوز ناوگانی اضافه نشده است.", 14f, Color.GRAY))
        else fleets.forEachIndexed { index, name ->
            val item = card().apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
            item.addView(label((index + 1).toString() + ". " + name), LinearLayout.LayoutParams(0, -2, 1f))
            item.addView(Button(this).apply { text = "حذف"; setOnClickListener { viewModel.removeFleet(name) } })
            fleetList.addView(item, verticalParams())
        }
        val values = if (fleets.isEmpty()) listOf("بدون ناوگان") else fleets
        fleetSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, values)
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(dp(14), dp(10), dp(14), dp(10))
        background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(13).toFloat(); setStroke(dp(1), Color.rgb(229, 235, 232)) }
    }

    private fun verticalParams() = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) }
    private fun label(value: String, size: Float = 15f, color: Int = Color.DKGRAY) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        gravity = android.view.Gravity.CENTER_VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL; textDirection = View.TEXT_DIRECTION_RTL
        setPadding(0, dp(5), 0, dp(5))
    }
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private companion object {
        val GREEN = Color.rgb(25, 75, 64)
        val SENSITIVE_KEYS = setOf("driverCode", "secondDriverCode", "ownerNationalId", "senderNationalId", "senderMobile", "senderPhone", "receiverNationalId", "receiverMobile", "receiverPhone")
    }
}
