The STACKIT plugin calls the official Java SDK from Kestra flows.

## Authentication

The service account key is not a task field. The SDK reads it from the environment (authentication example option 3):

- `STACKIT_SERVICE_ACCOUNT_KEY` — the key JSON
- `STACKIT_PRIVATE_KEY` — only when that JSON does not already contain the private key

Every task also takes `projectId` and `region`.

## Tasks

| Package | Tasks |
|---|---|
| `io.kestra.plugin.stackit.iaas.server` | `Create`, `Get`, `List`, `Start`, `Stop`, `Delete` |
| `io.kestra.plugin.stackit.iaas.volume` | `Create`, `List`, `Delete` |
| `io.kestra.plugin.stackit.iaas.network` | `Create`, `List`, `Delete` |

`server.Create` follows the SDK example. `machineType` is required and must be a machine type name that exists in the region. The SDK sample name is `t2i.1`. `availabilityZone` is optional; leave it out and STACKIT chooses the zone. The image is the boot volume source (`imageId`, default disk size 32 GiB).

Set the environment variables on the Kestra process, then run a flow such as:

```yaml
id: stackit_create_server
namespace: company.team

tasks:
  - id: create_server
    type: io.kestra.plugin.stackit.iaas.server.Create
    projectId: "{{ secret('STACKIT_PROJECT_ID') }}"
    region: eu01
    name: kestra-demo
    machineType: t2i.1
    imageId: "{{ secret('STACKIT_IMAGE_ID') }}"
    bootVolumeSizeGiB: 32

  - id: log_server
    type: io.kestra.plugin.core.log.Log
    message: "Server {{ outputs.create_server.serverId }} created"
```
