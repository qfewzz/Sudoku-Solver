# Sudoku-Solver

<img src="https://github.com/qfewzz/Sudoku-Solver/assets/57349843/6f252138-4bdc-4d4a-bbb8-0ea48d5bcc35" width=40% height=40%>

این پروژه با زبان kotlin نوشته شده است.

در main activity یک دکمه با نامgenerate  قرار داده شده که با هر بار کلیک کردن روی آن، یک sudoku قابل حل، بصورت تصادفی generate می شود. در بالای این دکمه یک EditText قرار دادرد که تعداد خانه های خالی موجود در sudoku ایجاد شده را مشخص می کند که درواقع نشان دهنده میزان سختی بازی است. مقدار پیشفرض آن در صورت خالی بودن 30 است.

در زیر این دکمه، read from file قرار دارد که با کلیک کردن روی آن کاربر می تواند یک فایل متنی حاوی یک sudoku را load کند. این فایل متنی کافی است دارای 81 تا عدد باشد، اینکه این اعداد چگونه از هم جدا شده اند یا حتی نشده اند، مهم نیست، زیرا طوری برنامه را نوشته ام که قادر است در هر حالت این فایل متنی را parse کند.

بعد از انتخاب فایل توسط کاربر، باتوجه به اینکه onActivityResult Deprecate شده است، از روش جدید بجای آن استفاده می کنیم.
  
```kotlin
private val resultLauncher =
  registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      val uri = result?.data?.data
      if (result.resultCode == Activity.RESULT_OK && uri != null) {
          val regex = Regex("""\d""")
          val text = contentResolver.openInputStream(uri)!!.reader().readText()
          regex.findAll(text).forEachIndexed { index, matchResult ->
              gridNumbers[index / dimensions][index % dimensions] = matchResult.value.toInt()
          }
          GlobalScope.launch(Dispatchers.IO) {
              solvingJob?.cancelAndJoin()
              showSudoku()
          }
      }
  }
```


با کلیک کردن روی دکمه solve، برنامه تلاش می کند که sudoku را حل نماید. اگر قابل حل نبود، پیغام مناسب بصورت Toast به کاربر نمایش داده می شود. درصورت قابل حل بودن، هنگامی که الگوریتم حل sudoku در حال اجرا است، قدم های صحیح برای حل بازی در این یک ArrayList<Action> ذخیره می شود و در نهایت برنامه بصورت step by step، تک تک خانه های خالی موجود را highlight کرده و عدد صحیح را در آن می نویسد. به ازای هر خانه، 500 میلی ثانیه برای انیمیشن تغییر رنگ آن خانه و یک ثانیه زمان انتظار در نظر گرفته شده است.
  
برای حل sudoku، از یک الگوریتم جستجور کامل استفاده شده که تمام حالت های ممکن را بصورت بازگشتی بررسی می کند. من در ابتدا سعی کردم خودم با استفاده از 3 حلقه for، خودم این الگوریتم را پیاده سازی کنم، اما در هنگام تست، نمیتوانست بعضی از sudoku ها را حل کند. درحالی که الگوریتم بازگشتی که از اینترنت پیدا کردم، می توانست. بعد از کمی بررسی متوجه شدم مشکل از کجاست. روش کار من اینگونه بود که هر خانه را بررسی می کند، اگر نمیتوانست هیچ عددی در آن قرار بگیرد(بطوری که sudoku  Valid باشد)، sudoku غیر قابل حل است. اگر فقط یک عدد می توانست در آن قرار بگیرد، اینکار را انجام می داد و به سراغ خانه خالی بعدی می رفت. اگر بیش از یک عدد در یک خانه قرار می گرفت، آن خانه را رها می کرد و به سراغ خانه های خالی بعدی می رفت. بعد از حل کردن اکثر خانه ها، گاهی مواقع وضعیتی پیش می آمد که چند خانه خالی داریم که همه در آنها بیش از یک عدد می توان قرار داد. پس هیچ خانه ای وجود ندارد که فقط یک عدد در آن قرار بگیرد. در این حالت ها، الگوریتم پیاد سازی شده توسط من با شکست مواجه می شد. در نهایت متوجه شدم که اگر به این وضعیت رسید، باید تمام حالت های ممکن برای این خانه ها را در نظر بگیرد و امتحان کند ببیند در کدام حالت می توان یک sudoku  Validبدست آورد. استفاده از یک الگوریتم بازگشتی یک روش است که پیاده سازی آن بسیار آسان تر از الگوریتم غیر بازگشتی است.
  
در فایل MainActivity.kt، از روش view binding بجای findViewById استفاده شده است. در تابع onCreate، در ابتدا تابع initLayout صدا زده می شود که یک Grid Layout با ابعاد 9*9 را بصورت Dynamic ایجاد می کند. در هرکدام از خانه های این Grid، یک TextView قرار داده شده که کل آن را پر می کند(GridLayout.FILL). همچنین برای دسترسی راحت تر، این TextView ها در یک آرایه global ذخیره می شوند. البته یک آرایه جداگانه برای اعدادجدول  sudoku نیز در نظر گرفته شده است.

```kotlin
fun initLayout() {
        binding.gridView.removeAllViews()
        for (i in 0 until dimensions) {
            for (j in 0 until dimensions) {
                val textview = TextView(this).apply {
                    setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24f)
                    gravity = Gravity.CENTER
                    setBackgroundColor(Color.TRANSPARENT)
                }
                val params = GridLayout.LayoutParams(
                    GridLayout.spec(i, GridLayout.FILL, 1f),
                    GridLayout.spec(j, GridLayout.FILL, 1f)
                ).apply {
                    width = 0
                    height = 0
                }

                binding.gridView.addView(textview, params)
                gridTextViews[i][j] = textview
            }
        }
    }
```

  هنگامی که روی دکمه solve کلیک می شود، یک kotlin coroutine ایجاد می شود(مشابه thread در java) که فرآیند حل را اجرا می کند و این coroutine در متغیر solvingJob ذخیره می شود تا بعدا در صورت نیاز بتوان آن را cancel کرد. قسمت هایی از کد که UI را تغییر می دهند، باید روی Main Thread یا همان UI Thread اجرا شوند که در زبان kotlin و داخل coroutine بصورت زیر انجام می شود.
```kotlin
withContextNonCancellable(Dispatchers.Main) {
  colorAnimation.end()
  textview!!.setBackgroundColor(Color.TRANSPARENT)
}
```
  برای انتخاب فایل sudoku توسط کاربر، بصورت زیر عمل می کنیم.
```kotlin
binding.btnReadFile.setOnClickListener {
  var intent = Intent(Intent.ACTION_GET_CONTENT)
  intent.type = "*/*"
  intent = Intent.createChooser(intent, "Choose a file")
  resultLauncher.launch(intent)
}
```
  
