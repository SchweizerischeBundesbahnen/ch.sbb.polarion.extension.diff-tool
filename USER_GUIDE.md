# User Guide

How to compare, copy and merge with the Diff Tool. The forms and navigation nodes used below have to be enabled in
the project first, see [Configuration](CONFIGURATION.md).

## Documents diffing
1. Open a document in Polarion.
2. In the toolbar choose Show Sidebar ➙ Document Properties ➙ Documents Comparison.
3. Choose another document and desired options and click `Compare`.
4. Another tab will be opened in browser listing documents differences. Later you can select certain differences and merge them from left to right document.

## Document copy creation
1. Open a document in Polarion.
2. In the toolbar choose Show Sidebar ➙ Document Properties ➙ Documents Copy.
3. Choose destination of target document, desired options and click `Create Document`.
4. When document is created you will see success message with a link to it.

## Chapter merge
1. Open the document into which content should be placed.
2. In the toolbar choose Show Sidebar ➙ Document Properties ➙ Documents Merge.
3. Choose the source document and enter the outline number of the chapter to be merged, eg. `2.1.1`.
4. Choose the copy mode: `copy` creates new work items, `move` moves work items and copies headings.
5. Enter the outline number of the chapter of the current document which is used as an anchor, eg. `3.1`.
6. Choose the insert mode: `under` places the content directly under the anchor chapter, `after` makes it a new
   chapter of the same level. Heading levels and outline numbers are adjusted accordingly, so anchor chapter `3.1`
   with insert mode `after` and source chapter `2` results in chapter `3.2`.
7. Click `Merge Chapter`. A dialog states what the merge will do and waits to be confirmed.
8. Confirm. The merge runs in the background, and the dialog it was confirmed in shows what it is doing. It cannot
   be closed while the merge runs.
9. When the merge has finished, the same dialog states what it did. Closing it reloads the document if the merge
   changed it.

> [!NOTE]
> A copied work item is never linked to the work item it was copied from. A work item cannot change its project, so in
> `move` mode a work item of another project is referenced in the target document and removed from the source one.

## Work items diffing
1. In Polarion navigation tree choose Diff Tool ➙ Multiple Work Items.
2. Choose target project, link role by which items to be linked, then select work items from table below to be compared, preliminary filtering them to show ones you need.
3. Finally, click Compare button.
4. Another tab will be opened in browser listing selected work items differences. Later you can select certain differences and merge them from left to right Work Item.

## Collections diffing
1. In Polarion navigation tree choose Diff Tool ➙ Collections.
2. You will see 2 tables listing collections. Left one is displaying collections from current project, right one is displaying collections from project selected in dropdown Target project.
3. Select one collection from left table and one collection from right table, preliminary filtering them to show ones you need.
4. Choose link role by which work items to be linked, then click Compare button.
5. Another tab will be opened in browser listing documents differences. You can change documents from collections to be compared in side pane. Later you can select certain differences and merge them from left to right document.

## Diff views
The diff view shows the differences in one of two views:
- `Side by side`: the source on the left, the target on the right. This is the default.
- `Unified`: one column. Removed content is red, added content is green.

To change the view, select it in `Diff view` in the control pane.
A change of the view keeps the selection and loads nothing again.
The unified view is kept in the page URL as `diffView=unified`, so a reload or a shared link keeps it.

In the unified view, each work item has one header row: its source title, the source ID and the target ID.
Tags on the header row give more information:
- `Only in source`: the work item is not in the target. A merge creates it there.
- `Only in target`: the work item is not in the source. A merge deletes it there.
- `Referenced`: the work item is referenced in its document.

A moved work item is shown at two positions:
- At its source position, with its differences and the tag `Moved to <outline number>`.
- At its target position, as a slim line with the tag `Moved from <outline number>`.

Click the tag to go to the other position.
The side by side view marks the same positions with `moved from` and `moved to`.

## Merging differences
1. In the diff view, select the checkboxes of the differences to merge, or select `Select all`.
2. Click `Merge into target`. The toolbar shows how many items are selected.
3. Read the confirmation dialog and click `Merge`.

A merge always goes from the source into the target.
`Merge into target` is disabled if the target is not the HEAD revision, or if you are not authorized to merge.

If the merge creates work items in the target, the confirmation dialog asks for the link role direction:
- `Direct`: each created work item links to its source work item.
- `Reverse`: each source work item links to the work item created from it.

## Diff colors
The diff view shows removed content in red and added content in green.
To swap the two colors, select `Swap diff colors` in the control pane.
The choice is kept in the page URL as `swapColors=true`, so a reload or a shared link keeps it.
