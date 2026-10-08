# LBC: lbc other imba

Полный поведенческий проход охватывает все293 источника `lbc_other_imba`: 178 проверены вручную, ещё115 — полным командным шаблоном с проверенными исключениями. Список методов/путей/хэшей находится в [read manifest](inventory/review_other_imba_read.json), все команды сохранены отдельно в [source manifest](inventory/review_other_imba_manifest.json). Caller означает подтверждённый вызов функцией, advancement или runtime-dispatch; отсутствие literal function-call не доказывает, что игрового входа нет. `dx/dy/dz` вместе с `distance` задают совместное пересечение, а `execute as` сам по себе не переносит позицию. `/effect give` измеряет длительность в секундах; `fuse:2s` в SNBT — Short на два тика (0,1 с), тогда как `schedule 2s` — две секунды.


## Генерация Cunny sign и макро-случайные тексты

`cunny_slab/tick` выбирает correction marker; `correction_cunny` преобразует X/Z каждого marker-а в целые координаты, затем добавляет независимо выбранное смещение−10..10 по каждой оси (Y остаётся прежним) и удаляет marker после работы. `correction_cunny_uoh` превращает воздух под позицией в barrel с loot table `lbc:imba/cunny_slabs`, выбирает дерево из 12 вариантов и rotation 0..15. Затем она дважды независимо выбирает номер 1..100: первый текст копирует в `t12..t42` для задней стороны, второй задаёт `t1..t4` для передней. `correction_cunny_uoh_kanikani` ставит waxed sign с обеими сторонами; текстовые макросы ниже только записывают четыре строки в storage и сами мир не меняют. Это не один фиксированный случайный текст: таблица показывает все 100 полных наборов четырёх строк, а выборы сторон независимы.

| Номер | Четыре строки, задаваемые в storage | Файл |
|---:|---|---|
| 1 | `t1`: `\uF82F\uF82FCUNNY CUNNY\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FCUTE AND FUNNY\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FUOOOOOOOOOOH\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FPLAP PLAP PLAP\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/1.mcfunction) |
| 2 | `t1`: `\uF82F\uF82FGET PREGNANT\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FGET PREGNANT\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FGET PREGNANT\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FGET PREGNANT\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/2.mcfunction) |
| 3 | `t1`: `\uF82F\uF82Fi really want\uF82F\uF82F`<br>`t2`: `\uF82F\uF82Fto lick her cute\uF82F\uF82F`<br>`t3`: `\uF82F\uF82Flittle soft\uF82F\uF82F`<br>`t4`: `\uF82F\uF82Floli armpits\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/3.mcfunction) |
| 4 | `t1`: `\uF82F\uF82FMAN I LOVE\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FLOLICON\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FCUNNY CUNNY\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FCUTE AND FUNNY\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/4.mcfunction) |
| 5 | `t1`: `\uF82F\uF82Fkani kani kani\uF82F\uF82F`<br>`t2`: `\uF82F\uF82Fkani kani kani\uF82F\uF82F`<br>`t3`: `\uF82F\uF82Fkani kani kani\uF82F\uF82F`<br>`t4`: `\uF82F\uF82Fkani kani kani\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/5.mcfunction) |
| 6 | `t1`: `\uF82F\uF82FPLAP PLAP PLAP\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FPLAP PLAP PLAP\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FPLAP PLAP PLAP\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FPLAP PLAP PLAP\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/6.mcfunction) |
| 7 | `t1`: `\uF82F\uF82FUUUUUUUUUU\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FOOOOOOOOOO\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FOOOOOOOOOO\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FHHHHHHHHHH\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/7.mcfunction) |
| 8 | `t1`: `\uF82F\uF82FTIGHT AS HELL\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FSEIA BELLY IS\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FSO FUCKIN SOFT\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FCANT RESIST\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/8.mcfunction) |
| 9 | `t1`: `\uF82F\uF82FGreat Wisdom\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FGoddess Nahida\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FSoft feet and so\uF82F\uF82F`<br>`t4`: `\uF82F\uF82Fcute tight cunny\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/9.mcfunction) |
| 10 | `t1`: `\uF82F\uF82FI WANT TO LICK\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FHER LOLI CUNNY\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FPLEASE PEE ON ME\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FUOOOOOOOOOOH\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/10.mcfunction) |
| 11 | `t1`: `\uF82F\uF82FPLEASE COME OVER\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FAND PEE ON ME\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FMY LITTLE CUTE\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FKISAKI CHAN !!!\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/11.mcfunction) |
| 12 | `t1`: `\uF82F\uF82F:sob: :sob: :sob:\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FGRRRRRRRRRRR!\uF82F\uF82F`<br>`t3`: `\uF82F\uF82F:angry: :angry:\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FCORRECTION!\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/12.mcfunction) |
| 13 | `t1`: `\uF82F\uF82FHOW YOU CAN\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FTEASE ADULTS\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FLIKE THIS YOU BRAT\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FIMMEDIATE CORRECTION\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/13.mcfunction) |
| 14 | `t1`: `\uF82F\uF82FCORRECTION\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FCORRECTION\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FCORRECTION\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FCORRECTION\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/14.mcfunction) |
| 15 | `t1`: `\uF82F\uF82FCIRNO HAS THE\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FTIGHTEST CUNNY\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FGRRR PLAP PLAP\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FPLAP PLAP PLAP\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/15.mcfunction) |
| 16 | `t1`: `\uF82F\uF82FNAHIDA PLEASE\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FSIT ON MY FACE\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FAND PEE IN MY MOUTH\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FGREAT GODDESS\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/16.mcfunction) |
| 17 | `t1`: `\uF82F\uF82FKISAKI YOU ARE SO\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FBEAUTIFUL IN THOSE\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FCHINESE DRESS\uF82F\uF82F`<br>`t4`: `\uF82F\uF82F!!CORRECTION!!\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/17.mcfunction) |
| 18 | `t1`: `\uF82F\uF82FI LOVE MY FLAT\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FSTUDENT WAIFUS\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FPEAK FEMALE BODY\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FUOOOOOOOH\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/18.mcfunction) |
| 19 | `t1`: `\uF82F\uF82FCUNNY LOVE IS\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FCUNNY LIVE\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FONE DAY I HOPE\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FI SEE CUNNY WIFE\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/19.mcfunction) |
| 20 | `t1`: `\uF82F\uF82FSPLURGE TONS\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FIN THOSE CUTE\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FLOLI SOFT ARMPITS\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FSO CUTE AND FUNNY\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/20.mcfunction) |
| 21 | `t1`: `\uF82F\uF82FLOLI FEET ARE THE\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FBEST, SO SOFT\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FCUTE LITTLE FEET\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FI LOVE YOU NAHIDA\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/21.mcfunction) |
| 22 | `t1`: `\uF82F\uF82FKLEE PLEASE LET ME\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FTOUCH YOUR PRESCIOUS\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FLITTLE LOLI TITS\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FMEET YOU IS SO LUCKY\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/22.mcfunction) |
| 23 | `t1`: `\uF82F\uF82FSPLURGE TONS\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FINSIDE YOUR LITTLE\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FSOFT LOLI CUNNY\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FIS THE DREAM\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/23.mcfunction) |
| 24 | `t1`: `\uF82F\uF82FPROFESSOR NIYANIYA\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FYOU ARE SOOOOOOOO\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FBREEDABLE IN\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FTHIS SWIMSUIT\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/24.mcfunction) |
| 25 | `t1`: `\uF82F\uF82FSIGEWINNE PLEASE\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FSTICK YOUR BIG\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FSYRINGE DIRECTLY\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FUP MY ASS PLEASE\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/25.mcfunction) |
| 26 | `t1`: `\uF82F\uF82FI WANT TO TOUCH YOU\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FTHERE MY CUTE LITTLE\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FSIGEWINNE, PLEASE\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FLET ME TOUCH YOU\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/26.mcfunction) |
| 27 | `t1`: `\uF82F\uF82FDORI HOW YOU CAN\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FTEASE ADULTS WITH\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FTHIS TIGHT LOLI CUNNY\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FCORRECTION NEEDED\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/27.mcfunction) |
| 28 | `t1`: `\uF82F\uF82FSAYU SO TIRED FROM\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FTRAININGS, HER SWEAT\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FIS SOOO YUMMY TO LICK\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FHER SWEATY ARMPITS\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/28.mcfunction) |
| 29 | `t1`: `\uF82F\uF82FSEX WITH EVERY\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FSTUDENT IN BLUE\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FARCHIVE! NO ONE\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FWILL ESCAPE!\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/29.mcfunction) |
| 30 | `t1`: `\uF82F\uF82FAMIRHAN PRIVET\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FКак дела, Амирхан\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FО. Здарова Амирхан\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FHi Amirhan!\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/30.mcfunction) |
| 31 | `t1`: `\uF82F\uF82FLOLI PUSSY HORSE\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FCOCK INSERTION\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FTENTACLES MONSTERS\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FCUM LOLI TIME STOP\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/31.mcfunction) |
| 32 | `t1`: `\uF82F\uF82FPUMP IN TIGHT\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FYAOYAO SWEET CUNNY\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FLOLI PUSSY\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FIMPREGNATION\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/32.mcfunction) |
| 33 | `t1`: `\uF82F\uF82FCUNNY CORRECTION\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FPLAP PLAP PLAP\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FRAPE LOLIS\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FTIGHT PUSSY\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/33.mcfunction) |
| 34 | `t1`: `\uF82F\uF82FGOON TO KIDS\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FFLUFFY LOLI HAIR\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FWANT TO COVER IT\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FIN CUM COMPLETELY\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/34.mcfunction) |
| 35 | `t1`: `\uF82F\uF82FLOLI MADE FOR\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FHUGE HORSE COCKS\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FCANT RESIST RAPING\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FA CHILDREN UOOOH\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/35.mcfunction) |
| 36 | `t1`: `\uF82F\uF82FBLUE ARCHIVE GIRLS\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FNEEDS TO BE LOVED\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FBY THEIR SENSEIS\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FCUNNY UOOOOOOOOH\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/36.mcfunction) |
| 37 | `t1`: `\uF82F\uF82FMY BINAH IS READY\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FTO GO STRAIGHT\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FINTO KISAKI TIGHTEST\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FCUNNY RIGHT NOW\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/37.mcfunction) |
| 38 | `t1`: `\uF82F\uF82FARONA IS SOOOOOO\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FBREEDABLE! NEEDS\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FTO BE PUMPED AT\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FLEAST 20 TIMES A DAY\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/38.mcfunction) |
| 39 | `t1`: `\uF82F\uF82FSOFT ARMPITS\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FBULGING BELLY WITH\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FHARD BINAH IN IT\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FLOLI IS PEAAAAK\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/39.mcfunction) |
| 40 | `t1`: `\uF82F\uF82FЯ Владислав Сафин\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FМне нравится быть фембоем\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FФембои - это патриатично!\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FСлава Русской Нации!\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/40.mcfunction) |
| 41 | `t1`: `\uF82F\uF82FSOMEDAY MY CUNNY\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FWIFE WILL COME\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FI BELIEVE SOMEDAY\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FI'LL HUG MY CUNNY WIFE\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/41.mcfunction) |
| 42 | `t1`: `\uF82F\uF82FALL LITTLE CUNNIES\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FNEED THEIR\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FFUTA MOM WITH\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FLARGE FUTA DICK\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/42.mcfunction) |
| 43 | `t1`: `\uF82F\uF82FSEIA IS THE MOST\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FBREEDABLE STUDENT\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FFROM BLUE ARCHIVE\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FNEED CORRECTION\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/43.mcfunction) |
| 44 | `t1`: `\uF82F\uF82FI DON'T WANT\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FKOKONA TO BE A\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FCHILD, I WANT HER TO\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FBEAR MY CHILD\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/44.mcfunction) |
| 45 | `t1`: `\uF82F\uF82FKOKONA DOESN'T WANT\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FTO BE TREATED AS CHILD\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FSHE'S A GROWN-UP ADULT\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FI ALREADY MADE HER ONE\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/45.mcfunction) |
| 46 | `t1`: `\uF82F\uF82FKOKONUT\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FINSIDE\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FKOKONA\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FUOOOOOOOOOOH\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/46.mcfunction) |
| 47 | `t1`: `\uF82F\uF82FUOOOOOOOGHHHH\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FAAAAAAAARRRGH\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FKOKONA CUNNY\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FGET PREGNANT\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/47.mcfunction) |
| 48 | `t1`: `\uF82F\uF82FKOKONA SAYS SHE'S AN\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FADULT, SO I INTRODUCED\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FHER TO ADULT WORLD\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FNOW SHE WANTS MORE\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/48.mcfunction) |
| 49 | `t1`: `\uF82F\uF82FUOOOH CUNNY\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FI NEED TO CORRECT\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FEVERYONE AND\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FEVERYDAY\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/49.mcfunction) |
| 50 | `t1`: `\uF82F\uF82FI LOVE KOKONA\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FCUTE AND FUNNY\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FDAUGHTERWIFE\uF82F\uF82F`<br>`t4`: `\uF82F\uF82F:sob: :sob: :sob:\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/50.mcfunction) |
| 51 | `t1`: `\uF82F\uF82FMY FAVORITE MILK\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FIS KOKONA MILK\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FOH I MEAN COCONUT\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FNAH KOKONA IS THE BEST\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/51.mcfunction) |
| 52 | `t1`: `\uF82F\uF82FTIGHT INFINITY\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FBREEDING TIME\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FPORTABLE WIFE\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FSO TIGHT TUNNEL\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/52.mcfunction) |
| 53 | `t1`: `\uF82F\uF82FUOOOOOOOOOH\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FGRRRRRRRRRRRRRR\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FRAPE CUNNY\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FPLAP PLAP PLAP PLAP\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/53.mcfunction) |
| 54 | `t1`: `\uF82F\uF82FI-I CAN'T TAKE IT\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FAKARI CHAN IS TOO CUTE\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FI CANNOT CONTAIN MY\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FTHIRST FOR THE CUNNY\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/54.mcfunction) |
| 55 | `t1`: `\uF82F\uF82FDRAINED PHYSICALLY\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FMENTALLY & FINANCIALLY\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FAKARI IS\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FRUTHLESS\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/55.mcfunction) |
| 56 | `t1`: `\uF82F\uF82FI CANT\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FMESUGAKIS ARE\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FTHE GREATEST GIFT\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FFROM GOD\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/56.mcfunction) |
| 57 | `t1`: `\uF82F\uF82FNOZOMI IS THE\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FMOST BREEDABLE\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FSTUDENT IN\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FBLUE ARCHIVE\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/57.mcfunction) |
| 58 | `t1`: `\uF82F\uF82FALL THE MESUGAKI REQUIRE\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FCORRECTION AND HAVE TO\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FBE TAUGHT A LESSON THEY\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FWON'T FORGET FOR MONTHS\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/58.mcfunction) |
| 59 | `t1`: `\uF82F\uF82FHIKARI AND NOZOMI\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FBREED BREED BREEED\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FTHEY WILL REMEMBER\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FTHIS CORRECTION FOREVER\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/59.mcfunction) |
| 60 | `t1`: `\uF82F\uF82FMY BINAH IS ON THE\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FSTRAIGHT WAY INSIDE\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FTHIS LITTLE CUTE\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FAND FUNNY LOLI PUSSY\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/60.mcfunction) |
| 61 | `t1`: `\uF82F\uF82FA MOTHER OF 1000000\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FPER SECOND AT LEAST\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FUOOH NOZOMI I'M ABOUT\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FTO UNWRAP MY GIFT TO YOU\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/61.mcfunction) |
| 62 | `t1`: `\uF82F\uF82FNOZOMI IS\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FBETTER THAN HIKARI\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FNEED CORRECTION\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FMOTHER OF TEN\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/62.mcfunction) |
| 63 | `t1`: `\uF82F\uF82FGOD I LOVE BRATTY\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FLITTLE GIRLS\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FWHO TEASE ME AT\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FEVERY OPPORTUNITY\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/63.mcfunction) |
| 64 | `t1`: `\uF82F\uF82FTHIS PROF CUNNY\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FPLOTTER ALWAYS RADIATING\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FSMELL OF THE SUN\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FALWAYS SHE PASSES NEARBY\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/64.mcfunction) |
| 65 | `t1`: `\uF82F\uF82FTHAT WOMB NEEDS\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FTO BE TAUGHT\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FA LESSON\uF82F\uF82F`<br>`t4`: `\uF82F\uF82F!!CORRECTION!!\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/65.mcfunction) |
| 66 | `t1`: `\uF82F\uF82FTHE 8 PRISONERS ABOUT\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FTO BE THE 8 CORRECTED\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FWIVES, INCLUDING\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FTHE PROFESSOR NIYANIYA\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/66.mcfunction) |
| 67 | `t1`: `\uF82F\uF82FSHE'S SMUG AND NEEDS\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FCORRECTION! THESE ARE\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FTHE FACTS AS WE KNOW THEM\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FCORRECTION IS NEEDED ASAP!\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/67.mcfunction) |
| 68 | `t1`: `\uF82F\uF82FI WANT TO BE\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FSURROUNDED BY\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FPROFESSOR NIYANIYA\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FAND MOMOKA SAKURAI\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/68.mcfunction) |
| 69 | `t1`: `\uF82F\uF82FUOOOOOOOH\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FCUNNY CUNNY\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FGRRRRRRRRRRRRRRR\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FCUTE AND FUNNY\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/69.mcfunction) |
| 70 | `t1`: `\uF82F\uF82FI LOVE HINA\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FONE OF THE MOST\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FTIGHTEST CUNNY\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FIN KIVOTOS\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/70.mcfunction) |
| 71 | `t1`: `\uF82F\uF82FHINA IS THE VISION\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FFOR THE BLIND\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FHINA IS THE CHEF\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FFOR THE HUNGRY\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/71.mcfunction) |
| 72 | `t1`: `\uF82F\uF82FHINA IS WHY I BREATH\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FHINA IS WHY I SMILE\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FHINA IS WHY I LAUGH\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FHINA IS MY LIFE\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/72.mcfunction) |
| 73 | `t1`: `\uF82F\uF82FEVERYTIME IT ALL\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FEND UP WITH HINA\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FPREGNANT AND ME\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FBEING THE DAD\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/73.mcfunction) |
| 74 | `t1`: `\uF82F\uF82FIT CAN'T BE HELPED\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FSO I'LL TAKE RESPONSIBILITY\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FLET'S REPOPULATE\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FA GALAXY, HINA\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/74.mcfunction) |
| 75 | `t1`: `\uF82F\uF82FHINA LET'S CUDDLE\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FTOGETHER ALL\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FNIGHT UWOOOOO\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FOOOOOGGGGGHHHHH\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/75.mcfunction) |
| 76 | `t1`: `\uF82F\uF82FYEAH YEAH YEAH\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FWHATEVER,\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FPLAP PLAP PLAP PLAP\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FPLAP PLAP PLAP PLAP\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/76.mcfunction) |
| 77 | `t1`: `\uF82F\uF82FPLANA AND ARONA\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FARE BOTH PLAPABLE\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FAND ADORABLE\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FCUTE LOLI STUDENTS\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/77.mcfunction) |
| 78 | `t1`: `\uF82F\uF82FNAHIDA AND KLEE\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FARE BOTH MY\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FCUTE AND FUNNY\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FWIFEDAUGHTERS\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/78.mcfunction) |
| 79 | `t1`: `\uF82F\uF82FUOOOOOOOOOOOOOH\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FARONA YOU HAVE MADE\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FME BROKE 9 TIMES IN\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FA ROW! CORRECTION!!\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/79.mcfunction) |
| 80 | `t1`: `\uF82F\uF82FREASON TO LOVE\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FINSTRUCTOR KOKONA\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FYOU DON'T HAVE TO WAIT\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FFOR HER TO TURN 16\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/80.mcfunction) |
| 81 | `t1`: `\uF82F\uF82FHER MUSTY SMELL AND\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FTHE SWEAT DROPPING\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FHOLY WATER AFTER HER\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FPERFORMANCE IS SO GOOD\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/81.mcfunction) |
| 82 | `t1`: `\uF82F\uF82FMARI IS OUR GUILDING\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FSTAR MARI I LOVE YOU\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FTHE SWEAT ON YOUR PIT\uF82F\uF82F`<br>`t4`: `\uF82F\uF82F:sob: :sob: :sob: :sob:\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/82.mcfunction) |
| 83 | `t1`: `\uF82F\uF82FUOOOOH MARI CHAN\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FMARI CHAN MARI CHAN\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FHAVE ALL MY CHILDREN\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FMARI IS MY LIGHT\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/83.mcfunction) |
| 84 | `t1`: `\uF82F\uF82FIMAGINE THE SMELL\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FAND TASTE OF HER\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FHOLY SWEAT WATER\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FFEELS LIKE HEAVEN\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/84.mcfunction) |
| 85 | `t1`: `\uF82F\uF82FMARI MY SAVIOR\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FMY SAVING GRACE\uF82F\uF82F`<br>`t3`: `\uF82F\uF82F28 KITTENS\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FCAN'T STOP PUMPING\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/85.mcfunction) |
| 86 | `t1`: `\uF82F\uF82FI CAN'T RESIST\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FCOLLECTING BOTTLES\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FFILLED WITH ALL KINDS\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FOF LIQUID FROM MARI!\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/86.mcfunction) |
| 87 | `t1`: `\uF82F\uF82FMARI IS JUST PERFECT\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FPERFECT ANGEL\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FIDOL OF PREGNANCY\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FI WANT YOU MARI\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/87.mcfunction) |
| 88 | `t1`: `\uF82F\uF82FYUUKA, DO YOU HAVE\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FGRAVITATIONAL PULL?\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FCAUSE I'M ATTRACTED TO YOU\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FMOTHER AT 16\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/88.mcfunction) |
| 89 | `t1`: `\uF82F\uF82FUOOOH CUNNY GRRRR\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FI'M GOING TO GET FRY\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FUOOOOOOOOOOOOOOOH\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FYUUKA I CAN'T\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/89.mcfunction) |
| 90 | `t1`: `\uF82F\uF82FYUUKA ARE GOING BEYOND\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FMY CALCULATIONS\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FSHE ARE MAKING MY BINAH\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FWEIGH OVER 100KG\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/90.mcfunction) |
| 91 | `t1`: `\uF82F\uF82FHIKARI WILL BE A\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FCONDUCTOR OF 100\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FCHILDREN! THAT\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FTUMMY WILL BEEEE!!!\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/91.mcfunction) |
| 92 | `t1`: `\uF82F\uF82FNOZOMI THIGT TUNNEL\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FWILL BE SHAPED\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FLIKE SENSEI'S\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FBIG TRAIN!!!\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/92.mcfunction) |
| 93 | `t1`: `\uF82F\uF82FHOPE YOU HAVE FLOOD\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FCONTROL MEASURES\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FHIKARI, BECAUSE THAT\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FTUNNEL IS ABOUT TO OVERFLOW\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/93.mcfunction) |
| 94 | `t1`: `\uF82F\uF82FHIKARI'S TUMMY HITS\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FTHAT FINE LINE BETWEEN\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FPLUMP AND FLAT, GOING\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FTO MAKE IT ROUND\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/94.mcfunction) |
| 95 | `t1`: `\uF82F\uF82FUOOOOOOH MY TRAIN\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FWILL GO STRAIGHT\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FINTO THE DARK TUNNEL\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FAND IT SON'T STOP\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/95.mcfunction) |
| 96 | `t1`: `\uF82F\uF82FI ENDED UP FLOODING\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FSAKI'S RABBIT HOLE\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FJUST LIKE MIYU'S,\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FMIYAKO'S AND MOE'S\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/96.mcfunction) |
| 97 | `t1`: `\uF82F\uF82FSHUN TAKES THE\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FPOTION WHENEVER\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FSENSEI IS AROUND\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FCHEEKY LITTLE BRAT\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/97.mcfunction) |
| 98 | `t1`: `\uF82F\uF82FSHUNNY IS GOING\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FTO BEAR MORE BABIES\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FTHAN ALL JACKIE\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FCHAN MOVIES COMBINED\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/98.mcfunction) |
| 99 | `t1`: `\uF82F\uF82FSHUNLING 9 YEARS\uF82F\uF82F`<br>`t2`: `\uF82F\uF82F9 YEARS EXPERIENCE\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FLITTLE GIRL!!!!!\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FSWEET LITTLE KIDDY SCENT\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/99.mcfunction) |
| 100 | `t1`: `\uF82F\uF82FSHUNNY GOING TO FEEL\uF82F\uF82F`<br>`t2`: `\uF82F\uF82FTHE POWER OF SENSEI\uF82F\uF82F`<br>`t3`: `\uF82F\uF82FINSIDE THE BODY\uF82F\uF82F`<br>`t4`: `\uF82F\uF82FOF CUTE AND FUNNY LOLI\uF82F\uF82F` | [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/100.mcfunction) |

## Поведенческий обзор и проверенные развилки

Эта часть сводит повторяющиеся обработчики в понятные циклы; ниже в каталоге остаются точные исходные функции и caller-и. Метод чтения всех источников — в [read manifest](inventory/review_other_imba_read.json), полный текст команд, outputs и literal callers — в [source manifest](inventory/review_other_imba_manifest.json). В семейных обзорах указаны также внешние entrypoints там, где один function call был бы неполной картиной.

### Цветные артефакты

`afkartifact5t` проверяет экипированный passive item по циклу `players_5t`; `damaged` и `when_jump` — отдельные входы для ударов и прыжка. Цветной артефакт срабатывает, когда под игроком найден блок соответствующей цветовой группы; у веток ниже нет расхода mana или автоматического удаления предмета. Все сроки `/effect give` — секунды.

| Цвет блока | Эффект владельцу | Эффект на других игроков/мобов и особое действие |
|---|---|---|
| black | снимает blindness, Night Vision 300s | blindness 30s противникам в 20; follow_range мобов вражеской команды становится 4 |
| blue | Water Breathing III и Conduit Power II на 30s | nautilus particles |
| brown | Saturation II на 1s, снимает hunger, `food_count +1` | Hunger II 30s противникам в 25 |
| cyan | Invisibility II и Night Vision II на 300s | Glowing 30s противникам в 300 |
| gray | Resistance V на 10s | barrier particles |
| green | только зелёные particles | в исходнике нет status-effect или изменения мира |
| light blue | Slow Falling на 300s | Levitation VI на 2s противникам в 20 |
| light gray | Resistance III на 300s | снимает Resistance с противников в 15 |
| lime | Jump Boost III на 300s, снимает собственную Levitation | Levitation I на 1s противникам в 15 |
| magenta | `mana +20` | `mana -20` противникам в 35, но только если у них mana≥20 |
| orange | Fire Resistance на 300s | заполняет fire куб 3×3×3 вокруг противников в 30 только в air-блоках |
| pink | Instant Health, Regeneration II 300s; снимает poison/wither/instant damage | Wither II 10s противникам в 20 |
| purple | — | Raid Omen amplifier 40 на 30s вражеским mobs в 20; portal particle и teleport sound повторены дважды |
| red | Strength I 300s, снимает Weakness | Weakness I 60s противникам в 15 |
| white | Speed II 300s, снимает Slowness | Slowness II 30s противникам в 25 |
| yellow | Haste XI 300s, снимает Mining Fatigue | Mining Fatigue III 60s противникам в 25 |

Цвета, не перечисленные как наносители статуса владельцу (green/purple/gray), не получают скрытого бонуса из имени: применяются только прямо указанные команды. Цветной effect повторно выдаётся каждые5 tick, пока игрок стоит на поддерживаемом `#minecraft:colored` блоке или в `#minecraft:in_colored`; это не одноразовый use с cooldown. Подробности отдельных веток: [цветовой helper, например yellow](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/yellow.mcfunction).

