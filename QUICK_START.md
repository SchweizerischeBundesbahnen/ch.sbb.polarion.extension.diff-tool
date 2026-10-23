# Quick Start

The latest version of the extension can be downloaded from the [releases page](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.diff-tool/releases/latest) and installed to Polarion instance without necessity to be compiled from the sources.
The extension should be copied to `<polarion_home>/polarion/extensions/ch.sbb.polarion.extension.diff-tool/eclipse/plugins` and changes will take effect after Polarion restart.
> [!IMPORTANT]
> Don't forget to clear `<polarion_home>/data/workspace/.config` folder after extension installation/update to make it work properly.

## Enable the extension in a project

After the restart, make the extension's forms and navigation nodes available where they are needed - each is a
one-line addition to the project configuration, described in [Configuration](CONFIGURATION.md):

* the [Documents Comparison](CONFIGURATION.md#documents-comparison-form-to-appear-on-a-documents-properties-pane),
  [Documents Copy](CONFIGURATION.md#documents-copy-form-to-appear-on-a-documents-properties-pane) and
  [Documents Merge](CONFIGURATION.md#documents-merge-form-to-appear-on-a-documents-properties-pane) forms on a
  Document's properties pane,
* the [Diff Tool nodes](CONFIGURATION.md#nodes-for-collections-and-work-items-diffing-to-appear-in-polarions-navigation-tree)
  in the navigation tree, for work items and collections diffing.

How each of them is used is described in the [User Guide](USER_GUIDE.md).
