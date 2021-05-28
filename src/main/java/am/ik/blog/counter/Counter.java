package am.ik.blog.counter;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;

import com.fasterxml.jackson.annotation.JsonCreator;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class Counter {
	@Id
	private String id;

	@Indexed
	private Instant timestamp;

	@Indexed
	private boolean browser;

	@Indexed
	private int entryId;

	@Indexed
	private URI source;

	private long value;

	@JsonCreator
	public static Counter newCounter(Instant timestamp, boolean browser, int entryId) {
		return new Counter(null, timestamp, browser, entryId, null, 1L);
	}

	public Counter(String id, Instant timestamp, boolean browser, int entryId, URI source, long value) {
		this.id = id;
		this.timestamp = truncateTimestamp(timestamp);
		this.browser = browser;
		this.entryId = entryId;
		this.source = source;
		this.value = value;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	public boolean isBrowser() {
		return browser;
	}

	public void setBrowser(boolean browser) {
		this.browser = browser;
	}

	public int getEntryId() {
		return entryId;
	}

	public void setEntryId(int entryId) {
		this.entryId = entryId;
	}

	public URI getSource() {
		return source;
	}

	public void setSource(URI source) {
		this.source = source;
	}

	public long getValue() {
		return value;
	}

	public void setValue(long value) {
		this.value = value;
	}

	public static Instant truncateTimestamp(Instant timestamp) {
		return timestamp.truncatedTo(CounterTemporalUnit.COUNTER);
	}

	private enum CounterTemporalUnit implements TemporalUnit {

		COUNTER("Counter", Duration.ofMinutes(15)),
		DAYS("Days", Duration.ofSeconds(86400));

		private final String name;

		private final Duration duration;

		private CounterTemporalUnit(String name, Duration estimatedDuration) {
			this.name = name;
			this.duration = estimatedDuration;
		}

		//-----------------------------------------------------------------------

		/**
		 * Gets the estimated duration of this unit in the ISO calendar system.
		 * <p>
		 * All of the units in this class have an estimated duration.
		 * Days vary due to daylight saving time, while months have different lengths.
		 *
		 * @return the estimated duration of this unit, not null
		 */
		@Override
		public Duration getDuration() {
			return duration;
		}

		/**
		 * Checks if the duration of the unit is an estimate.
		 * <p>
		 * All time units in this class are considered to be accurate, while all date
		 * units in this class are considered to be estimated.
		 * <p>
		 * This definition ignores leap seconds, but considers that Days vary due to
		 * daylight saving time and months have different lengths.
		 *
		 * @return true if the duration is estimated, false if accurate
		 */
		@Override
		public boolean isDurationEstimated() {
			return this.compareTo(DAYS) >= 0;
		}

		//-----------------------------------------------------------------------

		/**
		 * Checks if this unit is a date unit.
		 * <p>
		 * All units from days to eras inclusive are date-based.
		 * Time-based units and {@code FOREVER} return false.
		 *
		 * @return true if a date unit, false if a time unit
		 */
		@Override
		public boolean isDateBased() {
			return this.compareTo(DAYS) >= 0;
		}

		/**
		 * Checks if this unit is a time unit.
		 * <p>
		 * All units from nanos to half-days inclusive are time-based.
		 * Date-based units and {@code FOREVER} return false.
		 *
		 * @return true if a time unit, false if a date unit
		 */
		@Override
		public boolean isTimeBased() {
			return this.compareTo(DAYS) < 0;
		}

		//-----------------------------------------------------------------------
		@Override
		public boolean isSupportedBy(Temporal temporal) {
			return temporal.isSupported(this);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <R extends Temporal> R addTo(R temporal, long amount) {
			return (R) temporal.plus(amount, this);
		}

		//-----------------------------------------------------------------------
		@Override
		public long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive) {
			return temporal1Inclusive.until(temporal2Exclusive, this);
		}

		//-----------------------------------------------------------------------
		@Override
		public String toString() {
			return name;
		}

	}
}
