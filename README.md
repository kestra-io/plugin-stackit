# Kestra STACKIT Plugin

## Why

Teams need to create and manage STACKIT servers, volumes, and networks from a workflow without hand-written SDK client and authentication code.

## What

- Plugin components under `io.kestra.plugin.stackit`.
- Base class `AbstractStackit` (service account key JSON in memory, `projectId`, `region`, 30s client timeouts, 403 mapped to a permission error).
- IaaS tasks for servers, volumes, and networks via the official STACKIT Java SDK.

## Running Kestra locally with this plugin

1. Build the shadow JAR: `./gradlew shadowJar`. Output is in `build/libs/`.
2. Run `docker compose up`. `docker-compose.yml` mounts `build/libs/` to `/app/plugins/`.
3. Open the UI at [localhost:8080](http://localhost:8080).

### Plugins folder gotcha

Mounting a host folder onto `/app/plugins/` replaces the container's plugins directory. Core plugins stay in the Kestra binary; jars bundled only under `/app/plugins/` in the base image are hidden by the mount. Copy any extra plugin jars you need into `build/libs/` before starting.

### JFR startup error

On some hosts, `command: server local` fails with a JFR repository error under `/tmp`. Add a tmpfs or volume for `/tmp` (see `docker-compose.yml` in this repo). Upstream: [kestra-io/kestra#17405](https://github.com/kestra-io/kestra/issues/17405).

## Configuration

Worker-level STACKIT API overrides use `kestra.plugins.configurations` per task type. See `src/main/resources/doc/io.kestra.plugin.stackit.md` in this repository.

Example secrets for flows: `STACKIT_SERVICE_ACCOUNT_KEY`, `STACKIT_PROJECT_ID`, and `STACKIT_IMAGE_ID` for server create.

## Documentation

- [Kestra docs](https://kestra.io/docs)
- [Plugin Developer Guide](https://kestra.io/docs/plugin-developer-guide/)

## License

Apache 2.0