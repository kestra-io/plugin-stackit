The STACKIT plugin calls the official Java SDK from Kestra flows.

## Authentication

Every task takes `serviceAccountKey`, `projectId`, and `region`.

`serviceAccountKey` is the service account key JSON. Pass it with a Kestra secret. Kestra reads that secret and the task gives the JSON to the SDK in memory. On a default Kestra install the secret is the environment variable `STACKIT_SERVICE_ACCOUNT_KEY` on the Docker Compose `app` service. Do not paste the JSON into the flow. Use a portal key, which already contains the private key.

## Tasks

| Package | Tasks |
|---|---|
| `io.kestra.plugin.stackit.iaas.server` | `Create`, `Get`, `List`, `Start`, `Stop`, `Delete` |
| `io.kestra.plugin.stackit.iaas.volume` | `Create`, `List`, `Delete` |
| `io.kestra.plugin.stackit.iaas.network` | `Create`, `List`, `Delete` |

`server.Create` follows the SDK example. `machineType` is required and must be a machine type name that exists in the region. The SDK sample name is `t2i.1`. `availabilityZone` is optional; leave it out and STACKIT chooses the zone. The image is the boot volume source (`imageId`, default disk size 32 GiB).

Run a flow such as:

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
    machineType: t2i.1
    imageId: "{{ secret('STACKIT_IMAGE_ID') }}"
    bootVolumeSizeGiB: 32

  - id: log_server
    type: io.kestra.plugin.core.log.Log
    message: "Server {{ outputs.create_server.serverId }} created"
```
