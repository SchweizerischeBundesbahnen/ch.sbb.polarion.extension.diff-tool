# Configuration

How to make the Diff Tool available in a project, how to tune it in `polarion.properties`, and where its own
settings are. See the [Quick Start](QUICK_START.md) for the installation.

## Documents comparison form to appear on a Document's properties pane

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

## Documents copy form to appear on a Document's properties pane
Repeat the instructions above except that on the step 5 use the following line:
   ```xml
   …
   <extension id="copy-tool" label="Documents Copy" />
   …
   ```

## Documents merge form to appear on a Document's properties pane
Repeat the instructions above except that on the step 5 use the following line:
   ```xml
   …
   <extension id="merge-tool" label="Documents Merge" />
   …
   ```

## Nodes for collections and work items diffing to appear in Polarion's navigation tree
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

## Fine-tuning the communication between Polarion and Diff Tool extension

The Diff Tool UI makes numerous requests to Polarion using the REST API to retrieve information about documents and their workitems. These requests can be processed in parallel to improve performance.
The number of parallel requests can be configured in `polarion.properties` file:

```properties
ch.sbb.polarion.extension.diff-tool.chunk.size=2
```

Default value is `2`. Increasing this value may speed up the process but can also overload your Polarion server.

## Chapter merge timeouts

A chapter merge runs in the background and is polled by the panel which started it. How long a merge may run, and how
long its result is kept for the panel to pick up, can be configured in `polarion.properties` file:

```properties
ch.sbb.polarion.extension.diff-tool.chapter.merge.timeout=60
ch.sbb.polarion.extension.diff-tool.chapter.merge.result.timeout=30
```

Both values are minutes. A merge which runs longer than `chapter.merge.timeout` is given up on.

## Project duplication on large projects

Duplicating a project goes through Polarion's `IProjectLifecycleManager.createProject` API, which has to be wrapped in a single write transaction (this is the pattern Polarion's own "Create Project from Template" wizard uses). For very large source projects the long phase `[4/5] Creating project … from template` can run for tens of minutes inside that one transaction.

Polarion's SVN repository pool closes idle sessions after `com.polarion.repositorySessionTimeout` seconds (default **600 s / 10 min**). If the createProject phase exceeds this limit, the transaction fails to commit with `RepositoryTimeoutException: Failed to retrieve repository. Most likely the operation took too long`, and the whole duplication is rolled back.

For large projects, raise the timeout in `polarion.properties`:

```properties
# Maximum age (seconds) an SVN session may sit idle before the pool closes it.
# Must be larger than the longest createProject phase you expect. Default: 600.
com.polarion.repositorySessionTimeout=3600

# How often (seconds) the background timer scans for idle sessions. Default: 180.
com.polarion.repositorySessionTimeoutCheckInterval=600
```

Polarion must be restarted for these values to take effect — they are read once during startup. The current effective values are printed in the job log right after `[1/5] Validating request …`, so you can verify they were picked up.

## Extension configuration

1. On the top of the project's navigation pane click ⚙ (Actions) ➙ 🔧 Administration. Project's administration page will be opened.
2. On the administration's navigation pane select `Diff Tool`. There are 2 sub-menus with different configuration options for Diff Tool.
3. They have either `Quick Help` section with short description or their content is self-evident.
4. To change configuration of Diff Tool extension just edit corresponding section and press `Save` button.
