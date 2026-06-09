# Urbit MCP Development Tooling

Status: recommended developer tooling, not part of the phone runtime.

[`gwbtc/urbit-mcp`](https://github.com/gwbtc/urbit-mcp) is a general-purpose
MCP desk for Urbit. It exposes an authenticated HTTP MCP endpoint at `/mcp` on a
ship and gives Codex, Claude Code, and similar agents direct access to Urbit
development operations.

## Why It Matters Here

NativePlanet Mobile now depends on parent-side Urbit app behavior, especially
Artemis-backed mobile moon provisioning. A ship-side MCP bridge lets agents
inspect and modify the relevant Urbit desks directly instead of relying on
manual shell notes and one-off curl probes.

Useful capabilities include:

- reading Clay files
- inserting Clay files
- committing desks
- mounting desks
- installing apps
- poking local agents
- running JSON scries
- running desk tests

## Recommended Use

Install `%mcp` on a development or distro ship, not on the Android phone.

Candidate ships:

- the Artemis development ship
- the distro ship used to publish Artemis
- a disposable fake/dev ship for tool testing

Do not treat `%mcp` as a production phone dependency. The phone should still use
the narrow controller/provider/runtime APIs.

## Security Notes

- MCP access is equivalent to giving an agent operational access to the ship.
- Use an authenticated cookie or session header.
- Do not commit cookies, `+code` values, moon keys, or MCP headers.
- Prefer a development ship or short-lived credential while testing.

## Codex Configuration Shape

The upstream README shows this shape:

```toml
[mcp_servers.<ship-name>]
enabled = true
url = "https://example.tlon.network/mcp"
http_headers = { "Cookie" = "<urbauth-cookie>" }
```

Keep real cookie values in local ignored config only.

## NativePlanet Workflow

Once `%mcp` is installed on the parent/distro ship, agents can:

1. inspect the installed Artemis desk,
2. subscribe to Artemis `/moons` facts over the channel API,
3. poke Artemis with an `artemis-action`,
4. commit or install desk changes,
5. verify the parent-side API before a phone ROM build.

This should reduce the loop where Android changes are made before the Urbit-side
contract is actually available on the parent ship.
