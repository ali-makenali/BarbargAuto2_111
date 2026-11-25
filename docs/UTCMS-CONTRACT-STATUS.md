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
| WS01_InsertBOL | ثبت سند حمل | پاسخ کامل نیازمند نمونه واقعی |
| WS03_StartBOL | شروع حمل | ResponseBOLCnt |
| WS04_EndBOL | پایان حمل | اختلاف نام فیلدهای زمان در صفحات ۱۶ و ۲۶ |
| WS06_InsertBOLTrack | ثبت موقعیت مسیر | ResponseBOLCnt طبق متن ارسالی |

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

قرارداد ارسال کامل هنوز قابل پیاده‌سازی نیست؛ زیرا تعریف BOLGoodCnt و GPSCnt در صفحات دریافت‌شده وجود ندارد. فیلدهای مبهم مانند SerialNo و زمان Unix نیز نیازمند تأییدند.

## موارد حل‌نشده و مسدودکننده اتصال تولیدی

1. صفحه ۲۵ دریافت نشده است.
2. تعریف کامل GPSCnt در دست نیست.
3. تعریف کامل BOLGoodCnt در دست نیست.
4. الگوریتم رسمی GetSHA512Hash، نحوه تولید Salt و ترتیب دقیق بایت‌ها تأیید نشده است؛ پیاده‌سازی موجود در مخزن نباید به‌عنوان قرارداد رسمی تلقی شود.
5. WSDL یا نمونه رسمی request/response برای تعیین SOAP/JSON، نام دقیق فیلدها، namespace، نوع‌های عددی و رفتار پاسخ در دست نیست.
6. صفحه ۱۶ از EndTime و ReciptTime استفاده می‌کند، اما نمونه صفحه ۲۶ از EndDate و ReciptDate استفاده کرده است.
7. صفحه ۱۶ از زمان Unix integer یاد می‌کند، در حالی که نمونه صفحه ۲۶ رشته تاریخ و ساعت ارسال می‌کند.
8. آدرس نمونه صفحه ۲۶ از HTTP و host قدیمی استفاده می‌کند؛ صفحات دیگر HTTPS و مسیر متفاوتی معرفی می‌کنند. فقط endpoint رسمی و امن باید استفاده شود.
9. CompanyCode در امضای متد string است، اما نمونه آن را عددی فرستاده است.
10. نمونه صفحه ۲۶ پاسخ متنی برمی‌گرداند، اما کد مصرف‌کننده آن را مانند Dictionary بررسی می‌کند؛ تبدیل پاسخ در نمونه ناقص است.

## سیاست فعال‌سازی

- درخواست‌های شبکه تولیدی تا زمان تأیید قرارداد و دسترسی مجاز باید غیرفعال بمانند.
- تست‌ها با داده ساختگی و پاسخ mock انجام شوند.
- هیچ رمز، کد یک‌بارمصرف، شناسه ملی، شماره تلفن یا موقعیت واقعی در مخزن یا گزارش CI ثبت نشود.
- GPS فقط با مجوز روشن کاربر و در زمان نیاز دریافت شود؛ ردیابی پیوسته بدون اطلاع و رضایت صریح پیاده‌سازی نشود.
- ثبت واقعی باربرگ تنها پس از تأیید مجوز رسمی، endpoint، قرارداد و آزمون محیط مجاز قابل فعال‌سازی است.
