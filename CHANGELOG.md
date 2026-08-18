# Changelog

## Unreleased

Development continues as `2.0.1-SNAPSHOT`.

## 2.0.0 - 2026-08-18

- Upgrade generated deposits from Crossref schema 4.3.6 to 5.5.0.
- Validate transformation tests against the official Crossref 5.5.0 XSD 1.1 schema.
- Use the `similarity-check` crawler identifier for current deposits.
- Preserve the expanded Crossref contributor-role vocabulary supported by 5.5.0.

This is a major release because the output namespace and schema contract changed.

## 1.0.0

- Initial centralized Saxon-based JATS-to-Crossref transformer.
