# README

## Import data from newegg.com

If you running app from IntelliJ IDEA, you config should be like this:
```
Main class: com.fiftin.xpathcrawler.Main
Program arguments: <path-to-xpathcrawler>/src/test/resources/com/fiftin/xpathcrawler/newegg-single.json --parallel=10 --dir-DEPRECATED=<path-to-dump-of-www.newegg.com>\Product
```

## Import data from bhphotovideo.com
```
Main class: com.fiftin.xpathcrawler.Main
Program arguments: <path-to-xpathcrawler>/src/test/resources/com/fiftin/xpathcrawler/bhphotovideo-single.json --parallel=10 --dir-DEPRECATED=<path-to-dump-of-www.bhphotovideo.com>\Product
```



## Configuration
Файл конфигурации имеет JSON-формат. В конфигурационном файле указываются: 
* **url** - адрес или алгоритм определния адреса страниц для разбора. Может быть текстовым полем, а может быть блоком.
* **content** - набор пар "имя поля" - "путь к значению на странце заданный в XPath формате".
* **content-order** - массив имен полей в порядке вывода на печать. Актуально при выводе например в формате CSV.

### Configuration for parsing single page
Ниже приведен пример конфигурационного файла для разбора страницы **test-doc-1.html**.
```
{
  "url" : "file:./test-doc-1.html",
  "content" : {
    "Address" : "//tr[starts-with(td, 'Address')]/td[2]",
    "Phone" : "//tr[starts-with(td, 'Phone')]/td[2]",
    "Email" : "//div[@class='e']/table/tr[position()>1]/td[1]",
    "Fax" : "//div[@class='e']/table/tr[position()>1]/td[2]"
  },
  "content-order": [
    "Address",
    "Phone",
    "Email",
    "Fax"
  ]
}
```
### Configuration for parsing pages from N to M
Ниже приведен пример разбора набора страниц с 1 по 45500.
Поле **url** имеет формат **чать адреса{0}другая чать адреса**.
Программа в цикле перебирает все страницы с 1 по 45500, подставляя вмесво {0} 
текущий номер.
Начальный и последний номера страниц задаются соответвенно полями **start-index** и
**end-index**.
```
{
  "url" : "https://ncrdb.usga.org/NCRDB/courseTeeInfo.aspx?CourseID={0}",
  "start-index" : 1,
  "end-index" : 45500,
  "content" : {
    "Course ID" : ["const", "{0}"],
    "Name"      : "//td[@id='contentarea']//table[@id='gvCourse']/tr[@class='trLight_SV']/td[1]",
    "City"      : "//td[@id='contentarea']//table[@id='gvCourse']/tr[@class='trLight_SV']/td[2]",
    "State"     : "//td[@id='contentarea']//table[@id='gvCourse']/tr[@class='trLight_SV']/td[3]",
    "Tee"       : "//td[@id='contentarea']//table[@id='gvTee']/tr[@class='trLight_SV']/td[1]",
    "Tee Rating": "//td[@id='contentarea']//table[@id='gvTee']/tr[@class='trLight_SV']/td[2]",
    "Tee Slope" : "//td[@id='contentarea']//table[@id='gvTee']/tr[@class='trLight_SV']/td[3]",
    "Tee Front" : "//td[@id='contentarea']//table[@id='gvTee']/tr[@class='trLight_SV']/td[4]",
    "Tee Back"  : "//td[@id='contentarea']//table[@id='gvTee']/tr[@class='trLight_SV']/td[5]",
    "Tee Bogey Rating" : "//td[@id='contentarea']//table[@id='gvTee']/tr[@class='trLight_SV']/td[6]",
    "Tee Gender": "//td[@id='contentarea']//table[@id='gvTee']/tr[@class='trLight_SV']/td[7]"
  },
  "content-order" : [
    "Course ID",
    "Name",
    "City",
    "State",
    "Tee",
    "Tee Rating",
    "Tee Slope",
    "Tee Front",
    "Tee Back",
    "Tee Bogey Rating",
    "Tee Gender"
  ]
}
```
В данном примере также можно увидеть что поле **Course ID** имеет номер страницы в качестве значения.

### Конфигурация для разбора нескольких страниц рекурсивно

В этой конфигурации программа разбирает страницы по следующему алгоритму:
* Программа разбирает страницу с адресом заданным в поле **source-url**.
* Находит на ней ссылки по шаблону заданному в поле **link-path** в XPath формате.
* Полученные адреса использует для получения адресов следующего уровня (если блок содержит поле **url**)
или для разбора страницы по алгоритму описанному в разделе **Конфигурация для разбора 1 страницы**.