### Остальные mystical-артефакты: триггер и полная ветвь

| Вход | Условие и фактический эффект |
|---|---|
| `mystical:1` passive | `players_5t` каждые5t вызывает dispatcher только при `afkartifact5t_active` и без `stopper_magic` в20. Type3 (`nofluid`) заменяет water→frosted ice, lava→basalt, powder snow→snow_block в кубе25³ и передаёт falling blocks в16 helper-у замены. Type7 — цветовая таблица выше; type8 при `hplvl<MaxHealth` выдаёт Hunger III + Regeneration II по1s; type5 раз за проход очищает blindness/slowness/weakness/mining fatigue/glowing/hunger/instant damage/nausea/poison/unluck/wither, а levitation очищает только если ни один перечисленный reactive/techno/custom item не защищает её; type6 снимает все эффекты.
| `mystical:3` jump | `items/armor` проверяет предмет в inventory, отсутствие stopper_magic в20 и отсутствие игрока с `nomagic_active` в20. Type1 даёт Speed VI владельцу3s и Slowness III врагам3s в7; type2 — Levitation XI врагам1s в7, Slow Falling10s владельцу и облако; type3 конвертирует текущие Jump Boost amplification 32/16 в Slow Falling30/20s и очень высокий Jump Boost8/10s, либо снижает 8/4/2/1 на один шаг; затем безусловно добавляет Jump Boost I на10s.
| `mystical:4` hurt | `items/damaged` запускает dispatcher при таком custom_data где-либо в inventory и stopper_magic отсутствует в20. Type2 даёт Resistance V всем игрокам в7 на1s, Slow Falling5s владельцу и TNT fuse0; type3 place template `lbc:air11x11` integrity0.5 со смещением(−5,−8,−5) + портал/звук; type4 Weakness X на3s сущностям non-spectator в7; type5 даёт конверсию status-effects по таблице ниже; type6 summon-ит wolf с Owner UUID владельца. Type1 в dispatcher отсутствует и не запускает helper.
| `mystical:2` hit | `items/swords` требует предмет в inventory и отсутствие `nomagic_active` игрока и stopper_magic в20. Type1 — Regeneration III + Saturation V владельцу на2s, `food_count+1`, Wither V + Hunger100 врагам в0.01..7 с `HurtTime:10s`. Type2 — Poison V20s им в0.01..8; type3 — smoke, Blindness I5s игрокам и mob follow_range base3 в0.01..7; type4 — обоим hit-целям и владельцу запускает horizontal move4 (поворот перед helper-ом принудительно pitch0); type5 разворачивает всех найденных целей на180° в7; type6 добавляет владельцу1000 mana. Кулдауна в dispatcher нет.

У `speed` conversion helper-а проверки последовательны, поэтому эффекты накапливаются вместе с безусловным Strength I5s: Fire Resistance→Regeneration II10s; Resistance→Fire Resistance I40s и Fire Resistance II10s; Speed→Resistance I5s; Strength→Speed III5s. [Damage ветви мистического4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/damaged.mcfunction), [sword-hit ветви мистического2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/ondamage.mcfunction), [jump ветви](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/when_jump.mcfunction), [5t passive dispatcher](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/afkartifact5t.mcfunction).

### Legacy-артефакты, no-arrow/no-TNT и обработчики блокировки

Это самостоятельные вызовы с указанными периодическими входами; не сливаю их со `mystical:*` таблицей, потому что у них другие `custom_data` и selectors.

| Handler | Реальный вход и эффект |
|---|---|
| `shield` | `items/damaged_by_player` на событии урона от игрока, пока executor не spectator и глобальный `time_stopped` score не положительный: Resistance V2s + две barrier particles и anvil sound. |
| `noarrows` | Вызывается от `no_arrows_active` в `players_tick` и от shulker set bonus. На каждом проходе визуализирует и убивает все entity типа `#minecraft:arrows` в16; предметы-стрелы не возвращаются. |
| `notnt` | `players_2t` каждый2t при `notnt_active`: visual/sound и удаление всех `#minecraft:unstable` в24; для `tag=nuke` в24 запускает отдельный `stoppernuke`, затем убивает marker-ы нестабильных объектов. |
| `noeffects1` / `nohp` | `mystical:1,type=5/6` на passive-цикле5t: первый очищает список в верхней таблице, второй снимает все активные effects. |
| `noeffects1_old` / `nohp_old` | `mystical_old:1,type=1/3` в legacy 1s tick: первый очищает blindness, slowness, weakness, strength, levitation, slow_falling, haste и mining_fatigue; второй — regeneration, wither, poison, instant_damage, instant_health, absorption, health_boost. |
| `nomagic_old` | Legacy5t при `mystical_old:1,type=2`: убивает area-effect-cloud, evoker-fangs, experience-bottle, experience-orb и dragon-fireball в4. |
| `notnt_old` | Legacy5t при `mystical_old:1,type=5`: возвращает item TNT/fire charge/end crystal для соответствующих entity в радиусах5/6/7 и убивает creeper в5 вместе со всеми этими explosive entities. |
| `noarrows_old` | Legacy player tick при `mystical_old:1,type=4`: превращает стрелы в4 в dropped-arrow items, затем убивает исходные стрелы. |

[schedule старых предметов 1s](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/new_game/legacy_tick_1s_player.mcfunction), [5t](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/new_game/legacy_tick_5t_player.mcfunction), [каждый tick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/new_game/legacy_tick_player.mcfunction), [shield вход](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/damaged_by_player.mcfunction).

### Cunny Slab: выбор номера, расход и полный набор исходов

Caller `workingitems/cunny_item/select` читает `SelectedItem.components.minecraft:custom_data.cunny`, диспетчер выбирает соответствующую функцию `select1`, а ветвь `cunnys/0..21` очищает до одного предмета с данным ID; у 11–20, кроме сообщения и расхода, эффекта нет. Карточки с содержательным результатом:

| ID | Полное действие / условие и окончание |
|---:|---|
| 0 | Вызывает `swrg:game/start/0`, пишет всем `cunny_slab_0`; снимает один предмет с `cunny:0`. |
| 1 | Сохраняет команду исполнителя и удаляет четыре armor slots у каждого другого игрока с несовпадающей командой: selector `@a[distance=0.01..]` без верхней границы и без фильтра spectator. Потом частицы, сообщение и расход одного `cunny:1`. |
| 2 | Добавляет шесть attribute modifiers: armor +6, toughness +3, attack damage +6, scale ×0.5, max health ×1.5, movement speed ×1.2; выдаёт три loot (`inf_shulker`, `inf_steak`, `evolution_sword`), сообщает и тратит один предмет. Modifiers снимает общий `resets.mcfunction`, значит эффект не вечен. |
| 3 | Даёт владельцу Regeneration III infinite и ставит grassifier marker снизу. Тик marker-ов опускается по воздуху, поднимается если сверху блок; переводит клетку в grass, размножается на четыре грани пока там нет травы и заканчивает себя при уже существующей/поставленной траве. |
| 4 | Вызывает cultist helper ровно20 раз на 16 уникальных позициях радиуса2: 8 соседей на радиусе1, 4 диагонали радиуса1, затем 4 cardinal позиции радиуса2, каждая вызвана дважды. Каждый helper делает равновероятный бросок1..6; если исполнитель в team1..32, моб наследует его team, `lbcID2` и team_number, затем `summactivated`; при активном trinket handler запускается `on_summon`. |
| 5 | Выдаёт loot `lbc:imba/nahida_dakimakura`, сообщает и тратит slab. Если затем использовать item с `nahida_dakimakura:1` вне Nexus/imprinted, advancement `use_nahida` запускает временный `plapper` цикл и один из26 attribute-up outcomes; стоп handler снимает четыре ×0 debuff modifiers, когда advancement стал false. |
| 6 | Случайный другой не-spectator, не союзник получает `love_girl`; владелец получает `girl_seeker`, а выбранному игроку и владельцу рассылается сообщение. Если цель отсутствует, owner всё равно получает tag и предмет расходуется. В игровом цикле seeker запускает эффекты цели через players_2t, airborne target получает импульс в сторону seeker через players_tick; leave_death снимает оба тега. |
| 7 | Временно ставит `cstr` владельцу и создаёт whip loot для каждого другого non-spectator игрока без `cstr` (без фильтра команды); подхватывает соответствующий item entity и записывает UUID получателя в `teleportid`. Убирает временный tag, сообщает и тратит предмет. |
| 8 | Один раз выдаёт loot `cunny_slabs`, затем отдельно для каждого игрока прибавляет `twitching +60` и `cumming +160` и случайно выбирает ровно один из шести звуков; сообщение и расход. |
| 9 | Выбирает X и Z независимо из−1,000,000..1,000,000, телепортирует владельца на Y=320, ставит platform marker на100 выше, даёт Slow Falling60s и выполняет23 команды loot `cunny_slabs`; item consumed. Marker пытается fill-нуть площадку quartz 33×33 и удаляется только при успешном fill. |
| 10 | Три выдачи loot (`luckyblock_all_stacks`, `op_hoe`, `god_dice`), затем убивает одно случайное `@e` по миру без фильтра типа/команды, так что игроки тоже попадают в выбор; сообщает и тратит предмет. |
| 11–20 | Функции содержат только TODO-комментарий, сообщение владельцу и расход одного предмета соответствующего ID; игрового эффекта в командах нет. |
| 21 | До64 случайных `@e` без type/team/range фильтра получают вызов того же cultist helper в своей позиции; каждый успешный выбор создаёт отдельного случайного cultist. При пустом selector вызовов нет, но сообщение и расход выполняются. |

Шанс/геометрия в пунктах 4 и 21 — это повторные команды в исходниках, не «первые несколько» результатов. Функции с точными условиями и всеми SNBT: [выбор Cunny](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/cunny_item/select1.mcfunction) и [cultist template](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/4_spawn_cunny_spawn.mcfunction).

`cunnys/4_spawn_cunny_spawn` выбирает шесть непересекающихся 1/6 ветвей; все summon требуют PersistenceRequired, имеют пустую таблицу смерти и экипировку с drop chance0. Отличия, значимые для боя:

| random | Моб / tag | HP и оружие | Особые атрибуты |
|---:|---|---|---|
| 1 | wither skeleton `strangeness` | 30; bow | follow_range100, scale0.7, fall damage0 |
| 2 | skeleton `chaos` | 50; heart of the sea | follow_range100, scale0.55, fall damage0 |
| 3 | skeleton `luck`,`klee` | 40; totem | follow_range100, knockback resistance10, scale0.65, explosion knockback resistance100, fall damage0; boots blast protection99 |
| 4 | skeleton `omnipresence` | 40; bow | follow_range100, scale0.65, fall damage0 |
| 5 | skeleton `obsession` | 10; diamond axe | follow_range100, attack damage11, speed0.25, scale0.6, fall damage0 |
| 6 | zombie `hypocrisy` | 60; two iron swords | follow_range100, speed0.2, scale0.6, fall damage0 |

[Helper с подбором команды/score и вызовом on_summon](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/4_spawn_cunny.mcfunction). Item `nahida_dakimakura` после выдачи имеет самостоятельный use-advancement: [trigger](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/use_nahida.json), [handler](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/5_plapping.mcfunction), [stop](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/5_plapping_stop.mcfunction).

### Рунные предметы

Обычный вход для кастов — `workingitems/raycast_spells/select` по `SelectedItem.components.minecraft:custom_data.rune_*`; cast-функции ниже сразу тратят ровно один matching item или делают это при попадании. Fehu/Gifu продолжают луч только пока нет локального stopper-а. Для проверок с `dx=0,dy=0,dz=0` размер куба остаётся1×1×1; `distance` пересекается с этой областью, не расширяет её до сферы.

| Rune | Полное действие, цель, расход и reset |
|---|---|
| Ansuz | Удаляет одну `rune_ansuz` и сразу переключает исполнителя в creative; другого reset в helper нет. |
| Fehu | Рекурсивный луч шагает1 блоком, счётчик ограничен200; останавливается на non-air либо non-spectator mob в совмещённом 1×1×1 cube/distance15. Продолжение также блокирует игрок с `nomagic_active` в16 и stopper marker в20. На конце ставит `lbc:item_gallery` со сдвигом(−19,0,−19), удаляет одну rune и завершает Distance. Закомментированный summon `itemkiller` не входит в cast; standalone helper `fehu_itemkiller` лишь считает item entities в20 и убивает их, если их100+. |
| Gifu | Луч шагает0.33, предел Distance200; конец — первый non-air (команда смещает точку проверки назад0.33) либо non-spectator mob в совместном кубе1×1×1 и `distance..7`. Его рекурсию также блокируют `nomagic_active` игрок в7 и stopper в20. При конце rune расходуется и в текущей точке выполняются20 отдельных `loot spawn lbc:random_glack_1`. |
| Hagalaz | Тратит rune, вызывает внешний `swrg:maploader/clear`, убивает все dropped item entities и пишет всем красный subtitle. Четыре одинаковые kill-команды повторены, результат после первой одинаков. |
| Kenaz | Тратит rune и телепортирует все entity в измерение Nether с сохранением координат/угла через `execute as @e[distance=0.01..,tag=!core_marker,tag=!strict_map_object] at @s in minecraft:the_nether run tp @s ~ ~ ~ ~ ~`; нет type/team/range cap. |
| Nautiz | Тратит rune, рассылает всем subtitle, даёт Resistance V на10s, глобально выключает fall damage, переносит всех игроков в (0,1000,0), запускает `swrg:game/quickstart/1` и четыре раза убивает all entities кроме игроков/marker-ов. `keep_inventory` и fall damage — глобальные gamerules, эта функция их не возвращает. |
| Raido | Тратит rune, ставит fake-player score `bottom_nexus=1` и показывает всем subtitle; сам helper не переносит игроков и не вызывает maploader. |
| Thurisaz | Сначала сохраняет команду caster-а, уменьшает всем mobs (кроме союзников, spectator и `strict_map_object`) в0.01..50 десять характеристик почти до нуля и ставит tag `submissive`; затем тратит rune и владельцу добавляет modifiers: scale/attack/max-health/speed ×2, блок/сущность interaction range ×3, остальные перечисленные движения/высота/копание ×2; fall damage ×0.5. Tag `gigantic` включает player-tick helper: mobs в1 блоке получают100 falling-anvil damage на каждом проходе. Общий `resets.mcfunction` удаляет owner и negative modifiers, `gigantic` и связанные tags. |
| Uruz | Тратит rune, заменяет owner base attack damage на огромное literal integer (значение в source) и даёт Resistance V infinite. Сброс идёт через общий reset/death lifecycle, не локальный timer. |
| Wunjo | Тратит rune, показывает subtitle, добавляет владельцу16 `swrg.lives`, глобально включает `keep_inventory=true`; helper сам не возвращает gamerule в прежнее значение. |

Источники: [selector](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/raycast_spells/select.mcfunction), [Fehu hit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/fehu_cast_end.mcfunction), [Gifu reward](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/gifu_cast_end.mcfunction), [Thurisaz owner](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/thurisaz_cast.mcfunction), [Thurisaz targets](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/thurisaz_cast_others.mcfunction), [per-pass stomp](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/thurisaz_gigant_tick.mcfunction).

### Области, slab markers и генераторы

- `imbaxe_use` обходит выбранные entity-центры в измерении кроме Nexus, если игрок в 120 блоках, и применяет 216 `fill replace` команд: 215 block IDs и дополнительная селекторная палитра `#minecraft:leaves`, которая суммируется в выход `stick` в кубе 31³ на каждый центр. 215 уникальных block IDs удаляются `strict`; найденные количества складываются в score, затем 217 give-команд покрывают216 разных item ID (`bamboo_mosaic` указан дважды); счётчик всех листьев возвращается как `stick`, а три грибных блока получают отдельные имена/lore/model при выдаче. Контейнерное содержимое код отдельно не сохраняет. За полным command list — [manifest](inventory/review_other_imba_manifest.json).
- `blood_slab/place` идёт лучом от глаз шагами0.025; предел Distance1000 означает максимум25 блоков, не1000. Первый неблок-воздух создаёт raycast marker. В `tick_move` он размножается до шести гранных соседей, если блок не `#lbc:blood_slabbed` и нет другого маркера рядом, затем заменяет текущую клетку одним из32 исходов. При 5% и воздушных клетках на высотах1..5 запускает mob-ветку: 10 равновероятных вариантов, с piglin на двух номерах; после появления она бьёт на20 arrow mobs в радиусе4 (кроме `#lbc:bloody_slabby`) и добавляет до восьми диагональных соседей.
- `cum_slab/select` диспетчерит только marker с `central`/`down` и всегда удаляет сам marker после обработки. Central ставит нижнюю birch slab; если блок снизу занят, четыре свободных горизонтальных воздушных соседства получают central marker-ы, а если снизу воздух — ставится down marker. Down ставит double slab только если блок доступен; при неудаче marker удаляется, при успехе опускается на1 в следующий air и повторяет. Упёршись в занятое место, он создаёт до четырёх side marker-ов и безусловно ставит четыре горизонтальных double slab-а.
- `cunny_jungle` marker за вызов ставит mega jungle tree и выбирает один из16 случаев: `sob` — 4/16; angry/splurt/sweat — по4/16, каждый с четырьмя ориентациями. Затем призывает четыре correction marker-а; каждый сдвигает X/Z на10, ставит barrel-случай `cunny_slabs` и waxed sign с двумя независимо выбранными наборами по четыре строки (полная таблица 100×4 строк выше), после чего удаляет correction marker. Tick вызывает grassifier и с шансом1% убивает все cunny_jungle markers.
- `most` marker продвигается на 4 блока каждый update и ставит шаблон позади себя; с шансом50% строит до пяти кварцевых колонн вокруг случайной позиции, с шансом5% — парную кварцевую дорожку, а при каждом `Lifetime % 7 == 0` добавляет колонну из одного из шести шаблонов до нижней границы Y=10. Caller обрабатывает его только при игроке в 200 блоках; сама `most_tick` не убивает marker.

### Цепная молния и хаотические инструменты

