# ElasticBuffer Plugin Technical Documentation

## Overview
ElasticBuffer is a Bukkit/Paper plugin that centralizes server and plugin logs, ships them to an HTTP endpoint (e.g., Elasticsearch), and provides a lightweight anti-cheat reaction system. It exposes a public API for other plugins to push structured log entries with optional player context and custom fields.

## Architecture
- **ElasticBuffer (main class)** – Registers the plugin with the Bukkit service manager, initializes configuration, logging, anti-cheat, and schedules periodic log flushing every 1200 ticks (roughly 60 seconds).
- **ElasticBufferAPI** – Service-exposed API allowing external plugins to publish logs with arbitrary metadata or key values.
- **LogBuffer / LogEntry** – Thread-safe in-memory queue holding structured log entries until they are flushed.
- **ElasticBufferConfigManager** – Loads and validates `config.yml`, manages log-level filtering, HTTP destination details, SSL handling, and anti-cheat thresholds.
- **ElasticBufferPluginLogger & CustomLogHandler** – Plugin-specific logging wrapper and console handler that forwards server console output into the buffer.
- **EventLogger & ServerEventLogger** – Bukkit listeners that capture player/server events and push them into the API; `ServerEventLogger` also restores stdout/stderr on shutdown.
- **AntiCheatManager and checks** – Registers default PvP checks (`Reach`, `KillAura`, `Velocity`) backed by `SimpleAntiCheatCheck`, tracks violations (`PlayerViolationTracker`, `ViolationRecord`), and fires configured commands when thresholds are met.
- **CustomConsoleInjector** – Hooks console streams so that console output is also buffered and shipped through the same pipeline.

## Lifecycle
1. **Startup**: `onEnable` creates data folder, config manager, anti-cheat manager, log buffer, API service registrations, command executor (`/eb`), console handler/injector, and event listeners. A repeating async task is scheduled to flush buffered logs.
2. **Runtime**: Other plugins call the API to queue log entries. Console output and subscribed events are also queued. Anti-cheat checks raise violations that are evaluated against threshold rules to trigger server commands.
3. **Shutdown**: Console streams are restored and custom log handlers removed; buffered logs should be sent during the scheduled task window.

## Configuration (`config.yml`)
Key settings loaded by `ElasticBufferConfigManager` include:

- **Logging**
  - `log_level`: List of enabled levels (`INFO`, `WARNING`, `ERROR`, optional debug levels) controlling plugin logger output.
- **Elasticsearch/HTTP destination**
  - `webhookURL`: Base URL for the ingest endpoint.
  - `elasticsearch_port`: Destination port (default 9200).
  - `apiKey`: Optional API key for authenticated requests.
  - `index_pattern`: Index prefix/pattern, typically the server name.
  - `local`: Boolean indicating whether to target a local, unsecured endpoint.
  - `useSSL`: Enables HTTPS transport when true.
  - `authorization`: Toggles use of an `Authorization` header.
  - `checkCerts`: When true, loads a custom truststore; when false, trusts all certificates.
  - `truststorePath` / `truststorePassword`: Path and password for the truststore used when `checkCerts` is enabled.
  - `serverName`: Identifies the emitting server instance in outbound payloads.
- **Monitoring thresholds** (used by event/health logging)
  - `highMemoryUsageThreshold`, `lowTPSThreshold`, `highCpuUsageThreshold`, `highDiskUsageThreshold`, `monitoringIntervalTicks`.
- **Anti-cheat**
  - `anti-cheat.enabled`: Master toggle for anti-cheat processing.
  - `anti-cheat.violation-history-seconds`: Rolling window length for retaining violations.
  - `anti-cheat.thresholds`: List of threshold rules (severity/count/window/command) executed as console commands when conditions are met.
  - `reachWarningDistance`, `reachCriticalDistance`, `reachWarningSeverity`, `reachCriticalSeverity`: Reach-specific tuning values.
  - `killAuraLineOfSightSeverity`, `killAuraSameTickSeverity`, `killAuraSwitchSeverity`, `killAuraMaxHitsPerTick`, `killAuraSwitchIntervalMs`: KillAura detection parameters.

> **Note:** The plugin creates a default `config.yml` if missing. Update values through standard Bukkit config editing and reload via `/eb`.

## Commands
- `/eb` – Reloads configuration. Requires `elasticbuffer.reload` permission (`default: op`).

## Public API Usage
Other plugins can depend on ElasticBuffer via Bukkit services and push structured logs:

```java
ElasticBufferAPI api = Bukkit.getServicesManager().load(ElasticBufferAPI.class);
api.log("Player joined", "INFO", "MyPlugin", "txn-123", player.getName(), player.getUniqueId().toString());

// Include a numeric key value and custom fields
Map<String, Object> fields = new HashMap<>();
fields.put("keyValue", 42.0);
fields.put("region", "spawn");
api.log("Custom event", "INFO", "MyPlugin", "txn-456", player.getName(), player.getUniqueId().toString(), fields);
```

Internally, the API delegates to `ElasticBuffer.receiveLog`, which stamps timestamps, merges additional fields, and enqueues a `LogEntry`. Logs are flushed in batches on the configured interval.

## Anti-Cheat Integration
- Default checks (`Reach`, `KillAura`, `Velocity`) are registered at startup through `registerDefaultChecks`.
- Custom checks can be registered by calling `AntiCheatManager.registerCheck` with implementations of `AntiCheatCheck`.
- Violations are recorded in `PlayerViolationTracker`; threshold rules trigger console commands with placeholders such as `%player_name%`, `%player_uuid%`, `%player_ip%`, `%check_type%`, `%severity%`, and `%count%`.

## Logging and Console Capture
- The plugin attaches `CustomLogHandler` to the Bukkit logger to route console messages into the buffer.
- `CustomConsoleInjector` wraps `System.out`/`System.err` to capture low-level output and restores them during `onDisable`.

## Event Logging
- `EventLogger` and `ServerEventLogger` listen for relevant Bukkit events (player actions, server state) and submit structured logs through the API, enriching them with player identity and context fields.

## Data Flow Summary
1. Producers (other plugins, console, event listeners, anti-cheat) call `ElasticBufferAPI.log` or push violations.
2. `ElasticBuffer.receiveLog` enriches entries, enqueues them in `LogBuffer`, and respects enabled log levels for internal logging.
3. A scheduled task executes `sendLogs`, draining the buffer and transmitting batched payloads to the configured endpoint, honoring SSL/truststore settings.
4. Anti-cheat violations are evaluated continuously; matching threshold rules dispatch console commands.

## Development Notes
- The plugin registers itself and the API in Bukkit's service manager, so dependent plugins should request the service at runtime rather than keeping static references.
- When `checkCerts` is false, all SSL certificates are trusted; production deployments should set `checkCerts: true` and configure a truststore.
- The project uses bStats (plugin ID 23919) for anonymous metrics; outbound network access is required.

