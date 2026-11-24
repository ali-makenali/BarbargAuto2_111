package ir.barbarg.auto
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.Gravity
import android.widget.*
class MainActivity:Activity(){
 private val fleets=mutableListOf<String>(); private lateinit var list:LinearLayout
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
  val s=ScrollView(this); val p=LinearLayout(this).apply{orientation=1;setPadding(28,28,28,28);layoutDirection=View.LAYOUT_DIRECTION_RTL;textDirection=View.TEXT_DIRECTION_RTL};s.addView(p);setContentView(s)
  p.addView(label("باربرگ اتو ۲.۱",26f));p.addView(label("مدیریت چند ناوگان و زیرساخت اتصال وب‌سرویس"))
  val r=LinearLayout(this).apply{orientation=0};val e=EditText(this).apply{hint="نام ناوگان";setSingleLine(true)}
  r.addView(e,LinearLayout.LayoutParams(0,-2,1f));r.addView(Button(this).apply{text="افزودن";setOnClickListener{val n=e.text.toString().trim();if(n.isNotEmpty()&&!fleets.contains(n)){fleets.add(n);e.text.clear();refresh()}else Toast.makeText(this@MainActivity,"نام خالی یا تکراری است",0).show()}})
  p.addView(r);list=LinearLayout(this).apply{orientation=1};p.addView(list)
  p.addView(label("وضعیت: لایه هش و متدهای OTP طبق راهنما آماده شده‌اند؛ اجرای زنده تا تأیید پیش‌نیاز امنیتی و آزمون دسترسی متوقف است."))
  refresh()
 }
 private fun refresh(){list.removeAllViews();if(fleets.isEmpty())list.addView(label("هنوز ناوگانی اضافه نشده"));fleets.forEachIndexed{i,n->list.addView(label("${i+1}. $n"))}}
 private fun label(t:String,z:Float=16f)=TextView(this).apply{text=t;textSize=z;gravity=Gravity.CENTER_VERTICAL;setPadding(0,12,0,8)}
}