`chain_lightning` приходит из активного эффекта и стрелы, выбирает ближайшего допустимого моба в100, а ray-цепочка проверяет контакт каждые0.5 блока, ветвясь к до10 ближайшим допустимым мобам в100. Цепь не проверяет блоки на пути. В начале caster помечает своих ally по `lbcID2` или `team_number` глобально у всех игроков, без радиуса; они исключаются из целей, затем обе защиты глобально удаляются. На каждом `hit` удаляется этот tag, создаются три shield-blockable marker-а на10 tick и наносится16 arrow damage с одним защищённым игроком как источником урона. Счётчик эффекта уменьшается внешним handler-ом, который прекращает вызовы при0 или stopper в20; у helper-а нет собственной оплаты маны.

`chaos_hoe` активируется advancement-ом изменения прочности выбранного предмета с `custom_data.chaos_hoe:1`. Луч делает до100 шагов по0.1 (около10 блоков), и при каждом шаге проверяет свежий item entity `Age:0s` в1 блоке; найдя его, берёт `Item.id`, выравнивается на центр item-блока и запускает macro flood-fill. Flood-fill стирает центр командой `fill ... air destroy`, после чего рекурсивно ищет тот же block ID во всех26 соседних клетках; уже очищенные позиции естественно прерывают возврат. `chaos_hoe_use_rc_end` сбрасывает Distance в0, а caller продолжает обработку — пока вблизи остаётся свежий предмет возможны повторные срабатывания того же helper-а в одном лучевом проходе. Advancement отзывается после завершения cast; команда не очищает сам инструмент.

`chaos_pic` даёт Resistance V на1s, идёт максимум300 шагов по1 блоку сквозь воздух и на первом неблоковом блоке вызывает `dickcockgalactic`: TNT с `fuse:-1` и 70% шанс заменить блок контакта воздухом с drop-ом. Advancement `used_chaos_pic` отзывается после raycast; отдельного расхода предмета в этих командах нет.

`chaos_sho_use` снимает advancement после запуска420 `ray_pre` команд: локальные offsets составляют только28 комбинаций x=−3..3 и y=−1..2, каждая повторяется ровно15 раз. Лучи наследуют алгоритм piston sword: каждый проходит воздух шагом0.33 до Distance200 (около66 блоков), а при первом блоке выполняет clone/clear-ветку `ray_2_r`. Существенная общая зависимость: эта ветка читает fake-player `if lbc.math` и сразу возвращает fail при0; `ray_pre` для хаотической лопаты `if` не устанавливает, поэтому результат зависит от прежнего глобального значения, установленного, например, `piston_sword/click`. В `ray_2_r` при ненулевом флаге clone переносит блок на шаг вперёд, удаляет исходный и при неудачном состоянии вызывает clear следующего блока; это не обычный single-block hit.

`delta_tnt` запускает26 `delta_ex` marker-ов вокруг позиции и воспроизводит шесть primed-TNT звуков; каждый marker получает Lifetime1000. На каждом вызове `delta_ex_tick` выполняются20 отдельных `chance1` проверок, а не один выбор: 19 независимых возможных summon-ветвей (TNT fuse0/100/1000 ticks, с NoGravity/тегами, lightning и fireballs ExplosionPower1/5/10/15/20) плюс chance1-вызов `workingitems/exoperso4` к случайному marker-у `delta_ex` в10. При Lifetime1000 все fireball сущности проходят внешнюю `raycast_vpered_power`; эффект продолжается только пока marker dispatcher вызывает tick и marker существует. `dickcockgalactic` можно запускать отдельно; его TNT имеет отрицательный short fuse −1, а разрушение контактного блока отдельно бросается с шансом70.

Источники: [chain entry](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/chain_lightning.mcfunction), [chain branch](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/chain_lightning_loop_at.mcfunction), [hoe ray](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/chaos_hoe_use_rc.mcfunction), [hoe fill recursion](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/chaos_hoe_rec.mcfunction), [pic ray](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/chaos_pic_use_rc.mcfunction), [420 shovel commands](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/chaos_sho_use.mcfunction), [piston hit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/piston_sword/ray_2_r.mcfunction), [delta setup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/delta_tnt.mcfunction), [delta tick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/delta_ex_tick.mcfunction).

### Midas, Most и остановка времени

`raycastselector_imba` dispatches marker `midas_block` to a branch that sets its current block to raw gold, writes command text `midas` into `storage lbc.math cmd`, sends it through `run_command`, and kills the marker. Six neighbor-spawn/entity-conversion commands in `midas_block` are comments, so that branch does not perform the planned recursive spread. Direct manual `midas_block_entity` sets only its current block raw gold; its later tests use `@s[dx=0,dy=0,dz=0]` after moving command position by one or two blocks, so the executor's own bounding box lies outside those 1×1×1 cubes and the extra gold placements do not run. It removes temporary `hyi` and kills executor unless tagged `sin_greed_active`. Both `midas_block_entity_rec` functions contain only commented commands and do nothing when run.

`most_spawn` is reached from snowball projectile handling when the snowball item's custom_data has `most_imba:1`; it creates and orients an `imba_most` marker from the nearest non-spectator player with `snowball>=1`. Runtime dispatch proceeds only while a non-spectator player is within200; each call moves the marker four blocks forward and places template `lbc:most` at offset(−4,−10,−4). Two random tests run independently each update: chance50 chooses a five-branch quartz-pillar construction, and chance5 chooses a two-direction quartz path. Every 7th Lifetime triggers a vertical column stack while Y score remains10..319: choose template1..6, place it at Y−15, lower the score4 and recurse at Y−5. Quartz pillar/lantern columns choose x side +8..+16 or −16..−8 and y offset−20..10, run five branches, each placing pillar+lantern and extending upward at90% while the executor remains within20. The other structure chooses rotation0/90 and x,z−10..10,y−20..20, then draws two mirrored rows; each step fills a 5×5 chiseled-quartz layer and 3×3 quartz-brick layer, sometimes a sea lantern, and continues every5 blocks while within200 and its 80% recursion succeeds. No local end/kill command is present in `most_tick`; the dispatch's player-radius gate is the only in-family continuation guard.

`time_stopped_lol` branches on entity type. For non-player it removes the entire `Tags` and `active_effects` NBT lists and sets NoAI, Invulnerable, NoGravity, Silent; no restoration function appears in these helpers. For a player it adds `time_stopped_lol`, stores position/rotation×100 in global `storage lbc.math` under a key derived from scoreboard `lbcID2`, and clears effects. The player tick handler repeatedly clears effects, grants Resistance V/Weakness CXXVIII/Slowness CXXVIII for1s and teleports back to that saved pose; `leave_death` is the only removal of the tag found in the studied function graph; expiry of the effect counter alone does not end repeat teleports, so absent that cleanup the tag can persist. This tick does not restore saved pose or other effects. The effect wrapper stops invoking the helper at a `stopper` marker within20; it decrements the external effect counter each pass.

### Прочие ветви и незавершённые helpers

`place_cum_slab` — advancement entry: raycast от глаз идёт .025 блока за шаг максимум1000 раз (до25 блоков), на первом non-air alignment ставит `cum_slab` down marker; дальше включается тот же `cum_slab/select` цикл, но исходная руна этим helper-ом не очищается. `imbaxe_use` — отдельный advancement при изменении прочности предмета `imbaxe:1`; исключает nexus/imprinted и creative. В измерении вне Nexus он запускает сбор на подходящих entity-центрах, если не-spectator player есть в120; полная палитра 216 `fill replace` записывает найденные количества, которые макрос возвращает 216 unique item IDs (217 give-команд, в том числе дополнительный stick; chest/barrel содержимое отдельно не сохраняется). `tmp` при ручном запуске только обнуляет212 scoreboard-флагов семейства Imbaxe; выдачи/изменения мира нет, активный caller не найден в функциях job.

Источник: [place cum entry](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/place_cum_slab.mcfunction), [Most dispatch](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/raycastselector_imba.mcfunction), [Midas branch](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/midas_block.mcfunction), [time-stop player update](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/time_stopped_lol_player_tick.mcfunction), [Imbaxe entry](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/imbaxe_use.mcfunction), [Imbaxe collection](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/imbaxe_use_at.mcfunction), [Imbaxe return macro](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/imbaxe_use_macro.mcfunction), [standalone reset](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/tmp.mcfunction).

### Bad Apple и God Hedgehog

У bad-apple есть два разных предметных входа. Съеденный custom-data `bad_apple:1` вызывает `other/bad_apple`: выдаёт Absorption, Conduit Power, Fire Resistance, Haste, Health Boost, Invisibility, Night Vision, Regeneration, Resistance, Saturation, Slow Falling, Speed, Strength, Water Breathing по300s и Instant Health30s, затем отзывает advancement. Отдельный dropped item с `bad_apple_imba:1` помечается `items_tick_tag`/`items_tick_act` и проходит `item_tick`: после10 вызовов получает NoGravity, Glowing, MotionY=0.1; при Distance60 (3s только если updater зовёт эту функцию каждый server tick) создаёт badapple item_display, поворачивает к ближайшему non-spectator игроку и удаляет исходный item. Его raycast dispatcher запускает `display_tick`, где создаются шесть временных лучей, играет звук, вызывается macro `badapple start`, выставляется global score, ставится остановка через219s; сами шесть display немедленно убиваются, marker тоже. Плановая `end` вызывает macro `badapple stop`, удаляет gravity modifier у всех игроков и сбрасывает score. `players_second` содержит урон над black concrete и levitation над white concrete, но её единственный найденный вызов закомментирован — это не активный эффект цикла. [item handlers](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/bad_apple/item_tick.mcfunction), [scheduled start](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/bad_apple/display_tick.mcfunction), [end](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/bad_apple/end.mcfunction), [consumed apple](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/bad_apple.mcfunction).

God Hedgehog `tick` вызывается для `silverfish[tag=god_hedgehog]`: при Y≤0 существо переносится на (0,250,0), каждое обновление получает Slow Falling100s, а live God Hedgehog silverfish считаются через `execute store result ... if entity` (записывается число сущностей, не boolean). При счёте ≥2 текущий executor убивается. Глобальный `ticker % 500` выбирает ветку: 1 телепортирует и убивает прочих silverfish без тега; 10/20/30/40/50 запускают laser; 100..140 — dimension scrap; окна 240..280, 281..320, 321..360, 361..400, 401..440 вызывают по одному macro-варианту белой стрелы на случайном не-spectator игроке для каждого вошедшего tick.

| Ветка | Полный эффект и остановка |
|---|---|
| laser (`death_laser` → `laser_rc`) | Сбрасывает Distance, даёт владельцу Resistance V на1s, выбирает ближайшего допустимого моба в200 от глаз и рекурсивно создаёт 101 TNT с `fuse:0,explosion_power:10` с шагом3 (до300 блоков); затем обнуляет Motion и смещает silverfish на15 блоков, повернув к (0,150,0). Без цели raycast-часть не запускается. |
| dimension scrap (`spawn_dim_scrap`) | Создаёт item_display с моделью `lbc:hedgegod_eater` на10 блоков перед глазами, проигрывает звук и опускает silverfish на2 блока; дисплей продолжает цикл через `raycastselector_rare`/`hedgegod_eater_tick`. |
| eater-display цикл | Distance растёт на1 за вызов; на1 меняется масштаб, на3 с шансом96% создаётся дочерний display у ближайшей допустимой цели, на79 scale умножается ещё на20, на80 ставится `lbc:air_sphere_7x7x7` и мобам в4 наносится10 `outside_border`, на81 scale становится практически нулевым из-за целочисленного `data get`, а на83+ display удаляется. |
| white-arrow `hedgegod_white_arrow_tick` | При `Lifetime=40` вызывает `hedgegod_white_arrow_shoot`: звук/частицы, marker с Rotation90° и данными lifetime20, затем удаление текущего executor. Следующие шаги marker зависят от общих raycast-функций. |
| white-arrow формы | Каждая из пяти форм вызывает общий макрос создания одного display; параметры `i/j/k/r` различаются. Форма1 берёт `ticker%20` с scale0.1 (store в int округляет); форма2 — `ticker×7 mod180−90`; форма3 выбирает k от−10 до10; форма4 — `ticker×45 mod180−90`. Обычный и четыре формы используют пять окон ticker выше. |

Источники: [tick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/tick.mcfunction), [laser](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/death_laser.mcfunction), [raycast](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/laser_rc.mcfunction), [eater cycle](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/hedgegod_eater_tick.mcfunction), [eater burst](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/hedgegod_eater_boom.mcfunction), [arrow shoot](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/hedgegod_white_arrow_shoot.mcfunction), [white-arrow macro](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/spawn_white_arrows1.mcfunction).

## Файлы: other / artifact

### `lbc:other/artifact/afkartifact5t`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_5t.mcfunction`; условие вызова: `execute if entity @s[tag=afkartifact5t_active] unless entity @e[type=minecraft:marker,distance=..20,tag=stopper_magic,limit=1] run function lbc:other/artifact/afkartifact5t`. Передаёт шаг в `lbc:other/artifact/nofluid`, `lbc:other/artifact/colors`, `lbc:other/artifact/metabolism`, `lbc:other/artifact/noeffects1`, `lbc:other/artifact/nohp`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/afkartifact5t.mcfunction)

### `lbc:other/artifact/bigjump`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/when_jump.mcfunction`; условие вызова: `execute at @s[tag=!spectator,nbt={Inventory:[{components:{"minecraft:custom_data":{mystical:3,type:2}}}]}] run function lbc:other/artifact/bigjump`. Эффект: @e[distance=..7,tag=!spectator] получает `minecraft:levitation` на 1 секунд, amplifier 10; @s получает `minecraft:slow_falling` на 10 секунд, amplifier 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/bigjump.mcfunction)

### `lbc:other/artifact/block_otverditel`
`nofluid` вызывает этот helper для каждого falling_block в пределах 16; функция читает исходный `BlockState`, заменяет песок на sandstone, гравий на cobblestone, красный песок на red sandstone, все три состояния наковальни на iron block, а concrete powder всех 16 цветов — на concrete того же цвета. Stone bricks и netherite block выставляются тем же блоком. После сопоставления проигрываются дым и звук тушения; исходный falling_block удаляется. Полный список 26 проверок: [исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/block_otverditel.mcfunction).

### `lbc:other/artifact/colors`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/afkartifact5t.mcfunction`; условие вызова: `execute at @s[tag=!spectator,nbt={Inventory:[{components:{"minecraft:custom_data":{mystical:1,type:7}}}]}] if block ~ ~-0.5 ~ #minecraft:colored positioned ~ ~-0.5 ~ run function lbc:other/artifact/colors`. Передаёт шаг в `lbc:other/artifact/colors/red`, `lbc:other/artifact/colors/white`, `lbc:other/artifact/colors/blue`, `lbc:other/artifact/colors/yellow`, `lbc:other/artifact/colors/orange`, `lbc:other/artifact/colors/light_blue`, `lbc:other/artifact/colors/purple`, `lbc:other/artifact/colors/gray`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors.mcfunction)

### `lbc:other/artifact/corrosion`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/damaged.mcfunction`; условие вызова: `execute if entity @s[tag=!spectator,nbt={Inventory:[{components:{"minecraft:custom_data":{mystical:4,type:3}}}]}] run function lbc:other/artifact/corrosion`. Команда: `place template lbc:air11x11 ~-5 ~-8 ~-5 none none 0.5`; `particle minecraft:reverse_portal ~ ~ ~ 1 1 1 1 30 normal`; `playsound minecraft:block.portal.trigger master @a ~ ~ ~ 1 2`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/corrosion.mcfunction)

### `lbc:other/artifact/damaged`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/items/damaged.mcfunction`; условие вызова: `execute at @s if items entity @s container.* *[minecraft:custom_data~{mystical:4}] unless entity @e[type=minecraft:marker,distance=..20,tag=stopper_magic,limit=1] run function lbc:other/artifact/damaged`. Передаёт шаг в `lbc:other/artifact/explode`, `lbc:other/artifact/corrosion`, `lbc:other/artifact/weakall`, `lbc:other/artifact/speed`, `lbc:other/artifact/wolf`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/damaged.mcfunction)

### `lbc:other/artifact/explode`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/damaged.mcfunction`; условие вызова: `execute if entity @s[tag=!spectator,nbt={Inventory:[{components:{"minecraft:custom_data":{mystical:4,type:2}}}]}] run function lbc:other/artifact/explode`. Эффект: @a[distance=..7,tag=!spectator] получает `minecraft:resistance` на 1 секунд, amplifier 4; @s получает `minecraft:slow_falling` на 5 секунд, amplifier 0. Создаёт `tnt` с {fuse:0}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/explode.mcfunction)

### `lbc:other/artifact/highspeed`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/when_jump.mcfunction`; условие вызова: `execute at @s[tag=!spectator,nbt={Inventory:[{components:{"minecraft:custom_data":{mystical:3,type:1}}}]}] run function lbc:other/artifact/highspeed`. Эффект: @s получает `minecraft:speed` на 3 секунд, amplifier 5; @e[distance=0.01..7,tag=!spectator,predicate=!lbc:same_team] получает `minecraft:slowness` на 3 секунд, amplifier 2. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/highspeed.mcfunction)

### `lbc:other/artifact/kombojump`
Jump-event для артефакта `mystical:3,type:3`. Функция проверяет текущий Jump Boost: amplifier 32 даёт Slow Falling 30 секунд и Jump Boost amplifier 64 на 8 секунд; 16 — Slow Falling 20 секунд и Jump Boost 32 на 10 секунд; 8/4/2/1 соответственно повышаются до усилителей 16/8/4/2 на 10 секунд; любой Jump Boost также даёт усилитель 1 на 10 секунд. Вне зависимости от ветки последняя команда пытается дать Jump Boost I на 10 секунд; звук и poof проигрываются каждый вызов. Источник: [kombojump](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/kombojump.mcfunction).

### `lbc:other/artifact/metabolism`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/afkartifact5t.mcfunction`; условие вызова: `execute at @s[tag=!spectator,nbt={Inventory:[{components:{"minecraft:custom_data":{mystical:1,type:8}}}]}] run function lbc:other/artifact/metabolism`. Эффект: @s получает `minecraft:hunger` на 1 секунд, amplifier 2; @s получает `minecraft:regeneration` на 1 секунд, amplifier 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/metabolism.mcfunction)

### `lbc:other/artifact/noarrows`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_tick.mcfunction`; условие вызова: `execute if entity @s[tag=no_arrows_active] run function lbc:other/artifact/noarrows`. Завершение: удаляет сущность (`@e[type=#minecraft:arrows,distance=..16]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/noarrows.mcfunction)

### `lbc:other/artifact/noarrows_old`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/new_game/legacy_tick_player.mcfunction`; условие вызова: `execute at @s[nbt={Inventory:[{id:"minecraft:repeating_command_block",components:{"minecraft:custom_data":{mystical_old:1,type:4}}}]}] run function lbc:other/artifact/noarrows_old`. Создаёт `item` с {Item:{id:"minecraft:arrow",count:1}}. Завершение: удаляет сущность (`@e[type=#minecraft:arrows,distance=..4]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/noarrows_old.mcfunction)

### `lbc:other/artifact/noeffects1`
Пятитиковый пассив для артефакта `mystical:1,type:5`: очищает у владельца blindness, slowness, weakness, mining fatigue, glowing, hunger, instant damage, nausea, poison, unluck и wither. Levitation снимается только если нет `reactive:1` на ботинках, `technomk2` на ботинках/поножах, `techno:1` на поножах и `reactive_shish:1` ни в armor slots, ни в руках. Поэтому активная техника сохраняет levitation-ветку, остальные перечисленные эффекты всё равно удаляются. [Источник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/noeffects1.mcfunction).

### `lbc:other/artifact/noeffects1_old`
Legacy-пассив раз в секунду очищает у владельца восемь эффектов: blindness, slowness, weakness, strength, levitation, slow falling, haste и mining fatigue. Это старый `mystical_old:1,type:1` предмет; функция не снимает другие эффекты и не расходует предмет. [Источник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/noeffects1_old.mcfunction).

### `lbc:other/artifact/nofluid`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/afkartifact5t.mcfunction`; условие вызова: `execute at @s[tag=!spectator,nbt={Inventory:[{components:{"minecraft:custom_data":{mystical:1,type:3}}}]}] run function lbc:other/artifact/nofluid`. Изменяет блоки: fill ~12 ~12 ~12 ~-12 ~-12 ~-12 minecraft:basalt replace minecraft:lava; fill ~12 ~12 ~12 ~-12 ~-12 ~-12 minecraft:frosted_ice replace minecraft:water; fill ~12 ~12 ~12 ~-12 ~-12 ~-12 minecraft:snow_block replace minecraft:powder_snow. Передаёт шаг в `lbc:other/artifact/block_otverditel`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/nofluid.mcfunction)

### `lbc:other/artifact/nofluid_old`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/new_game/legacy_tick_5t_player.mcfunction`; условие вызова: `execute at @s[nbt={Inventory:[{id:"minecraft:repeating_command_block",components:{"minecraft:custom_data":{mystical_old:1,type:6}}}]}] run function lbc:other/artifact/nofluid_old`. Изменяет блоки: fill ~2 ~2 ~2 ~-2 ~-2 ~-2 minecraft:basalt replace minecraft:lava; fill ~2 ~2 ~2 ~-2 ~-2 ~-2 minecraft:frosted_ice replace minecraft:water. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/nofluid_old.mcfunction)

### `lbc:other/artifact/nohp`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/afkartifact5t.mcfunction`; условие вызова: `execute at @s[tag=!spectator,nbt={Inventory:[{components:{"minecraft:custom_data":{mystical:1,type:6}}}]}] run function lbc:other/artifact/nohp`. Эффект: снимает эффекты с @s. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/nohp.mcfunction)

### `lbc:other/artifact/nohp_old`
Legacy-пассив `mystical_old:1,type:3`, вызываемый раз в секунду: снимает regeneration, wither, poison, instant damage, instant health, absorption и health boost с владельца. Отдельного порога здоровья или расхода предмета в функции нет. [Источник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/nohp_old.mcfunction).

### `lbc:other/artifact/nomagic`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_2t.mcfunction`; условие вызова: `execute at @s[tag=nomagic_active] run function lbc:other/artifact/nomagic`. Завершение: удаляет сущность (`@e[distance=..25,tag=magic]`); удаляет сущность (`@e[type=#minecraft:magic_entities,distance=..25]`); удаляет сущность (`@e[distance=..25,tag=hedgehog]`); удаляет сущность (`@e[distance=..25,tag=magicalsheep]`); удаляет сущность (`@e[distance=..25,tag=summactivated]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/nomagic.mcfunction)

### `lbc:other/artifact/nomagic_old`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/new_game/legacy_tick_5t_player.mcfunction`; условие вызова: `execute at @s[nbt={Inventory:[{id:"minecraft:repeating_command_block",components:{"minecraft:custom_data":{mystical_old:1,type:2}}}]}] run function lbc:other/artifact/nomagic_old`. Завершение: удаляет сущность (`@e[type=minecraft:area_effect_cloud,distance=..4]`); удаляет сущность (`@e[type=minecraft:evoker_fangs,distance=..4]`); удаляет сущность (`@e[type=minecraft:experience_bottle,distance=..4]`); удаляет сущность (`@e[type=minecraft:experience_orb,distance=..4]`); удаляет сущность (`@e[type=minecraft:dragon_fireball,distance=..4]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/nomagic_old.mcfunction)

