package am.ik.blog.counter;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

import org.springframework.data.mongodb.core.query.Criteria;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public class CriteriaBuilder {
	private final URI source;

	private final Instant from;

	private final Instant to;

	private final Optional<Integer> entryId;

	private final Optional<Boolean> browser;

	public CriteriaBuilder(URI source, Instant from, Instant to, Optional<Integer> entryId, Optional<Boolean> browser) {
		this.source = source;
		this.from = from;
		this.to = to;
		this.entryId = entryId;
		this.browser = browser;
	}

	public Criteria build() {
		final Instant now = Counter.truncateTimestamp(Instant.now());
		final Criteria criteria =
				where("source").is(source)
						.and("timestamp")
						.gte(from).lte(to);
		entryId.ifPresent(id -> criteria.and("entryId").is(id));
		browser.ifPresent(b -> criteria.and("browser").is(b));
		return criteria;
	}
}
