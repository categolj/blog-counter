package am.ik.blog.counter;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class CounterService {
	private final ReactiveMongoTemplate mongoTemplate;

	public CounterService(ReactiveMongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public Mono<Counter> increment(Counter counter) {
		final Query query = Query.query(Criteria.where("entryId").is(counter.getEntryId())
				.and("browser").is(counter.isBrowser())
				.and("timestamp").is(counter.getTimestamp()));
		final Update update = new Update().inc("value", 1);
		final FindAndModifyOptions options = FindAndModifyOptions.options()
				.returnNew(true);
		return this.mongoTemplate.findAndModify(query, update, options, Counter.class)
				.switchIfEmpty(this.mongoTemplate.insert(counter));
	}

	public Flux<CounterSummary> report(Criteria criteria) {
		final Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(criteria),
				Aggregation.group("timestamp")
						.first("timestamp").as("timestamp")
						.sum("value").as("count"),
				Aggregation.sort(Direction.ASC, "timestamp"));
		return this.mongoTemplate.aggregate(aggregation, Counter.class, CounterSummary.class);
	}

}