### `lbc:other/artifact/notnt`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_2t.mcfunction`; условие вызова: `execute at @s[tag=notnt_active] run function lbc:other/artifact/notnt`. Передаёт шаг в `lbc:other/stoppernuke`. Завершение: удаляет сущность (`@e[type=#minecraft:unstable,distance=..24]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/notnt.mcfunction)

### `lbc:other/artifact/notnt_old`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/new_game/legacy_tick_5t_player.mcfunction`; условие вызова: `execute at @s[nbt={Inventory:[{id:"minecraft:repeating_command_block",components:{"minecraft:custom_data":{mystical_old:1,type:5}}}]}] run function lbc:other/artifact/notnt_old`. Создаёт `item` с {Item:{id:"minecraft:tnt",count:1}}; `item` с {Item:{id:"minecraft:fire_charge",count:1}}; `item` с {Item:{id:"minecraft:end_crystal",count:1}}. Завершение: удаляет сущность (`@e[type=minecraft:creeper,distance=..5]`); удаляет сущность (`@e[type=minecraft:tnt,distance=..5]`); удаляет сущность (`@e[type=minecraft:fireball,distance=..6]`); удаляет сущность (`@e[type=minecraft:end_crystal,distance=..7]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/notnt_old.mcfunction)

### `lbc:other/artifact/ondamage`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/items/swords.mcfunction`; условие вызова: `execute at @s[nbt={Inventory:[{components:{"minecraft:custom_data":{mystical:2}}}]}] unless entity @a[gamemode=!spectator,distance=..20,tag=nomagic_active,limit=1] unless entity @e[type=minecraft:marker,distance=..20,tag=stopper_magic,limit=1] run function lbc:other/artifact/ondamage`. Эффект: @e[distance=0.01..8,tag=!spectator,nbt={HurtTime:10s},predicate=!lbc:same_team] получает `minecraft:poison` на 20 секунд, amplifier 4. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number. Передаёт шаг в `lbc:other/artifact/vampirism`, `lbc:other/artifact/smoke`, `lbc:other/artifact/speedup`, `lbc:other/artifact/reducecooldown`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/ondamage.mcfunction)

### `lbc:other/artifact/ondamage_old`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/items/swords.mcfunction`; условие вызова: `execute at @s[nbt={Inventory:[{components:{"minecraft:custom_data":{mystical_old:2}}}]}] run function lbc:other/artifact/ondamage_old`. Эффект: @s[nbt={Inventory:[{components:{"minecraft:custom_data":{mystical_old:2,type:1}}}]}] получает `minecraft:regeneration` на 1 секунд, amplifier 1; @s[nbt={Inventory:[{components:{"minecraft:custom_data":{mystical_old:2,type:1}}}]}] получает `minecraft:saturation` на 1 секунд, amplifier 1; @s[nbt={Inventory:[{components:{"minecraft:custom_data":{mystical_old:2,type:2}}}]}] получает `minecraft:strength` на 2 секунд, amplifier 0; @e[distance=..8] получает `minecraft:wither` на 4 секунд, amplifier 10; @e[distance=..8] получает `minecraft:regeneration` на 4 секунд, amplifier 2. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/ondamage_old.mcfunction)

### `lbc:other/artifact/reducecooldown`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/ondamage.mcfunction`; условие вызова: `execute at @s[tag=!spectator,nbt={Inventory:[{components:{"minecraft:custom_data":{mystical:2,type:6}}}]}] run function lbc:other/artifact/reducecooldown`. Состояние scoreboard: scoreboard players add @s mana 1000. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/reducecooldown.mcfunction)

### `lbc:other/artifact/shield`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/items/damaged_by_player.mcfunction`; условие вызова: `execute if entity @s[gamemode=!spectator] at @s unless score time_stopped lbc.math matches 1.. run function lbc:other/artifact/shield`. Эффект: @s получает `minecraft:resistance` на 2 секунд, amplifier 4. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/shield.mcfunction)

### `lbc:other/artifact/smoke`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/ondamage.mcfunction`; условие вызова: `execute at @s[tag=!spectator,nbt={Inventory:[{components:{"minecraft:custom_data":{mystical:2,type:3}}}]}] run function lbc:other/artifact/smoke`. Эффект: @a[gamemode=!spectator,distance=0.01..7] получает `minecraft:blindness` на 5 секунд, amplifier 0. Меняет атрибут: @s minecraft:follow_range base set 3. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/smoke.mcfunction)

### `lbc:other/artifact/speed`
При событии `damaged` для `mystical:4,type:5` проверяются имеющиеся эффекты и накладываются их пары: Fire Resistance → Regeneration II на 10 секунд; Resistance → Fire Resistance I на 40 секунд и Fire Resistance II на 10 секунд; Speed → Resistance I на 5 секунд; Strength → Speed III на 5 секунд. Каждое условие независимое, а Strength I на 5 секунд выдаётся безусловно; проигрываются взмах и звук дракона. Эффекты могут перекрывать друг друга по стандартному правилу силы/длительности. [Источник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/speed.mcfunction).

### `lbc:other/artifact/speedup`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/ondamage.mcfunction`; условие вызова: `execute if entity @s[tag=!spectator,nbt={Inventory:[{components:{"minecraft:custom_data":{mystical:2,type:4}}}]}] as @e[distance=0.01..8,tag=!spectator,nbt={HurtTime:10s},predicate=!lbc:same_team] at @s rotated ~ 0 run function lbc:other/artifact/speedup`. Команда: `particle minecraft:firework ~ ~1 ~ 0.5 0.7 0.5 0.02 5 normal`; `execute positioned ^ ^ ^1 run particle minecraft:firework ~ ~1 ~ 0.5 0.7 0.5 0.02 5 normal`; `execute positioned ^ ^ ^2 run particle minecraft:firework ~ ~1 ~ 0.5 0.7 0.5 0.02 5 normal`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/speedup.mcfunction)

### `lbc:other/artifact/vampirism`
Реакция `ondamage` для `mystical:2,type:1`: владелец получает Regeneration III и Saturation V на 2 секунды, `food_count` увеличивается на 1. Всем не-spectator противникам вне своей команды в distance 0.01..7 с `HurtTime:10s` выдаются Wither V и Hunger amplifier 99 на 2 секунды; совпадение `HurtTime` выбирает цель непосредственно после удара. В конце — сердца и звук; предмет не расходуется этим helper-ом. [Источник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/vampirism.mcfunction).

### `lbc:other/artifact/weakall`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/damaged.mcfunction`; условие вызова: `execute if entity @s[tag=!spectator,nbt={Inventory:[{components:{"minecraft:custom_data":{mystical:4,type:4}}}]}] run function lbc:other/artifact/weakall`. Эффект: @e[distance=..7,tag=!spectator] получает `minecraft:weakness` на 3 секунд, amplifier 9. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/weakall.mcfunction)

### `lbc:other/artifact/when_jump`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/items/armor.mcfunction`; условие вызова: `execute at @s[nbt={Inventory:[{components:{"minecraft:custom_data":{mystical:3}}}]}] unless entity @e[type=minecraft:marker,distance=..20,tag=stopper_magic,limit=1] unless entity @a[gamemode=!spectator,distance=..20,tag=nomagic_active] run function lbc:other/artifact/when_jump`. Передаёт шаг в `lbc:other/artifact/highspeed`, `lbc:other/artifact/bigjump`, `lbc:other/artifact/kombojump`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/when_jump.mcfunction)

### `lbc:other/artifact/wolf`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/damaged.mcfunction`; условие вызова: `execute if entity @s[tag=!spectator,nbt={Inventory:[{components:{"minecraft:custom_data":{mystical:4,type:6}}}]}] run function lbc:other/artifact/wolf`. Создаёт `wolf` с {DeathLootTable:"empty",Sitting:0b,CollarColor:14b,Tags:["summoned"],attributes:[{id:"minecraft:follow_range",base:200},{id:"minecraft:movement_speed",base:0.5},{id:"minecraft:atta. Обновляет NBT/components: modify entity @e[tag=summoned,sort=nearest,limit=1] Owner set from entity @s UUID. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/wolf.mcfunction)

## Файлы: other / artifact / colors

### `lbc:other/artifact/colors/black`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors.mcfunction`; условие вызова: `execute if block ~ ~ ~ #minecraft:black at @s run function lbc:other/artifact/colors/black`. Эффект: снимает эффекты с @s; @s получает `minecraft:night_vision` на 300 секунд, amplifier 0; @a[gamemode=!spectator,distance=0.01..20,predicate=!lbc:same_team] получает `minecraft:blindness` на 30 секунд, amplifier 0. Меняет атрибут: @s minecraft:follow_range base set 4. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/black.mcfunction)

### `lbc:other/artifact/colors/blue`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors.mcfunction`; условие вызова: `execute if block ~ ~ ~ #minecraft:blue at @s run function lbc:other/artifact/colors/blue`. Эффект: @s получает `minecraft:water_breathing` на 30 секунд, amplifier 2; @s получает `minecraft:conduit_power` на 30 секунд, amplifier 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/blue.mcfunction)

### `lbc:other/artifact/colors/brown`
Если под владельцем brown-colored block, даёт Saturation II на 1 секунду, очищает hunger и увеличивает `food_count` на 1. Одновременно накладывает Hunger II на 30 секунд всем не-spectator противникам вне команды в distance 0.01..25; играет burp и показывает cooked-beef particle. [Источник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/brown.mcfunction).

### `lbc:other/artifact/colors/cyan`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors.mcfunction`; условие вызова: `execute if block ~ ~ ~ #minecraft:cyan at @s run function lbc:other/artifact/colors/cyan`. Эффект: @s получает `minecraft:invisibility` на 300 секунд, amplifier 1; @s получает `minecraft:night_vision` на 300 секунд, amplifier 1; @e[distance=0.01..300,tag=!spectator,predicate=!lbc:same_team] получает `minecraft:glowing` на 30 секунд, amplifier 0. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/cyan.mcfunction)

### `lbc:other/artifact/colors/gray`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors.mcfunction`; условие вызова: `execute if block ~ ~ ~ #minecraft:gray at @s run function lbc:other/artifact/colors/gray`. Эффект: @s получает `minecraft:resistance` на 10 секунд, amplifier 4. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/gray.mcfunction)

### `lbc:other/artifact/colors/green`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors.mcfunction`; условие вызова: `execute if block ~ ~ ~ #minecraft:green at @s run function lbc:other/artifact/colors/green`. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/green.mcfunction)

### `lbc:other/artifact/colors/light_blue`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors.mcfunction`; условие вызова: `execute if block ~ ~ ~ #minecraft:light_blue at @s run function lbc:other/artifact/colors/light_blue`. Эффект: @s получает `minecraft:slow_falling` на 300 секунд, amplifier 0; @e[distance=0.01..20,tag=!spectator,predicate=!lbc:same_team] получает `minecraft:levitation` на 2 секунд, amplifier 5. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/light_blue.mcfunction)

### `lbc:other/artifact/colors/light_gray`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors.mcfunction`; условие вызова: `execute if block ~ ~ ~ #minecraft:light_gray at @s run function lbc:other/artifact/colors/light_gray`. Эффект: @s получает `minecraft:resistance` на 300 секунд, amplifier 2; снимает эффекты с @e[distance=0.01..15,tag=!spectator,predicate=!lbc:same_team]. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/light_gray.mcfunction)

### `lbc:other/artifact/colors/lime`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors.mcfunction`; условие вызова: `execute if block ~ ~ ~ #minecraft:lime at @s run function lbc:other/artifact/colors/lime`. Эффект: @s получает `minecraft:jump_boost` на 300 секунд, amplifier 2; снимает эффекты с @s; @e[distance=0.01..15,tag=!spectator,predicate=!lbc:same_team] получает `minecraft:levitation` на 1 секунд, amplifier 0. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/lime.mcfunction)

### `lbc:other/artifact/colors/magenta`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors.mcfunction`; условие вызова: `execute if block ~ ~ ~ #minecraft:magenta at @s run function lbc:other/artifact/colors/magenta`. Состояние scoreboard: scoreboard players add @s mana 20; scoreboard players operation *tempt lbcID2 = @s team_number; scoreboard players remove @a[distance=0.01..35,scores={mana=20..},predicate=!lbc:same_team] mana 20. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/magenta.mcfunction)

### `lbc:other/artifact/colors/orange`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors.mcfunction`; условие вызова: `execute if block ~ ~ ~ #minecraft:orange at @s run function lbc:other/artifact/colors/orange`. Эффект: @s получает `minecraft:fire_resistance` на 300 секунд, amplifier 0. Изменяет блоки: fill ~1 ~1 ~1 ~-1 ~-1 ~-1 minecraft:fire replace #minecraft:airs. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/orange.mcfunction)

### `lbc:other/artifact/colors/pink`
При pink-блоке под владельцем даёт Instant Health, Regeneration II на 300 секунд, снимает с владельца poison, wither и instant damage; противники вне команды в distance 0.01..20 получают Wither II на 10 секунд. Показываются heart particles. [Источник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/pink.mcfunction).

### `lbc:other/artifact/colors/purple`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors.mcfunction`; условие вызова: `execute if block ~ ~ ~ #minecraft:purple at @s run function lbc:other/artifact/colors/purple`. Эффект: @e[type=#minecraft:mobs,distance=0.01..20,tag=!spectator,predicate=!lbc:same_team] получает `minecraft:raid_omen` на 30 секунд, amplifier 40. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/purple.mcfunction)

### `lbc:other/artifact/colors/red`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors.mcfunction`; условие вызова: `execute if block ~ ~ ~ #minecraft:red at @s run function lbc:other/artifact/colors/red`. Эффект: @s получает `minecraft:strength` на 300 секунд, amplifier 0; снимает эффекты с @s; @e[distance=0.01..15,tag=!spectator,predicate=!lbc:same_team] получает `minecraft:weakness` на 60 секунд, amplifier 0. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/red.mcfunction)

### `lbc:other/artifact/colors/white`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors.mcfunction`; условие вызова: `execute if block ~ ~ ~ #minecraft:white at @s run function lbc:other/artifact/colors/white`. Эффект: @s получает `minecraft:speed` на 300 секунд, amplifier 1; снимает эффекты с @s; @e[distance=0.01..25,tag=!spectator,predicate=!lbc:same_team] получает `minecraft:slowness` на 30 секунд, amplifier 1. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/white.mcfunction)

### `lbc:other/artifact/colors/yellow`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors.mcfunction`; условие вызова: `execute if block ~ ~ ~ #minecraft:yellow at @s run function lbc:other/artifact/colors/yellow`. Эффект: @s получает `minecraft:haste` на 300 секунд, amplifier 10; снимает эффекты с @s; @e[distance=0.01..25,tag=!spectator,predicate=!lbc:same_team] получает `minecraft:mining_fatigue` на 60 секунд, amplifier 2. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/yellow.mcfunction)

## Файлы: other / god_hedgehog

### `lbc:other/god_hedgehog/death_laser`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/tick.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 10 run return run function lbc:other/god_hedgehog/death_laser`. Эффект: @s получает `None` на None секунд, amplifier 0. Обновляет NBT/components: modify entity @s Motion set value [0.0,0.0,0.0]. Состояние scoreboard: scoreboard players set @s Distance 0. Передаёт шаг в `lbc:other/god_hedgehog/laser_rc`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/death_laser.mcfunction)

### `lbc:other/god_hedgehog/hedgegod_eater_boom`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/hedgegod_eater_tick.mcfunction`; условие вызова: `execute if score @s Distance matches 80 run function lbc:other/god_hedgehog/hedgegod_eater_boom`. Наносит 10 урона по @s типа `outside_border`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/hedgegod_eater_boom.mcfunction)

### `lbc:other/god_hedgehog/hedgegod_eater_tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_rare.mcfunction`; условие вызова: `execute if entity @s[tag=hedgegod_eater] run return run function lbc:other/god_hedgehog/hedgegod_eater_tick`. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/god_hedgehog/hedgegod_eater_tick_size1`, `lbc:other/god_hedgehog/hedgegod_eater_tick_spawn_new`, `lbc:other/god_hedgehog/hedgegod_eater_tick1`, `lbc:other/god_hedgehog/hedgegod_eater_boom`, `lbc:other/god_hedgehog/hedgegod_eater_tick2`. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/hedgegod_eater_tick.mcfunction)

### `lbc:other/god_hedgehog/hedgegod_eater_tick1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/hedgegod_eater_tick.mcfunction`; условие вызова: `execute if score @s Distance matches 79 run function lbc:other/god_hedgehog/hedgegod_eater_tick1`. Обновляет NBT/components: modify entity @s interpolation_duration set value 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/hedgegod_eater_tick1.mcfunction)

### `lbc:other/god_hedgehog/hedgegod_eater_tick2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/hedgegod_eater_tick.mcfunction`; условие вызова: `execute if score @s Distance matches 81 run function lbc:other/god_hedgehog/hedgegod_eater_tick2`. Команда: `execute store result entity @s transformation.scale[0] double 0.0000001 run data get entity @s transformation.scale[0] 1`; `execute store result entity @s transformation.scale[1] double 0.0000001 run data get entity @s transformation.scale[1] 1`; `execute store result entity @s transformation.scale[2] double 0.0000001 run data get entity @s transformation.scale[2] 1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/hedgegod_eater_tick2.mcfunction)

### `lbc:other/god_hedgehog/hedgegod_eater_tick_size1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/hedgegod_eater_tick.mcfunction`; условие вызова: `execute if score @s Distance matches 1 run function lbc:other/god_hedgehog/hedgegod_eater_tick_size1`. Обновляет NBT/components: modify entity @s interpolation_duration set value 80. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/hedgegod_eater_tick_size1.mcfunction)

### `lbc:other/god_hedgehog/hedgegod_eater_tick_spawn_new`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/hedgegod_eater_tick.mcfunction`; условие вызова: `execute if score @s Distance matches 3 if predicate lbc:chance96 at @s facing entity @e[type=#minecraft:mobs,tag=!spectator,sort=nearest,limit=1,tag=!strict_map_object,type=!silverfish] eyes positioned ^ ^ ^0.3 rotated as @s positioned ^ ^ ^1 facing entity @s eyes facing ^ ^ ^-1 positioned as @s positioned ^ ^ ^4 run function lbc:other/god_hedgehog/hedgegod_eater_tick_spawn_new`. Создаёт `item_display` с {Tags:["slowraycast","rare","hedgegod_eater","magic"],view_range:1000.0f,interpolation_duration:1,teleport_duration:1,item_display:"fixed",transformation:{left_rotation:[0.0f,0.0f,. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/hedgegod_eater_tick_spawn_new.mcfunction)

### `lbc:other/god_hedgehog/hedgegod_white_arrow_shoot`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/hedgegod_white_arrow_tick.mcfunction`; условие вызова: `execute if score @s Lifetime matches 40 run function lbc:other/god_hedgehog/hedgegod_white_arrow_shoot`. Создаёт `marker` с {data:{lifetime:20},Tags:["raycastarrow","player","slowraycast"],Rotation:[90.0f,0.0f]}. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/hedgegod_white_arrow_shoot.mcfunction)

### `lbc:other/god_hedgehog/hedgegod_white_arrow_tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_rare.mcfunction`; условие вызова: `execute if entity @s[tag=floatingwhitearrow] run return run function lbc:other/god_hedgehog/hedgegod_white_arrow_tick`. Передаёт шаг в `lbc:other/god_hedgehog/hedgegod_white_arrow_shoot`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/hedgegod_white_arrow_tick.mcfunction)

### `lbc:other/god_hedgehog/laser_rc`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/death_laser.mcfunction`; условие вызова: `execute anchored eyes positioned ^ ^ ^ facing entity @e[limit=1,type=#mobs,tag=!spectator,tag=!strict_map_object,sort=nearest,distance=..200,type=!silverfish] eyes run function lbc:other/god_hedgehog/laser_rc`. Создаёт `tnt` с {fuse:0,explosion_power:10,CustomName:{"translate":"godbam"}}. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/god_hedgehog/laser_rc`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/laser_rc.mcfunction)

### `lbc:other/god_hedgehog/spawn_dim_scrap`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/tick.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 100..140 anchored eyes positioned ^ ^ ^ run return run function lbc:other/god_hedgehog/spawn_dim_scrap`. Создаёт `item_display`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/spawn_dim_scrap.mcfunction)

### `lbc:other/god_hedgehog/spawn_white_arrows`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/tick.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 240..280 as @r[gamemode=!spectator] at @s run return run function lbc:other/god_hedgehog/spawn_white_arrows`. Передаёт шаг в `lbc:other/god_hedgehog/spawn_white_arrows1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/spawn_white_arrows.mcfunction)

### `lbc:other/god_hedgehog/spawn_white_arrows1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/spawn_white_arrows.mcfunction`; условие вызова: `function lbc:other/god_hedgehog/spawn_white_arrows1 with storage lbc.math`. Создаёт `item_display`. Передаёт шаг в `lbc:other/god_hedgehog/spawn_white_arrows1at`. Использует макропараметры `$(i)`, `$(r)`, `$(j)`, `$(k)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/spawn_white_arrows1.mcfunction)

### `lbc:other/god_hedgehog/spawn_white_arrows1at`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/spawn_white_arrows1.mcfunction`; условие вызова: `$execute positioned ~ ~.4 ~ rotated $(i) $(r) positioned ^ ^ ^10 facing entity @s eyes positioned ^$(j) ^ ^ rotated ~$(k) ~ summon item_display run function lbc:other/god_hedgehog/spawn_white_arrows1at`. Обновляет NBT/components: merge entity @s {interpolation_duration:1,teleport_duration:1,item_display:"fixed",transformation:{"translation": [0.0, 0.0, 0. Передаёт шаг в `lbc:items/wands/sauvojen_wand/meditate/loop_particle_white`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/spawn_white_arrows1at.mcfunction)

### `lbc:other/god_hedgehog/spawn_white_arrows_form1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/tick.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 281..320 as @r[gamemode=!spectator] at @s run return run function lbc:other/god_hedgehog/spawn_white_arrows_form1`. Состояние scoreboard: scoreboard players operation tmp lbc.math = ticker lbc.math; scoreboard players operation tmp lbc.math %= 20 lbc.math; scoreboard players operation tmp lbc.math -= 10 lbc.math. Передаёт шаг в `lbc:other/god_hedgehog/spawn_white_arrows1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/spawn_white_arrows_form1.mcfunction)

