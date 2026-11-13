# Contribute to PistonQueue

Contributions can fix behavior, improve documentation, or add focused tests.
Read the [installation and configuration wiki](https://github.com/AlexProgrammerDE/PistonQueue/wiki) before diagnosing setup problems.

## Before you start

Read [the support guide](SUPPORT.md) for questions and issue routing.
Search existing issues and pull requests. Discuss larger API, architecture, or dependency changes before implementation.

Work from `main` and target that branch in your pull request.
Keep each change focused. Avoid unrelated formatting and dependency updates.

## Prepare a checkout

Use JDK 25 for the Gradle daemon. The plugin modules select Java 21 through Gradle toolchains.

Run the commands below from the repository root unless a command names another directory.
On Windows, use `gradlew.bat` in place of `./gradlew` for Gradle commands.

## Repository layout

- `shared/`: queue behavior shared by platforms.
- `bukkit/`, `bungee/`, `velocity/`: platform integrations.
- `placeholder/`: placeholder integration.
- `universal/`: combined plugin artifact.

## Verify your change

```bash
./gradlew test spotlessCheck
./gradlew build
```

Keep queue policy in shared code and platform event handling in the adapters. Cover ordering, reserved slots, disconnects, and reconnects with focused tests. For proxy changes, verify login and backend transfers on an isolated test network.

Run the relevant checks before review. State the command and result in the pull request.
If a check cannot run, explain the missing dependency or service. Do not claim it passed.
Keep generated artifacts consistent with their source and review their diff.

## Style and documentation

Follow the existing code conventions and repository formatter. Keep commit hooks enabled.
Add focused tests for changed logic when practical. Avoid tests that only assert source strings.
Update documentation when commands, APIs, configuration, or expected behavior change.
Keep examples small and reproducible. Preserve exact identifiers, commands, and error messages.

## Open a pull request

Explain the problem and resulting behavior. Link related issues without a placeholder issue number.
Describe changes to queue ordering, reserved slots, auth flow, or connection handling.
Include commands and results. State any runtime checks that remain necessary.
Respond to review with a correction or concrete evidence.

Use Conventional Commits: `type(scope): description`, for example `docs(contributing): explain local validation`.
Use a meaningful scope, or omit it. Keep the subject concise and imperative.
Add a body when the reason or compatibility impact is not obvious.

For vulnerabilities, follow [the security reporting instructions](SECURITY.md).
Remove credentials and private data from examples, logs, and screenshots.
