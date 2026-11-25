# وضعیت قرارداد UTCMS — نسخه کاری ۲.۳

این فایل از صفحات مستنداتی که کاربر در گفتگو ارائه کرده استخراج شده است. این متن، WSDL رسمی یا تأییدیه اتصال تولیدی نیست.

## سرویس‌ها و مدل‌های پاسخ شناخته‌شده

| عملیات | کاربرد | پاسخ مستند |
|---|---|---|
| WS39_RegUserGenerateOTP | درخواست OTP | شناسه IDNo یا ExceptionCnt |
| WS40_RegUserAuthenticateOTP | تأیید OTP | پاسخ کامل نیازمند نمونه/صفحه مربوط |
| WS21_GetRegisterUserInfo | اطلاعات کاربر | RegisterInfoCnt |
| WS22_GetSystemOptions | قواعد سامانه | SystemOptionCnt |
| WS31_GetBaseInfoGoodType | انواع کالا | KeyValueCnt[] |
| WS32_GetBaseInfoLoadingType | انواع بارگیر | KeyValueCnt[] |
| WS33_GetBaseInfoPackingType | انواع بسته‌بندی | KeyValueCnt[] |
| WS34_GetBaseInfoGood | فهرست کالا | KeyValueCnt[] |
| WS43_GetBaseInfoProvinceCity | استان‌ها و شهرها | ProvinceCityCnt[] |
| WS01_InsertBOL | ثبت سند حمل | کد پیگیری یا ExceptionCnt؛ جزئیات پاسخ موفق نیازمند نمونه واقعی |
| WS03_StartBOL | شروع حمل | ResponseBOLCnt |
| WS04_EndBOL | پایان حمل | اختلاف نام فیلدهای زمان در صفحات ۱۶ و ۲۶ |
| WS06_InsertBOLTrack | ثبت موقعیت مسیر | ResponseBOLCnt طبق متن ارسالی |

## اطلاعات پایه نوع بارگیر — صفحه ۲۲

### KeyValueCnt
- `Key`: کد، `int`
- `Value`: مقدار/عنوان، `string`

### WS32_GetBaseInfoLoadingType
امضا:
`WS32_GetBaseInfoLoadingType(string CompanyCode, int Salt, string HashedValue)`

- پارامترها: `CompanyCode` رشته، `Salt` عدد صحیح، `HashedValue` رشته.
- پاسخ موفق: آرایه `KeyValueCnt[]`.
- پاسخ خطا: `ExceptionCnt`.
- خطاها:
  - `-1`: خطای ناشناخته
  - `445031`: دسترسی کاربر به سامانه غیرفعال است
  - `445003`: ورود ناموفق
  - `445004`: فیلدهای ورودی باید پر شوند
- آدرس سرویس در OCR به‌صورت وارونه آمده و با صفحات قبلی ITMBOL هم‌خوانی دارد؛ تا تأیید WSDL به‌عنوان endpoint اجرایی استفاده نمی‌شود.

## تکمیل سرویس استان و شهر و ابتدای الگوریتم هش — صفحه ۲۵

### WS43_GetBaseInfoProvinceCity — خطاها
این صفحه ادامه توضیحات سرویس دریافت اطلاعات استان و شهر در صفحه ۲۴ است.

- پاسخ خطا: `ExceptionCnt` با فیلدهای `ErrCode` از نوع `Int` و `ErrDesc` از نوع `String`.
- خطاها:
  - `-1`: خطای ناشناخته
  - `445031`: دسترسی کاربر به سامانه غیرفعال است
  - `445003`: ورود ناموفق
  - `445004`: فیلدهای ورودی باید پر شوند
  - `445118`: دریافت و به‌روزرسانی اطلاعات شهر و استان برای هر کاربر، روزی یک‌بار مجاز است.
- آدرس سرویس در OCR به‌صورت وارونه آمده است؛ تا تطبیق با WSDL، endpoint اجرایی تأییدشده محسوب نمی‌شود.