### `lbc:other/god_hedgehog/spawn_white_arrows_form2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/tick.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 321..360 as @r[gamemode=!spectator] at @s run return run function lbc:other/god_hedgehog/spawn_white_arrows_form2`. Состояние scoreboard: scoreboard players operation tmp lbc.math = ticker lbc.math; scoreboard players operation tmp lbc.math *= 7 lbc.math; scoreboard players operation tmp lbc.math %= 180 lbc.math; scoreboard players remove tmp lbc.math 90. Передаёт шаг в `lbc:other/god_hedgehog/spawn_white_arrows1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/spawn_white_arrows_form2.mcfunction)

### `lbc:other/god_hedgehog/spawn_white_arrows_form3`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/tick.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 361..400 as @r[gamemode=!spectator] at @s run return run function lbc:other/god_hedgehog/spawn_white_arrows_form3`. Передаёт шаг в `lbc:other/god_hedgehog/spawn_white_arrows1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/spawn_white_arrows_form3.mcfunction)

### `lbc:other/god_hedgehog/spawn_white_arrows_form4`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/tick.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 401..440 as @r[gamemode=!spectator] at @s run return run function lbc:other/god_hedgehog/spawn_white_arrows_form4`. Состояние scoreboard: scoreboard players operation tmp lbc.math = ticker lbc.math; scoreboard players operation tmp lbc.math *= 45 lbc.math; scoreboard players operation tmp lbc.math %= 180 lbc.math; scoreboard players remove tmp lbc.math 90. Передаёт шаг в `lbc:other/god_hedgehog/spawn_white_arrows1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/spawn_white_arrows_form4.mcfunction)

### `lbc:other/god_hedgehog/tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/mobs_tick_lbc.mcfunction`; условие вызова: `execute if entity @s[tag=god_hedgehog] run function lbc:other/god_hedgehog/tick`. Эффект: @s получает `None` на None секунд, amplifier 0. Состояние scoreboard: scoreboard players operation tmp lbc.math = ticker lbc.math; scoreboard players operation tmp lbc.math %= 500 lbc.math. Передаёт шаг в `lbc:other/god_hedgehog/death_laser`, `lbc:other/god_hedgehog/spawn_dim_scrap`, `lbc:other/god_hedgehog/spawn_white_arrows`, `lbc:other/god_hedgehog/spawn_white_arrows_form1`, `lbc:other/god_hedgehog/spawn_white_arrows_form2`, `lbc:other/god_hedgehog/spawn_white_arrows_form3`, `lbc:other/god_hedgehog/spawn_white_arrows_form4`. Завершение: удаляет сущность (`@s`); удаляет сущность (`@e[type=silverfish,tag=!god_hedgehog]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/tick.mcfunction)

## Файлы: other / imba

### `lbc:other/imba/chain_lightning`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/effects/effect_chain_lightning.mcfunction`; условие вызова: `execute unless entity @e[type=minecraft:marker,distance=..20,tag=stopper,limit=1] run function lbc:other/imba/chain_lightning`. Передаёт шаг в `lbc:other/imba/chain_lightning_loop`, `lbc:other/imba/chain_lightning_hit_at`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/chain_lightning.mcfunction)

### `lbc:other/imba/chain_lightning_hit_at`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/chain_lightning.mcfunction`; условие вызова: `execute as @e[tag=hit,tag=!strict_map_object] at @s run function lbc:other/imba/chain_lightning_hit_at`. Наносит 16 урона по @s типа `arrow`. Создаёт `marker` с {data:{lifetime:10},Tags:["raycastmolnia","slowraycast","shield_blockable","mecha","player"],Rotation:[0.0f,-90.0f]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/chain_lightning_hit_at.mcfunction)

### `lbc:other/imba/chain_lightning_loop`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/chain_lightning.mcfunction`; условие вызова: `execute facing entity @e[type=#minecraft:mobs,distance=..100,tag=!nodmg,tag=!nodmg1,tag=!spectator,limit=1,tag=!strict_map_object] feet positioned ^ ^ ^ run function lbc:other/imba/chain_lightning_loop`. Передаёт шаг в `lbc:other/imba/chain_lightning_loop_end`, `lbc:other/imba/chain_lightning_loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/chain_lightning_loop.mcfunction)

### `lbc:other/imba/chain_lightning_loop_at`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/chain_lightning_loop_end.mcfunction`; условие вызова: `execute as @e[distance=..1,tag=!hit,tag=!spectator,tag=!nodmg,tag=!nodmg1] at @s run function lbc:other/imba/chain_lightning_loop_at`. Передаёт шаг в `lbc:other/imba/chain_lightning_loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/chain_lightning_loop_at.mcfunction)

### `lbc:other/imba/chain_lightning_loop_end`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/chain_lightning_loop.mcfunction`; условие вызова: `execute if entity @e[distance=..1,tag=!hit,tag=!spectator,tag=!nodmg,tag=!nodmg1,limit=1] run function lbc:other/imba/chain_lightning_loop_end`. Передаёт шаг в `lbc:other/imba/chain_lightning_loop_at`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/chain_lightning_loop_end.mcfunction)

### `lbc:other/imba/chaos_hoe_rec`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/chaos_hoe_rec.mcfunction`; условие вызова: `$execute positioned ~1 ~ ~ if block ~ ~ ~ $(id) run function lbc:other/imba/chaos_hoe_rec with storage lbc.math`. Изменяет блоки: fill ~ ~ ~ ~ ~ ~ minecraft:air destroy. Передаёт шаг в `lbc:other/imba/chaos_hoe_rec`. Использует макропараметры `$(id)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/chaos_hoe_rec.mcfunction)

### `lbc:other/imba/chaos_hoe_use`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_tick.mcfunction`; условие вызова: `execute if entity @s[advancements={lbc:use_chaos_hoe=true}] run function lbc:other/imba/chaos_hoe_use`. Состояние scoreboard: scoreboard players set @s Distance 0. Передаёт шаг в `lbc:other/imba/chaos_hoe_use_rc`. Завершение: сбрасывает advancement для повторного запуска. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/chaos_hoe_use.mcfunction)

### `lbc:other/imba/chaos_hoe_use_rc`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/chaos_hoe_use.mcfunction`; условие вызова: `execute anchored eyes positioned ^ ^ ^ run function lbc:other/imba/chaos_hoe_use_rc`. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/imba/chaos_hoe_use_rc_end`, `lbc:other/imba/chaos_hoe_use_rc`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/chaos_hoe_use_rc.mcfunction)

### `lbc:other/imba/chaos_hoe_use_rc_end`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/chaos_hoe_use_rc.mcfunction`; условие вызова: `execute if entity @e[type=minecraft:item,distance=..1,limit=1,nbt={Age:0s}] run function lbc:other/imba/chaos_hoe_use_rc_end`. Обновляет NBT/components: modify storage lbc.math id set from entity @e[type=minecraft:item,distance=..1,limit=1,nbt={Age:0s}] Item.id. Состояние scoreboard: scoreboard players set @s Distance 0. Передаёт шаг в `lbc:other/imba/chaos_hoe_rec`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/chaos_hoe_use_rc_end.mcfunction)

### `lbc:other/imba/chaos_pic_use`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Эффект: @s получает `minecraft:resistance` на 1 секунд, amplifier 4. Состояние scoreboard: scoreboard players set @s Distance 0. Передаёт шаг в `lbc:other/imba/chaos_pic_use_rc`. Завершение: сбрасывает advancement для повторного запуска. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/chaos_pic_use.mcfunction)

### `lbc:other/imba/chaos_pic_use_rc`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/chaos_pic_use.mcfunction`; условие вызова: `execute anchored eyes positioned ^ ^ ^ run function lbc:other/imba/chaos_pic_use_rc`. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/imba/dickcockgalactic`, `lbc:other/imba/chaos_pic_use_rc`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/chaos_pic_use_rc.mcfunction)

### `lbc:other/imba/chaos_sho_use`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Передаёт шаг в `lbc:items/swords/piston_sword/ray_pre`. Завершение: сбрасывает advancement для повторного запуска. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/chaos_sho_use.mcfunction)

### `lbc:other/imba/delta_ex_tick`
Каждый проход маркера `delta_ex` независимо бросает predicate `chance1` для каждой из 20 команд — это 20 отдельных 1%-ветвей за вызов, и несколько могут сработать вместе: TNT с `fuse:100s` (100 тиков = 5 секунд), безгравитационный такой TNT, lightning, fireballs с ExplosionPower 1/5/10/15/20, TNT с fuse 0, два семейства `groundbam` с fuse 100 или 1000 тиков (включая теги `evoker`, `fire`, `electro`, `strong`), вызов `exoperso4` при выбранном `delta_ex` marker в радиусе10 и перенаправление fireball при `Lifetime=1000` в `raycast_vpered_power`. Никакая из 19 строк не завершает/удаляет текущий marker; его жизненный срок задан при создании. [Источник и все ветви](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/delta_ex_tick.mcfunction).

### `lbc:other/imba/dickcockgalactic`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/chaos_pic_use_rc.mcfunction`; условие вызова: `execute unless block ~ ~ ~ #minecraft:airs run function lbc:other/imba/dickcockgalactic`. Создаёт `tnt` с {fuse:-1s}. Изменяет блоки: fill ~ ~ ~ ~ ~ ~ minecraft:air destroy. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/dickcockgalactic.mcfunction)

### `lbc:other/imba/imbaxe_use`
При advancement `lbc:imbaxe` функция обнуляет счётчики 216 выходных категорий, затем для каждого подходящего entity `@e[type=#minecraft:mobs_and_markers,tag=!spectator,tag=!imbaxe_used]` в измерении кроме Nexus, если рядом до 120 блоков есть не-spectator player, запускает сбор данных из куба 31×31×31 вокруг entity. Удалённые блоки складываются в общие scoreboard-счётчики, после цикла они копируются в `storage lbc.math` и макрос выдаёт игроку соответствующие предметы; функция отзывает advancement, чтобы повторный запуск был возможен. Это не обычная добыча одного блока: она проходит много типов и центров, разрушая все подходящие блоки в их кубах. За полной таблицей 216 целевых block ID и всех итоговых `give`-команд — [полный машинный manifest](inventory/review_other_imba_manifest.json); основной источник: [imbaxe_use](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/imbaxe_use.mcfunction).

### `lbc:other/imba/imbaxe_use_at`
На каждой выбранной позиции выполняются 216 `fill`-команд: в кубе 31³ (`~-15..~15` на каждой оси) блоки из фиксированной палитры заменяются воздухом через `strict`, число удалённых блоков записывается в отдельный scoreboard и добавляется к общей сумме. Позиция берётся от каждого entity указанного caller-а; player остаётся executor/получателем, потому что caller меняет только позицию через `execute at`. Контейнерные данные и block entity отдельным кодом не сохраняются. [Все 216 точных replace-ID и координаты](inventory/review_other_imba_manifest.json); [fill-проход](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/imbaxe_use_at.mcfunction).

### `lbc:other/imba/imbaxe_use_macro`
Macro использует результаты счётчиков как количества для 217 `give @s`-команд: 216 уникальных item IDs; `bamboo_mosaic` встречается дважды. Palette состоит из215 конкретных block IDs и отдельного `#minecraft:leaves`, которое конвертируется в счетчик `stick`. Значения не ограничены первыми несколькими типами, это возвращаемые количества всей палитры. В выдачу включены все блоки из harvesting-list и три специально оформленных Lucky Block предмета из red/brown mushroom blocks и mushroom stem; у них заданы custom name, lore, block state и item model. Условие `IF` пропускает выдачу для нулевого счёта (в таком случае подставляется count 0). Раскрытая последовательность команд находится в [manifest](inventory/review_other_imba_manifest.json) и [macro-source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/imbaxe_use_macro.mcfunction).

### `lbc:other/imba/midas_block`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_imba.mcfunction`; условие вызова: `execute if entity @s[tag=midas_block] align xyz positioned ~.5 ~.5 ~.5 run return run function lbc:other/imba/midas_block`. Изменяет блоки: setblock ~ ~ ~ minecraft:raw_gold_block. Обновляет NBT/components: modify storage lbc.math cmd set value "midas". Передаёт шаг в `lbc:run_command`. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/midas_block.mcfunction)

### `lbc:other/imba/midas_block_entity`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Изменяет блоки: setblock ~ ~ ~ minecraft:raw_gold_block; setblock ~ ~ ~ minecraft:raw_gold_block strict. Завершение: удаляет сущность (`@s[tag=!sin_greed_active]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/midas_block_entity.mcfunction)

### `lbc:other/imba/midas_block_entity_rec`

Файл пуст: при вызове командами Minecraft эффекта не будет. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/midas_block_entity_rec.mcfunction)

### `lbc:other/imba/midas_block_entity_rec_pre`

Файл пуст: при вызове командами Minecraft эффекта не будет. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/midas_block_entity_rec_pre.mcfunction)

### `lbc:other/imba/most_spawn`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/projectiles/eggs.mcfunction`; условие вызова: `execute if data entity @e[type=minecraft:snowball,sort=nearest,limit=1] Item.components.minecraft:custom_data{"most_imba":1} run function lbc:other/imba/most_spawn`. Создаёт `marker` с {Tags:["imba_rc","slowraycast","imba_most","mecha"]}. Обновляет NBT/components: modify entity @e[type=minecraft:marker,distance=..0.01,tag=imba_most,tag=!actived,limit=1] Rotation set from entity @p[gamemode. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_spawn.mcfunction)

### `lbc:other/imba/most_tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_imba.mcfunction`; условие вызова: `execute if entity @s[tag=imba_most] if entity @a[gamemode=!spectator,distance=..200] run return run function lbc:other/imba/most_tick`. Состояние scoreboard: scoreboard players operation tmp lbc.math = @s Lifetime; scoreboard players operation tmp lbc.math %= 7 lbc.math. Передаёт шаг в `lbc:other/imba/most_tick_random_colon_select`, `lbc:other/imba/most_tick_random_double_most_create`, `lbc:other/imba/most_tick_column_pre`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick.mcfunction)

### `lbc:other/imba/most_tick_column`
При начале колонны выбирает один из шести шаблонов `most_column_1..6`, размещает его на 15 блоков ниже маркера и запускает следующий этап на 5 блоков ниже; scoreboard `y` на каждом шаге уменьшается на 4. Повтор идёт, пока `y` остаётся 10..319, так что число шаблонов зависит от стартовой высоты и не фиксировано. [Источник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick_column.mcfunction).

### `lbc:other/imba/most_tick_column_pre`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 0 run function lbc:other/imba/most_tick_column_pre`. Передаёт шаг в `lbc:other/imba/most_tick_column`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick_column_pre.mcfunction)

### `lbc:other/imba/most_tick_random_colon`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick_random_colon_select_2.mcfunction`; условие вызова: `$execute positioned ^$(p1) ^ ^ positioned ~ ~$(p2) ~ run function lbc:other/imba/most_tick_random_colon`. Передаёт шаг в `lbc:other/imba/most_tick_random_colon_2`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick_random_colon.mcfunction)

### `lbc:other/imba/most_tick_random_colon_2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick_random_colon.mcfunction`; условие вызова: `execute positioned ~1 ~ ~ run function lbc:other/imba/most_tick_random_colon_2`. Изменяет блоки: setblock ~ ~-1 ~ minecraft:quartz_pillar; setblock ~ ~ ~ minecraft:sea_lantern. Передаёт шаг в `lbc:other/imba/most_tick_random_colon_2`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick_random_colon_2.mcfunction)

### `lbc:other/imba/most_tick_random_colon_select`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick.mcfunction`; условие вызова: `execute if predicate lbc:chance50 run function lbc:other/imba/most_tick_random_colon_select`. Передаёт шаг в `lbc:other/imba/most_tick_random_colon_select_2`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick_random_colon_select.mcfunction)

### `lbc:other/imba/most_tick_random_colon_select_2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick_random_colon_select.mcfunction`; условие вызова: `function lbc:other/imba/most_tick_random_colon_select_2 with storage lbc.math`. Передаёт шаг в `lbc:other/imba/most_tick_random_colon`. Использует макропараметры `$(p1)`, `$(p2)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick_random_colon_select_2.mcfunction)

### `lbc:other/imba/most_tick_random_double_most_create`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick.mcfunction`; условие вызова: `execute if predicate lbc:chance5 run function lbc:other/imba/most_tick_random_double_most_create`. Обновляет NBT/components: modify storage lbc.math p1 set value 0; modify storage lbc.math p1 set value 90. Передаёт шаг в `lbc:other/imba/most_tick_random_double_most_create_2`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick_random_double_most_create.mcfunction)

### `lbc:other/imba/most_tick_random_double_most_create_2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick_random_double_most_create.mcfunction`; условие вызова: `function lbc:other/imba/most_tick_random_double_most_create_2 with storage lbc.math`. Передаёт шаг в `lbc:other/imba/most_tick_random_double_most_create_3`. Использует макропараметры `$(p1)`, `$(p2)`, `$(p3)`, `$(p4)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick_random_double_most_create_2.mcfunction)

### `lbc:other/imba/most_tick_random_double_most_create_3`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick_random_double_most_create_2.mcfunction`; условие вызова: `$execute rotated $(p1) 0 positioned ~$(p2) ~$(p3) ~$(p4) run function lbc:other/imba/most_tick_random_double_most_create_3`. Состояние scoreboard: scoreboard players set c lbc.math 0. Передаёт шаг в `lbc:other/imba/most_tick_random_double_most_create_loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick_random_double_most_create_3.mcfunction)

### `lbc:other/imba/most_tick_random_double_most_create_loop`
Генератор quartz-структуры рисует площадку chiseled quartz 5×5 и нижний слой quartz bricks 3×3, добавляет четыре верхних угловых блока; в половине проходов центр становится sea lantern. Счётчик `c` сначала набирает проходы, после второго шага с шансом 80% строится боковой/нижний продолжатель. Затем recursive-проход смещается на 5 блоков вперёд, пока executor в пределах 200; вызывающая функция делает второй такой же проход с поворотом 180°. Итого это повторяемая дорожка с развилкой, не единственная конструкция из нескольких исходных строк. [Источник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick_random_double_most_create_loop.mcfunction).

### `lbc:other/imba/most_tick_random_double_most_create_loop_2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick_random_double_most_create_loop_2.mcfunction`; условие вызова: `execute if score timp lbc.math matches 1 positioned ~ ~-1 ~ run function lbc:other/imba/most_tick_random_double_most_create_loop_2`. Изменяет блоки: fill ~1 ~ ~ ~-1 ~ ~ minecraft:quartz_pillar strict; fill ~ ~ ~1 ~ ~ ~-1 minecraft:quartz_pillar strict. Передаёт шаг в `lbc:other/imba/most_tick_random_double_most_create_loop_2`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick_random_double_most_create_loop_2.mcfunction)

### `lbc:other/imba/most_tick_random_double_most_create_loop_3`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick_random_double_most_create_loop.mcfunction`; условие вызова: `execute if score c lbc.math matches 2.. if predicate lbc:chance80 run function lbc:other/imba/most_tick_random_double_most_create_loop_3`. Состояние scoreboard: scoreboard players set c lbc.math 0. Передаёт шаг в `lbc:other/imba/most_tick_random_double_most_create_loop_2`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick_random_double_most_create_loop_3.mcfunction)

### `lbc:other/imba/place_cum_slab`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Состояние scoreboard: scoreboard players set @s Distance 0. Передаёт шаг в `lbc:other/imba/place_cum_slab_recur`. Завершение: сбрасывает advancement для повторного запуска. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/place_cum_slab.mcfunction)

### `lbc:other/imba/place_cum_slab_recur`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/place_cum_slab.mcfunction`; условие вызова: `execute anchored eyes positioned ^ ^ ^ run function lbc:other/imba/place_cum_slab_recur`. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/imba/place_cum_slab_spawn`, `lbc:other/imba/place_cum_slab_recur`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/place_cum_slab_recur.mcfunction)

### `lbc:other/imba/place_cum_slab_spawn`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/place_cum_slab_recur.mcfunction`; условие вызова: `execute unless block ~ ~ ~ #minecraft:airs align xyz positioned ~.5 ~.5 ~.5 run function lbc:other/imba/place_cum_slab_spawn`. Создаёт `marker` с {Tags:["slowraycast","imba_rc","cum_slab","down"]}. Состояние scoreboard: scoreboard players set @s Distance 10000. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/place_cum_slab_spawn.mcfunction)

### `lbc:other/imba/time_stopped_lol`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/effects/effect_timestop.mcfunction`; условие вызова: `execute unless entity @e[type=minecraft:marker,distance=..20,tag=stopper,limit=1] run function lbc:other/imba/time_stopped_lol`. Передаёт шаг в `lbc:other/imba/time_stopped_lol_player`, `lbc:other/imba/time_stopped_lol_mob`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/time_stopped_lol.mcfunction)

### `lbc:other/imba/time_stopped_lol_mob`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/time_stopped_lol.mcfunction`; условие вызова: `execute unless entity @s[type=minecraft:player] run function lbc:other/imba/time_stopped_lol_mob`. Обновляет NBT/components: remove entity @s Tags; modify entity @s NoAI set value 1b; modify entity @s Invulnerable set value 1b. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/time_stopped_lol_mob.mcfunction)

### `lbc:other/imba/time_stopped_lol_player`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/time_stopped_lol.mcfunction`; условие вызова: `execute if entity @s[type=minecraft:player] run function lbc:other/imba/time_stopped_lol_player`. Эффект: снимает эффекты с @s. Передаёт шаг в `lbc:other/imba/time_stopped_lol_player_1`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/time_stopped_lol_player.mcfunction)

### `lbc:other/imba/time_stopped_lol_player_1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/time_stopped_lol_player.mcfunction`; условие вызова: `function lbc:other/imba/time_stopped_lol_player_1 with storage lbc.math`. Использует макропараметры `$(tmp)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/time_stopped_lol_player_1.mcfunction)

### `lbc:other/imba/time_stopped_lol_player_tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_tick.mcfunction`; условие вызова: `execute if entity @s[tag=time_stopped_lol] run function lbc:other/imba/time_stopped_lol_player_tick`. Эффект: снимает эффекты с @s; @s получает `minecraft:resistance` на 1 секунд, amplifier 4; @s получает `minecraft:weakness` на 1 секунд, amplifier 127; @s получает `minecraft:slowness` на 1 секунд, amplifier 127. Передаёт шаг в `lbc:other/imba/time_stopped_lol_player_tick_tp`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/time_stopped_lol_player_tick.mcfunction)

### `lbc:other/imba/time_stopped_lol_player_tick_tp`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/time_stopped_lol_player_tick.mcfunction`; условие вызова: `function lbc:other/imba/time_stopped_lol_player_tick_tp with storage lbc.math`. Обновляет NBT/components: modify storage lbc.math tmp1 set from storage lbc.math $(tmp)_1; modify storage lbc.math tmp2 set from storage lbc.math $(tmp)_2; modify storage lbc.math tmp3 set from storage lbc.math $(tmp)_3. Передаёт шаг в `lbc:other/imba/time_stopped_lol_player_tick_tp_2`. Использует макропараметры `$(tmp)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/time_stopped_lol_player_tick_tp.mcfunction)

