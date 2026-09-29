# Velocity API

The Diff Tool's diff functions can be called from Velocity, e.g. in a Live Report or a wiki page, through the
`$diffTool` variable of the Velocity context.

## Diffing text or HTML content
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
## Diffing work items w/o document context

`diffWorkItems` compares two work items and returns a WorkItemsPairDiff object containing all differences.

### Parameters

| Parameter       | Type        | Required | Description                                                                                                                                   |
|-----------------|-------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| `leftProjectId` | `String`    | Yes      | The project ID of the left (reference) work item for comparison context                                                                       |
| `leftWorkItem`  | `IWorkItem` | No       | The first work item to compare (left side). Can be `null`.                                                                                    |
| `rightWorkItem` | `IWorkItem` | No       | The second work item to compare (right side). Can be `null`.                                                                                  |
| `configName`    | `String`    | Yes      | The name of the diff configuration to use (e.g., "Default"). Determines which fields are compared and how differences are calculated.         |
| `linkRole`      | `String`    | No       | The role/type of link between paired work items (e.g., "parent", "relates_to"). Can be `null` if no link relationship needs to be considered. |

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

#set($rightWorkItem = $workItems.get(0))
#set($rightWorkItem = $workItems.get(1))

 #set($diffResult = $diffTool.diffWorkItems($projectId, $rightWorkItem, $rightWorkItem, "Default", ""))

<h3>Work Item Differences</h3>

<table border="1" cellspacing="0" cellpadding="5">
  <tr>
    <th>Field</th>
    <th>$rightWorkItem.id</th>
    <th>$rightWorkItem.id</th>
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
