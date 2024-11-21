# Setting up Debugging for a React App in IntelliJ IDEA

## Prerequisites

`JavaScript and TypeScript` plugin should be installed.

### Setting Up CORS
Add the following lines to /etc/apache2/conf-enabled/polarion.conf:
```properties
Header set Access-Control-Allow-Origin "*"
Header set Access-Control-Allow-Methods "*"
Header set Access-Control-Allow-Headers "*"
```
Additionally, create a `.env.development.local` file based on the provided template in the project.

## Quick Start

Open `ui/package.json` and start the React app by running the `dev` script using green start icon. It will create two new run configurations: npm and javascript debug.
Go to `Run` > `Edit Configurations…` and open the `npm` configuration.
Adjust the URL to be opened after debug start, e.g.
`http://localhost:3000?sourceProjectId=elibrary&sourceSpaceId=Specification&sourceDocument=doc1&targetProjectId=elibrary&targetSpaceId=Specification&targetDocument=doc2&linkRole=derived_from&config=Default`.

## Manual Setup

1. Configure a JavaScript Debug Configuration:
    - Go to `Run` > `Edit Configurations…`.
    - Click the `+` symbol to add a new configuration.
    - Select `JavaScript Debug`.
    - Name the configuration (e.g., "Diff-Tool: doc1-doc2 JavaScript Debug").
    - Set the `URL` to be opened after debug start, e.g.
      `http://localhost:3000?sourceProjectId=elibrary&sourceSpaceId=Specification&sourceDocument=doc1&targetProjectId=elibrary&targetSpaceId=Specification&targetDocument=doc2&linkRole=derived_from&config=Default`.

2. Configure a React Debug Configuration:
    - Go to `Run` > `Edit Configurations…`.
    - Click the `+` symbol to add a new configuration.
    - Select `npm`.
    - Name the configuration (e.g., "Diff-Tool: doc1-doc2 Debug").
    - Set the `Package.json` to the `package.json` file of the React app, for example `~/Projects/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.diff-tool/ui/package.json`.
    - Set the `Command` to `run`.
    - Set the `Scripts` to `dev`.

## Project stored configurations

There are stored examples of debug configurations in `.run` folder. They can be adjusted and imported to IntelliJ IDEA.
