import com.google.gson.Gson
import com.smushytaco.lwjgl_gradle.Preset
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.FileSystemException
plugins {
	alias(libs.plugins.loom)
	alias(libs.plugins.lwjgl)
    java
}
val modVersion: Provider<String> = providers.gradleProperty("mod_version")
val modGroup: Provider<String> = providers.gradleProperty("mod_group")
val modName: Provider<String> = providers.gradleProperty("mod_name")

val javaVersion: Provider<Int> = libs.versions.java.map { it.toInt() }

base.archivesName = modName
group = modGroup.get()
version = modVersion.get()

class AccountsJson(val accounts: List<Account>)
class Account(val profile: Profile, val ygg: YGG)
class YGG(val token: String)
class Profile(val name: String, val id: String)

val prismAccountsFile = providers.provider {
	val explicit = providers.gradleProperty("prism.accounts.file").orNull
	if (explicit != null) {
		val explicitFile = File(explicit)
		if (explicitFile.exists()) return@provider explicitFile
	}

	val home = System.getProperty("user.home")

	val candidates = buildList {
		// Windows
		System.getenv("APPDATA")?.let { add(File(it, "PrismLauncher/accounts.json")) }
		System.getenv("HOMEPATH")?.let { add(File(it, "scoop/persist/prismlauncher/accounts.json")) }
		// Linux / XDG
		val xdgDataHome = System.getenv("XDG_DATA_HOME")
		if (xdgDataHome != null) {
			add(File(xdgDataHome, "PrismLauncher/accounts.json"))
		} else {
			add(File(home, ".local/share/PrismLauncher/accounts.json"))
		}
		// Flatpak
		add(File(home, ".var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/accounts.json"))
		// macOS
		add(File(home, "Library/Application Support/PrismLauncher/accounts.json"))
	}
	candidates.firstOrNull(File::exists)
}