### `lbc:other/imba/time_stopped_lol_player_tick_tp_2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/time_stopped_lol_player_tick_tp.mcfunction`; условие вызова: `function lbc:other/imba/time_stopped_lol_player_tick_tp_2 with storage lbc.math`. Использует макропараметры `$(tmp1)`, `$(tmp2)`, `$(tmp3)`, `$(tmp4)`, `$(tmp5)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/time_stopped_lol_player_tick_tp_2.mcfunction)

### `lbc:other/imba/tmp`
Файл не имеет найденного literal caller в проиндексированных функциях. При ручном запуске он только сбрасывает 212 scoreboard-счётчиков/флагов семейств блоков; выдачи или изменения мира в нём нет. Это незавершённая/служебная инициализация, её нельзя принимать за полный imbaxe-конвертер. [Полный перечень reset-команд](inventory/review_other_imba_manifest.json), [tmp.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/tmp.mcfunction).

## Файлы: other / imba / bad_apple

### `lbc:other/imba/bad_apple/display_tick`
Вызов создаёт шесть вертикальных item_display лучей `lbc:laser_minigun_ray` (scale 0.1×100×0.1), случайно поворачивает каждый, запускает macro-текст `badapple start`, выдаёт squid-ink particles, ставит глобальный score `badapple=1` и schedule-ит `end` через 219 секунд. Затем в том же вызове убивает все `badapple_exp` displays и executor; следовательно эти шесть display не остаются как долгоживущие лучи. Завершение после задержки вызывает `badapple stop`, снимает gravity modifier у игроков и сбрасывает флаг. [Старт](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/bad_apple/display_tick.mcfunction), [end](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/bad_apple/end.mcfunction).

### `lbc:other/imba/bad_apple/display_tick1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/bad_apple/display_tick.mcfunction`; условие вызова: `function lbc:other/imba/bad_apple/display_tick1 with storage lbc.math`. Использует макропараметры `$(b)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/bad_apple/display_tick1.mcfunction)

### `lbc:other/imba/bad_apple/display_tick_rand_rot`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/bad_apple/display_tick.mcfunction`; условие вызова: `execute as @e[type=item_display,distance=..0.01,tag=badapple_exp] at @s run function lbc:other/imba/bad_apple/display_tick_rand_rot`. Команда: `execute store result entity @s Rotation[0] int 1 run random value -180..180`; `execute store result entity @s Rotation[1] int 1 run random value -90..90`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/bad_apple/display_tick_rand_rot.mcfunction)

### `lbc:other/imba/bad_apple/end`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/only_end_resets_oncebadapple.mcfunction`; условие вызова: `function lbc:other/imba/bad_apple/end`. Обновляет NBT/components: modify storage lbc.math b set value b. Состояние scoreboard: scoreboard players set badapple lbc.math 0. Передаёт шаг в `lbc:other/imba/bad_apple/end1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/bad_apple/end.mcfunction)

### `lbc:other/imba/bad_apple/end1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/bad_apple/end.mcfunction`; условие вызова: `function lbc:other/imba/bad_apple/end1 with storage lbc.math`. Использует макропараметры `$(b)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/bad_apple/end1.mcfunction)

### `lbc:other/imba/bad_apple/item_tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/items_tick_act.mcfunction`; условие вызова: `execute if entity @s[tag=bad_apple_imba] run function lbc:other/imba/bad_apple/item_tick`. Обновляет NBT/components: modify entity @s NoGravity set value true; modify entity @s Glowing set value true; modify entity @s Motion set value [0.0,0.1,0.0]. Состояние scoreboard: scoreboard players add @s Distance 1; scoreboard players operation math lbc.math = @s Distance; scoreboard players operation math lbc.math %= 2 lbc.math. Передаёт шаг в `lbc:other/imba/bad_apple/item_tick_spawn`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/bad_apple/item_tick.mcfunction)

### `lbc:other/imba/bad_apple/item_tick_spawn`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/bad_apple/item_tick.mcfunction`; условие вызова: `execute if score @s Distance matches 60 run function lbc:other/imba/bad_apple/item_tick_spawn`. Создаёт `item_display` с {item:{id:"acacia_boat",components:{"minecraft:item_model":"lbc:badapple"},count:1},Tags:["slowraycast","imba_rc","badappleimba"],interpolation_duration:1,teleport_duration:1}. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/bad_apple/item_tick_spawn.mcfunction)

### `lbc:other/imba/bad_apple/players_second`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Эффект: @s получает `None` на None секунд, amplifier 0. Наносит 4 урона по @s типа `out_of_world`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/bad_apple/players_second.mcfunction)

### `lbc:other/imba/bad_apple/spawn`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/items_tick_tag.mcfunction`; условие вызова: `execute if entity @s[nbt={Item:{components:{"minecraft:custom_data":{bad_apple_imba:1}}}}] run function lbc:other/imba/bad_apple/spawn`. Обновляет NBT/components: modify entity @s PickupDelay set value 32000. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/bad_apple/spawn.mcfunction)

## Файлы: other / imba / blood_slab

### `lbc:other/imba/blood_slab/place`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Состояние scoreboard: scoreboard players set @s Distance 0. Передаёт шаг в `lbc:other/imba/blood_slab/place_recur`. Завершение: сбрасывает advancement для повторного запуска. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/blood_slab/place.mcfunction)

### `lbc:other/imba/blood_slab/place_recur`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/blood_slab/place.mcfunction`; условие вызова: `execute anchored eyes positioned ^ ^ ^ run function lbc:other/imba/blood_slab/place_recur`. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/imba/blood_slab/spawn`, `lbc:other/imba/blood_slab/place_recur`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/blood_slab/place_recur.mcfunction)

### `lbc:other/imba/blood_slab/spawn`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/blood_slab/place_recur.mcfunction`; условие вызова: `execute unless block ~ ~ ~ #minecraft:airs align xyz positioned ~.5 ~.5 ~.5 run function lbc:other/imba/blood_slab/spawn`. Создаёт `marker` с {Tags:["slowraycast","imba_rc","blood_slab","magic"]}. Состояние scoreboard: scoreboard players set @s Distance 10000. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/blood_slab/spawn.mcfunction)

### `lbc:other/imba/blood_slab/tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_imba.mcfunction`; условие вызова: `execute if entity @s[tag=blood_slab] run return run function lbc:other/imba/blood_slab/tick`. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number. Передаёт шаг в `lbc:other/imba/blood_slab/tick_move`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/blood_slab/tick.mcfunction)

### `lbc:other/imba/blood_slab/tick_move`
Локальный marker раз в вызов создаёт до шести соседних marker-ов по граням, если блок в направлении не принадлежит `#lbc:blood_slabbed` и там ещё нет blood marker. В собственной клетке он равновероятно выбирает один из 32 записанных случаев: air/cave_air/void_air, netherrack, lava, acacia planks, fire, nether bricks (два номера дают один блок), красные wool/nether bricks/sandstone/sand/glass/redstone/concrete/powder/glazed terracotta/terracotta, TNT, magma block, nether wart block, три вида basalt, soul sand/soil, bone block, blackstone, nether gold/quartz ore или fire coral block. С вероятностью 5% добавляется mob-выход, только если пять блоков над позицией — air; иначе он сразу завершает себя. Полная нумерация всех 32 бросков — [source manifest](inventory/review_other_imba_manifest.json), [tick_move](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/blood_slab/tick_move.mcfunction).

### `lbc:other/imba/blood_slab/tick_random_mob_spawn`
Mob-ветка после 5%-проверки равновероятно выбирает 1 из 10 исходов: piglin (два номера), piglin brute, magma cube, ghast (появляется выше на 6), wither skeleton, blaze, hoglin, zombified piglin или helper `lbc:drops/mom`. После этого играет взрыв и lava particles, всем `#minecraft:mobs` в сферическом distance≤4 (кроме `#lbc:bloody_slabby`) наносится 20 arrow damage, а восемь диагональных соседей получают blood marker, если там нет существующего маркера. [Все 10 ветвей](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/blood_slab/tick_random_mob_spawn.mcfunction).

## Файлы: other / imba / cum_slab

### `lbc:other/imba/cum_slab/afterdown`
Когда нисходящий marker встречает опору, функция ставит четыре double birch slab по сторонам света и создаёт там central marker-ы только в air-клетках, где ещё нет cum marker. Никакого таймера/удаления этих поставленных slab здесь нет. [Источник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cum_slab/afterdown.mcfunction).

### `lbc:other/imba/cum_slab/central`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/cum_slab/select.mcfunction`; условие вызова: `execute if entity @s[tag=central] run function lbc:other/imba/cum_slab/central`. Создаёт `marker` с {Tags:["slowraycast","imba_rc","cum_slab","down"]}. Изменяет блоки: setblock ~ ~ ~ minecraft:birch_slab[type=bottom]. Передаёт шаг в `lbc:other/imba/cum_slab/central_yes`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cum_slab/central.mcfunction)

### `lbc:other/imba/cum_slab/central_yes`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/cum_slab/central.mcfunction`; условие вызова: `execute unless block ~ ~-1 ~ #minecraft:airs run function lbc:other/imba/cum_slab/central_yes`. Создаёт `marker` с {Tags:["slowraycast","imba_rc","cum_slab","central"]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cum_slab/central_yes.mcfunction)

### `lbc:other/imba/cum_slab/down`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/cum_slab/select.mcfunction`; условие вызова: `execute if entity @s[tag=down] run function lbc:other/imba/cum_slab/down`. Изменяет блоки: setblock ~ ~ ~ minecraft:birch_slab[type=double]. Передаёт шаг в `lbc:other/imba/cum_slab/down_yes`. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cum_slab/down.mcfunction)

### `lbc:other/imba/cum_slab/down_yes`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/cum_slab/down.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 1 run function lbc:other/imba/cum_slab/down_yes`. Создаёт `marker` с {Tags:["slowraycast","imba_rc","cum_slab","down"]}. Передаёт шаг в `lbc:other/imba/cum_slab/afterdown`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cum_slab/down_yes.mcfunction)

### `lbc:other/imba/cum_slab/select`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_imba.mcfunction`; условие вызова: `execute if entity @s[tag=cum_slab] run return run function lbc:other/imba/cum_slab/select`. Передаёт шаг в `lbc:other/imba/cum_slab/central`, `lbc:other/imba/cum_slab/down`. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cum_slab/select.mcfunction)

## Файлы: other / imba / cunny_slab

### `lbc:other/imba/cunny_slab/cast`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbcr/function/raycast.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{cunny_slab:1}}}}] unless entity @e[type=minecraft:marker,distance=..20,tag=stopper_magic,limit=1] anchored eyes positioned ^ ^ ^ run function lbc:other/imba/cunny_slab/cast`. Создаёт `marker` с {Tags:["slowraycast","imba_rc","cunny_jungle"]}. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cast.mcfunction)

### `lbc:other/imba/cunny_slab/correction_cunny`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/tick.mcfunction`; условие вызова: `execute as @e[type=marker,tag=cunnycorrector,distance=..0.1] at @s run function lbc:other/imba/cunny_slab/correction_cunny`. Передаёт шаг в `lbc:universal_pos_razb_10`, `lbc:other/imba/cunny_slab/correction_cunny_uoh`. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/correction_cunny.mcfunction)

### `lbc:other/imba/cunny_slab/correction_cunny_uoh`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/correction_cunny.mcfunction`; условие вызова: `execute at @s run function lbc:other/imba/cunny_slab/correction_cunny_uoh`. Изменяет блоки: fill ~ ~-1 ~ ~ ~-1 ~ barrel{LootTable:"lbc:imba/cunny_slabs"} replace #airs. Обновляет NBT/components: modify storage lbc:math wood set value "oak"; modify storage lbc:math wood set value "birch"; modify storage lbc:math wood set value "spruce". Передаёт шаг в `lbc:other/imba/cunny_slab/correction_cunny_uoh_pasta_select`, `lbc:other/imba/cunny_slab/correction_cunny_uoh_kanikani`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/correction_cunny_uoh.mcfunction)

### `lbc:other/imba/cunny_slab/correction_cunny_uoh_kanikani`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/correction_cunny_uoh.mcfunction`; условие вызова: `function lbc:other/imba/cunny_slab/correction_cunny_uoh_kanikani with storage lbc:math`. Изменяет блоки: setblock ~ ~ ~ $(wood)_sign[rotation=$(rot)]{is_waxed:true,front_text:{has_glowing_text:true,color:"white",messages:[{"text":"$(t. Использует макропараметры `$(wood)`, `$(rot)`, `$(t1)`, `$(t2)`, `$(t3)`, `$(t4)`, `$(t12)`, `$(t22)`, `$(t32)`, `$(t42)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/correction_cunny_uoh_kanikani.mcfunction)

### `lbc:other/imba/cunny_slab/correction_cunny_uoh_pasta_select`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/correction_cunny_uoh.mcfunction`; условие вызова: `function lbc:other/imba/cunny_slab/correction_cunny_uoh_pasta_select with storage lbc:math`. Передаёт шаг в `lbc:other/imba/cunny_slab/pasta/`. Использует макропараметры `$(t)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/correction_cunny_uoh_pasta_select.mcfunction)

### `lbc:other/imba/cunny_slab/end`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Создаёт `marker` с {Tags:["slowraycast","imba_rc","cunny_jungle"]}. Состояние scoreboard: scoreboard players set @s Distance 1000. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/end.mcfunction)

### `lbc:other/imba/cunny_slab/tick`
Каждый проход jungle-marker ставит `mega_jungle_tree`, затем равновероятно выбирает один из 16 template outcomes: `sob` занимает 4/16, а angry/splurt/sweat — каждый по четырём поворотам. Создаются четыре `cunnycorrector` markers, на каждом вызывается сборка надписи/бочки; после этого запускается `tick1`, который spreadplayers перемещает executor под Y=300 в пределах30. С шансом 1% удаляется случайный `cunny_jungle` marker; текущий marker сам здесь не убивается, его общая lifetime-обработка находится в marker dispatcher. [Все ветви](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/tick.mcfunction), [spread](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/tick1.mcfunction).

### `lbc:other/imba/cunny_slab/tick1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/tick.mcfunction`; условие вызова: `function lbc:other/imba/cunny_slab/tick1`. Команда: `spreadplayers ~ ~ 0 30 under 300 false @s`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/tick1.mcfunction)

## Файлы: other / imba / cunny_slab / cunnys

### `lbc:other/imba/cunny_slab/cunnys/0`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/cunny_item/select1.mcfunction`; условие вызова: `execute if score cunny lbc.math matches 0 run function lbc:other/imba/cunny_slab/cunnys/0`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/0.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/cunny_item/select1.mcfunction`; условие вызова: `execute if score cunny lbc.math matches 1 run function lbc:other/imba/cunny_slab/cunnys/1`. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/1.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/10`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/cunny_item/select1.mcfunction`; условие вызова: `execute if score cunny lbc.math matches 10 run function lbc:other/imba/cunny_slab/cunnys/10`. Loot: give lbc:luckyblock_all_stacks → @s; give lbc:op_hoe → @s; give lbc:god_dice → @s. Завершение: удаляет сущность (`@e[limit=1,sort=random]`); расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/10.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/11`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/cunny_item/select1.mcfunction`; условие вызова: `execute if score cunny lbc.math matches 11 run function lbc:other/imba/cunny_slab/cunnys/11`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/11.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/12`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/cunny_item/select1.mcfunction`; условие вызова: `execute if score cunny lbc.math matches 12 run function lbc:other/imba/cunny_slab/cunnys/12`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/12.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/13`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/cunny_item/select1.mcfunction`; условие вызова: `execute if score cunny lbc.math matches 13 run function lbc:other/imba/cunny_slab/cunnys/13`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/13.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/14`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/cunny_item/select1.mcfunction`; условие вызова: `execute if score cunny lbc.math matches 14 run function lbc:other/imba/cunny_slab/cunnys/14`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/14.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/15`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/cunny_item/select1.mcfunction`; условие вызова: `execute if score cunny lbc.math matches 15 run function lbc:other/imba/cunny_slab/cunnys/15`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/15.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/16`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/cunny_item/select1.mcfunction`; условие вызова: `execute if score cunny lbc.math matches 16 run function lbc:other/imba/cunny_slab/cunnys/16`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/16.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/17`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/cunny_item/select1.mcfunction`; условие вызова: `execute if score cunny lbc.math matches 17 run function lbc:other/imba/cunny_slab/cunnys/17`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/17.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/18`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/cunny_item/select1.mcfunction`; условие вызова: `execute if score cunny lbc.math matches 18 run function lbc:other/imba/cunny_slab/cunnys/18`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/18.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/19`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/cunny_item/select1.mcfunction`; условие вызова: `execute if score cunny lbc.math matches 19 run function lbc:other/imba/cunny_slab/cunnys/19`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/19.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/cunny_item/select1.mcfunction`; условие вызова: `execute if score cunny lbc.math matches 2 run function lbc:other/imba/cunny_slab/cunnys/2`. Loot: give lbc:inf_shulker → @s; give lbc:inf_steak → @s; give lbc:evolution_sword → @s. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/2.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/20`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/cunny_item/select1.mcfunction`; условие вызова: `execute if score cunny lbc.math matches 20 run function lbc:other/imba/cunny_slab/cunnys/20`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/20.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/21`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/cunny_item/select1.mcfunction`; условие вызова: `execute if score cunny lbc.math matches 21 run function lbc:other/imba/cunny_slab/cunnys/21`. Передаёт шаг в `lbc:other/imba/cunny_slab/cunnys/4_spawn_cunny`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/21.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/3`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/cunny_item/select1.mcfunction`; условие вызова: `execute if score cunny lbc.math matches 3 run function lbc:other/imba/cunny_slab/cunnys/3`. Эффект: @s получает `None` на None секунд, amplifier 0. Создаёт `marker` с {Tags:["slowraycast","rare","grassifier"]}. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/3.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/3_grassifier`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_rare.mcfunction`; условие вызова: `execute if entity @s[tag=grassifier] run return run function lbc:other/imba/cunny_slab/cunnys/3_grassifier`. Создаёт `marker` с {Tags:["slowraycast","rare","grassifier"]}. Изменяет блоки: fill ~ ~ ~ ~ ~ ~ grass_block. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/3_grassifier.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/4`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/cunny_item/select1.mcfunction`; условие вызова: `execute if score cunny lbc.math matches 4 run function lbc:other/imba/cunny_slab/cunnys/4`. Передаёт шаг в `lbc:other/imba/cunny_slab/cunnys/4_spawn_cunny`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/4.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/4_spawn_cunny`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/21.mcfunction`; условие вызова: `execute at @e[limit=64,sort=random] run function lbc:other/imba/cunny_slab/cunnys/4_spawn_cunny`. Состояние scoreboard: scoreboard players operation @e[type=#minecraft:mobs,tag=summoned_cultist,tag=!summactivated,distance=..0.01,limit=1] lb; scoreboard players operation @e[type=#minecraft:mobs,tag=summoned_cultist,tag=!summactivated,distance=..0.01,limit=1] te. Передаёт шаг в `lbc:other/imba/cunny_slab/cunnys/4_spawn_cunny_spawn`, `lbc:swrg_kit_integration/game/trinkets/on_summon`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/4_spawn_cunny.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/4_spawn_cunny_spawn`
Каждый вызов кидает `random 1..6` и призывает ровно одного cultist без loot, выбирая одинаковым шансом wither skeleton/strangeness (30 HP), skeleton/chaos (50), skeleton/luck (40), skeleton/omnipresence (40), skeleton/obsession (10, attack 11/speed .25) или zombie/hypocrisy (60, dual iron swords). У всех задан follow range100 и cosmetic faction armor; свойства здоровья/брони/размера отличаются по выбранной ветви и сохранены в исходнике. Сам helper не задаёт expiry: summon entities живут по обычным условиям мира. [Полные шесть SNBT-ветвей](inventory/review_other_imba_manifest.json), [source](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/4_spawn_cunny_spawn.mcfunction).

### `lbc:other/imba/cunny_slab/cunnys/5`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/cunny_item/select1.mcfunction`; условие вызова: `execute if score cunny lbc.math matches 5 run function lbc:other/imba/cunny_slab/cunnys/5`. Loot: give lbc:imba/nahida_dakimakura → @s. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/5.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/5_plapping`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Передаёт шаг в `lbc:other/imba/cunny_slab/cunnys/5_plapping_start`, `lbc:other/attributes/random_attrs_up`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/5_plapping.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/5_plapping_start`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/5_plapping.mcfunction`; условие вызова: `execute unless entity @s[tag=plapper] run function lbc:other/imba/cunny_slab/cunnys/5_plapping_start`. Команда: `attribute @s max_health modifier add plapper_debuff -1 add_multiplied_total`; `attribute @s max_absorption modifier add plapper_debuff -1 add_multiplied_total`; `attribute @s attack_damage modifier add plapper_debuff -1 add_multiplied_total`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/5_plapping_start.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/5_plapping_stop`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_tick.mcfunction`; условие вызова: `execute if entity @s[tag=plapper,advancements={lbc:use_nahida=false}] run function lbc:other/imba/cunny_slab/cunnys/5_plapping_stop`. Команда: `attribute @s max_health modifier remove plapper_debuff`; `attribute @s max_absorption modifier remove plapper_debuff`; `attribute @s attack_damage modifier remove plapper_debuff`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/5_plapping_stop.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/6`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/cunny_item/select1.mcfunction`; условие вызова: `execute if score cunny lbc.math matches 6 run function lbc:other/imba/cunny_slab/cunnys/6`. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/6.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/6_move`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_tick.mcfunction`; условие вызова: `execute if entity @s[tag=love_girl,predicate=!lbc:on_ground] facing entity @a[tag=girl_seeker,limit=1,sort=nearest] feet run function lbc:other/imba/cunny_slab/cunnys/6_move`. Обновляет NBT/components: modify storage lbc.math cmd set value "motion add ^ ^ ^.1 @s". Передаёт шаг в `lbc:run_command`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/6_move.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/6_seek`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_2t.mcfunction`; условие вызова: `execute if entity @s[tag=girl_seeker] run function lbc:other/imba/cunny_slab/cunnys/6_seek`. Передаёт шаг в `lbc:armor/powerhelmet_particle`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/6_seek.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/7`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/cunny_item/select1.mcfunction`; условие вызова: `execute if score cunny lbc.math matches 7 run function lbc:other/imba/cunny_slab/cunnys/7`. Передаёт шаг в `lbc:other/imba/cunny_slab/cunnys/7_whip_create`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/7.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/7_whip_create`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/7.mcfunction`; условие вызова: `execute as @a[tag=!cstr,gamemode=!spectator] positioned ~ ~1 ~ run function lbc:other/imba/cunny_slab/cunnys/7_whip_create`. Loot: spawn lbc:anal_whip_named → ~. Обновляет NBT/components: modify entity @e[type=item,limit=1,distance=..0.01,tag=whipcur,tag=!active] Item.components."minecraft:custom_data".teleportid . [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/7_whip_create.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/8`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/cunny_item/select1.mcfunction`; условие вызова: `execute if score cunny lbc.math matches 8 run function lbc:other/imba/cunny_slab/cunnys/8`. Loot: spawn lbc:imba/cunny_slabs → ~. Передаёт шаг в `lbc:other/imba/cunny_slab/cunnys/8_ejaculation`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/8.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/8_ejaculation`
Один проход увеличивает `twitching` на 60 и `cumming` на 160, затем равновероятно проигрывает один из шести звуков: `icum`, `yamai_desu`, `yamai_desu_2`, `yamai_desu_3`, `ooh` или `nyah`. Этот helper не выдаёт все шесть звуков за раз. [Источник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/8_ejaculation.mcfunction).

