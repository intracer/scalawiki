# Monument CSV Export — Design Spec

**Date:** 2026-03-17
**Branch:** export-monuments
**Module:** scalawiki-wlx
**Scope:** UA campaign only (mapping is hardcoded to `ua_uk.json`)

---

## Overview

Add a `--export-csv [file]` flag to `Statistics.main()` that fetches all monument data using raw template parsing (`parseToMaps`) and writes it to a CSV file, with field names mapped to canonical English names via `ua_uk.json`.

---

## Data Flow

```
Statistics.main()
  └── StatParams + StatConfig (new --export-csv [file] flag)
       └── if flag set:
            MonumentQuery.byMonumentTemplateMaps()
              └── byMonumentTemplateMapsAsync()
                   └── byMonumentTemplateGeneric[Map[String,String]](parser = parseToMaps)
                        └── per page: WlxTemplateParser(listConfig, pageName).parseToMaps(wikiText)
            MonumentCsvExporter.export(maps, mapping, outputPath)
              └── UaUkJsonMapping.load("monuments_config/ua_uk.json")
                   └── applyMapping() per row
              └── CSVWriter.open(outputPath) → write header + rows
```

---

## Field Mapping (`ua_uk.json`)

Two-level mapping applied in sequence:

### Level 1 — `fields` array

Each entry maps a localized source name to an English dest name:
- If `dest` is non-empty: rename key `source` → `dest` in the output row
- If `dest` is empty string: **drop the field from the output** (these are intentionally suppressed internal/exclusion fields like `виключена`, `паспорт`, `наказ`)

### Level 2 — `sql_data` object

Applied after Level 1. Entries have one of three types:
- `"type": "Field"`: if the current row contains a key matching `value`, rename it to the `sql_data` entry key (e.g., row has `rayon` → rename to `adm2` because `sql_data.adm2.value = "rayon"`)
- `"type": "Text"`: inject a literal column `sql_data_key → value` into every row (e.g., `adm0 = "ua"`, `lang = "uk"`)
- `"type": "Raw"`: **skip** — these are SQL expressions not meaningful for CSV export (e.g., `adm1` with `LOWER(\`iso_oblast\`)`)

### Unmapped fields

Any field not found in the Level 1 `fields` array keeps its original name in the output.

### `applyMapping` algorithm (step by step)

```
Input: row = Map[String, String], mapping = UaUkMapping

Step 1 (Level 1):
  For each (key, value) in row:
    if fieldMap contains key:
      dest = fieldMap(key)
      if dest.nonEmpty → emit (dest, value)
      else             → drop the field
    else → emit (key, value)
  Result: intermediateRow

Step 2 (Level 2 — Field):
  For each (sqlKey, sqlEntry) in sqlMap where sqlEntry.entryType == "Field":
    if intermediateRow contains sqlEntry.value:
      if sqlKey == sqlEntry.value: no-op (rename to self)
      else if intermediateRow already contains sqlKey: keep existing sqlKey value, drop sqlEntry.value key
      else: rename key sqlEntry.value → sqlKey
    else (sqlEntry.value absent from intermediateRow): no column emitted for this sql_data entry

Step 3 (Level 2 — Text):
  For each (sqlKey, sqlEntry) in sqlMap where sqlEntry.entryType == "Text":
    add (sqlKey → sqlEntry.value) to intermediateRow

Output: intermediateRow
```

---

## Column Ordering in CSV

Headers are ordered as follows for reproducibility:
1. `sql_data` keys in their JSON insertion order (both `Field` and `Text` types)
2. Remaining keys from the mapped row in alphabetical order

This ensures stable output across runs and makes the most-important columns appear first.

---

## Components

### `MonumentQueryApi` — refactor (existing class)

Extract a private generic method shared by both Monument parsing and raw-map extraction. The `parser` is expressed as a two-argument function `(pageName, wikiText) => Iterable[T]`, with `listConfig` and `template` closed over at each call site. This avoids threading internal implementation details through the generic signature.

