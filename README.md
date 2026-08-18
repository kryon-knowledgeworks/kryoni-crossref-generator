# JATS to Crossref transformer

This repository is the single versioned owner of the JATS-to-Crossref 5.5.0 transformation. It packages the canonical XSLT with SaxonJ-HE behind a Java API and an executable CLI. Consumer projects depend on a released Maven version; they do not copy or rename the stylesheet.

## Why one stylesheet

The supplied `_web.xsl` and `_web1.xsl` files are forks, not cleanly separated formats. The canonical stylesheet starts from the newer fork, restores JATS ISSN fallback and ORCID normalization, and exposes the actual project choices as parameters:

| Parameter | Default | Purpose |
|---|---|---|
| `metadata_precedence` | `meta-first` | Use `meta-first` for current journal/DB values or `jats-first` for source-document values. |
| `require_issn` | `false` | Fail instead of omitting ISSN when neither source supplies one. |
| `ignore_issn` | `true` | Compatibility alias for old callers; `false` requires an ISSN. |
| `emit_free_to_read` | `false` | Explicitly emit the separate Crossref free-to-read access indicator. |

Add a second XSLT only if a consumer has a genuinely different output contract (for example, another Crossref schema generation). In that case, make it a thin named profile that imports shared modules—never `web2`, `final`, or copied files.

## Build and run

Requirements: JDK 17+ and Maven 3.9+.

```shell
mvn clean verify
java -jar target/jats-crossref-transformer-2.0.0-SNAPSHOT-all.jar \
  --input article.xml \
  --meta metadata.xml \
  --output crossref.xml \
  --param metadata_precedence=meta-first \
  --param require_issn=false
```

`mvn verify` validates generated output against Crossref's official XSD 1.1 schema at `https://data.crossref.org/schemas/crossref5.5.0.xsd`; the test therefore requires network access.

The XML reader permits the JATS `DOCTYPE` but disables loading external DTDs and entities, so transformations do not depend on a machine-local DTD and do not fetch arbitrary external resources from the input document.

## Consume from another Maven project

First install a snapshot locally while developing:

```shell
mvn install
```

Then use the artifact from the consumer:

```xml
<dependency>
  <groupId>com.kryonknowledgeworks</groupId>
  <artifactId>jats-crossref-transformer</artifactId>
  <version>2.0.0</version>
</dependency>
```

```java
new CrossrefTransformer().transform(
    Path.of("article.xml"),
    Path.of("metadata.xml"),
    Path.of("crossref.xml"),
    Map.of("metadata_precedence", "meta-first"));
```

For shared use, publish releases to your existing Nexus/Artifactory/GitHub Packages repository. Keep repository credentials in Maven `settings.xml`, not in this repository.

## Versioning and maintenance

- Use semantic versions for this artifact. Bug-compatible mapping fixes are patches; new optional mapping features are minors; output-breaking changes or a schema-generation change are majors.
- Pin consumer projects to a released version. Avoid `LATEST`, version ranges, and direct XSL filesystem paths.
- Change the XSLT and its tests in the same pull request. `mvn verify` compiles the stylesheet and runs transformation tests.
- Reference DOIs are discovered in nested `pub-id` or `ext-link` structures and normalized from resolver URLs or `doi:` prefixes to bare DOI values.
- License mapping selects either metadata or JATS according to `metadata_precedence`, deduplicates identical entries, and emits one `license_ref` per distinct license/application tuple. Open-access licenses do not implicitly emit the separate `free_to_read` indicator.
- Tag releases (`v1.0.0`) and retain `SNAPSHOT` only on active development branches.
- Before public redistribution, confirm and document the license/provenance of the Aptara/Crossref-derived stylesheet. SaxonJ-HE itself is MPL-2.0.

The Crossref output uses schema `5.5.0`. Treat future Crossref schema upgrades as intentional, tested releases rather than silently changing them with a Saxon upgrade.
