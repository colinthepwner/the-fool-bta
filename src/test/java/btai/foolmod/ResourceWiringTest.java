package btai.foolmod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import btai.foolmod.entity.FoolEntity;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ResourceWiringTest {

	private static byte[] read(String path) throws IOException {
		try (InputStream in = ResourceWiringTest.class.getResourceAsStream(path)) {
			if (in == null) {
				return null;
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[8192];
			int n;
			while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
			return out.toByteArray();
		}
	}

	private static JsonObject json(String path) throws IOException {
		byte[] b = read(path);
		assertNotNull(b, "missing resource: " + path);
		return JsonParser.parseString(new String(b, StandardCharsets.UTF_8)).getAsJsonObject();
	}

	@Test
	@DisplayName("sounds.json defines exactly the keys the entity plays")
	public void soundKeysMatchTheEntity() throws IOException {
		JsonObject sounds = json("/assets/foolmod/sounds/sounds.json");

		for (String full : new String[]{FoolEntity.SOUND_IDLE, FoolEntity.SOUND_HIT}) {
			assertTrue(full.startsWith("foolmod:"), full + " should be namespaced to foolmod");
			String bare = full.substring("foolmod:".length());
			assertTrue(sounds.has(bare), "sounds.json has no entry for '" + bare + "' (used as " + full + ")");
		}
	}

	@Test
	@DisplayName("every ogg referenced by sounds.json exists and is real Ogg data")
	public void soundFilesExist() throws IOException {
		JsonObject sounds = json("/assets/foolmod/sounds/sounds.json");
		List<String> checked = new ArrayList<>();
		for (String key : sounds.keySet()) {
			JsonArray arr = sounds.getAsJsonObject(key).getAsJsonArray("sounds");
			assertNotNull(arr, key + " has no 'sounds' array");
			for (JsonElement e : arr) {
				if (!e.isJsonPrimitive()) continue;
				String rel = e.getAsString();
				String path = "/assets/foolmod/sounds/" + rel;
				byte[] data = read(path);
				assertNotNull(data, "sound file missing: " + path + " (referenced by '" + key + "')");
				assertTrue(data.length > 512, path + " is suspiciously small (" + data.length + " bytes)");
				assertEquals('O', (char) data[0], path + " is not Ogg data");
				assertEquals('g', (char) data[1], path + " is not Ogg data");
				assertEquals('g', (char) data[2], path + " is not Ogg data");
				assertEquals('S', (char) data[3], path + " is not Ogg data");
				checked.add(rel + " (" + data.length + "B)");
			}
		}
		assertEquals(2, checked.size(), "expected two sound files, found " + checked);
	}

	@Test
	@DisplayName("sound subtitles are translated")
	public void subtitlesHaveTranslations() throws IOException {
		JsonObject sounds = json("/assets/foolmod/sounds/sounds.json");
		byte[] lang = read("/assets/foolmod/lang/en_US/en_US.lang");
		assertNotNull(lang, "missing en_US.lang");
		String text = new String(lang, StandardCharsets.UTF_8);
		for (String key : sounds.keySet()) {
			JsonElement sub = sounds.getAsJsonObject(key).get("subtitle");
			if (sub == null) continue;
			assertTrue(text.contains(sub.getAsString() + "="),
					"no translation for subtitle '" + sub.getAsString() + "' in en_US.lang");
		}
	}

	@Test
	@DisplayName("the entity model manifest is keyed to the registered entity id")
	public void modelManifestMatchesEntityId() throws IOException {
		JsonObject models = json("/assets/foolmod/models/entity/models.json");
		JsonObject renderers = models.getAsJsonObject("renderers");
		assertNotNull(renderers, "models.json has no 'renderers' object");

		assertTrue(renderers.has("foolmod:fool"),
				"models.json declares " + renderers.keySet() + " but the entity registers as 'foolmod:fool'");
		JsonObject fool = renderers.getAsJsonObject("foolmod:fool");
		assertTrue(fool.has("main"),
				"FoolRenderer.getActiveModel asks for the model named \"main\"; the manifest must define it");
	}

	@Test
	@DisplayName("the bundled skin is a valid player-sized PNG")
	public void skinIsValid() throws IOException {
		byte[] png = read("/assets/foolmod/skin.png");
		assertNotNull(png, "missing /assets/foolmod/skin.png — the Fool would render untextured");
		assertEquals((byte) 0x89, png[0], "not a PNG");
		assertEquals('P', (char) png[1]);
		assertEquals('N', (char) png[2]);
		assertEquals('G', (char) png[3]);
		int w = ((png[16] & 0xFF) << 24) | ((png[17] & 0xFF) << 16) | ((png[18] & 0xFF) << 8) | (png[19] & 0xFF);
		int h = ((png[20] & 0xFF) << 24) | ((png[21] & 0xFF) << 16) | ((png[22] & 0xFF) << 8) | (png[23] & 0xFF);
		assertEquals(64, w, "skin width should be 64, got " + w);
		assertTrue(h == 64 || h == 32, "skin height should be 64 (modern) or 32 (legacy), got " + h);
	}

	@Test
	@DisplayName("the mod icon exists")
	public void iconExists() throws IOException {
		assertNotNull(read("/icon.png"), "fabric.mod.json declares icon.png but it is not packaged");
	}

	@Test
	@DisplayName("every class named in fabric.mod.json actually exists")
	public void entrypointClassesResolve() throws IOException {
		JsonObject mod = json("/fabric.mod.json");
		assertEquals("foolmod", mod.get("id").getAsString(), "mod id must match the asset namespace");
		JsonObject entrypoints = mod.getAsJsonObject("entrypoints");
		assertNotNull(entrypoints, "no entrypoints declared");
		int checked = 0;
		for (String kind : entrypoints.keySet()) {
			for (JsonElement e : entrypoints.getAsJsonArray(kind)) {
				String className = e.getAsString();
				try {
					Class.forName(className, false, ResourceWiringTest.class.getClassLoader());
				} catch (ClassNotFoundException ex) {
					throw new AssertionError("entrypoint '" + kind + "' names a missing class: " + className);
				}
				checked++;
			}
		}
		assertTrue(checked >= 4, "expected at least four entrypoints, found " + checked);
	}

	@Test
	@DisplayName("the version placeholder was expanded at build time")
	public void versionWasSubstituted() throws IOException {
		JsonObject mod = json("/fabric.mod.json");
		String version = mod.get("version").getAsString();
		assertTrue(!version.contains("${"), "fabric.mod.json still contains an unexpanded placeholder: " + version);
	}
}
