package am.ik.blog;

import am.ik.blog.counter.CounterSummary;
import io.cloudevents.spring.webflux.CloudEventHttpMessageReader;
import io.cloudevents.spring.webflux.CloudEventHttpMessageWriter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.codec.CodecConfigurer.CustomCodecs;
import org.springframework.nativex.hint.NativeHint;
import org.springframework.nativex.hint.TypeHint;

@SpringBootApplication
@NativeHint(options = { "--enable-all-security-services" })
@TypeHint(types = CounterSummary.class)
public class CounterApplication {

	public static void main(String[] args) {
		SpringApplication.run(CounterApplication.class, args);
	}

	@Configuration
	public static class CloudEventHandlerConfiguration implements CodecCustomizer {

		@Override
		public void customize(CodecConfigurer configurer) {
			final CustomCodecs codecs = configurer.customCodecs();
			codecs.register(new CloudEventHttpMessageReader());
			codecs.register(new CloudEventHttpMessageWriter());
		}

	}
}
