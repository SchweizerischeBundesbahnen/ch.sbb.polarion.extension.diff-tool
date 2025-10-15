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

...and then to merge selected diffs in any direction. First 2 options are available either via selecting 2 certain documents
or via a collection of documents.

In case of diffing work items, appropriate counterpart work items (from another document or another project) are always seeking
by selected link role.

Additionally, the extension provides functionality to make a copy of selected document in other location.

> [!IMPORTANT]
> Starting from version 5.0.0 only latest version of Polarion is supported.
> Right now it is Polarion 2506.

## Quick start

The latest version of the extension can be downloaded from the [releases page](../../releases/latest) and installed to Polarion instance without necessity to be compiled from the sources.
The extension should be copied to `<polarion_home>/polarion/extensions/ch.sbb.polarion.extension.diff-tool/eclipse/plugins` and changes will take effect after Polarion restart.
> [!IMPORTANT]
> Don't forget to clear `<polarion_home>/data/workspace/.config` folder after extension installation/update to make it work properly.

## Compatibility

This extension is compatible with:
* Polarion 2506
* Java 17
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
mvn clean install -P install-to-local-polarion
```

For automated installation with maven env variable `POLARION_HOME` should be defined and point to folder where Polarion is installed.

Changes only take effect after restart of Polarion.

## Polarion configuration

### Documents comparison form to appear on a Document's properties pane

1. Open a project where you wish Documents comparison to be available
2. On the top of the project's navigation pane click ⚙ (Actions) ➙ 🔧 Administration. Project's administration page will be opened.
3. On the administration's navigation pane select Documents & Pages ➙ Document Properties Sidebar.
4. In opened Edit Project Configuration editor find `sections`-element:
   ```xml
   …
   <sections>
     <section id="fields"/>
     …
   </sections>
   …
   ```
5. And insert following new line inside this element:
   ```xml
   …
   <extension id="diff-tool" label="Documents Comparison" />
   …
   ```
6. Save changes by clicking 💾 Save

### Documents copy form to appear on a Document's properties pane
Repeat the instructions above except that on the step 5 use the following line:
   ```xml
   …
   <extension id="copy-tool" label="Documents Copy" />
   …
   ```

### Nodes for collections and work items diffing to appear in Polarion's navigation tree
1. Open a project where you wish these nodes to be available
2. On the top of the project's navigation pane click ⚙ (Actions) ➙ 🔧 Administration. Project's administration page will be opened.
3. On the administration's navigation pane select Portal ➙ Topics.
4. Depending on which view type you are using choose to edit either Default or Admin view.
5. In opened Topics Configuration editor insert following line inside `topics`-element:
   ```xml
   …
   <topic id="diff-tool"/>
   …
   ```
6. Save changes by clicking 💾 Save

### Fine-tuning the communication between Polarion and Diff Tool extension

The Diff Tool UI makes numerous requests to Polarion using the REST API to retrieve information about documents and their workitems. These requests can be processed in parallel to improve performance.
The number of parallel requests can be configured in `polarion.properties` file:

```properties
ch.sbb.polarion.extension.diff-tool.chunk.size=2
```

Default value is `2`. Increasing this value may speed up the process but can also overload your Polarion server.

## Extension Configuration

1. On the top of the project's navigation pane click ⚙ (Actions) ➙ 🔧 Administration. Project's administration page will be opened.
2. On the administration's navigation pane select `Diff Tool`. There are 2 sub-menus with different configuration options for Diff Tool.
3. They have either `Quick Help` section with short description or their content is self-evident.
4. To change configuration of Diff Tool extension just edit corresponding section and press `Save` button.

## Usage
### Documents diffing
1. Open a document in Polarion.
2. In the toolbar choose Show Sidebar ➙ Document Properties ➙ Documents Comparison.
3. Choose another document and desired options and click `Compare`.
4. Another tab will be opened in browser listing documents differences. Later you can select certain differences and merge them in any direction.

### Document copy creation
1. Open a document in Polarion.
2. In the toolbar choose Show Sidebar ➙ Document Properties ➙ Documents Copy.
3. Choose destination of target document, desired options and click `Create Document`.
4. When document is created you will see success message with a link to it.

### Work items diffing
1. In Polarion navigation tree choose Diff Tool ➙ Multiple Work Items.
2. Choose target project, link role by which items to be linked, then select work items from table below to be compared, preliminary filtering them to show ones you need.
3. Finally, click Compare button.
4. Another tab will be opened in browser listing selected work items differences. Later you can select certain differences and merge them in any direction.

### Collections diffing
1. In Polarion navigation tree choose Diff Tool ➙ Collections.
2. You will see 2 tables listing collections. Left one is displaying collections from current project, right one is displaying collections from project selected in dropdown Target project.
3. Select one collection from left table and one collection from right table, preliminary filtering them to show ones you need.
4. Choose link role by which work items to be linked, then click Compare button.
5. Another tab will be opened in browser listing documents differences. You can change documents from collections to be compared in side pane. Later you can select certain differences and merge them in any direction.

## REST API

This extension provides REST API. OpenAPI Specification can be obtained [here](docs/openapi.json).

## Calling diff methods from Velocity Context
### Diffing text or HTML content
`diffText` and `diffHtml` functions are available in Velocity context referenced by `$diffTool` variable.
Example:

```velocity
$diffTool.diffText("Some text", "Some another text").getResult()
```

or

```velocity
$diffTool.diffHtml("<html><body><div>Some text</div></body></html>", "<html><body><div>Some another text</div></body></html>").getResult()
```

Also, `isDifferent` can be used if you need to show something specific for cases when the values are the same:

```velocity
#set($diffResult = $diffTool.diffText("Some text", "Some text"))
#if($diffResult.isDifferent())
  $diffResult.getResult()