### الگوریتم GetSHA512Hash — اطلاعات ناقص صفحه ۲۵
بخش قابل‌خواندن از نمونه کد نشان می‌دهد:
- `salt` با عبارت `new Random(DateTime.Now.Millisecond).Next()` مقداردهی می‌شود.
- رمز عبور با `Encoding.Unicode.GetBytes(password)` به بایت تبدیل می‌شود.
- روی بایت‌های رمز عبور SHA-512 اعمال می‌شود.
- خروجی هش رمز عبور با Base64 به رشته تبدیل می‌شود.
- سپس بایت‌های عبارت آغازین `CompanyCode + passwordHashValue + sa...` با `Encoding.Unicode` ساخته می‌شوند.

**محدودیت مهم:** متن کد در همین نقطه قطع شده است. ادامه ورودی SHA-512، نحوه استفاده از `salt`، محاسبه نهایی `hashedValue` و قالب دقیق خروجی معلوم نیست. بنابراین پیاده‌سازی هش نهایی یا ارسال درخواست واقعی بر اساس این صفحه به‌تنهایی مجاز و قابل‌اتکا نیست. همچنین `Random` معمولی مولد تصادفی رمزنگارانه نیست؛ این نمونه را بدون تأیید مستندات رسمی، الگوی امنیتی مناسب تلقی نمی‌کنیم.

## اطلاعات پایه بسته‌بندی و کالا — صفحه ۲۳

### WS33_GetBaseInfoPackingType
امضا:
`WS33_GetBaseInfoPackingType(string CompanyCode, int Salt, string HashedValue)`

- پارامترها: `CompanyCode` رشته، `Salt` عدد صحیح، `HashedValue` رشته.
- پاسخ موفق: آرایه `KeyValueCnt[]`.
- پاسخ خطا: `ExceptionCnt`.
- خطاها:
  - `-1`: خطای ناشناخته
  - `445031`: دسترسی کاربر به سامانه غیرفعال است
  - `445003`: ورود ناموفق
  - `445004`: فیلدهای ورودی باید پر شوند

### WS34_GetBaseInfoGood
امضا:
`WS34_GetBaseInfoGood(string CompanyCode, int Salt, string HashedValue)`

- پارامترها: `CompanyCode` رشته، `Salt` عدد صحیح، `HashedValue` رشته.
- طبق صفحات ۲۱ تا ۲۴، پاسخ موفق این سرویس آرایه `KeyValueCnt[]` و پاسخ خطا `ExceptionCnt` است.
- فهرست خطاها در متن ارسالی صفحه ۲۳ برای این متد تکرار نشده؛ بنابراین کدهای خطا را به این عملیات نسبت نمی‌دهیم.
- آدرس در OCR به‌صورت وارونه درج شده و تا تأیید WSDL به‌عنوان endpoint اجرایی استفاده نمی‌شود.

## جزئیات عملیات حمل — صفحات ۱۳ و ۱۷

### WS03_StartBOL
امضا:
`WS03_StartBOL(string CompanyCode, int Salt, string HashedValue, string BOLTraceCode, int StartTime, GPSCnt StartPosition)`

- `CompanyCode`: رشته
- `Salt`: عدد صحیح؛ طبق سند با `GetSHA512Hash` محاسبه می‌شود
- `HashedValue`: رشته؛ طبق سند با `GetSHA512Hash` محاسبه می‌شود
- `BOLTraceCode`: کد رهگیری سند
- `StartTime`: زمان شروع به‌صورت Unix DateTime و عدد صحیح
- `StartPosition`: شیء `GPSCnt`
- یادداشت سند به صفحه ۱۶ برای Context ارجاع می‌دهد.
- پاسخ موفق: `ResponseBOLCnt` طبق صفحات قبلی.

