package am.ik.blog.counter.web;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import am.ik.blog.counter.Counter;
import am.ik.blog.counter.CounterService;
import am.ik.blog.counter.CounterSummary;
import am.ik.blog.counter.CounterSummary.ByBrowser;
import am.ik.blog.counter.CriteriaBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class CounterController {
	private final CounterService counterService;

	private final ObjectMapper objectMapper;

	public CounterController(CounterService counterService, ObjectMapper objectMapper) {
		this.counterService = counterService;
		this.objectMapper = objectMapper;
	}

	@PostMapping(path = "/")
	public Mono<?> counter(@RequestBody CloudEvent body) throws Exception {
		if (!"counter".equalsIgnoreCase(body.getType())) {
			return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "'type' must be 'counter'. given: " + body.getType()));
		}
		final Counter counter = this.objectMapper.readValue(body.getData().toBytes(), Counter.class);
		counter.setSource(body.getSource());
		return this.counterService.increment(counter);
	}

	@GetMapping(path = "/{source}")
	public Flux<?> reportSummary(
			@PathVariable("source") URI source,
			@RequestParam(name = "from") Optional<Instant> from,
			@RequestParam(name = "to") Optional<Instant> to,
			@RequestParam(name = "entryId") Optional<Integer> entryId,
			@RequestParam(name = "browser") Optional<Boolean> browser,
			@RequestParam(name = "interpolate", defaultValue = "true") boolean interpolate) {
		final Instant now = Counter.truncateTimestamp(Instant.now());
		final Instant f = from.orElse(now.minus(1, ChronoUnit.DAYS));
		final Instant t = to.orElse(now);
		final Criteria criteria = new CriteriaBuilder(source, f, t, entryId, browser).build();
		return this.counterService.reportSummary(criteria)
				.collectList()
				.map(list -> interpolate ? interpolate(list, f, t, timestamp -> new CounterSummary(timestamp, 0)) : list)
				.flatMapIterable(Function.identity());
	}

	@GetMapping(path = "/{source}/browser")
	public Flux<?> reportSummaryByBrowser(
			@PathVariable("source") URI source,
			@RequestParam(name = "from") Optional<Instant> from,
			@RequestParam(name = "to") Optional<Instant> to,
			@RequestParam(name = "entryId") Optional<Integer> entryId,
			@RequestParam(name = "interpolate", defaultValue = "true") boolean interpolate) {
		final Instant now = Counter.truncateTimestamp(Instant.now());
		final Instant f = from.orElse(now.minus(1, ChronoUnit.DAYS));
		final Instant t = to.orElse(now);
		final Criteria criteria = new CriteriaBuilder(source, f, t, entryId, Optional.empty()).build();
		return this.counterService.reportSummaryByBrowser(criteria)
				.collectList()
				.map(list -> interpolate ? interpolate(list, f, t, timestamp -> new ByBrowser(timestamp, 0, true)) : list)
				.flatMapIterable(Function.identity())
				.collectMultimap(CounterSummary::getTimestamp, Function.identity())
				.flatMapIterable(Map::entrySet)
				.sort(Comparator.comparing(Entry::getKey))
				.map(entry -> {
					final Map<String, Object> map = new HashMap<>();
					map.put("timestamp", entry.getKey());
					entry.getValue().forEach(c -> {
						if (c.isBrowser()) {
							map.put("browser", c.getCount());
						}
						else {
							map.put("nonBrowser", c.getCount());
						}
					});
					return map;
				});
	}

	static public <T extends CounterSummary> List<T> interpolate(List<T> counts, Instant from, Instant to, Function<Instant, T> zeroCount) {
		final List<T> interpolated = new ArrayList<>();
		Instant t = Counter.truncateTimestamp(from);
		final Map<Instant, T> countMap = counts.stream().collect(Collectors.toMap(CounterSummary::getTimestamp, Function.identity()));
		while (t.isBefore(to)) {
			final T count = countMap.get(t);
			if (count != null) {
				interpolated.add(count);
			}
			else {
				interpolated.add(zeroCount.apply(t));
			}
			t = t.plus(15, ChronoUnit.MINUTES);
		}
		return interpolated;
	}
}
