# The Fool

A mod for [Better than Adventure](https://betterthanadventure.net/) **8.0.1**.

Adds a single new mob: the Fool.

It is rare, it does not keep to the night, and it is not friendly. Beyond that, you are better off
finding out for yourself.

---

## Install

Drop the jar into your `mods` folder. HalpLibe is bundled, so nothing else is required.

No release yet — build it yourself for now (see below).

## Building

```bash
./gradlew build
```

The jar is written to `build/libs/`.

To have the build copy it straight into your game afterwards, set `mods.deploy.dir` in
`gradle.properties` or pass it on the command line:

```bash
./gradlew build -Pmods.deploy.dir=/path/to/.minecraft/mods
```

If the game is running and holding the jar, the copy waits for it to close.

## Tests

```bash
./gradlew test
```

## License

CC0-1.0. Do what you like with it.
