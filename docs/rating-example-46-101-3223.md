# WLM UA 2026 rating: worked example for a monument without photos

**Monument:** `46-101-3223` (Lviv / Львів)
**List:** <https://w.wiki/UA2B>
**`бали` value written to the list:** `13`
(it was `18` before the interior-bonus fix — see the note at the end)

---

## English

### Where the rules live

The rating is assembled by `Rater.fromConfig`
(`scalawiki-wlx/src/main/scala/org/scalawiki/wlx/stat/rating/Rater.scala`) from the
`rates."2026"` block of
`scalawiki-wlx/src/main/resources/wlm_ua.conf`. For 2026 it is a `RateSum` of five
sub-raters (see `Rater2026Spec`). The `--fill-lists-rating` job
(`RatingListFiller`) computes a per-monument figure and writes it into the `бали`
list-template parameter, which the list template renders as a "(+ N балів)" hint
next to the upload link.

Every bonus is computed against **what was already known about the monument before
the contest** — i.e. photos uploaded in previous contest years that carried the
monument template (`Rater.oldImagesByMonumentId`). Photos that exist on Commons or
Wikipedia but were never entered into WLM are invisible to the algorithm.

### The components for `46-101-3223`

| # | Rater | Rule (Регламент) | Input for this monument | Points | In list `бали`? |
|---|-------|------------------|-------------------------|-------:|:---------------:|
| 1 | `NumberOfMonuments` (base) | п. 7.3.1 / 9.4 | Region `46` (Lviv oblast). Not in the war-region list `01, 14, 23, 44, 65, 85`, so the plain base rate applies. | **1** | yes |
| 2 | `NumberOfAuthorsBonus` | п. 7.3.2 | 0 authors have photographed it before → range `0-0` → 12. | **12** | yes |
| 3 | `NumberOfImagesInPlaceBonus` | п. 7.3.3 | Keyed on the **settlement** (KOATUU `46101`, Львів), not on the monument. Lviv already has ≥ 50 prior contest photos of its monuments, which is outside every range (`0-0` … `10-49`) → default 0. | **0** | yes |
| 4 | `NumberOfInteriorImagesBonus` | п. 7.3.5 | 0 known interior photos of this monument → range `0-0` → 5. **Only an interior photo earns this — excluded from the list figure.** | **5** | **no** |
| 5 | `OldPhotosBonus` | п. 7.3.4 | Requires at least one existing photo **and** all of them dated before 2020-01-01. With no photos at all it does not qualify. | **0** | yes |

**List `бали` = 1 + 12 + 0 + 0 = 13.**
An interior photo would additionally get the +5 from component 4 (13 + 5 = 18).

### Why it comes out this way

- The one large contributor for an unphotographed Lviv monument is the **authors
  bonus** (12 — nobody has shot it yet).
- The **place bonus is 0 only because Lviv is photo-rich.** The same monument in a
  small village with few prior uploads would earn +1 … +10 from this component.
- The **old-photos bonus is 0 by design** for a monument with zero photos — it
  only rewards monuments whose *entire* existing photo set predates 2020.
- The **interior bonus (+5) is not in the list figure.** Per п. 7.3.5 only an
  interior photo earns it, and many monuments (crosses, graves, kurhany,
  archaeological sites, free-standing monuments, parks, …) have no interior at
  all. It is still added by the full `Rater` when an interior photo is actually
  scored.
- If this monument were in one of the occupied / front-line regions, line 1 would
  be **10** instead of 1, giving a list figure of **22** (the `freshWar` case in
  `Rater2026Spec`).

### The interior-bonus fix

Before the fix, `RatingListFiller` summed **all five** raters, so this monument's
list value was `18` — including a +5 that:

1. a regular (exterior) photo never actually collects — `Rater.rate` only takes
   `(monumentId, author)` and never sees the candidate image, so the +5 was really
   the *maximum attainable*, not what an ordinary upload gets; and
2. is meaningless for a monument that physically has no interior — the code has no
   notion of monument type.

The fix adds `Rater.appliesToRegularPhoto` (default `true`, overridden to `false`
in `NumberOfInteriorImagesBonus`). `RatingListFiller.ratings` now drops raters
where that flag is `false`, so the list `бали` reflects an ordinary upload. The
full `Rater` used for post-contest scoring is unchanged.

---

## Українською

### Де описані правила

Рейтинг збирається методом `Rater.fromConfig`
(`scalawiki-wlx/src/main/scala/org/scalawiki/wlx/stat/rating/Rater.scala`) з блоку
`rates."2026"` файлу
`scalawiki-wlx/src/main/resources/wlm_ua.conf`. Для 2026 року це `RateSum` — сума
п’яти окремих оцінювачів (див. `Rater2026Spec`). Завдання `--fill-lists-rating`
(`RatingListFiller`) обчислює число для кожної пам’ятки й записує його в параметр
списку `бали`, який шаблон списку показує як підказку «(+ N балів)» біля
посилання для завантаження.

