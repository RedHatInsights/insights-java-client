# Code style guide

NOTE: The commit messages and need to run the formatter are requirements, whereas the code style points are a set of recommendations.

## Formatting

Before you commit, make sure to run the Spotless formatter:

	$ mvn spotless:apply

We adhere to [Semantic Versioning](https://semver.org/) and reinforce this practice with [Conventional Commits](https://www.conventionalcommits.org/)

In its simplest form, a commit message should be structured as follows:

```
<type>[optional scope]: <description>
```

with the type being one of a number of types: `build`, `ci`, `chore`, `docs`, `feat`, `fix`, `perf`, `refactor`, `revert`, `style`, `test`.

If the change introduces a breaking change (which will necessitate a major version bump), then the commit message should be suffixed with `!`.

## Java constructors vs factory methods

- Prefer plain constructors to _factory_ static methods (e.g., `new ABC()` over `ABC.create()` / `ABC.of()`).
- Do use factory methods when:
- some initialization logic cannot be expressed from a constructor, or it becomes inelegant,
- hiding a concrete implementation behind an interface (e.g., `val vertx = Vertx.vertx();`).

## Getters and setters

- Prefer the JavaBeans convention of `getABC()` / `setABC(abc)` for domain objects that are intended to be serialized through _Jackson_.

## Top-level components

- Prepend `Insights` to the interface definitions of top-level components (e.g., `InsightsHttpClient`).
