# Kestra STACKIT Plugin

## What

- Provides plugin components under `io.kestra.plugin.stackit`.
- Includes the base class `AbstractStackit` (service account key JSON passed in memory to the STACKIT Java SDK, `projectId`, `region`, 30s client timeouts, and 403 translated to a service-account permission error).
- Built against Kestra 1.3.39 and Java 21.

## Why

- What user problem does this solve? Teams need to create and manage STACKIT servers, volumes, and networks from a workflow without hand-written SDK client and authentication code.
- Why would a team adopt this plugin in a workflow? It shares one connection across IaaS tasks and calls the official STACKIT Java SDK for servers, volumes, and networks.
- What operational/business outcome does it enable? Server, volume, and network changes run as declarative Kestra flows.

## How

### Architecture

Single-module plugin. Source packages under `io.kestra.plugin`:

- `stackit.auth` (`AbstractStackit`)
- `stackit.iaas.server` (`Create`, `Get`, `List`, `Start`, `Stop`, `Delete`)
- `stackit.iaas.volume` (`Create`, `List`, `Delete`)
- `stackit.iaas.network` (`Create`, `List`, `Delete`)

`server.Create` uses `machineType` (sample `t2i.1`), a boot volume from `imageId` (default 32 GiB) and the required `networkId`. `availabilityZone` is optional.

Examples use the secrets `STACKIT_SERVICE_ACCOUNT_KEY`, `STACKIT_PROJECT_ID`, and `STACKIT_IMAGE_ID`.

Infrastructure dependencies (Docker Compose services):

- `app`

### Key Plugin Classes

- `io.kestra.plugin.stackit.auth.AbstractStackit`
- `io.kestra.plugin.stackit.iaas.server.Create`, `Get`, `List`, `Start`, `Stop`, `Delete`
- `io.kestra.plugin.stackit.iaas.volume.Create`, `List`, `Delete`
- `io.kestra.plugin.stackit.iaas.network.Create`, `List`, `Delete`

### Project Structure

```
plugin-stackit/
├── src/main/java/io/kestra/plugin/stackit/
├── src/test/java/io/kestra/plugin/stackit/
├── build.gradle
└── README.md
```

## Local rules

- Base the wording on the implemented packages and classes, not on template README text.

## References

- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
