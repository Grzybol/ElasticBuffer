Minecraft ElasticBuffer API plugin.
Configurable connection details - also works with local Elastic/Kibana without security enabled for testing.
How to use it?
https://jitpack.io/#Grzybol/ElasticBuffer
[![](https://jitpack.io/v/Grzybol/ElasticBuffer.svg)](https://jitpack.io/#Grzybol/ElasticBuffer)

### Anti-cheat module

The plugin now ships with a modular anti-cheat reaction system aimed at PvP checks (reach, KillAura, velocity and more). Every
check type is registered through the `AntiCheatManager` so custom checks can be plugged in without touching the core.

Configuration example:

```yaml
anti-cheat:
  enabled: true
  violation-history-seconds: 120
  thresholds:
    - severityMin: 5
      countMin: 2
      windowSeconds: 45
      command: "warn %player_name% [Reach] severity=%severity% count=%count%"
    - severityMin: 7
      countMin: 3
      windowSeconds: 90
      command: "kick %player_name% Excessive violations (%check_type%)"
```

Supported placeholders: `%player_name%`, `%player_uuid%`, `%player_ip%`, `%check_type%`, `%severity%`, `%count%`. Commands fire as
console once the moving window meets both severity and count thresholds, and multiple rules can be configured to chain actions.
