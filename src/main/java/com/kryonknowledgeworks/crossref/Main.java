package com.kryonknowledgeworks.crossref;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        try {
            Arguments parsed = Arguments.parse(args);
            if (parsed.help) {
                printUsage();
                return;
            }
            new CrossrefTransformer().transform(parsed.input, parsed.metadata, parsed.output, parsed.parameters);
            System.out.println("Created " + parsed.output.toAbsolutePath().normalize());
        } catch (Exception e) {
            System.err.println("Transformation failed: " + e.getMessage());
            System.exit(2);
        }
    }

    private static void printUsage() {
        System.out.println("""
                Usage:
                  java -jar jats-crossref-transformer-*-all.jar \\
                    --input article.xml --meta metadata.xml --output deposit.xml \\
                    [--param metadata_precedence=meta-first] [--param require_issn=false]

                Parameters:
                  metadata_precedence  meta-first (default) or jats-first
                  require_issn         true to fail when neither source contains an ISSN
                  ignore_issn          legacy compatibility alias (false means ISSN is required)
                  emit_free_to_read    true only when content should explicitly be marked free to read
                """);
    }

    private static final class Arguments {
        private Path input;
        private Path metadata;
        private Path output;
        private boolean help;
        private final Map<String, String> parameters = new LinkedHashMap<>();

        private static Arguments parse(String[] args) {
            Arguments result = new Arguments();
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--help", "-h" -> result.help = true;
                    case "--input" -> result.input = Path.of(value(args, ++i, "--input"));
                    case "--meta" -> result.metadata = Path.of(value(args, ++i, "--meta"));
                    case "--output" -> result.output = Path.of(value(args, ++i, "--output"));
                    case "--param" -> {
                        String assignment = value(args, ++i, "--param");
                        int separator = assignment.indexOf('=');
                        if (separator < 1) {
                            throw new IllegalArgumentException("--param must be name=value");
                        }
                        result.parameters.put(assignment.substring(0, separator), assignment.substring(separator + 1));
                    }
                    default -> throw new IllegalArgumentException("Unknown argument: " + args[i]);
                }
            }
            if (!result.help && (result.input == null || result.metadata == null || result.output == null)) {
                throw new IllegalArgumentException("--input, --meta, and --output are required; use --help for usage");
            }
            return result;
        }

        private static String value(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            return args[index];
        }
    }
}
