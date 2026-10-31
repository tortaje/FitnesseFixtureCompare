# Integrating fitnesse-compare in a content-owning fixture

This is the contract for fixtures that already hold a payload (an HTTP response body, a Kafka record, an MQ message, an
S3 object) and want to offer `|check|compare to file|expected/order.json|OK|` without any new Slim symbol or shared
static state.

## 1. Dependency

Declare the jar `provided`: it is already on the FitNesse `!path` next to your own jar, and your jar gets no new
transitive dependency.

```xml
<dependency>
  <groupId>io.github.tortaje</groupId>
  <artifactId>fitnesse-compare</artifactId>
  <version>1.0.0</version>
  <scope>provided</scope>
</dependency>
```

Pages must put `fitnesse-compare-<version>.jar` on `!path` as well.

## 2. The shortcut

```java
public String compareToFile(String path) {
    try { return Comparisons.compareToFile(Path.of(path), body()).toHtml(); }
    catch (ComparisonException e) { throw new RestFixtureException(e.getMessage(), e); }
}
```

(`body()` is whatever returns the content your fixture holds, as a `String`; `RestFixtureException` is your
fixture's own Slim exception.) Imports: `io.github.tortaje.compare.engine.Comparisons` and
`io.github.tortaje.compare.engine.ComparisonException`.

For the record formats (JSON Lines, text, CSV), pass options; the shortcut above is `CompareOptions.defaults()`:

```java
public String compareToFile(String path, String key) {
    try {
        return Comparisons.compareToFile(Path.of(path), body(), CompareOptions.defaults().key(key)).toHtml();
    } catch (ComparisonException e) { throw new RestFixtureException(e.getMessage(), e); }
}
```

`key(...)` implies `unordered()`; `separator(char)` sets the CSV separator (a `.tsv` expected file defaults to tab).
An option that does not apply to the format (`key` on a `.txt` or `.json` file) is a `ComparisonException`, so the same
`catch` turns it into your fixture's exception. Import `io.github.tortaje.compare.engine.CompareOptions` too.

## 3. Rules

- **Slim name**: use `compare to file` (method `compareToFile`) so pages read the same in every fixture.
- **Return value**: return `toHtml()`. It is `OK` when the content matches, otherwise a `<div>` listing the differences
  (FitNesse renders a returned value as HTML only when it starts with an allowed tag). Pages assert it with `check`.
- **Exceptions**: `ComparisonException` is unchecked and carries a neutral message (no Slim wrapper): missing expected
  file, unknown extension, malformed document, unknown wildcard, invalid regex, option not applicable, CSV field
  count. Wrap it in your fixture's own Slim exception, whose message FitNesse shows as `message:<<…>>` in a
  yellow cell. A difference is never an exception: it only fails the `check`.
- **Paths**: relative to the FitNesse working directory, like any other file argument; files are read as UTF-8.
- **Format**: taken from the expected file's extension (case-insensitive): `.json`, `.xml`, `.jsonl` / `.ndjson`
  (JSON Lines), `.txt` / `.log` (text), `.csv` / `.tsv` (CSV). Anything else is a `ComparisonException` listing them.
  To force a format, call `Comparisons.compare(Format, expected, actual, options)`, or `compareJsonLines`,
  `compareText`, `compareCsv` directly.
- **Wildcards** (`${any}`, `${regex:…}`, …) are interpreted in the expected file only; the actual content is never
  scanned for markers.

## 4. API stability

The package `io.github.tortaje.compare.engine` (`Comparisons`, `Format`, `ComparisonResult`, `Difference`,
`DifferenceKind`, `CompareOptions`, `ComparisonException`) is the public API and follows semantic versioning: no
breaking change before the next major version. Everything under `io.github.tortaje.compare.engine.internal` is not
API and may change in any release; do not use it. The fixture classes (`JsonComparison`, `XmlComparison`,
`JsonLinesComparison`, `TextComparison`, `CsvComparison`) are for wiki pages, not for reuse from Java.
