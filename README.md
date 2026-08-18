# JATS to Crossref transformer

This repository is the single versioned owner of the JATS-to-Crossref transformation. It packages the canonical XSLT with SaxonJ-HE behind a Java API and an executable CLI. Consumer projects depend on a released Maven version; they do not copy or rename the stylesheet.

## Why one stylesheet

The supplied `_web.xsl` and `_web1.xsl` files are forks, not cleanly separated formats. The canonical stylesheet starts from the newer fork, restores JATS ISSN fallback and ORCID normalization, and exposes the actual project choices as parameters:

| Parameter | Default | Purpose |
|---|---|---|
| `metadata_precedence` | `meta-first` | Use `meta-first` for current journal/DB values or `jats-first` for source-document values. |
| `require_issn` | `false` | Fail instead of omitting ISSN when neither source supplies one. |
| `ignore_issn` | `true` | Compatibility alias for old callers; `false` requires an ISSN. |

Add a second XSLT only if a consumer has a genuinely different output contract (for example, another Crossref schema generation). In that case, make it a thin named profile that imports shared modules—never `web2`, `final`, or copied files.

## Build and run

Requirements: JDK 17+ and Maven 3.9+.

```shell
mvn clean verify
java -jar target/jats-crossref-transformer-1.0.0-SNAPSHOT-all.jar \
  --input article.xml \
  --meta metadata.xml \
  --output crossref.xml \
  --param metadata_precedence=meta-first \
  --param require_issn=false
```

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
  <version>1.0.0</version>
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
- Tag releases (`v1.0.0`) and retain `SNAPSHOT` only on active development branches.
- Before public redistribution, confirm and document the license/provenance of the Aptara/Crossref-derived stylesheet. SaxonJ-HE itself is MPL-2.0.

The Crossref output currently remains schema `4.3.6`, matching the supplied stylesheet. Treat a future Crossref schema upgrade as an intentional, tested release rather than silently changing it with a Saxon upgrade.