### `lbc:other/imba/cunny_slab/cunnys/9`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/cunny_item/select1.mcfunction`; условие вызова: `execute if score cunny lbc.math matches 9 run function lbc:other/imba/cunny_slab/cunnys/9`. Передаёт шаг в `lbc:other/imba/cunny_slab/cunnys/9_tp`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/9.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/9_platform`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_rare.mcfunction`; условие вызова: `execute if entity @s[tag=platform_marker] run return run function lbc:other/imba/cunny_slab/cunnys/9_platform`. Изменяет блоки: fill ~-16 ~ ~-16 ~16 ~ ~16 quartz_block. Состояние scoreboard: scoreboard players set if lbc.math 0. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/9_platform.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/9_tp`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/9.mcfunction`; условие вызова: `function lbc:other/imba/cunny_slab/cunnys/9_tp with storage lbc:math`. Передаёт шаг в `lbc:other/imba/cunny_slab/cunnys/9_tp_at`. Использует макропараметры `$(x)`, `$(z)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/9_tp.mcfunction)

### `lbc:other/imba/cunny_slab/cunnys/9_tp_at`
Macro-вызов из варианта cunny 9 телепортирует владельца на случайные X/Z в диапазоне −1,000,000..1,000,000 при Y=320; там появляется rare platform marker на 100 блоков выше, владелец получает Slow Falling на 60 секунд и 23 независимые выдачи loot table `lbc:imba/cunny_slabs`. Число loot-вызовов фиксировано (23), количество/тип предметов в каждом броске определяется loot table. [Источник с полным списком выдач](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/9_tp_at.mcfunction).

## Файлы: other / imba / cunny_slab / pasta

Динамический вызов использует macro `$(t)`: `correction_cunny_uoh` задаёт `t` случайно в диапазоне 1..100 и дважды вызывает selector с `with storage lbc:math`; этот каталог содержит все literal варианты, не независимые триггеры. Для каждого значения ниже показаны строки t1..t4, которые записываются в storage.

| t | t1 / t2 / t3 / t4 |
|---:|---|
| 1 | `\uF82F\uF82FCUNNY CUNNY\uF82F\uF82F / \uF82F\uF82FCUTE AND FUNNY\uF82F\uF82F / \uF82F\uF82FUOOOOOOOOOOH\uF82F\uF82F / \uF82F\uF82FPLAP PLAP PLAP\uF82F\uF82F` |
| 2 | `\uF82F\uF82FGET PREGNANT\uF82F\uF82F / \uF82F\uF82FGET PREGNANT\uF82F\uF82F / \uF82F\uF82FGET PREGNANT\uF82F\uF82F / \uF82F\uF82FGET PREGNANT\uF82F\uF82F` |
| 3 | `\uF82F\uF82Fi really want\uF82F\uF82F / \uF82F\uF82Fto lick her cute\uF82F\uF82F / \uF82F\uF82Flittle soft\uF82F\uF82F / \uF82F\uF82Floli armpits\uF82F\uF82F` |
| 4 | `\uF82F\uF82FMAN I LOVE\uF82F\uF82F / \uF82F\uF82FLOLICON\uF82F\uF82F / \uF82F\uF82FCUNNY CUNNY\uF82F\uF82F / \uF82F\uF82FCUTE AND FUNNY\uF82F\uF82F` |
| 5 | `\uF82F\uF82Fkani kani kani\uF82F\uF82F / \uF82F\uF82Fkani kani kani\uF82F\uF82F / \uF82F\uF82Fkani kani kani\uF82F\uF82F / \uF82F\uF82Fkani kani kani\uF82F\uF82F` |
| 6 | `\uF82F\uF82FPLAP PLAP PLAP\uF82F\uF82F / \uF82F\uF82FPLAP PLAP PLAP\uF82F\uF82F / \uF82F\uF82FPLAP PLAP PLAP\uF82F\uF82F / \uF82F\uF82FPLAP PLAP PLAP\uF82F\uF82F` |
| 7 | `\uF82F\uF82FUUUUUUUUUU\uF82F\uF82F / \uF82F\uF82FOOOOOOOOOO\uF82F\uF82F / \uF82F\uF82FOOOOOOOOOO\uF82F\uF82F / \uF82F\uF82FHHHHHHHHHH\uF82F\uF82F` |
| 8 | `\uF82F\uF82FTIGHT AS HELL\uF82F\uF82F / \uF82F\uF82FSEIA BELLY IS\uF82F\uF82F / \uF82F\uF82FSO FUCKIN SOFT\uF82F\uF82F / \uF82F\uF82FCANT RESIST\uF82F\uF82F` |
| 9 | `\uF82F\uF82FGreat Wisdom\uF82F\uF82F / \uF82F\uF82FGoddess Nahida\uF82F\uF82F / \uF82F\uF82FSoft feet and so\uF82F\uF82F / \uF82F\uF82Fcute tight cunny\uF82F\uF82F` |
| 10 | `\uF82F\uF82FI WANT TO LICK\uF82F\uF82F / \uF82F\uF82FHER LOLI CUNNY\uF82F\uF82F / \uF82F\uF82FPLEASE PEE ON ME\uF82F\uF82F / \uF82F\uF82FUOOOOOOOOOOH\uF82F\uF82F` |
| 11 | `\uF82F\uF82FPLEASE COME OVER\uF82F\uF82F / \uF82F\uF82FAND PEE ON ME\uF82F\uF82F / \uF82F\uF82FMY LITTLE CUTE\uF82F\uF82F / \uF82F\uF82FKISAKI CHAN !!!\uF82F\uF82F` |
| 12 | `\uF82F\uF82F:sob: :sob: :sob:\uF82F\uF82F / \uF82F\uF82FGRRRRRRRRRRR!\uF82F\uF82F / \uF82F\uF82F:angry: :angry:\uF82F\uF82F / \uF82F\uF82FCORRECTION!\uF82F\uF82F` |
| 13 | `\uF82F\uF82FHOW YOU CAN\uF82F\uF82F / \uF82F\uF82FTEASE ADULTS\uF82F\uF82F / \uF82F\uF82FLIKE THIS YOU BRAT\uF82F\uF82F / \uF82F\uF82FIMMEDIATE CORRECTION\uF82F\uF82F` |
| 14 | `\uF82F\uF82FCORRECTION\uF82F\uF82F / \uF82F\uF82FCORRECTION\uF82F\uF82F / \uF82F\uF82FCORRECTION\uF82F\uF82F / \uF82F\uF82FCORRECTION\uF82F\uF82F` |
| 15 | `\uF82F\uF82FCIRNO HAS THE\uF82F\uF82F / \uF82F\uF82FTIGHTEST CUNNY\uF82F\uF82F / \uF82F\uF82FGRRR PLAP PLAP\uF82F\uF82F / \uF82F\uF82FPLAP PLAP PLAP\uF82F\uF82F` |
| 16 | `\uF82F\uF82FNAHIDA PLEASE\uF82F\uF82F / \uF82F\uF82FSIT ON MY FACE\uF82F\uF82F / \uF82F\uF82FAND PEE IN MY MOUTH\uF82F\uF82F / \uF82F\uF82FGREAT GODDESS\uF82F\uF82F` |
| 17 | `\uF82F\uF82FKISAKI YOU ARE SO\uF82F\uF82F / \uF82F\uF82FBEAUTIFUL IN THOSE\uF82F\uF82F / \uF82F\uF82FCHINESE DRESS\uF82F\uF82F / \uF82F\uF82F!!CORRECTION!!\uF82F\uF82F` |
| 18 | `\uF82F\uF82FI LOVE MY FLAT\uF82F\uF82F / \uF82F\uF82FSTUDENT WAIFUS\uF82F\uF82F / \uF82F\uF82FPEAK FEMALE BODY\uF82F\uF82F / \uF82F\uF82FUOOOOOOOH\uF82F\uF82F` |
| 19 | `\uF82F\uF82FCUNNY LOVE IS\uF82F\uF82F / \uF82F\uF82FCUNNY LIVE\uF82F\uF82F / \uF82F\uF82FONE DAY I HOPE\uF82F\uF82F / \uF82F\uF82FI SEE CUNNY WIFE\uF82F\uF82F` |
| 20 | `\uF82F\uF82FSPLURGE TONS\uF82F\uF82F / \uF82F\uF82FIN THOSE CUTE\uF82F\uF82F / \uF82F\uF82FLOLI SOFT ARMPITS\uF82F\uF82F / \uF82F\uF82FSO CUTE AND FUNNY\uF82F\uF82F` |
| 21 | `\uF82F\uF82FLOLI FEET ARE THE\uF82F\uF82F / \uF82F\uF82FBEST, SO SOFT\uF82F\uF82F / \uF82F\uF82FCUTE LITTLE FEET\uF82F\uF82F / \uF82F\uF82FI LOVE YOU NAHIDA\uF82F\uF82F` |
| 22 | `\uF82F\uF82FKLEE PLEASE LET ME\uF82F\uF82F / \uF82F\uF82FTOUCH YOUR PRESCIOUS\uF82F\uF82F / \uF82F\uF82FLITTLE LOLI TITS\uF82F\uF82F / \uF82F\uF82FMEET YOU IS SO LUCKY\uF82F\uF82F` |
| 23 | `\uF82F\uF82FSPLURGE TONS\uF82F\uF82F / \uF82F\uF82FINSIDE YOUR LITTLE\uF82F\uF82F / \uF82F\uF82FSOFT LOLI CUNNY\uF82F\uF82F / \uF82F\uF82FIS THE DREAM\uF82F\uF82F` |
| 24 | `\uF82F\uF82FPROFESSOR NIYANIYA\uF82F\uF82F / \uF82F\uF82FYOU ARE SOOOOOOOO\uF82F\uF82F / \uF82F\uF82FBREEDABLE IN\uF82F\uF82F / \uF82F\uF82FTHIS SWIMSUIT\uF82F\uF82F` |
| 25 | `\uF82F\uF82FSIGEWINNE PLEASE\uF82F\uF82F / \uF82F\uF82FSTICK YOUR BIG\uF82F\uF82F / \uF82F\uF82FSYRINGE DIRECTLY\uF82F\uF82F / \uF82F\uF82FUP MY ASS PLEASE\uF82F\uF82F` |
| 26 | `\uF82F\uF82FI WANT TO TOUCH YOU\uF82F\uF82F / \uF82F\uF82FTHERE MY CUTE LITTLE\uF82F\uF82F / \uF82F\uF82FSIGEWINNE, PLEASE\uF82F\uF82F / \uF82F\uF82FLET ME TOUCH YOU\uF82F\uF82F` |
| 27 | `\uF82F\uF82FDORI HOW YOU CAN\uF82F\uF82F / \uF82F\uF82FTEASE ADULTS WITH\uF82F\uF82F / \uF82F\uF82FTHIS TIGHT LOLI CUNNY\uF82F\uF82F / \uF82F\uF82FCORRECTION NEEDED\uF82F\uF82F` |
| 28 | `\uF82F\uF82FSAYU SO TIRED FROM\uF82F\uF82F / \uF82F\uF82FTRAININGS, HER SWEAT\uF82F\uF82F / \uF82F\uF82FIS SOOO YUMMY TO LICK\uF82F\uF82F / \uF82F\uF82FHER SWEATY ARMPITS\uF82F\uF82F` |
| 29 | `\uF82F\uF82FSEX WITH EVERY\uF82F\uF82F / \uF82F\uF82FSTUDENT IN BLUE\uF82F\uF82F / \uF82F\uF82FARCHIVE! NO ONE\uF82F\uF82F / \uF82F\uF82FWILL ESCAPE!\uF82F\uF82F` |
| 30 | `\uF82F\uF82FAMIRHAN PRIVET\uF82F\uF82F / \uF82F\uF82FКак дела, Амирхан\uF82F\uF82F / \uF82F\uF82FО. Здарова Амирхан\uF82F\uF82F / \uF82F\uF82FHi Amirhan!\uF82F\uF82F` |
| 31 | `\uF82F\uF82FLOLI PUSSY HORSE\uF82F\uF82F / \uF82F\uF82FCOCK INSERTION\uF82F\uF82F / \uF82F\uF82FTENTACLES MONSTERS\uF82F\uF82F / \uF82F\uF82FCUM LOLI TIME STOP\uF82F\uF82F` |
| 32 | `\uF82F\uF82FPUMP IN TIGHT\uF82F\uF82F / \uF82F\uF82FYAOYAO SWEET CUNNY\uF82F\uF82F / \uF82F\uF82FLOLI PUSSY\uF82F\uF82F / \uF82F\uF82FIMPREGNATION\uF82F\uF82F` |
| 33 | `\uF82F\uF82FCUNNY CORRECTION\uF82F\uF82F / \uF82F\uF82FPLAP PLAP PLAP\uF82F\uF82F / \uF82F\uF82FRAPE LOLIS\uF82F\uF82F / \uF82F\uF82FTIGHT PUSSY\uF82F\uF82F` |
| 34 | `\uF82F\uF82FGOON TO KIDS\uF82F\uF82F / \uF82F\uF82FFLUFFY LOLI HAIR\uF82F\uF82F / \uF82F\uF82FWANT TO COVER IT\uF82F\uF82F / \uF82F\uF82FIN CUM COMPLETELY\uF82F\uF82F` |
| 35 | `\uF82F\uF82FLOLI MADE FOR\uF82F\uF82F / \uF82F\uF82FHUGE HORSE COCKS\uF82F\uF82F / \uF82F\uF82FCANT RESIST RAPING\uF82F\uF82F / \uF82F\uF82FA CHILDREN UOOOH\uF82F\uF82F` |
| 36 | `\uF82F\uF82FBLUE ARCHIVE GIRLS\uF82F\uF82F / \uF82F\uF82FNEEDS TO BE LOVED\uF82F\uF82F / \uF82F\uF82FBY THEIR SENSEIS\uF82F\uF82F / \uF82F\uF82FCUNNY UOOOOOOOOH\uF82F\uF82F` |
| 37 | `\uF82F\uF82FMY BINAH IS READY\uF82F\uF82F / \uF82F\uF82FTO GO STRAIGHT\uF82F\uF82F / \uF82F\uF82FINTO KISAKI TIGHTEST\uF82F\uF82F / \uF82F\uF82FCUNNY RIGHT NOW\uF82F\uF82F` |
| 38 | `\uF82F\uF82FARONA IS SOOOOOO\uF82F\uF82F / \uF82F\uF82FBREEDABLE! NEEDS\uF82F\uF82F / \uF82F\uF82FTO BE PUMPED AT\uF82F\uF82F / \uF82F\uF82FLEAST 20 TIMES A DAY\uF82F\uF82F` |
| 39 | `\uF82F\uF82FSOFT ARMPITS\uF82F\uF82F / \uF82F\uF82FBULGING BELLY WITH\uF82F\uF82F / \uF82F\uF82FHARD BINAH IN IT\uF82F\uF82F / \uF82F\uF82FLOLI IS PEAAAAK\uF82F\uF82F` |
| 40 | `\uF82F\uF82FЯ Владислав Сафин\uF82F\uF82F / \uF82F\uF82FМне нравится быть фембоем\uF82F\uF82F / \uF82F\uF82FФембои - это патриатично!\uF82F\uF82F / \uF82F\uF82FСлава Русской Нации!\uF82F\uF82F` |
| 41 | `\uF82F\uF82FSOMEDAY MY CUNNY\uF82F\uF82F / \uF82F\uF82FWIFE WILL COME\uF82F\uF82F / \uF82F\uF82FI BELIEVE SOMEDAY\uF82F\uF82F / \uF82F\uF82FI'LL HUG MY CUNNY WIFE\uF82F\uF82F` |
| 42 | `\uF82F\uF82FALL LITTLE CUNNIES\uF82F\uF82F / \uF82F\uF82FNEED THEIR\uF82F\uF82F / \uF82F\uF82FFUTA MOM WITH\uF82F\uF82F / \uF82F\uF82FLARGE FUTA DICK\uF82F\uF82F` |
| 43 | `\uF82F\uF82FSEIA IS THE MOST\uF82F\uF82F / \uF82F\uF82FBREEDABLE STUDENT\uF82F\uF82F / \uF82F\uF82FFROM BLUE ARCHIVE\uF82F\uF82F / \uF82F\uF82FNEED CORRECTION\uF82F\uF82F` |
| 44 | `\uF82F\uF82FI DON'T WANT\uF82F\uF82F / \uF82F\uF82FKOKONA TO BE A\uF82F\uF82F / \uF82F\uF82FCHILD, I WANT HER TO\uF82F\uF82F / \uF82F\uF82FBEAR MY CHILD\uF82F\uF82F` |
| 45 | `\uF82F\uF82FKOKONA DOESN'T WANT\uF82F\uF82F / \uF82F\uF82FTO BE TREATED AS CHILD\uF82F\uF82F / \uF82F\uF82FSHE'S A GROWN-UP ADULT\uF82F\uF82F / \uF82F\uF82FI ALREADY MADE HER ONE\uF82F\uF82F` |
| 46 | `\uF82F\uF82FKOKONUT\uF82F\uF82F / \uF82F\uF82FINSIDE\uF82F\uF82F / \uF82F\uF82FKOKONA\uF82F\uF82F / \uF82F\uF82FUOOOOOOOOOOH\uF82F\uF82F` |
| 47 | `\uF82F\uF82FUOOOOOOOGHHHH\uF82F\uF82F / \uF82F\uF82FAAAAAAAARRRGH\uF82F\uF82F / \uF82F\uF82FKOKONA CUNNY\uF82F\uF82F / \uF82F\uF82FGET PREGNANT\uF82F\uF82F` |
| 48 | `\uF82F\uF82FKOKONA SAYS SHE'S AN\uF82F\uF82F / \uF82F\uF82FADULT, SO I INTRODUCED\uF82F\uF82F / \uF82F\uF82FHER TO ADULT WORLD\uF82F\uF82F / \uF82F\uF82FNOW SHE WANTS MORE\uF82F\uF82F` |
| 49 | `\uF82F\uF82FUOOOH CUNNY\uF82F\uF82F / \uF82F\uF82FI NEED TO CORRECT\uF82F\uF82F / \uF82F\uF82FEVERYONE AND\uF82F\uF82F / \uF82F\uF82FEVERYDAY\uF82F\uF82F` |
| 50 | `\uF82F\uF82FI LOVE KOKONA\uF82F\uF82F / \uF82F\uF82FCUTE AND FUNNY\uF82F\uF82F / \uF82F\uF82FDAUGHTERWIFE\uF82F\uF82F / \uF82F\uF82F:sob: :sob: :sob:\uF82F\uF82F` |
| 51 | `\uF82F\uF82FMY FAVORITE MILK\uF82F\uF82F / \uF82F\uF82FIS KOKONA MILK\uF82F\uF82F / \uF82F\uF82FOH I MEAN COCONUT\uF82F\uF82F / \uF82F\uF82FNAH KOKONA IS THE BEST\uF82F\uF82F` |
| 52 | `\uF82F\uF82FTIGHT INFINITY\uF82F\uF82F / \uF82F\uF82FBREEDING TIME\uF82F\uF82F / \uF82F\uF82FPORTABLE WIFE\uF82F\uF82F / \uF82F\uF82FSO TIGHT TUNNEL\uF82F\uF82F` |
| 53 | `\uF82F\uF82FUOOOOOOOOOH\uF82F\uF82F / \uF82F\uF82FGRRRRRRRRRRRRRR\uF82F\uF82F / \uF82F\uF82FRAPE CUNNY\uF82F\uF82F / \uF82F\uF82FPLAP PLAP PLAP PLAP\uF82F\uF82F` |
| 54 | `\uF82F\uF82FI-I CAN'T TAKE IT\uF82F\uF82F / \uF82F\uF82FAKARI CHAN IS TOO CUTE\uF82F\uF82F / \uF82F\uF82FI CANNOT CONTAIN MY\uF82F\uF82F / \uF82F\uF82FTHIRST FOR THE CUNNY\uF82F\uF82F` |
| 55 | `\uF82F\uF82FDRAINED PHYSICALLY\uF82F\uF82F / \uF82F\uF82FMENTALLY & FINANCIALLY\uF82F\uF82F / \uF82F\uF82FAKARI IS\uF82F\uF82F / \uF82F\uF82FRUTHLESS\uF82F\uF82F` |
| 56 | `\uF82F\uF82FI CANT\uF82F\uF82F / \uF82F\uF82FMESUGAKIS ARE\uF82F\uF82F / \uF82F\uF82FTHE GREATEST GIFT\uF82F\uF82F / \uF82F\uF82FFROM GOD\uF82F\uF82F` |
| 57 | `\uF82F\uF82FNOZOMI IS THE\uF82F\uF82F / \uF82F\uF82FMOST BREEDABLE\uF82F\uF82F / \uF82F\uF82FSTUDENT IN\uF82F\uF82F / \uF82F\uF82FBLUE ARCHIVE\uF82F\uF82F` |
| 58 | `\uF82F\uF82FALL THE MESUGAKI REQUIRE\uF82F\uF82F / \uF82F\uF82FCORRECTION AND HAVE TO\uF82F\uF82F / \uF82F\uF82FBE TAUGHT A LESSON THEY\uF82F\uF82F / \uF82F\uF82FWON'T FORGET FOR MONTHS\uF82F\uF82F` |
| 59 | `\uF82F\uF82FHIKARI AND NOZOMI\uF82F\uF82F / \uF82F\uF82FBREED BREED BREEED\uF82F\uF82F / \uF82F\uF82FTHEY WILL REMEMBER\uF82F\uF82F / \uF82F\uF82FTHIS CORRECTION FOREVER\uF82F\uF82F` |
| 60 | `\uF82F\uF82FMY BINAH IS ON THE\uF82F\uF82F / \uF82F\uF82FSTRAIGHT WAY INSIDE\uF82F\uF82F / \uF82F\uF82FTHIS LITTLE CUTE\uF82F\uF82F / \uF82F\uF82FAND FUNNY LOLI PUSSY\uF82F\uF82F` |
| 61 | `\uF82F\uF82FA MOTHER OF 1000000\uF82F\uF82F / \uF82F\uF82FPER SECOND AT LEAST\uF82F\uF82F / \uF82F\uF82FUOOH NOZOMI I'M ABOUT\uF82F\uF82F / \uF82F\uF82FTO UNWRAP MY GIFT TO YOU\uF82F\uF82F` |
| 62 | `\uF82F\uF82FNOZOMI IS\uF82F\uF82F / \uF82F\uF82FBETTER THAN HIKARI\uF82F\uF82F / \uF82F\uF82FNEED CORRECTION\uF82F\uF82F / \uF82F\uF82FMOTHER OF TEN\uF82F\uF82F` |
| 63 | `\uF82F\uF82FGOD I LOVE BRATTY\uF82F\uF82F / \uF82F\uF82FLITTLE GIRLS\uF82F\uF82F / \uF82F\uF82FWHO TEASE ME AT\uF82F\uF82F / \uF82F\uF82FEVERY OPPORTUNITY\uF82F\uF82F` |
| 64 | `\uF82F\uF82FTHIS PROF CUNNY\uF82F\uF82F / \uF82F\uF82FPLOTTER ALWAYS RADIATING\uF82F\uF82F / \uF82F\uF82FSMELL OF THE SUN\uF82F\uF82F / \uF82F\uF82FALWAYS SHE PASSES NEARBY\uF82F\uF82F` |
| 65 | `\uF82F\uF82FTHAT WOMB NEEDS\uF82F\uF82F / \uF82F\uF82FTO BE TAUGHT\uF82F\uF82F / \uF82F\uF82FA LESSON\uF82F\uF82F / \uF82F\uF82F!!CORRECTION!!\uF82F\uF82F` |
| 66 | `\uF82F\uF82FTHE 8 PRISONERS ABOUT\uF82F\uF82F / \uF82F\uF82FTO BE THE 8 CORRECTED\uF82F\uF82F / \uF82F\uF82FWIVES, INCLUDING\uF82F\uF82F / \uF82F\uF82FTHE PROFESSOR NIYANIYA\uF82F\uF82F` |
| 67 | `\uF82F\uF82FSHE'S SMUG AND NEEDS\uF82F\uF82F / \uF82F\uF82FCORRECTION! THESE ARE\uF82F\uF82F / \uF82F\uF82FTHE FACTS AS WE KNOW THEM\uF82F\uF82F / \uF82F\uF82FCORRECTION IS NEEDED ASAP!\uF82F\uF82F` |
| 68 | `\uF82F\uF82FI WANT TO BE\uF82F\uF82F / \uF82F\uF82FSURROUNDED BY\uF82F\uF82F / \uF82F\uF82FPROFESSOR NIYANIYA\uF82F\uF82F / \uF82F\uF82FAND MOMOKA SAKURAI\uF82F\uF82F` |
| 69 | `\uF82F\uF82FUOOOOOOOH\uF82F\uF82F / \uF82F\uF82FCUNNY CUNNY\uF82F\uF82F / \uF82F\uF82FGRRRRRRRRRRRRRRR\uF82F\uF82F / \uF82F\uF82FCUTE AND FUNNY\uF82F\uF82F` |
| 70 | `\uF82F\uF82FI LOVE HINA\uF82F\uF82F / \uF82F\uF82FONE OF THE MOST\uF82F\uF82F / \uF82F\uF82FTIGHTEST CUNNY\uF82F\uF82F / \uF82F\uF82FIN KIVOTOS\uF82F\uF82F` |
| 71 | `\uF82F\uF82FHINA IS THE VISION\uF82F\uF82F / \uF82F\uF82FFOR THE BLIND\uF82F\uF82F / \uF82F\uF82FHINA IS THE CHEF\uF82F\uF82F / \uF82F\uF82FFOR THE HUNGRY\uF82F\uF82F` |
| 72 | `\uF82F\uF82FHINA IS WHY I BREATH\uF82F\uF82F / \uF82F\uF82FHINA IS WHY I SMILE\uF82F\uF82F / \uF82F\uF82FHINA IS WHY I LAUGH\uF82F\uF82F / \uF82F\uF82FHINA IS MY LIFE\uF82F\uF82F` |
| 73 | `\uF82F\uF82FEVERYTIME IT ALL\uF82F\uF82F / \uF82F\uF82FEND UP WITH HINA\uF82F\uF82F / \uF82F\uF82FPREGNANT AND ME\uF82F\uF82F / \uF82F\uF82FBEING THE DAD\uF82F\uF82F` |
| 74 | `\uF82F\uF82FIT CAN'T BE HELPED\uF82F\uF82F / \uF82F\uF82FSO I'LL TAKE RESPONSIBILITY\uF82F\uF82F / \uF82F\uF82FLET'S REPOPULATE\uF82F\uF82F / \uF82F\uF82FA GALAXY, HINA\uF82F\uF82F` |
| 75 | `\uF82F\uF82FHINA LET'S CUDDLE\uF82F\uF82F / \uF82F\uF82FTOGETHER ALL\uF82F\uF82F / \uF82F\uF82FNIGHT UWOOOOO\uF82F\uF82F / \uF82F\uF82FOOOOOGGGGGHHHHH\uF82F\uF82F` |
| 76 | `\uF82F\uF82FYEAH YEAH YEAH\uF82F\uF82F / \uF82F\uF82FWHATEVER,\uF82F\uF82F / \uF82F\uF82FPLAP PLAP PLAP PLAP\uF82F\uF82F / \uF82F\uF82FPLAP PLAP PLAP PLAP\uF82F\uF82F` |
| 77 | `\uF82F\uF82FPLANA AND ARONA\uF82F\uF82F / \uF82F\uF82FARE BOTH PLAPABLE\uF82F\uF82F / \uF82F\uF82FAND ADORABLE\uF82F\uF82F / \uF82F\uF82FCUTE LOLI STUDENTS\uF82F\uF82F` |
| 78 | `\uF82F\uF82FNAHIDA AND KLEE\uF82F\uF82F / \uF82F\uF82FARE BOTH MY\uF82F\uF82F / \uF82F\uF82FCUTE AND FUNNY\uF82F\uF82F / \uF82F\uF82FWIFEDAUGHTERS\uF82F\uF82F` |
| 79 | `\uF82F\uF82FUOOOOOOOOOOOOOH\uF82F\uF82F / \uF82F\uF82FARONA YOU HAVE MADE\uF82F\uF82F / \uF82F\uF82FME BROKE 9 TIMES IN\uF82F\uF82F / \uF82F\uF82FA ROW! CORRECTION!!\uF82F\uF82F` |
| 80 | `\uF82F\uF82FREASON TO LOVE\uF82F\uF82F / \uF82F\uF82FINSTRUCTOR KOKONA\uF82F\uF82F / \uF82F\uF82FYOU DON'T HAVE TO WAIT\uF82F\uF82F / \uF82F\uF82FFOR HER TO TURN 16\uF82F\uF82F` |
| 81 | `\uF82F\uF82FHER MUSTY SMELL AND\uF82F\uF82F / \uF82F\uF82FTHE SWEAT DROPPING\uF82F\uF82F / \uF82F\uF82FHOLY WATER AFTER HER\uF82F\uF82F / \uF82F\uF82FPERFORMANCE IS SO GOOD\uF82F\uF82F` |
| 82 | `\uF82F\uF82FMARI IS OUR GUILDING\uF82F\uF82F / \uF82F\uF82FSTAR MARI I LOVE YOU\uF82F\uF82F / \uF82F\uF82FTHE SWEAT ON YOUR PIT\uF82F\uF82F / \uF82F\uF82F:sob: :sob: :sob: :sob:\uF82F\uF82F` |
| 83 | `\uF82F\uF82FUOOOOH MARI CHAN\uF82F\uF82F / \uF82F\uF82FMARI CHAN MARI CHAN\uF82F\uF82F / \uF82F\uF82FHAVE ALL MY CHILDREN\uF82F\uF82F / \uF82F\uF82FMARI IS MY LIGHT\uF82F\uF82F` |
| 84 | `\uF82F\uF82FIMAGINE THE SMELL\uF82F\uF82F / \uF82F\uF82FAND TASTE OF HER\uF82F\uF82F / \uF82F\uF82FHOLY SWEAT WATER\uF82F\uF82F / \uF82F\uF82FFEELS LIKE HEAVEN\uF82F\uF82F` |
| 85 | `\uF82F\uF82FMARI MY SAVIOR\uF82F\uF82F / \uF82F\uF82FMY SAVING GRACE\uF82F\uF82F / \uF82F\uF82F28 KITTENS\uF82F\uF82F / \uF82F\uF82FCAN'T STOP PUMPING\uF82F\uF82F` |
| 86 | `\uF82F\uF82FI CAN'T RESIST\uF82F\uF82F / \uF82F\uF82FCOLLECTING BOTTLES\uF82F\uF82F / \uF82F\uF82FFILLED WITH ALL KINDS\uF82F\uF82F / \uF82F\uF82FOF LIQUID FROM MARI!\uF82F\uF82F` |
| 87 | `\uF82F\uF82FMARI IS JUST PERFECT\uF82F\uF82F / \uF82F\uF82FPERFECT ANGEL\uF82F\uF82F / \uF82F\uF82FIDOL OF PREGNANCY\uF82F\uF82F / \uF82F\uF82FI WANT YOU MARI\uF82F\uF82F` |
| 88 | `\uF82F\uF82FYUUKA, DO YOU HAVE\uF82F\uF82F / \uF82F\uF82FGRAVITATIONAL PULL?\uF82F\uF82F / \uF82F\uF82FCAUSE I'M ATTRACTED TO YOU\uF82F\uF82F / \uF82F\uF82FMOTHER AT 16\uF82F\uF82F` |
| 89 | `\uF82F\uF82FUOOOH CUNNY GRRRR\uF82F\uF82F / \uF82F\uF82FI'M GOING TO GET FRY\uF82F\uF82F / \uF82F\uF82FUOOOOOOOOOOOOOOOH\uF82F\uF82F / \uF82F\uF82FYUUKA I CAN'T\uF82F\uF82F` |
| 90 | `\uF82F\uF82FYUUKA ARE GOING BEYOND\uF82F\uF82F / \uF82F\uF82FMY CALCULATIONS\uF82F\uF82F / \uF82F\uF82FSHE ARE MAKING MY BINAH\uF82F\uF82F / \uF82F\uF82FWEIGH OVER 100KG\uF82F\uF82F` |
| 91 | `\uF82F\uF82FHIKARI WILL BE A\uF82F\uF82F / \uF82F\uF82FCONDUCTOR OF 100\uF82F\uF82F / \uF82F\uF82FCHILDREN! THAT\uF82F\uF82F / \uF82F\uF82FTUMMY WILL BEEEE!!!\uF82F\uF82F` |
| 92 | `\uF82F\uF82FNOZOMI THIGT TUNNEL\uF82F\uF82F / \uF82F\uF82FWILL BE SHAPED\uF82F\uF82F / \uF82F\uF82FLIKE SENSEI'S\uF82F\uF82F / \uF82F\uF82FBIG TRAIN!!!\uF82F\uF82F` |
| 93 | `\uF82F\uF82FHOPE YOU HAVE FLOOD\uF82F\uF82F / \uF82F\uF82FCONTROL MEASURES\uF82F\uF82F / \uF82F\uF82FHIKARI, BECAUSE THAT\uF82F\uF82F / \uF82F\uF82FTUNNEL IS ABOUT TO OVERFLOW\uF82F\uF82F` |
| 94 | `\uF82F\uF82FHIKARI'S TUMMY HITS\uF82F\uF82F / \uF82F\uF82FTHAT FINE LINE BETWEEN\uF82F\uF82F / \uF82F\uF82FPLUMP AND FLAT, GOING\uF82F\uF82F / \uF82F\uF82FTO MAKE IT ROUND\uF82F\uF82F` |
| 95 | `\uF82F\uF82FUOOOOOOH MY TRAIN\uF82F\uF82F / \uF82F\uF82FWILL GO STRAIGHT\uF82F\uF82F / \uF82F\uF82FINTO THE DARK TUNNEL\uF82F\uF82F / \uF82F\uF82FAND IT SON'T STOP\uF82F\uF82F` |
| 96 | `\uF82F\uF82FI ENDED UP FLOODING\uF82F\uF82F / \uF82F\uF82FSAKI'S RABBIT HOLE\uF82F\uF82F / \uF82F\uF82FJUST LIKE MIYU'S,\uF82F\uF82F / \uF82F\uF82FMIYAKO'S AND MOE'S\uF82F\uF82F` |
| 97 | `\uF82F\uF82FSHUN TAKES THE\uF82F\uF82F / \uF82F\uF82FPOTION WHENEVER\uF82F\uF82F / \uF82F\uF82FSENSEI IS AROUND\uF82F\uF82F / \uF82F\uF82FCHEEKY LITTLE BRAT\uF82F\uF82F` |
| 98 | `\uF82F\uF82FSHUNNY IS GOING\uF82F\uF82F / \uF82F\uF82FTO BEAR MORE BABIES\uF82F\uF82F / \uF82F\uF82FTHAN ALL JACKIE\uF82F\uF82F / \uF82F\uF82FCHAN MOVIES COMBINED\uF82F\uF82F` |
| 99 | `\uF82F\uF82FSHUNLING 9 YEARS\uF82F\uF82F / \uF82F\uF82F9 YEARS EXPERIENCE\uF82F\uF82F / \uF82F\uF82FLITTLE GIRL!!!!!\uF82F\uF82F / \uF82F\uF82FSWEET LITTLE KIDDY SCENT\uF82F\uF82F` |
| 100 | `\uF82F\uF82FSHUNNY GOING TO FEEL\uF82F\uF82F / \uF82F\uF82FTHE POWER OF SENSEI\uF82F\uF82F / \uF82F\uF82FINSIDE THE BODY\uF82F\uF82F / \uF82F\uF82FOF CUTE AND FUNNY LOLI\uF82F\uF82F` |