```
{
  "url": {
    "source-url": "http://www.worldgolf.com/courses/usa/",
    "link-path": "//div[@id='maincontent']/div[@class='objectwrap']/div[@class='sublocationswrap']/div[@class='groupbody']//a",
    "url": {
      "link-path": "//div[@id='maincontent']/div[@class='objectwrap']/div[@class='sublocationswrap']/div[@class='groupbody']//a",
      "url": {
        "link-path": "//div[@id='maincontent']/div[@class='objectwrap']/div[@class='courseswrap']/div[@class='groupbody']//strong/a",
        "content": {
          "Name"      : "//div[@id='maincontent']//div[@class='headerwrap']//h1",
          "Contact"   : "//div[@id='maincontent']//div[@class='contactswrap']//address",
          "Type"      : "//div[@id='maincontent']//div[@class='basicswrap']/table/tr[th='Type']/td",
          "Holes"     : "//div[@id='maincontent']//div[@class='basicswrap']/table/tr[th='Holes']/td",
          "Tee"       : "//div[@id='maincontent']//div[@class='teeswrap']/table/tr[@class='item']/td[1]",
          "Tee Par"   : "//div[@id='maincontent']//div[@class='teeswrap']/table/tr[@class='item']/td[2]",
          "Tee Length": "//div[@id='maincontent']//div[@class='teeswrap']/table/tr[@class='item']/td[3]",
          "Tee Slope" : "//div[@id='maincontent']//div[@class='teeswrap']/table/tr[@class='item']/td[4]",
          "Tee Rating": "//div[@id='maincontent']//div[@class='teeswrap']/table/tr[@class='item']/td[5]"
        },
        "content-order": [
          "Name",
          "Contact",
          "Type",
          "Holes",
          "Tee",
          "Tee Par",
          "Tee Length",
          "Tee Rating",
          "Tee Slope"
        ]
      }
    }
  }
}
```
В данном случае название поля **url** не логично, это факт. 

### Смешанная конфигурация
Конфигурация может включать описанные выше 2 конфигурации.
Пример
```
{
  "start-index" : 1,
  "end-index" : 260,
  "url": {
    "source-url": "http://7days.ru/news/{0}.htm",
    "link-path": "//div[@class='b-columns__col-in']/div[2]//div[@class='b-story__title']/a",
    "content": {
      "Title": "//div[@class='b-story__content']//h1[@class='b-story__title']",
      "Subtitle": "//div[@class='b-story__content']//div[@class='b-story__subtitle']",
      "URL": ["const", "{url}"],
      "Image": "//div[@class='b-story__content']//img[@class='b-content-image__image']/@src",
      "Description": "//div[@class='b-pure-content b-pure-content_type_detail b-story__section j-story-content']//p"
    },
    "content-order": [
      "Title",
      "Subtitle",
      "URL",
      "Image",
      "Description"
    ]
  }
}
```
В данном примере также можно увидеть что поле **URL** имеет адрес страницы в качестве значения.

### Разбор по файлу
```
{
  "list-file" : {
    "file-naturalKey" : "/home/denis/states-golfhub.txt",
    "var"   : "state"
  },
  "url" : {
    "source-url" : "http://www.golfhub.com/{state}/GolfCourses",
    "link-path": "//div[@id='golfcourselist']//div[@class='coursefooter']/a[1]",
    "content" : {
      "Name"      : "//div[@id='pageheader']/h1/a",
      "Holes"     : "//div[@id='main']/p[@class='vitals']",
      "Address"   : "//div[@id='course-address']/p[@class='address']/a[1]",
      "City"      : "//div[@id='course-address']/p[@class='address']/span[1]",
      "State"     : "//div[@id='course-address']/p[@class='address']/span[2]",
      "Zip"     : "//div[@id='course-address']/p[@class='address']/a[@class='hidlink']",
      "Tee"       : "//div[@id='scorecard']/table/tr[position()>1]/td[@class='yardage naturalKey']",
      "Tee Length": "//div[@id='scorecard']/table/tr[position()>1]/td[@class='yardage total']"
    },
    "content-order" : [
      "Name",
      "Holes",
      "Address",
      "City",
      "State",
      "Zip",
      "Tee",
      "Tee Length"
    ]
  }
}
```
В данном примере из файла **states-golfhub.txt** берется список штатов. Название
штата помещается в переменную *state*.
Для каждого из штатов формируется URL по которому осуществляется разбор.


### Разбор списка URLов
```
{
  "encoding": "windows-1251",
  "url": [
    "http://7days.ru/news/tatyana-navka-gorditsya-svoimi-dochermi.htm",
    "http://7days.ru/news/v-33-goda-yuriy-yakovlev-chudom-izbezhal-gibeli.htm",
    "http://7days.ru/beauty/hairstyles/keyt-middlton-otrugali-za-sedye-volosy.htm"
  ],
  "content": {
    "Title": "//div[@class='b-story__content']//h1[@class='b-story__title']",
    "Subtitle": "//div[@class='b-story__content']//div[@class='b-story__subtitle']",
    "URL": [
      "const",
      "{url}"
    ],
    "Image": "//div[@class='b-story__content']//img[@class='b-content-image__image']/@src",
    "Description": "//div[@class='b-pure-content b-pure-content_type_detail b-story__section j-story-content']//p"
  },
  "content-order": [
    "Title",
    "Subtitle",
    "URL",
    "Image",
    "Description"
  ]
}
```

### Дополнительные поля конфигурации
* **encoding** - кодировка разбираемой страницы.
* **delay** - задержка между итерациями разбора.
* **start-with** - адрес страницы с который будет начат разбор.

### Опции командной строки
* **--need-make-dump** - указывает программе что необходимо сохранять разбираемые страницы в локальной файловой системе.
* **--load-from-dump** - указывает программе что необходимо брать разбираемые страницы из локальной файловой системы.
