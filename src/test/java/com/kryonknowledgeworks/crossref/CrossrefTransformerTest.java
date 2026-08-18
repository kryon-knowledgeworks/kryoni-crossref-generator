package com.kryonknowledgeworks.crossref;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathFactory;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrossrefTransformerTest {
    @TempDir
    Path temp;

    @Test
    void transformsWithMetaFirstPolicyAndNormalizesOrcid() throws Exception {
        Path jats = temp.resolve("article.xml");
        Path meta = temp.resolve("meta.xml");
        Path output = temp.resolve("deposit.xml");
        Files.writeString(jats, """
                <article xmlns:xlink="http://www.w3.org/1999/xlink">
                  <front>
                    <journal-meta>
                      <journal-title-group><journal-title>JATS title</journal-title></journal-title-group>
                      <issn pub-type="epub">1111-2222</issn>
                      <publisher><publisher-name>Example Publisher</publisher-name></publisher>
                    </journal-meta>
                    <article-meta>
                      <article-id pub-id-type="doi">10.5555/example</article-id>
                      <title-group><article-title>Example article</article-title></title-group>
                      <contrib-group><contrib contrib-type="author"><contrib-id contrib-id-type="orcid">0000-0001-2345-6789</contrib-id><name><surname>Doe</surname><given-names>Jane</given-names></name></contrib></contrib-group>
                      <pub-date pub-type="epub"><year>2026</year></pub-date>
                      <permissions><license license-type="open-access" xlink:href="https://creativecommons.org/licenses/by/4.0/"/></permissions>
                    </article-meta>
                  </front>
                  <back>
                    <ref-list>
                      <ref id="R1"><element-citation publication-type="journal">
                        <article-title>Cited article</article-title><source>Cited Journal</source><year>2025</year>
                        <comment><ext-link ext-link-type="doi" xlink:href="https://doi.org/10.1234/Cited.DOI"/></comment>
                      </element-citation></ref>
                    </ref-list>
                  </back>
                </article>
                """);
        Files.writeString(meta, """
                <meta>
                  <depositor_name>Deposit Team</depositor_name>
                  <email_address>deposit@example.org</email_address>
                  <registrant>Example Publisher</registrant>
                  <journalTitle>Configured title</journalTitle>
                  <issnOnline>1234-5679</issnOnline>
                  <landingUrl>https://example.org/articles/example</landingUrl>
                  <license>https://creativecommons.org/licenses/by/4.0/</license>
                  <license>https://creativecommons.org/licenses/by/4.0/</license>
                </meta>
                """);

        new CrossrefTransformer().transform(jats, meta, output, Map.of("require_issn", "true"));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        var document = factory.newDocumentBuilder().parse(output.toFile());
        assertEquals("Configured title", document.getElementsByTagNameNS("*", "full_title").item(0).getTextContent());
        assertEquals("http://www.crossref.org/schema/5.5.0", document.getDocumentElement().getNamespaceURI());
        assertEquals("5.5.0", document.getDocumentElement().getAttribute("version"));
        assertEquals("1234-5679", document.getElementsByTagNameNS("*", "issn").item(0).getTextContent());
        assertEquals("https://orcid.org/0000-0001-2345-6789", document.getElementsByTagNameNS("*", "ORCID").item(0).getTextContent());
        assertEquals("10.1234/Cited.DOI", XPathFactory.newInstance().newXPath()
                .evaluate("string(//*[local-name()='citation']/*[local-name()='doi'])", document));
        assertEquals(1, document.getElementsByTagNameNS("http://www.crossref.org/AccessIndicators.xsd", "license_ref").getLength());
        assertEquals(0, document.getElementsByTagNameNS("http://www.crossref.org/AccessIndicators.xsd", "free_to_read").getLength());
        validateAgainstCrossref55(output);

        Path freeToReadOutput = temp.resolve("deposit-free-to-read.xml");
        new CrossrefTransformer().transform(jats, meta, freeToReadOutput,
                Map.of("require_issn", "true", "emit_free_to_read", "true"));
        var freeToReadDocument = factory.newDocumentBuilder().parse(freeToReadOutput.toFile());
        assertEquals(1, freeToReadDocument
                .getElementsByTagNameNS("http://www.crossref.org/AccessIndicators.xsd", "free_to_read").getLength());
    }

    @Test
    void supportsJatsFirstWithoutAnotherStylesheet() throws Exception {
        Path jats = temp.resolve("article.xml");
        Path meta = temp.resolve("meta.xml");
        Path output = temp.resolve("deposit.xml");
        Files.writeString(jats, "<article><front><journal-meta><journal-title-group><journal-title>JATS title</journal-title></journal-title-group><publisher><publisher-name>P</publisher-name></publisher></journal-meta><article-meta><article-id pub-id-type=\"doi\">10.5555/x</article-id><title-group><article-title>A</article-title></title-group></article-meta></front></article>");
        Files.writeString(meta, "<meta><depositor_name>D</depositor_name><email_address>d@example.org</email_address><journalTitle>Meta title</journalTitle><landingUrl>https://example.org/x</landingUrl></meta>");

        new CrossrefTransformer().transform(jats, meta, output, Map.of("metadata_precedence", "jats-first"));

        String result = Files.readString(output);
        assertTrue(result.contains("<full_title>JATS title</full_title>"));
    }

    private static void validateAgainstCrossref55(Path output) throws Exception {
        SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
        var schema = schemaFactory.newSchema(
                URI.create("https://data.crossref.org/schemas/crossref5.5.0.xsd").toURL());
        schema.newValidator().validate(new javax.xml.transform.stream.StreamSource(output.toFile()));
    }
}
