# The Fool

A mod for [Better than Adventure](https://betterthanadventure.net/) **8.0.1**.

Adds a single new mob: the Fool.

It is rare, it does not keep to the night, and it is not friendly. Beyond that, you are better off
finding out for yourself.

It also brings two things of its own: **Joxe Dust**, a powder worth laying down, and **Fool's Gold** —
a bar, a set of tools and a suit of armour, in colours no honest metal comes in.

---

## Install

Grab the jar from [Releases](https://github.com/colinthepwner/the-fool-bta/releases) and drop it into
your `mods` folder. HalpLibe is bundled, so nothing else is required.

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