```scala
private def byMonumentTemplateGeneric[T](
    generatorTemplate: String,
    date: Option[ZonedDateTime],
    listTemplate: Option[String],
    parser: (String, String) => Iterable[T]   // (pageName, wikiText)
): Future[Iterable[T]]
```

This replaces the body of `byMonumentTemplateAsync`. Both code paths (with and without `date`) are preserved inside this generic method. The existing `byMonumentTemplateAsync` becomes a one-liner delegating to this method with the Monument parser. The new `byMonumentTemplateMapsAsync` delegates with the `parseToMaps` parser.

```scala
// Existing (refactored) — listConfig and template closed over from parameters:
def byMonumentTemplateAsync(...): Future[Iterable[Monument]] =
  byMonumentTemplateGeneric(...,
    (page, text) => Monument.monumentsFromText(text, page, template, listConfig))

// New — WlxTemplateParser constructed per page, listConfig closed over:
def byMonumentTemplateMapsAsync(...): Future[Iterable[Map[String, String]]] =
  byMonumentTemplateGeneric(...,
    (page, text) => new WlxTemplateParser(listConfig, page).parseToMaps(text))
```

Note: `WlxTemplateParser` is an instance class requiring a `ListConfig` and page name; it is constructed per page inside the parser lambda.

### `MonumentQuery` trait — add async + blocking methods

```scala
def byMonumentTemplateMapsAsync(
    generatorTemplate: String = defaultListTemplate,
    date: Option[ZonedDateTime] = None,
    listTemplate: Option[String] = None
): Future[Iterable[Map[String, String]]]

final def byMonumentTemplateMaps(
    generatorTemplate: String = defaultListTemplate,
    date: Option[ZonedDateTime] = None,
    listTemplate: Option[String] = None
): Iterable[Map[String, String]] =
  Await.result(byMonumentTemplateMapsAsync(generatorTemplate, date, listTemplate), Timeout)
```

Follows the same pattern as the existing `byMonumentTemplate` / `byMonumentTemplateAsync` pair.

### `UaUkJsonMapping` (new — `org.scalawiki.wlx`)

```scala
case class SqlEntry(entryType: String, value: String)  // entryType: "Field" | "Text" | "Raw"

case class UaUkMapping(
    fieldMap: Map[String, String],       // source → dest (empty dest = suppress)
    sqlMap: Map[String, SqlEntry],       // sqlKey → SqlEntry (ordered, LinkedHashMap)
    sqlKeyOrder: Seq[String]             // insertion order of sql_data keys
)

object UaUkJsonMapping {
  def load(resourcePath: String): UaUkMapping   // reads from classpath resources
  def applyMapping(row: Map[String, String], mapping: UaUkMapping): Map[String, String]
  def headerColumns(rows: Iterable[Map[String, String]], mapping: UaUkMapping): Seq[String]
  // sql_data keys (insertion order) ++ remaining keys (alphabetical)
}
```

### `MonumentCsvExporter` (new — `org.scalawiki.wlx`)

```scala
object MonumentCsvExporter {
  def export(rows: Iterable[Map[String, String]], mapping: UaUkMapping, outputPath: String): Unit
}
```

- Materializes `rows` once to collect all keys
- Applies `UaUkJsonMapping.applyMapping` to each row
- Uses `UaUkJsonMapping.headerColumns` for deterministic column order
- Uses `com.github.tototoshi.csv.CSVWriter` for output (handles quoting/escaping)
- Empty input: writes no output (no header-only file)

### `StatParams` and `StatConfig` — add flag

`StatParams`:
```scala
val exportCsv: ScallopOption[String] = opt[String](
  name = "export-csv",
  required = false,
  argName = "file",
  descr = "Export monuments to CSV. Optional filename; defaults to <campaign>-YYYY-MM-DD-HHmm.csv"
)
```

`StatConfig` (the case class populated by `StatParams.parse()`):
```scala
exportCsv: Option[String] = None
```

### `Statistics.main()` — wire up

