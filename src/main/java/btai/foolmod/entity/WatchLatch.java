package btai.foolmod.entity;

public final class WatchLatch {

	private final int spotDwell;
	private final int clearDwell;

	private boolean believed;
	private int watchedTicks;
	private int unwatchedTicks;

	public WatchLatch(int spotDwell, int clearDwell) {
		this.spotDwell = spotDwell;
		this.clearDwell = clearDwell;
	}

	public void update(boolean watchedNow) {
		if (watchedNow) {
			unwatchedTicks = 0;
			if (++watchedTicks >= spotDwell) {
				believed = true;
			}
		} else {
			watchedTicks = 0;
			if (believed && ++unwatchedTicks >= clearDwell) {
				believed = false;
				unwatchedTicks = 0;
			}
		}
	}

	public boolean believed() {
		return believed;
	}

	public void reset() {
		believed = false;
		watchedTicks = 0;
		unwatchedTicks = 0;
	}
}
