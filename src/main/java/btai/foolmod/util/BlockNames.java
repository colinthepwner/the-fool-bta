package btai.foolmod.util;

import java.util.Set;
import net.minecraft.core.block.Block;

public final class BlockNames {

	private BlockNames() {
	}

	public static String of(Block<?> block) {
		if (block == null) {
			return "";
		}
		String value = block.namespaceId().value();
		int slash = value.lastIndexOf('/');
		return (slash >= 0 ? value.substring(slash + 1) : value).toLowerCase();
	}

	public static boolean hasToken(Block<?> block, String token) {
		return containsToken(of(block), token);
	}

	public static boolean hasAnyToken(Block<?> block, Set<String> tokens) {
		String name = of(block);
		if (name.isEmpty()) {
			return false;
		}
		int start = 0;
		while (start <= name.length()) {
			int end = name.indexOf('_', start);
			if (end < 0) end = name.length();
			if (tokens.contains(name.substring(start, end))) {
				return true;
			}
			start = end + 1;
		}
		return false;
	}

	private static boolean containsToken(String name, String token) {
		int start = 0;
		while (start <= name.length()) {
			int end = name.indexOf('_', start);
			if (end < 0) end = name.length();
			if (name.regionMatches(start, token, 0, Math.max(end - start, token.length()))
					&& end - start == token.length()) {
				return true;
			}
			start = end + 1;
		}
		return false;
	}
}