```scala
params.statConfig.exportCsv.foreach { _ =>
  val path = params.statConfig.exportCsv
    .filter(_.nonEmpty)
    .getOrElse(defaultCsvFilename(contest))
  val maps = monumentQuery.byMonumentTemplateMaps()
  val mapping = UaUkJsonMapping.load("monuments_config/ua_uk.json")
  MonumentCsvExporter.export(maps, mapping, path)
  return  // do not run statistics reporters
}
```

**Default filename format:** `<campaign>-yyyy-MM-dd-HHmm.csv`
The campaign string is used as-is (e.g. `WLM-UA`), consistent with how it is used elsewhere in the codebase (cache filenames, etc.).
Example: `WLM-UA-2026-03-17-1430.csv`

---

## Testing

### `UaUkJsonMappingSpec` (new)
- Loads `ua_uk.json` from test resources; verifies `fieldMap` entries and `sqlKeyOrder`
- `applyMapping`: source field renamed to dest (`назва` → `name`)
- `applyMapping`: dest re-mapped via sql_data Field (`rayon` → `adm2`)
- `applyMapping`: Text literal injected (`adm0` = `"ua"`, `lang` = `"uk"`)
- `applyMapping`: Raw entry (`adm1`) is skipped — not injected
- `applyMapping`: empty-dest source field (`паспорт`) is dropped from output
- `applyMapping`: field absent from all mappings kept as-is (original name)
- `headerColumns`: sql_data keys come first in insertion order, then remaining keys alphabetically
- Full round-trip: realistic monument row maps to expected output map

### `MonumentCsvExporterSpec` (new)
- Single row: header matches expected columns, values correct
- Multiple rows with different key sets: header is union (sql_data order first, then alphabetical)
- Values containing commas, quotes, and newlines are properly escaped by CSVWriter
- Empty input: produces no output file (or zero bytes)

### `MonumentQueryApiSpec` (new file)
- `byMonumentTemplateMaps` returns correct `Iterable[Map[String, String]]` using `MockBotSpec`/`HttpStub` with canned wiki pages
- `byMonumentTemplateGeneric` refactor: existing `MonumentDbSpec` / `StatisticsSpec` tests continue to pass unchanged

### `StatisticsIntegrationSpec` (extend or new)
- `--export-csv` without filename: triggers CSV export, default filename matches regex `WLM-UA-\d{4}-\d{2}-\d{2}-\d{4}\.csv`
- `--export-csv myfile.csv`: that exact filename is used
- `--export-csv` flag: statistics reporters are not invoked
- No `--export-csv` flag: existing statistics flow runs unchanged

---

## Files Changed / Created

| File | Change |
|------|--------|
| `scalawiki-wlx/.../query/MonumentQuery.scala` | Add `byMonumentTemplateMapsAsync` + `byMonumentTemplateMaps`; refactor impl to use `byMonumentTemplateGeneric` |
| `scalawiki-wlx/.../UaUkJsonMapping.scala` | New — JSON mapping loader, `applyMapping`, `headerColumns` |
| `scalawiki-wlx/.../MonumentCsvExporter.scala` | New — CSV writer |
| `scalawiki-wlx/.../stat/StatParams.scala` | Add `--export-csv` flag |
| `scalawiki-wlx/.../stat/StatConfig.scala` | Add `exportCsv: Option[String]` field |
| `scalawiki-wlx/.../stat/Statistics.scala` | Wire up export path in `main()` |
| `scalawiki-wlx/src/main/resources/monuments_config/ua_uk.json` | Fill empty-dest entries incrementally as fields are observed |
| `UaUkJsonMappingSpec.scala` | New |
| `MonumentCsvExporterSpec.scala` | New |
| `MonumentQueryApiSpec.scala` | New |
| `StatisticsIntegrationSpec.scala` | New or extended |

---

## Out of Scope

- CSV import / round-trip back to wiki
- Support for non-UA campaigns (mapping is UA-specific via hardcoded `ua_uk.json`)
- GUI or web interface
- Filling all empty-dest entries upfront — done incrementally after observing real output
