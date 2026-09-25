[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.diff-tool&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.diff-tool)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.diff-tool&metric=bugs)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.diff-tool)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.diff-tool&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.diff-tool)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.diff-tool&metric=coverage)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.diff-tool)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.diff-tool&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.diff-tool)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.diff-tool&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.diff-tool)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.diff-tool&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.diff-tool)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.diff-tool&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.diff-tool)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.diff-tool&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.diff-tool)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.diff-tool&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.diff-tool)

# Polarion ALM extension for diffing and merging

This Polarion extension provides functionality of diffing:
* fields of 2 documents (either different or the same in different revisions)
* work items of 2 documents (also either of different documents or of the same in different revisions)
* inlined content of 2 documents, inserted there not via work items, but directly
* arbitrary set of work items in one project with their counterpart work items from another project

...and then to merge selected diffs from left to right document. First 2 options are available either via selecting 2 certain documents
or via a collection of documents.

In case of diffing work items, appropriate counterpart work items (from another document or another project) are always seeking
by selected link role.

Additionally, the extension provides functionality to make a copy of selected document in other location, as well as to copy
or move a chapter of one document, with everything below it, into another document.

> [!IMPORTANT]
> Starting from version 5.0.0 only latest version of Polarion is supported.
> Right now it is Polarion 2606.

## Documentation

| Document | Description |
|---|---|
| [Quick start](QUICK_START.md) | Install the extension and enable it in a project |
| [User guide](USER_GUIDE.md) | Documents diffing, copy and chapter merge, work items and collections diffing |
| [Configuration](CONFIGURATION.md) | Enabling the forms and navigation nodes, performance tuning, chapter merge timeouts, project duplication |
| [Velocity API](VELOCITY_API.md) | `$diffTool.diffText`, `diffHtml` and `diffWorkItems` in the Velocity context |
| [REST API](docs/openapi.json) | OpenAPI specification |

## Compatibility

This extension is compatible with:
* Polarion 2606
* Java 21
* [PDF-Exporter](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter) v9

## Build

This extension can be produced using maven:

```bash
mvn clean package
```

## Installation to Polarion

To install the extension to Polarion `ch.sbb.polarion.extension.diff-tool-<version>.jar`
should be copied to `<polarion_home>/polarion/extensions/ch.sbb.polarion.extension.diff-tool/eclipse/plugins`
It can be done manually or automated using maven build:

```bash
mvn clean install -P local-install-into-polarion
```

For automated installation with maven env variable `POLARION_HOME` should be defined and point to folder where Polarion is installed.

Changes only take effect after restart of Polarion.

## REST API

This extension provides REST API. OpenAPI Specification can be obtained [here](docs/openapi.json).
