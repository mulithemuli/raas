package at.muli.raas;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.mongodb.MongoClientOptions;

import at.muli.raas.RegexStatsRepository.RegexStatsPO;
import lombok.Getter;

@SpringBootApplication(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@RestController
public class RaasApplication {

	private static final Object LAST_USED_REGEX_LOCK = new Object();
	
	@Autowired
	private SimpMessagingTemplate wsMessagingTemplate;
	
	@Autowired
	private RegexLoader regexLoader;
	
	private RegexStats lastUsedRegex;
	
	private Map<String, RegexStats> regexUsages = new ConcurrentHashMap<>();
	
	public static void main(String[] args) {
		SpringApplication.run(RaasApplication.class, args);
	}
	
	@RequestMapping(value = "lastUsedRegex", method = RequestMethod.POST)
	public RegexStats updateLastUsedRegex(@RequestParam("regex") String regex) {
		RegexStats lastUsageStats = regexUsages.merge(regex, new RegexStats(regex), (o, n) -> o.use());
		synchronized (LAST_USED_REGEX_LOCK) {
			lastUsedRegex = lastUsageStats;
		}
		wsMessagingTemplate.convertAndSend("/topic/lastUsedRegex", lastUsageStats);
		regexLoader.saveLastUsedRegex(lastUsageStats);
		return lastUsageStats;
	}
	
	@RequestMapping(value = "lastUsedRegex", method = RequestMethod.GET)
	public RegexStats getLastUsedRegex() {
		return Optional.ofNullable(lastUsedRegex).orElse(new RegexStats());
	}
	
	@RequestMapping(value = "mostUsedRegex", method = RequestMethod.GET)
	public List<RegexStats> getMostUsedRegex() {
		return regexUsages.values().parallelStream()
				.sorted(Comparator
						.comparing(RegexStats::getUsed).reversed()
						.thenComparing(RegexStats::getLastUsed))
				.collect(Collectors.toList());
	}
	
	@PostConstruct
	public void loadStats() {
		lastUsedRegex = regexLoader.loadLastUsedRegex();
		regexUsages = regexLoader.loadRegexUsages();	
	}
	
	@Bean
	@ConditionalOnMongoUrl
	public RegexLoader regexLoader() {
		return new RegexLoaderDB();
	}
	
	@Bean
	@ConditionalOnMissingBean(RegexLoader.class)
	public RegexLoader regexLoaderNop() {
		return new RegexLoader() {
			@Override
			public RegexStats loadLastUsedRegex() {
				return null;
			}
			
			@Override
			public Map<String, RegexStats> loadRegexUsages() {
				return new ConcurrentHashMap<>();
			}
			
			@Override
			public void saveLastUsedRegex(RegexStats regexStats) {
			}
		};
	}
	
	public interface RegexLoader {
		
		RegexStats loadLastUsedRegex();
		
		Map<String, RegexStats> loadRegexUsages();
		
		void saveLastUsedRegex(RegexStats regexStats);
	}
	
	public static class RegexLoaderDB implements RegexLoader {
		
		@Autowired
		private RegexStatsRepository regexStatsRepository;
		
		public RegexLoaderDB() {
		}
		
		public RegexStats loadLastUsedRegex() {
			return RegexStats.from(regexStatsRepository.findTopByOrderByLastUsedDesc());
		}
		
		@Override
		public Map<String, RegexStats> loadRegexUsages() {
			return regexStatsRepository.findAll().parallelStream().map(RegexStats::from).collect(Collectors.toConcurrentMap(RegexStats::getRegex, rs -> rs));
		}
		
		public void saveLastUsedRegex(RegexStats regexStats) {
			// when the save fails ... it fails.
			new Thread(() -> regexStatsRepository.save(RegexStatsPO.fromRegexStats(regexStats))).start();
		}
	}
	
	@Getter
	public static class RegexStats implements Serializable {
		
		private static final long serialVersionUID = 1L;

		private String regex;
		
		private Long used;
		
		private ZonedDateTime lastUsed;
		
		private RegexStats() {
			this.used = 0L;
		}
		
		public RegexStats(String regex) {
			this.regex = regex;
			this.used = 1L;
			this.lastUsed = ZonedDateTime.now();
		}
		
		private static RegexStats from(RegexStatsPO regexStatsPO) {
			RegexStats regexStats = new RegexStats();
			regexStats.regex = regexStatsPO.getRegex();
			regexStats.used = regexStatsPO.getUsed();
			regexStats.lastUsed = ZonedDateTime.ofInstant(regexStatsPO.getLastUsed().toInstant(), ZoneId.of("UTC"));
			return regexStats;
		}
		
		public RegexStats use() {
			this.used += 1;
			this.lastUsed = ZonedDateTime.now();
			return this;
		}
	}
	
	@ConditionalOnMongoUrl
	@Configuration
	public static class ConditionalMongoAutoConfiguration extends MongoAutoConfiguration {
		
		public ConditionalMongoAutoConfiguration(MongoProperties properties, ObjectProvider<MongoClientOptions> options, Environment environment) {
			super(properties, options, environment);
		}
	}
		
	@ConditionalOnMongoUrl
	@Configuration
	public static class ConditionalMongoDataAutoConfiguration extends MongoDataAutoConfiguration {
		
		public ConditionalMongoDataAutoConfiguration(ApplicationContext applicationContext, MongoProperties properties) {
			super(applicationContext, properties);
		}
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE, ElementType.METHOD })
	@ConditionalOnProperty({"spring.data.mongodb.host", "spring.data.mongodb.host"})
	public static @interface ConditionalOnMongoUrl {
	}
	
	@Configuration
	@EnableWebSocketMessageBroker
	public static class WebsocketConfig implements WebSocketMessageBrokerConfigurer {
		
		@Override
		public void configureMessageBroker(MessageBrokerRegistry registry) {
			registry.enableSimpleBroker("/topic");
		}
		
		@Override
		public void registerStompEndpoints(StompEndpointRegistry registry) {
			registry.addEndpoint("/raasWs").withSockJS();
		}
	}
}
