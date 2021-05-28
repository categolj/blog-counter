package am.ik.blog.counter.web;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import am.ik.blog.counter.Counter;
import am.ik.blog.counter.CounterService;
import am.ik.blog.counter.CounterSummary;
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
	public Flux<?> report(
			@PathVariable("source") URI source,
			@RequestParam(name = "from") Optional<Instant> from,
			@RequestParam(name = "to") Optional<Instant> to,
			@RequestParam(name = "entryId") Optional<Integer> entryId,
			@RequestParam(name = "browser") Optional<Boolean> browser) {
		final Instant now = Counter.truncateTimestamp(Instant.now());
		final Instant f = from.orElse(now.minus(1, ChronoUnit.DAYS));
		final Instant t = to.orElse(now);
		final Criteria criteria = new CriteriaBuilder(source, f, t, entryId, browser).build();
		return this.counterService.report(criteria)
				.collectList()
				.map(list -> interpolate(list, f, t))
				.flatMapIterable(Function.identity());
	}

	static public List<CounterSummary> interpolate(List<CounterSummary> counts, Instant from, Instant to) {
		final List<CounterSummary> interpolated = new ArrayList<>();
		Instant t = Counter.truncateTimestamp(from);
		final Map<Instant, CounterSummary> countMap = counts.stream().collect(Collectors.toMap(CounterSummary::getTimestamp, Function.identity()));
		while (t.isBefore(to)) {
			final CounterSummary count = countMap.get(t);
			if (count != null) {
				interpolated.add(count);
			}
			else {
				interpolated.add(new CounterSummary(t, 0));
			}
			t = t.plus(15, ChronoUnit.MINUTES);
		}
		return interpolated;
	}
}
