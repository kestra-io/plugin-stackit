The STACKIT plugin calls the official Java SDK from Kestra flows.

## Authentication

Every task takes `serviceAccountKey`, `projectId`, and `region`.

`serviceAccountKey` is the service account key JSON. Pass it with a Kestra secret, for example `{{ secret('STACKIT_SERVICE_ACCOUNT_KEY') }}`. See the [secrets documentation](https://kestra.io/docs/concepts/secret) for how secrets are defined and resolved on your deployment. The task gives the rendered JSON to the SDK in memory. Create the key in the STACKIT portal and use the downloaded JSON; it must include `credentials.privateKey`. The task fails if the private key is missing.

## Worker settings

Flows do not set API endpoints. On the Kestra worker you can optionally point STACKIT tasks at custom hosts (for example a private API) with `kestra.plugins.configurations`. Set `type` to each task's fully qualified class name and repeat the same `customEndpoint` and `tokenCustomUrl` values (YAML anchors help). Omit this for the public STACKIT endpoints.

```yaml
kestra:
  plugins:
    configurations:
      - type: io.kestra.plugin.stackit.iaas.server.Create
        values: &stackit-endpoints
          customEndpoint: https://iaas.api.example.test
          tokenCustomUrl: https://iaas.api.example.test/token
      - type: io.kestra.plugin.stackit.iaas.server.List
        values: *stackit-endpoints
      # repeat for other STACKIT task types you use
```

## Tasks

| Package | Tasks |
|---|---|
| `io.kestra.plugin.stackit.iaas.server` | `Create`, `Get`, `List`, `Start`, `Stop`, `Delete` |
| `io.kestra.plugin.stackit.iaas.volume` | `Create`, `List`, `Delete` |
| `io.kestra.plugin.stackit.iaas.network` | `Create`, `List`, `Delete` |

`server.Create` requires `machineType`, `imageId`, and `networkId` (the server is attached to that network). `availabilityZone` is optional. Boot disk size defaults to 32 GiB (allowed range 1 to 16384 GiB).

`network.Create` sets `nameserver` only when you pass it in the flow. IPv4 prefix length defaults to 24 (allowed range 8 to 30).

Run a flow such as:

```yaml
id: stackit_create_server
namespace: company.team

tasks:
  - id: create_network
    type: io.kestra.plugin.stackit.iaas.network.Create
    serviceAccountKey: "{{ secret('STACKIT_SERVICE_ACCOUNT_KEY') }}"
    projectId: "{{ secret('STACKIT_PROJECT_ID') }}"
    region: eu01
    name: kestra-network

  - id: create_server
    type: io.kestra.plugin.stackit.iaas.server.Create
    serviceAccountKey: "{{ secret('STACKIT_SERVICE_ACCOUNT_KEY') }}"
    projectId: "{{ secret('STACKIT_PROJECT_ID') }}"
    region: eu01
    name: kestra-demo
    machineType: t2i.1
    imageId: "{{ secret('STACKIT_IMAGE_ID') }}"
    networkId: "{{ outputs.create_network.networkId }}"
    bootVolumeSizeGiB: 32

  - id: log_server
    type: io.kestra.plugin.core.log.Log
    message: "Server {{ outputs.create_server.serverId }} created"
```