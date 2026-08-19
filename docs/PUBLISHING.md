# Publishing to Maven Central

Releases are published to Maven Central through the Sonatype Central Publisher Portal. Consumer projects do not need GitHub credentials, a Maven `settings.xml` entry, or a custom `<repository>` declaration.

## One-time publisher setup

1. Sign in at <https://central.sonatype.com/>.
2. Create and verify the namespace `com.kryonknowledgeworks`. This normally requires proving control of `kryonknowledgeworks.com`.
3. On the Central Portal account page, generate a user token. The generated token username and token password are distinct from the interactive account password.
4. Create a GPG signing key for the organization and publish its public key to a public keyserver.
5. Add these GitHub Actions repository secrets:

   | Secret | Value |
   |---|---|
   | `CENTRAL_USERNAME` | Central Portal user-token username |
   | `CENTRAL_TOKEN` | Central Portal user-token password |
   | `GPG_PRIVATE_KEY` | ASCII-armored private key exported with `gpg --armor --export-secret-keys KEY_ID` |
   | `GPG_PASSPHRASE` | Passphrase protecting that private key |

Never commit Central credentials, a private signing key, or its passphrase.

## Release configuration

The `central-release` Maven profile:

- signs the POM, normal library JAR, sources JAR, and Javadoc JAR;
- creates the Maven Central bundle and checksums;
- uploads, validates, and publishes the release through the Central Portal;
- waits until the deployment reaches the published state.

Normal `mvn verify` builds do not activate signing or publishing.

## Release procedure

Maven Central releases are immutable. Choose a new version and verify it before creating its tag. The repository is currently prepared for `2.0.1`:

```shell
mvn versions:set -DnewVersion=2.0.1 -DgenerateBackupPoms=false
mvn clean verify
git add pom.xml
git commit -m "Release 2.0.1"
git tag -a v2.0.1 -m "JATS to Crossref transformer 2.0.1"
git push origin main --follow-tags
```

The tag workflow verifies that `v2.0.1` exactly matches POM version `2.0.1`, reruns the tests, signs every artifact, and publishes it to Maven Central. Do not move or reuse a release tag.

After publication, advance the branch:

```shell
mvn versions:set -DnewVersion=2.0.2-SNAPSHOT -DgenerateBackupPoms=false
git add pom.xml
git commit -m "Start 2.0.2 development"
git push origin main
```

## Local Central deployment

CI is preferred. For an authorized local deployment, put the generated Central user-token credentials in the developer's `~/.m2/settings.xml`:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd">
  <servers>
    <server>
      <id>central</id>
      <username>${env.CENTRAL_USERNAME}</username>
      <password>${env.CENTRAL_TOKEN}</password>
    </server>
  </servers>
</settings>
```

Import the signing key into GnuPG, supply its passphrase securely through the `MAVEN_GPG_PASSPHRASE` environment variable or `gpg-agent`, and run:

```shell
mvn -Pcentral-release clean deploy
```

## Consumer configuration

Once Central has synchronized the release, another Maven project needs only:

```xml
<dependency>
  <groupId>com.kryonknowledgeworks</groupId>
  <artifactId>jats-crossref-transformer</artifactId>
  <version>2.0.1</version>
</dependency>
```

The dependency-bundled `all` CLI JAR is intentionally not published to Maven Central. It remains a local build artifact; consumers should use the normal library artifact.
