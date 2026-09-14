# Spectra — private HolyWorld review

This repository is **PRIVATE**. It is a moderator review copy, not a public open-source release and not evidence of HolyWorld approval. Sharing the URL does not grant access; invite the moderator's exact GitHub account.

## Scope

Minecraft 1.21.4 / Fabric / Java 21. All client feature implementations, menu, HUD, mixins, assets and HolyWorld feature-control integration are included. The gameplay feature IDs match the production source used for release 188; see [the catalogue](review/feature-ids.json).

Production licensing, loader, backend, signing keys, entitlement/bootstrap code, native protection, obfuscation rules and private release metadata are intentionally excluded. Review-only changes are limited to the local-profile bootstrap, removing nativeization annotations, UI diagnostic enum relocation, and replacing the protected release-notes proxy. There is no review-only hidden gameplay whitelist or alternate set of gameplay features.

The review build can run as a normal Fabric mod. It does not create a production subscription, server lease or signed production artifact. The original protected production pipeline remains separate.

## Build and test

Install a full Java 21 JDK. Run:

```text
./gradlew test build
```

On Windows use `gradlew.bat test build`. Put the remapped review JAR from `build/libs/` in a Minecraft 1.21.4 Fabric installation with Fabric API. Do not install a second Spectra JAR alongside it. Sodium is optional at runtime; the adapter compiles against its public Maven artifact.

The regression tests exercise the raw JSON payload codec, connection/request matching, fail-closed policy, blocklist validation, cooldown transitions, source removals, and the actual compiled feature catalogue. They do **not** simulate a complete live HolyWorld session, certify server permission, prove mod compatibility for every possible mod, or replace moderator review.

## HolyWorld protocol

Channel: `liteapi:feature-control`. On HolyWorld connection, send raw UTF-8 JSON (no Minecraft string prefix):

```json
{"id":"<uuid>","method":"checkFeatures","payload":{"client":"spectra","features":["<stable feature IDs>"]}}
```

Only a valid reply from the current connection with the matching request ID and `ok: true` unlocks allowed features. Submitted blocked IDs remain disabled and hidden in menu cards, keybind settings, HUD/editor and notifications. Commands, macros and mass-item-drop controls have their own IDs and guards.

Before a valid reply, after errors, malformed replies or the five-second timeout, Spectra features stay closed on HolyWorld. Reconnect to retry. Features remain normally available outside HolyWorld; no production license check is removed from the real release. Requests are not spammed. A conflicting mod that already owns the same Fabric payload registration causes a fail-closed compatibility warning rather than replacing its receiver.

Known host detection covers HolyWorld's .me/.ru domains and subdomains, with a server-brand fallback for IP/alias connections. The API must be available on the specific HolyWorld mode tested. An empty blocklist is a technical response, not a formal approval of the client.

## Removed behavior

FT/HW/RW special-item binds and their automatic use/inventory logic, HW event NPC automation, See Invisibles and renderer hooks, dormant auto-duel implementation/types, dormant Flight/Speed/Velocity/NoFall/NoSlow/Nuker setting types, unused cheat translations, packet movement bypass and silent-rotation/corrected-targeting hooks have been removed from active production and review source.

AutoEat checks only the existing nine hotbar slots. It cannot eat from inventory/offhand or move inventory food into the hotbar. It is still automation and must be evaluated by HolyWorld like other automation features.

HW cooldown-completion notifications are passive and share FT's tracker. HW's existing detected structure timers feed the same Structures HUD as FT.

## Review and permission

Contact the HolyWorld moderator before assuming private access substitutes for an open/public GitHub link. No code-signing certificate is supplied. See [the submission checklist](review/SUBMISSION-RU.md). Keep the repository private; never publish it as a workaround.

Official references:

- [HolyWorld feature-control documentation](https://github.com/HolyWorldMC/wiki/blob/master/docs/lite-anarchy/api/feature.md)
- [HolyWorld protocol documentation](https://github.com/HolyWorldMC/wiki/blob/master/docs/lite-anarchy/api/protocol.md)
- [Official Fabric example](https://github.com/HolyWorldMC/liteapi-fabric-example)
- [GitHub private repository invitations](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/repository-access-and-collaboration/inviting-collaborators-to-a-personal-repository)

All rights reserved. Private review access does not authorize public redistribution.