loom {
    customMinecraftMetadata.set("https://downloads.betterthanadventure.net/bta-client/${libs.versions.btaChannel.get()}/${libs.versions.bta.get()}/manifest.json")
	runs {
		prismAccountsFile.orNull?.let { file ->
			val account: Provider<Account> = providers.fileContents(layout.file(providers.provider { file }))
				.asText
				.map { jsonStr ->
					val accountNumber = (providers.gradleProperty("prism.accounts.number").orNull?.toInt() ?: 1) - 1
					val accounts = Gson().fromJson(jsonStr, AccountsJson::class.java).accounts
					accounts.getOrNull(accountNumber.coerceIn(0, accounts.size - 1))
						?: error("No PrismLauncher accounts found in ${file.absolutePath}")
				}
			register("clientAuth") {
				inherit(getByName("client"))
				configName = "Minecraft Client (Auth)"
				val acc = account.get()
				programArgs("--username", acc.profile.name, "--uuid", acc.profile.id, "--session", acc.ygg.token)
			}
		}
	}
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
    maven("https://maven.thesignalumproject.net/infrastructure") { name = "SignalumMavenInfrastructure" }
    maven("https://maven.thesignalumproject.net/releases") { name = "SignalumMavenReleases" }
	maven("https://maven.thesignalumproject.net/nightly") { name = "SignalumMavenNightly" }
    ivy("https://github.com/Better-than-Adventure") {
        patternLayout { artifact("[organisation]/releases/download/[revision]/[module]-bta-[revision].jar") }
        metadataSources { artifact() }
    }
    ivy("https://downloads.betterthanadventure.net/bta-client/${libs.versions.btaChannel.get()}/") {
        patternLayout { artifact("/v[revision]/client.jar") }
        metadataSources { artifact() }
    }
    ivy("https://downloads.betterthanadventure.net/bta-server/${libs.versions.btaChannel.get()}/") {
        patternLayout { artifact("/v[revision]/server.jar") }
        metadataSources { artifact() }
    }
    ivy("https://piston-data.mojang.com") {
        patternLayout { artifact("v1/[organisation]/[revision]/[module].jar") }
        metadataSources { artifact() }
    }
}
lwjgl {
	version = libs.versions.lwjgl
	implementation(Preset.MINIMAL_OPENGL)
}
dependencies {
    minecraft("::${libs.versions.bta.get()}")

	runtimeOnly(libs.clientJar)
	implementation(libs.loader)
	// This mod genuinely needs Halplibe at runtime — HalpLibe.registerMod (without which nothing under
	// /assets/foolmod/ is discovered: skin, entity model manifest, sounds), EntityHelper for the entity
	// registration, and the GameStart/Model entrypoints. So it is nested into the jar with include():
	// the mod works dropped into a fresh mods folder on its own. Fabric Loader de-duplicates against a
	// standalone halplibe if one is already installed, preferring the newer of the two.
	implementation(libs.halplibe)
	include(libs.halplibe)
	implementation(libs.modMenu)
	implementation(libs.legacyLwjgl)

	implementation(libs.slf4jApi)
	implementation(libs.guava)
	implementation(libs.log4j.slf4j2.impl)
	implementation(libs.log4j.core)
	implementation(libs.log4j.api)
	implementation(libs.log4j.api12)
	implementation(libs.gson)

	// --- test harness ---------------------------------------------------------
	// The test source set inherits the BTA/Loom classpath through testImplementation extending
	// implementation, so tests run against the real Blocks/Materials registries rather than fakes.
	testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
	testImplementation("org.mockito:mockito-core:5.14.2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
java {
	toolchain {
		languageVersion = javaVersion.map { JavaLanguageVersion.of(it) }
		vendor = JvmVendorSpec.ADOPTIUM
	}
	sourceCompatibility = JavaVersion.toVersion(javaVersion.get())
	targetCompatibility = JavaVersion.toVersion(javaVersion.get())
	withSourcesJar()
}
val licenseFile = run {
	val rootLicense = layout.projectDirectory.file("LICENSE")
	val parentLicense = layout.projectDirectory.file("../LICENSE")
	when {
		rootLicense.asFile.exists() -> {
			logger.lifecycle("Using LICENSE from project root: {}", rootLicense.asFile)
			rootLicense
		}
		parentLicense.asFile.exists() -> {
			logger.lifecycle("Using LICENSE from parent directory: {}", parentLicense.asFile)
			parentLicense
		}
		else -> {
			logger.warn("No LICENSE file found in project or parent directory.")
			null
		}
	}
}
tasks {
	withType<JavaCompile>().configureEach {
		options.encoding = "UTF-8"
		options.compilerArgs.addAll(listOf("-Xmaxerrs", "3000"))
		sourceCompatibility = javaVersion.get().toString()
		targetCompatibility = javaVersion.get().toString()
		if (javaVersion.get() > 8) options.release = javaVersion
	}
	withType<UpdateDaemonJvm>().configureEach {
		languageVersion = libs.versions.gradleJava.map { JavaLanguageVersion.of(it.toInt()) }
		vendor = JvmVendorSpec.ADOPTIUM
	}
	withType<JavaExec>().configureEach { defaultCharacterEncoding = "UTF-8" }
	withType<Javadoc>().configureEach { options.encoding = "UTF-8" }
	withType<Test>().configureEach {
		defaultCharacterEncoding = "UTF-8"
		useJUnitPlatform()
		// Pathfinding tests allocate large node pools and run searches over big synthetic worlds.
		maxHeapSize = "2g"
		testLogging {
			events("passed", "failed", "skipped")
			showStandardStreams = true
			exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
		}
	}
	withType<Jar>().configureEach {
		licenseFile?.let {
			from(it) {
				rename { original -> "${original}_${archiveBaseName.get()}" }
			}
		}
	}
	processResources {
		val resourceMap = mapOf(
			"version" to modVersion.get(),
			"fabricloader" to libs.versions.loader.get(),
			"halplibe" to libs.versions.halplibe.get(),
			"java" to libs.versions.java.get(),
			"modmenu" to libs.versions.modMenu.get()
		)
		inputs.properties(resourceMap)
		filesMatching("fabric.mod.json") { expand(resourceMap) }
		filesMatching("**/*.mixins.json") { expand(resourceMap.filterKeys { it == "java" }) }
	}
}
// Removes LWJGL2 dependencies
configurations.configureEach { exclude(group = "org.lwjgl.lwjgl") }

// --- Optionally copy the built jar into the game's mods folder ---------------
// Opt in by setting mods.deploy.dir in gradle.properties (untracked) or on the command line:
//   ./gradlew build -Pmods.deploy.dir=/path/to/.minecraft/mods
// Unset, the task simply does nothing. If the game is running (jar locked), the deploy WAITS for it
// to close, then copies. Cap the wait with -Pmods.deploy.timeoutSeconds=N (0 or unset = forever).
val modsDeployDir: Provider<String> = providers.gradleProperty("mods.deploy.dir").orElse("")
val modsDeployTimeout: Provider<String> = providers.gradleProperty("mods.deploy.timeoutSeconds").orElse("0")

val deployToMods by tasks.registering {
	description = "Copies the remapped mod jar into the game's mods folder (waits for the game to close if it's locked)."
	group = "loom"
	// the remapped artifact Loom writes to build/libs/<name>-<version>.jar
	val jarFileProvider = layout.buildDirectory.file("libs/${modName.get()}-${modVersion.get()}.jar")
	val destDirProvider = modsDeployDir
	val timeoutProvider = modsDeployTimeout
	doLast {
		val configured = destDirProvider.get()
		if (configured.isBlank()) {
			return@doLast // not opted in — nothing to do
		}
		val destDir = File(configured)
		if (!destDir.isDirectory) {
			logger.warn("[deployToMods] mods folder not found, skipping: $destDir")
			return@doLast
		}
		val src = jarFileProvider.get().asFile
		if (!src.isFile) {
			logger.warn("[deployToMods] built jar not found, skipping: $src")
			return@doLast
		}
		val target = destDir.resolve(src.name)
		val timeoutMs = (timeoutProvider.get().toLongOrNull() ?: 0L) * 1000L
		val deadline = if (timeoutMs > 0L) System.currentTimeMillis() + timeoutMs else Long.MAX_VALUE
		val pollMs = 3000L
		var notified = false
		var elapsed = 0L
		while (true) {
			try {
				Files.copy(src.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
				logger.lifecycle("[deployToMods] deployed ${src.name} -> $destDir")
				break
			} catch (e: FileSystemException) {
				// jar is locked — almost always because the game is running and holding it
				if (System.currentTimeMillis() >= deadline) {
					logger.warn("[deployToMods] timed out waiting for ${target.name} to unlock; not deployed.")
					break
				}
				if (!notified) {
					logger.lifecycle("[deployToMods] ${target.name} is locked (game running?) — waiting for the game to close to deploy... (Ctrl+C to skip)")
					notified = true
				} else if (elapsed % 30000L == 0L) {
					logger.lifecycle("[deployToMods] still waiting for the game to close...")
				}
				Thread.sleep(pollMs)
				elapsed += pollMs
			}
		}
	}
}

tasks.named("build") { finalizedBy(deployToMods) }