### `lbc:other/imba/cunny_slab/pasta/1`
Вариант `1` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/1.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/10`
Вариант `10` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/10.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/100`
Вариант `100` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/100.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/11`
Вариант `11` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/11.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/12`
Вариант `12` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/12.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/13`
Вариант `13` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/13.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/14`
Вариант `14` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/14.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/15`
Вариант `15` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/15.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/16`
Вариант `16` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/16.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/17`
Вариант `17` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/17.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/18`
Вариант `18` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/18.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/19`
Вариант `19` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/19.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/2`
Вариант `2` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/2.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/20`
Вариант `20` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/20.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/21`
Вариант `21` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/21.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/22`
Вариант `22` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/22.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/23`
Вариант `23` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/23.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/24`
Вариант `24` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/24.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/25`
Вариант `25` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/25.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/26`
Вариант `26` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/26.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/27`
Вариант `27` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/27.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/28`
Вариант `28` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/28.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/29`
Вариант `29` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/29.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/3`
Вариант `3` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/3.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/30`
Вариант `30` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/30.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/31`
Вариант `31` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/31.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/32`
Вариант `32` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/32.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/33`
Вариант `33` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/33.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/34`
Вариант `34` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/34.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/35`
Вариант `35` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/35.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/36`
Вариант `36` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/36.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/37`
Вариант `37` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/37.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/38`
Вариант `38` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/38.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/39`
Вариант `39` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/39.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/4`
Вариант `4` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/4.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/40`
Вариант `40` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/40.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/41`
Вариант `41` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/41.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/42`
Вариант `42` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/42.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/43`
Вариант `43` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/43.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/44`
Вариант `44` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/44.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/45`
Вариант `45` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/45.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/46`
Вариант `46` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/46.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/47`
Вариант `47` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/47.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/48`
Вариант `48` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/48.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/49`
Вариант `49` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/49.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/5`
Вариант `5` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/5.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/50`
Вариант `50` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/50.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/51`
Вариант `51` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/51.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/52`
Вариант `52` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/52.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/53`
Вариант `53` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/53.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/54`
Вариант `54` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/54.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/55`
Вариант `55` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/55.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/56`
Вариант `56` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/56.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/57`
Вариант `57` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/57.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/58`
Вариант `58` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/58.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/59`
Вариант `59` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/59.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/6`
Вариант `6` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/6.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/60`
Вариант `60` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/60.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/61`
Вариант `61` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/61.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/62`
Вариант `62` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/62.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/63`
Вариант `63` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/63.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/64`
Вариант `64` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/64.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/65`
Вариант `65` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/65.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/66`
Вариант `66` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/66.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/67`
Вариант `67` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/67.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/68`
Вариант `68` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/68.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/69`
Вариант `69` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/69.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/7`
Вариант `7` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/7.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/70`
Вариант `70` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/70.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/71`
Вариант `71` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/71.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/72`
Вариант `72` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/72.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/73`
Вариант `73` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/73.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/74`
Вариант `74` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/74.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/75`
Вариант `75` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/75.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/76`
Вариант `76` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/76.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/77`
Вариант `77` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/77.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/78`
Вариант `78` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/78.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/79`
Вариант `79` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/79.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/8`
Вариант `8` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/8.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/80`
Вариант `80` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/80.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/81`
Вариант `81` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/81.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/82`
Вариант `82` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/82.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/83`
Вариант `83` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/83.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/84`
Вариант `84` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/84.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/85`
Вариант `85` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/85.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/86`
Вариант `86` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/86.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/87`
Вариант `87` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/87.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/88`
Вариант `88` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/88.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/89`
Вариант `89` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/89.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/9`
Вариант `9` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/9.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/90`
Вариант `90` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/90.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/91`
Вариант `91` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/91.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/92`
Вариант `92` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/92.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/93`
Вариант `93` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/93.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/94`
Вариант `94` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/94.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/95`
Вариант `95` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/95.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/96`
Вариант `96` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/96.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/97`
Вариант `97` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/97.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/98`
Вариант `98` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/98.mcfunction)
### `lbc:other/imba/cunny_slab/pasta/99`
Вариант `99` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/99.mcfunction)
## Файлы: other / imba / runes

### `lbc:other/imba/runes/ansuz_cast`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/raycast_spells/select.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{rune_ansuz:1}}}}] at @s run function lbc:other/imba/runes/ansuz_cast`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/ansuz_cast.mcfunction)

### `lbc:other/imba/runes/fehu_cast`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/raycast_spells/select.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{rune_fehu:1}}}}] run function lbc:other/imba/runes/fehu_cast`. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/imba/runes/fehu_cast_end`, `lbc:other/imba/runes/fehu_cast`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/fehu_cast.mcfunction)

### `lbc:other/imba/runes/fehu_cast_end`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/fehu_cast.mcfunction`; условие вызова: `execute unless block ~ ~ ~ #minecraft:airs run function lbc:other/imba/runes/fehu_cast_end`. Состояние scoreboard: scoreboard players set @s Distance 1000. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/fehu_cast_end.mcfunction)

### `lbc:other/imba/runes/fehu_itemkiller`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_imba.mcfunction`; условие вызова: `execute if entity @s[tag=itemkiller] run function lbc:other/imba/runes/fehu_itemkiller`. Завершение: удаляет сущность (`@e[type=item,distance=..20]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/fehu_itemkiller.mcfunction)

### `lbc:other/imba/runes/gifu_cast`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/raycast_spells/select.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{rune_gifu:1}}}}] run function lbc:other/imba/runes/gifu_cast`. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/imba/runes/gifu_cast_end`, `lbc:other/imba/runes/gifu_cast`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/gifu_cast.mcfunction)

### `lbc:other/imba/runes/gifu_cast_end`
На конце луча Gifu ставит Distance=1000, тратит один `rune_gifu` и выполняет 20 отдельных `loot spawn ... lbc:random_glack_1` в точке попадания; это двадцать независимых loot-table бросков, а не одна выдача. [Источник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/gifu_cast_end.mcfunction).

### `lbc:other/imba/runes/hagalaz_cast`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/raycast_spells/select.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{rune_hagalaz:1}}}}] at @s run function lbc:other/imba/runes/hagalaz_cast`. Завершение: удаляет сущность (`@e[type=minecraft:item]`); расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/hagalaz_cast.mcfunction)

### `lbc:other/imba/runes/kenaz_cast`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/raycast_spells/select.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{rune_kenaz:1}}}}] at @s run function lbc:other/imba/runes/kenaz_cast`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/kenaz_cast.mcfunction)

### `lbc:other/imba/runes/nautiz_cast`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/raycast_spells/select.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{rune_nautiz:1}}}}] at @s run function lbc:other/imba/runes/nautiz_cast`. Эффект: @a получает `None` на None секунд, amplifier 0. Завершение: расходует/очищает предметы; удаляет сущность (`@e[type=!minecraft:marker,type=!minecraft:player]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/nautiz_cast.mcfunction)

### `lbc:other/imba/runes/raido_cast`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/raycast_spells/select.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{rune_raido:1}}}}] at @s run function lbc:other/imba/runes/raido_cast`. Состояние scoreboard: scoreboard players set bottom_nexus lbc.math 1. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/raido_cast.mcfunction)

### `lbc:other/imba/runes/thurisaz_cast`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/raycast_spells/select.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{rune_thurisaz:1}}}}] at @s run function lbc:other/imba/runes/thurisaz_cast`. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number. Передаёт шаг в `lbc:other/imba/runes/thurisaz_cast_others`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/thurisaz_cast.mcfunction)

### `lbc:other/imba/runes/thurisaz_cast_others`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/thurisaz_cast.mcfunction`; условие вызова: `execute as @e[type=#minecraft:mobs,distance=0.01..50,tag=!spectator,tag=!strict_map_object,predicate=!lbc:same_team] at @s run function lbc:other/imba/runes/thurisaz_cast_others`. Команда: `attribute @s minecraft:scale modifier add minecraft:rune_thurisaz_negative -0.9375 add_multiplied_total`; `attribute @s minecraft:attack_damage modifier add minecraft:rune_thurisaz_negative -0.9375 add_multiplied_total`; `attribute @s minecraft:jump_strength modifier add minecraft:rune_thurisaz_negative -0.875 add_multiplied_total`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/thurisaz_cast_others.mcfunction)

### `lbc:other/imba/runes/thurisaz_gigant_tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_tick.mcfunction`; условие вызова: `execute if entity @s[tag=gigantic] run function lbc:other/imba/runes/thurisaz_gigant_tick`. Наносит 100 урона по @s типа `falling_anvil`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/thurisaz_gigant_tick.mcfunction)

### `lbc:other/imba/runes/uruz_cast`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/raycast_spells/select.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{rune_uruz:1}}}}] at @s run function lbc:other/imba/runes/uruz_cast`. Эффект: @s получает `minecraft:resistance` на infinite секунд, amplifier 4. Меняет атрибут: @s minecraft:attack_damage base set 10000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/uruz_cast.mcfunction)

### `lbc:other/imba/runes/wunjo_cast`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/raycast_spells/select.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{rune_wunjo:1}}}}] at @s run function lbc:other/imba/runes/wunjo_cast`. Состояние scoreboard: scoreboard players add @s swrg.lives 16. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/wunjo_cast.mcfunction)

