# Polarion ALM extension to compare objects of different types

This Polarion extension provides functionality of diffing content of 2 documents (either different or the same in different revisions) and later to merge selected diffs in both directions.

## Quick start (deployment only - no compilation needed!)

The latest version of the extension can be downloaded from the [releases page](../../releases/latest) and installed to Polarion instance without needs to be compiled from the sources.
The extension should be copied to `<polarion_home>/polarion/extensions/ch.sbb.polarion.extension.diff-tool/eclipse/plugins` and changes will take effect after Polarion restart.
> [!IMPORTANT]
> Don't forget to clear `<polarion_home>/data/workspace/.config` folder after extension installation to make it work properly.

## Compatibility

This extension is compatible with Polarion 2310 and 2404.
This extension is compatible with Java 17.
This extension is compatible with PDF-Exporter v7.x.x. If you are using PDF-Exporter v6.x.x, please use version 3.x.x of this extension.

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

## Extension Configuration

1. On the top of the project's navigation pane click ⚙ (Actions) ➙ 🔧 Administration. Project's administration page will be opened.
2. On the administration's navigation pane select `Diff Tool`. There are 2 sub-menus with different configuration options for Diff Tool.
3. They have either `Quick Help` section with short description or their content is self-evident.
4. To change configuration of Diff Tool extension just edit corresponding section and press `Save` button.

## Usage

1. Open a document in Polarion.
2. In the toolbar choose Show Sidebar ➙ Document Properties.
3. Choose another document and desired options in the `Diff Tool` block and click `Compare`.
4. Another tab will be opened in browser listing documents differences. Later you can select certain differences and merge them in any direction.


## REST API
This extension provides REST API. OpenAPI Specification can be obtained [here](docs/openapi.json).

## Calling REST endpoints from a Live Report Page

```html
<script>
function compare() {
    const projectId1 = document.getElementById('projectId1').value;
    const space1 = document.getElementById('space1').value;
    const name1 = document.getElementById('name1').value;
    const projectId2 = document.getElementById('projectId2').value;
    const space2 = document.getElementById('space2').value;
    const name2 = document.getElementById('name2').value;
    const requestBody = JSON.stringify({'module1': {"projectId": projectId1, "space": space1, "name": name1}, 'module2': {"projectId": projectId2, "space": space2, "name": name2}});
    fetch('/polarion/diff-tool/rest/internal/comparison/documents', {
        method: 'POST',
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        },
        body: requestBody
    }).then(response => {
        if (response.ok) {
            return response.json().then(data => data.equal ? "Equal!" : "Not equal!")
        } else {
            return response.text()
        }
    }).then(text => {
        alert(text)
    });
}
</script>

<input id='projectId1' type='text' value='elibrary'/>
<input id='space1' type='text' value='_default'/>
<input id='name1' type='text' value='Doc1'/><br/>
<input id='projectId2' type='text' value='elibrary'/>
<input id='space2' type='text' value='_default'/>
<input id='name2' type='text' value='Doc2'/><br/>
<button onclick='compare()'>Compare</button>
```

## Calling diff methods from Velocity Context

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
