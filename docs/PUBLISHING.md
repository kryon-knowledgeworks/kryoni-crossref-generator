# Publishing the Maven library

Use a private Maven registry until the redistribution license of the inherited stylesheet has been confirmed. GitHub Packages is the simplest option when the source and consumer projects are already on GitHub; use the organization's Nexus or Artifactory instance if one is already managed centrally.

The normal artifact is the reusable library. The `all` classifier is the executable CLI and should not be used as a consumer dependency.

## GitHub Packages setup

This project is linked to `kryon-knowledgeworks/kryoni-crossref-generator`. Its `pom.xml` already contains this deployment configuration:

```xml
<distributionManagement>
  <repository>
    <id>github</id>
    <name>GitHub Packages</name>
    <url>https://maven.pkg.github.com/kryon-knowledgeworks/kryoni-crossref-generator</url>
  </repository>
  <snapshotRepository>
    <id>github</id>
    <name>GitHub Packages</name>
    <url>https://maven.pkg.github.com/kryon-knowledgeworks/kryoni-crossref-generator</url>
  </snapshotRepository>
</distributionManagement>
```

Put credentials in the developer or CI user's Maven `settings.xml`, never in the POM or Git repository:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd">
  <servers>
    <server>
      <id>github</id>
      <username>${env.GITHUB_ACTOR}</username>
      <password>${env.GITHUB_TOKEN}</password>
    </server>
  </servers>
</settings>
```

The server `id` must match the repository `id` in the POM. For a developer workstation, set `GITHUB_ACTOR` to the GitHub username and `GITHUB_TOKEN` to a classic personal access token with package permissions. The committed GitHub Actions workflow creates Maven settings automatically and uses the repository's `GITHUB_TOKEN` with `contents: read` and `packages: write` permissions.

## Release

Keep `SNAPSHOT` versions for integration testing. Releases are published by `.github/workflows/ci.yml` only from an annotated version tag. The tag must exactly match the non-snapshot POM version:

```shell
mvn versions:set -DnewVersion=2.0.0 -DgenerateBackupPoms=false
mvn clean verify
git add pom.xml
git commit -m "Release 2.0.0"
git tag -a v2.0.0 -m "JATS to Crossref transformer 2.0.0"
git push origin main --follow-tags
```

The tag workflow reruns all tests and then executes `mvn clean deploy`. Never overwrite a released version; increment the patch, minor, or major version and publish a new artifact.

After releasing, advance the development branch to the next snapshot, for example `2.0.1-SNAPSHOT`.

## Consume from another project

Add the registry and normal library dependency to the consumer POM:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/kryon-knowledgeworks/kryoni-crossref-generator</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.kryonknowledgeworks</groupId>
    <artifactId>jats-crossref-transformer</artifactId>
    <version>2.0.0</version>
  </dependency>
</dependencies>
```

The consumer also needs a matching `github` server entry in its user or CI Maven `settings.xml`. GitHub Packages requires authentication even when downloading many package configurations.

## Maven Central later

Maven Central is public and released coordinates are immutable. Do not publish there until the stylesheet provenance and redistribution license are documented. A Central release also needs a project URL, SCM and developer metadata, a declared license, source and Javadoc JARs, and artifact signing. This build already attaches the source and Javadoc JARs; the legal and project identity metadata must be supplied before a Central release.
