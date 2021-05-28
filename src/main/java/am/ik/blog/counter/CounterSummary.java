package am.ik.blog.counter;

import java.time.Instant;

public class CounterSummary {
	private final Instant timestamp;

	private final long count;

	public CounterSummary(Instant timestamp, long count) {
		this.timestamp = timestamp;
		this.count = count;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public long getCount() {
		return count;
	}
}
