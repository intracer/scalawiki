# Monument CSV Export — Design Spec

**Date:** 2026-03-17
**Branch:** export-monuments
**Module:** scalawiki-wlx

---

## Overview

Add a `--export-csv [file]` flag to `Statistics.main()` that fetches all monument data using raw template parsing (`parseToMaps`) and writes it to a CSV file, with field names mapped to canonical English names via `ua_uk.json`.

---

## Data Flow

```
Statistics.main()
  └── StatParams (new --export-csv [file] flag)
       └── if flag set:
            MonumentQuery.byMonumentTemplateMaps()
              └── fetchPages() [extracted private helper, shared with byMonumentTemplateAsync]
                   └── WlxTemplateParser.parseToMaps() per page
            MonumentCsvExporter.export(maps, mapping, outputPath)
              └── UaUkJsonMapping.load("ua_uk.json")
                   └── apply two-level mapping to each row
              └── CSVWriter.open(outputPath) → write header + rows
```

---

## Field Mapping (`ua_uk.json`)

The mapping is two-level:

**Level 1 — `fields` array:** Maps localized source names to English dest names.
- `source` → `dest` (e.g., `назва` → `name`, `район` → `rayon`)
- If `dest` is empty string: field is kept under its original source name (to be fixed incrementally as fields are observed in output)

**Level 2 — `sql_data` object:** Re-maps and augments.
- `"type": "Field"`: re-maps a dest name to a sql_data key (e.g., `rayon` → `adm2`)
- `"type": "Text"`: injects a literal value column (e.g., `adm0` = `"ua"`, `lang` = `"uk"`)

**Unmapped fields:** Any field not found in either mapping level keeps its original name.

---

## Components

### `MonumentQueryApi` — refactor (existing class)

Extract private helper:

```scala
private def fetchPages(
    generatorTemplate: String,
    date: Option[ZonedDateTime],
    listTemplate: Option[String]
): Future[Seq[(String, ListConfig)]]  // (wikiText, listConfig) per page
```

Both `byMonumentTemplateAsync` (existing) and the new `byMonumentTemplateMapsAsync` call `fetchPages`, then apply their respective parsers. No change to existing public behavior.

### `MonumentQuery` trait — add one method

```scala
def byMonumentTemplateMaps(
    generatorTemplate: String = defaultListTemplate,
    date: Option[ZonedDateTime] = None,
    listTemplate: Option[String] = None
): Iterable[Map[String, String]]
```

### `UaUkJsonMapping` (new — `org.scalawiki.wlx`)

```scala
case class SqlEntry(valueType: String, value: String)  // type: "Field" | "Text"
case class UaUkMapping(
    fieldMap: Map[String, String],       // source → dest
    sqlMap: Map[String, (String, SqlEntry)]  // dest → (sqlKey, SqlEntry)
)

object UaUkJsonMapping {
  def load(resourcePath: String): UaUkMapping
  def applyMapping(row: Map[String, String], mapping: UaUkMapping): Map[String, String]
}
```

`applyMapping` algorithm:
1. For each `(k, v)` in `row`: rename key via `fieldMap` if present and dest non-empty, else keep `k`
2. For each entry in `sql_data` with `"type": "Field"`: if the current map contains the intermediate dest key, rename it to the sql_data key
3. For each entry in `sql_data` with `"type": "Text"`: inject `sqlKey → literalValue` into the result

### `MonumentCsvExporter` (new — `org.scalawiki.wlx`)

```scala
object MonumentCsvExporter {
  def export(rows: Iterable[Map[String, String]], mapping: UaUkMapping, outputPath: String): Unit
}
```

- Collects union of all mapped-row keys across all rows to determine header columns
- Writes header row, then one data row per monument
- Uses `com.github.tototoshi.csv.CSVWriter` (already on classpath)

### `StatParams` — add flag

```scala
val exportCsv: ScallopOption[String] = opt[String](
  name = "export-csv",
  required = false,
  argName = "file",
  descr = "Export monuments to CSV. Optional filename; defaults to <campaign>-YYYY-MM-DD-HHmm.csv"
)
```

### `Statistics.main()` — wire up

```scala
val path = params.exportCsv.toOption.getOrElse(defaultCsvFilename(contest))
// defaultCsvFilename: s"${contest.campaign}-${LocalDateTime.now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm"))}.csv"
val maps = monumentQuery.byMonumentTemplateMaps()
val mapping = UaUkJsonMapping.load("monuments_config/ua_uk.json")
MonumentCsvExporter.export(maps, mapping, path)
// return — do not run statistics reporters
```

---

## Default Filename

Format: `<campaign>-yyyy-MM-dd-HHmm.csv`
Example: `WLM-UA-2026-03-17-1430.csv`

Rationale: includes campaign, date, hour and minute so repeated runs on the same day produce distinct filenames.

---

## Testing

### `UaUkJsonMappingSpec`
- Loads `ua_uk.json` from test resources; verifies `fieldMap` and `sqlMap` are built correctly
- `applyMapping`: source field renamed to dest (`назва` → `name`)
- `applyMapping`: dest re-mapped via sql_data Field (`rayon` → `adm2`)
- `applyMapping`: Text literal injected (`adm0` = `"ua"`, `lang` = `"uk"`)
- `applyMapping`: empty-dest source field kept under original name
- `applyMapping`: field absent from all mappings kept as-is
- Full round-trip: realistic monument row maps to expected output map

### `MonumentCsvExporterSpec`
- Single row: header matches keys, values correct
- Multiple rows with different key sets: header is union of all keys
- Values containing commas, quotes, and newlines are properly escaped
- Empty input: produces empty output (or header-only, TBD)

### `MonumentQueryApiSpec` (extend or add)
- `byMonumentTemplateMaps` returns correct `Iterable[Map[String, String]]` using `MockBotSpec`/`HttpStub` with canned wiki pages
- `fetchPages` refactor: existing `byMonumentTemplate` tests continue to pass unchanged

### `StatisticsIntegrationSpec` (extend or add)
- `--export-csv` flag triggers CSV export path; statistics reporters are not invoked
- Omitting filename: default filename matches pattern `<campaign>-\d{4}-\d{2}-\d{2}-\d{4}\.csv`
- Providing filename: that exact path is used

---

## Files Changed / Created

| File | Change |
|------|--------|
| `scalawiki-wlx/.../query/MonumentQuery.scala` | Add `byMonumentTemplateMaps` to trait and impl; extract `fetchPages` |
| `scalawiki-wlx/.../UaUkJsonMapping.scala` | New — JSON mapping loader and applier |
| `scalawiki-wlx/.../MonumentCsvExporter.scala` | New — CSV writer |
| `scalawiki-wlx/.../stat/StatParams.scala` | Add `--export-csv` flag |
| `scalawiki-wlx/.../stat/Statistics.scala` | Wire up export path in `main()` |
| `scalawiki-wlx/src/main/resources/monuments_config/ua_uk.json` | Fill in empty-dest entries incrementally |
| Test files (4 specs above) | New |

---

## Out of Scope

- CSV import / round-trip back to wiki
- Support for non-UA campaigns in the JSON mapping (ua_uk.json is UA-specific)
- GUI or web interface
