package com.kryonknowledgeworks.crossref;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.Xslt30Transformer;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XdmAtomicValue;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Thread-safe facade around the packaged, immutable stylesheet. */
public final class CrossrefTransformer {
    private static final String STYLESHEET = "/xslt/jats-to-crossref.xsl";

    private final Processor processor;
    private final XsltExecutable executable;

    public CrossrefTransformer() {
        try {
            processor = new Processor(false);
            XsltCompiler compiler = processor.newXsltCompiler();
            URL resource = CrossrefTransformer.class.getResource(STYLESHEET);
            if (resource == null) {
                throw new IllegalStateException("Packaged stylesheet is missing: " + STYLESHEET);
            }
            executable = compiler.compile(new StreamSource(resource.toExternalForm()));
        } catch (SaxonApiException e) {
            throw new IllegalStateException("Packaged stylesheet does not compile", e);
        }
    }

    public void transform(Path jatsXml, Path metadataXml, Path outputXml)
            throws IOException, SaxonApiException {
        transform(jatsXml, metadataXml, outputXml, Map.of());
    }

    public void transform(Path jatsXml, Path metadataXml, Path outputXml, Map<String, String> parameters)
            throws IOException, SaxonApiException {
        Path input = jatsXml.toAbsolutePath().normalize();
        Path metadata = metadataXml.toAbsolutePath().normalize();
        Path output = outputXml.toAbsolutePath().normalize();
        if (!Files.isRegularFile(input)) {
            throw new IOException("JATS input does not exist: " + input);
        }
        if (!Files.isRegularFile(metadata)) {
            throw new IOException("Metadata input does not exist: " + metadata);
        }
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }

        Map<QName, net.sf.saxon.s9api.XdmValue> stylesheetParameters = new LinkedHashMap<>();
        stylesheetParameters.put(new QName("meta"), new XdmAtomicValue(metadata.toUri().toString()));
        parameters.forEach((name, value) ->
                stylesheetParameters.put(new QName(name), new XdmAtomicValue(value)));

        try (InputStream inputStream = Files.newInputStream(input);
             OutputStream outputStream = Files.newOutputStream(output)) {
            Xslt30Transformer transformer = executable.load30();
            transformer.setStylesheetParameters(stylesheetParameters);
            Serializer serializer = processor.newSerializer(outputStream);
            serializer.setOutputProperty(Serializer.Property.INDENT, "yes");
            transformer.transform(safeSource(input, inputStream), serializer);
        }
    }

    private static SAXSource safeSource(Path path, InputStream stream) throws IOException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XMLReader reader = factory.newSAXParser().getXMLReader();
            reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
            reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            InputSource inputSource = new InputSource(stream);
            inputSource.setSystemId(path.toUri().toString());
            return new SAXSource(reader, inputSource);
        } catch (ParserConfigurationException | org.xml.sax.SAXException e) {
            throw new IOException("Unable to configure secure XML parser", e);
        }
    }
}
