package am.ik.blog.counter;

import java.time.Instant;

public interface TimestampHolder<T> {
	Instant getTimestamp();

	T unwrap();
}
