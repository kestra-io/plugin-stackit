The STACKIT plugin connects Kestra flows to STACKIT (compute, storage, projects, load balancers) using the official Java SDK.

## Authentication

Tasks share connection settings:

- `serviceAccountKey` — service account key JSON (inline, a Kestra secret, or a `kestra://` URI to a stored file)
- `projectId` — STACKIT project UUID
- `region` — STACKIT region (for example `eu01`)

## IaaS — Create server

Task type: `io.kestra.plugin.stackit.iaas.server.Create`

Creates a server in the given project and region. Required fields: `name`, `machineType`, `availabilityZone`. Optional: `imageId`.

Example flow:

```yaml
id: stackit_create_server
namespace: company.team

tasks:
  - id: create_server
    type: io.kestra.plugin.stackit.iaas.server.Create
    serviceAccountKey: "{{ secret('STACKIT_SERVICE_ACCOUNT_KEY') }}"
    projectId: "{{ secret('STACKIT_PROJECT_ID') }}"
    region: eu01
    name: kestra-demo
    machineType: g2i.2
    availabilityZone: eu01-1

  - id: log_server
    type: io.kestra.plugin.core.log.Log
    message: "Server {{ outputs.create_server.serverId }} created"
```

Outputs include `serverId`, `name`, `status`, `machineType`, and `availabilityZone`.

Run `./gradlew lintPluginDocs` before pushing to validate plugin documentation.