### خطاهای WS01_InsertBOL — صفحه ۱۳
- `-1`: خطای ناشناخته
- `445001`: کد شرکت یا کلمه عبور نادرست
- `445003`: ورود ناموفق
- `445004`: فیلدهای ورودی باید پر شوند
- `445005`: مقادیر فیلدهای ورودی نامعتبر
- `445006`: پلاک وجود ندارد
- `445007`: راننده وجود ندارد
- `445008`: راننده در فهرست رانندگان ناوگان با آن پلاک نیست
- `445009`: پروانه فعالیت ناوگان اعتبار ندارد
- `445010`: راننده مجاز نیست
- `445012`: سیستم هوشمند ناوگان غیرفعال است
- `445017`: نوع گواهینامه راننده برای ناوگان تأیید نیست
- `445018`: اطلاعات ناوگان توسط نیروی انتظامی تأیید نشده
- `445021`: سند قبلاً ثبت شده است
- `445024`: مالک ناوگان تغییر کرده است
- `445025`: اطلاعات GPS ناوگان ثبت نشده است
- `445026`: موقعیت باید ارسال شود
- `445046`: تعداد اسناد حقیقی مجاز بیش از حد است
- `445051`: کالا با کد واردشده وجود ندارد
- `445052`: مقدار بیمه باید وارد شود
- `445059`: نوع بار با کد واردشده وجود ندارد
- `445080`: موقعیت خارج از محدوده مجاز است

### WS06_InsertBOLTrack
امضا:
`WS06_InsertBOLTrack(string CompanyCode, int Salt, string HashedValue, string BOLTraceCode, int EventTime, GPSCnt EventPosition)`

- `CompanyCode`: رشته
- `Salt`: عدد صحیح
- `HashedValue`: رشته
- `BOLTraceCode`: کد رهگیری سند
- `EventTime`: زمان تردد به‌صورت Unix DateTime و عدد صحیح
- `EventPosition`: شیء `GPSCnt`
- خطاها: `-1` ناشناخته؛ `445001` شناسه کاربری یا رمز اشتباه؛ `445003` ورود ناموفق؛ `445030` زمان عبور بعد از زمان جاری؛ `445080` موقعیت خارج محدوده مجاز.
- صفحه ۱۷ می‌گوید پاسخ خطا `ExceptionCnt` است؛ شرح متنی موفقیت کد رهگیری را ذکر می‌کند، اما جدول همراه همان صفحه ساختار `ResponseBOLCnt` با `ResponseStatus`, `ResponseMessage`, `TraceCode`, `State` را نشان می‌دهد. این تعارض تا دریافت نمونه پاسخ یا WSDL حل‌شده تلقی نمی‌شود.
- آدرس در OCR به شکل وارونه/ناقص آمده و باید با WSDL یا صفحه واضح‌تر تأیید شود؛ endpoint از روی OCR بازسازی و فعال نشده است.

## ساختارهای شناخته‌شده

### KeyValueCnt
- Key: int
- Value: string

### ProvinceCityCnt
- PROVINCECODE: int
- PROVINCEDESC: string
- CITYCODE: string
- CITYDESC: string

### RegisterInfoCnt
- IDNo: int
- UserType: byte

### ResponseBOLCnt
- ResponseStatus: string
- ResponseMessage: string
- TraceCode: string
- State: string

### SystemOptionCnt
- Boolean: 01A, 02A, 03A, 04A, 05A, 06A
- Boolean: A17, A72, A73, A74, A75, A76, A77, A78, A79
- Boolean: A18, A82, A83, A84, A85, A86, A87, A88, A89
- Boolean: 01B, 02B
- Boolean: 31B تا 39B
- Boolean: 41B تا 49B
- String: C01, C02, C03
- Boolean: C04, C05, D01, D02

## ساختار BOLCnt شناخته‌شده