#else
  No changes
#end
```
### Diffing work items w/o document context

`diffWorkItems` compares two work items and returns a WorkItemsPairDiff object containing all differences.

### Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `leftProjectId` | `String` | Yes | The project ID of the left (reference) work item for comparison context |
| `workItemA` | `IWorkItem` | No | The first work item to compare (left side). Can be `null`. |
| `workItemB` | `IWorkItem` | No | The second work item to compare (right side). Can be `null`. |
| `configName` | `String` | Yes | The name of the diff configuration to use (e.g., "Default"). Determines which fields are compared and how differences are calculated. |
| `linkRole` | `String` | No | The role/type of link between paired work items (e.g., "parent", "relates_to"). Can be `null` if no link relationship needs to be considered. |

### Returns

`WorkItemsPairDiff` - An object containing all field-level differences between the work items, accessible via `fieldDiffsMap` where each entry contains the field name and diff values.

### Example Usage

```velocity
## Get current project object
#set($projectId = $page.fields().project().projectId())
#set($project = $projectService.getProject($projectId))

<h2>Selected Work Items in Project: $project.name</h2>


## Example: Specific IDs in this project
#set($query = "project.id:$projectId AND (id:EL-1 OR id:EL-2)")
#set($workItems = $trackerService.queryWorkItems($query, "id"))

#set($wiA = $workItems.get(0))
#set($wiB = $workItems.get(1))

 #set($diffResult = $diffTool.diffWorkItems($wiA, $wiB, "Default", ""))


<h3>Work Item Differences</h3>

<table border="1" cellspacing="0" cellpadding="5">
  <tr>
    <th>Field</th>
    <th>$wiA.id</th>
    <th>$wiB.id</th>
  </tr>

## Loop through all field differences
#foreach($fieldId in $diffResult.fieldDiffsMap.keySet())
  #set($fieldDiff = $diffResult.fieldDiffsMap.get($fieldId))
  <tr>
    <td>$fieldDiff.getName()</td>
    <td>$fieldDiff.getDiffLeft()</td>
    <td>$fieldDiff.getDiffRight()</td>
  </tr>
#end
</table>
```