Кожен бонус рахується від того, **що вже було відомо про пам’ятку до початку
конкурсу** — тобто від фотографій, завантажених у попередні роки конкурсу з
шаблоном пам’ятки (`Rater.oldImagesByMonumentId`). Фотографії, які є на Вікісховищі
чи у Вікіпедії, але ніколи не подавалися на ВЛП, алгоритм не бачить.

### Складники для `46-101-3223`

| № | Оцінювач | Пункт Регламенту | Вхідні дані для цієї пам’ятки | Бали | У списку `бали`? |
|---|----------|------------------|------------------------------|-----:|:----------------:|
| 1 | `NumberOfMonuments` (базовий) | п. 7.3.1 / 9.4 | Регіон `46` (Львівська область). Не входить до переліку воєнних регіонів `01, 14, 23, 44, 65, 85`, тож застосовується звичайний базовий бал. | **1** | так |
| 2 | `NumberOfAuthorsBonus` | п. 7.3.2 | Пам’ятку раніше фотографували 0 авторів → діапазон `0-0` → 12. | **12** | так |
| 3 | `NumberOfImagesInPlaceBonus` | п. 7.3.3 | Прив’язаний до **населеного пункту** (КОАТУУ `46101`, Львів), а не до пам’ятки. У Львові вже є ≥ 50 конкурсних фотографій його пам’яток — це поза всіма діапазонами (`0-0` … `10-49`) → 0 за замовчуванням. | **0** | так |
| 4 | `NumberOfInteriorImagesBonus` | п. 7.3.5 | 0 відомих фотографій інтер’єру цієї пам’ятки → діапазон `0-0` → 5. **Цей бонус отримує лише фотографія інтер’єру — у число списку не входить.** | **5** | **ні** |
| 5 | `OldPhotosBonus` | п. 7.3.4 | Потрібна хоча б одна наявна фотографія **та** щоб усі вони були датовані до 01.01.2020. Без жодної фотографії умова не виконується. | **0** | так |

**`бали` у списку = 1 + 12 + 0 + 0 = 13.**
Фотографія інтер’єру додатково отримала б +5 зі складника 4 (13 + 5 = 18).

### Чому саме так

- Єдиний великий внесок для несфотографованої львівської пам’ятки — **бонус за
  авторів** (12 — її ще ніхто не знімав).
- **Бонус за населений пункт дорівнює 0 лише тому, що Львів багатий на світлини.**
  Та сама пам’ятка в невеликому селі з малою кількістю попередніх завантажень
  отримала б від цього складника +1 … +10.
- **Бонус за старі фотографії дорівнює 0 за задумом** для пам’ятки без жодної
  світлини — він винагороджує лише ті пам’ятки, у яких *усі* наявні фотографії
  зроблено до 2020 року.
- **Бонус за інтер’єр (+5) у число списку не входить.** За п. 7.3.5 його отримує
  лише фотографія інтер’єру, а багато пам’яток (хрести, поховання, кургани,
  археологічні пам’ятки, окремо розташовані пам’ятники, парки, …) інтер’єру не
  мають. Повний `Rater` усе одно додає його, коли оцінюється справжня фотографія
  інтер’єру.
- Якби ця пам’ятка була в одному з окупованих / прифронтових регіонів, рядок 1
  дорівнював би **10** замість 1, а число у списку було б **22** (випадок
  `freshWar` у `Rater2026Spec`).

### Виправлення бонусу за інтер’єр

До виправлення `RatingListFiller` підсумовував **усі п’ять** оцінювачів, тож
значення для цієї пам’ятки було `18` — з тими +5, які:

1. звичайна (зовнішня) фотографія насправді не отримує — `Rater.rate` приймає лише
   `(monumentId, author)` і ніколи не бачить самого зображення, тож ці +5 були
   *максимально досяжним* значенням, а не тим, що дає звичайне завантаження; і
2. не мають сенсу для пам’ятки, яка фізично не має інтер’єру — код не враховує тип
   пам’ятки.

Виправлення додає `Rater.appliesToRegularPhoto` (за замовчуванням `true`,
перевизначено на `false` у `NumberOfInteriorImagesBonus`).
`RatingListFiller.ratings` тепер відкидає оцінювачів із цим прапорцем `false`, тож
`бали` у списку відповідають звичайному завантаженню. Повний `Rater` для
підрахунку результатів після конкурсу не змінено.