صفحات ۱۰ و ۱۱ نام و نوع ۵۰ فیلد را معرفی می‌کنند؛ شامل پلاک، راننده، مالک، فرستنده، گیرنده، کرایه، تاریخ صدور، نشانی بارگیری/تخلیه، فهرست کالاها، پرداخت، بیمه و مختصات مبدأ/مقصد.

### BOLGoodCnt — صفحه ۱۲
- GoodID: int (اجباری)
- WeightKg: float (اجباری)
- Value: float (اجباری)
- PackingTypeID: int (اجباری)
- GoodtypeID: int (اجباری)
- Description: string
- Image: Byte[]

### GPSCnt — صفحه ۱۲
- Latitude, Longitude, Altitude, Bearing: float
- NumberOfSatellite, PDOP, GPSSpeed, GPSMaxSpeed: byte
- GPSTotalTraveledDistance: int

در صفحه ۱۲ برای فیلدهای GPS علامت اجباری درج نشده است؛ بنابراین در مدل Kotlin به‌صورت nullable نگهداری می‌شوند. نوع باینری Image ثبت شده، اما شیوه انتقال آن در JSON/SOAP (مثلاً Base64 یا ساختار دیگر) هنوز تأیید نشده است.

با دریافت صفحه ۱۲، مدل‌های Kotlin مربوط به BOLGoodCnt و GPSCnt اضافه شده‌اند. مدل کامل BOLCnt و serializer درخواست هنوز باید پس از تأیید قالب سیمی، تاریخ‌ها و نام پارامترها تکمیل شوند. فیلدهای مبهم مانند SerialNo و زمان Unix نیز نیازمند تأییدند.

## موارد حل‌نشده و مسدودکننده اتصال تولیدی

1. صفحه ۲۵ دریافت نشده است.
2. الگوریتم رسمی GetSHA512Hash، نحوه تولید Salt و ترتیب دقیق بایت‌ها تأیید نشده است؛ پیاده‌سازی موجود در مخزن نباید به‌عنوان قرارداد رسمی تلقی شود.
3. WSDL یا نمونه رسمی request/response برای تعیین SOAP/JSON، نام دقیق فیلدها، namespace، نوع‌های عددی و رفتار پاسخ در دست نیست.
4. صفحه ۱۶ از EndTime و ReciptTime استفاده می‌کند، اما نمونه صفحه ۲۶ از EndDate و ReciptDate استفاده کرده است.
5. صفحه ۱۶ از زمان Unix integer یاد می‌کند، در حالی که نمونه صفحه ۲۶ رشته تاریخ و ساعت ارسال می‌کند.
6. آدرس نمونه صفحه ۲۶ از HTTP و host قدیمی استفاده می‌کند؛ صفحات دیگر HTTPS و مسیر متفاوتی معرفی می‌کنند. فقط endpoint رسمی و امن باید استفاده شود.
7. CompanyCode در امضای متد string است، اما نمونه آن را عددی فرستاده است.
8. نمونه صفحه ۲۶ پاسخ متنی برمی‌گرداند، اما کد مصرف‌کننده آن را مانند Dictionary بررسی می‌کند؛ تبدیل پاسخ در نمونه ناقص است.

## سیاست فعال‌سازی

- درخواست‌های شبکه تولیدی تا زمان تأیید قرارداد و دسترسی مجاز باید غیرفعال بمانند.
- تست‌ها با داده ساختگی و پاسخ mock انجام شوند.
- هیچ رمز، کد یک‌بارمصرف، شناسه ملی، شماره تلفن یا موقعیت واقعی در مخزن یا گزارش CI ثبت نشود.
- GPS فقط با مجوز روشن کاربر و در زمان نیاز دریافت شود؛ ردیابی پیوسته بدون اطلاع و رضایت صریح پیاده‌سازی نشود.
- ثبت واقعی باربرگ تنها پس از تأیید مجوز رسمی، endpoint، قرارداد و آزمون محیط مجاز قابل فعال‌سازی است.
