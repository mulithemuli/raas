package at.muli.raas;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.Getter;

@SpringBootApplication
@RestController
public class RaasApplication {

	private static final Object LAST_USED_REGEX_LOCK = new Object();
	
	@Autowired
	private SimpMessagingTemplate wsMessagingTemplate;
	
	private RegexStats lastUsedRegex;
	
	private Map<String, RegexStats> regexUsages = new ConcurrentHashMap<>();
	
	public static void main(String[] args) {
		SpringApplication.run(RaasApplication.class, args);
	}
	
	@RequestMapping(value = "lastUsedRegex", method = RequestMethod.POST)
	public boolean updateLastUsedRegex(@RequestParam("regex") String regex) {
		RegexStats lastUsageStats = regexUsages.merge(regex, new RegexStats(regex), (o, n) -> o.use());
		synchronized (LAST_USED_REGEX_LOCK) {
			lastUsedRegex = lastUsageStats;
		}
		wsMessagingTemplate.convertAndSend("/topic/lastUsedRegex", lastUsageStats);
		return true;
	}
	
	@RequestMapping(value = "lastUsedRegex", method = RequestMethod.GET)
	public RegexStats getLastUsedRegex() {
		return lastUsedRegex;
	}
	
	@RequestMapping(value = "mostUsedRegex", method = RequestMethod.GET)
	public List<RegexStats> getMostUsedRegex() {
		return regexUsages.values().parallelStream()
				.sorted(Comparator
						.comparing(RegexStats::getUsed).reversed()
						.thenComparing(RegexStats::getLastUsed))
				.collect(Collectors.toList());
	}
	
	@Getter
	public static class RegexStats implements Serializable {
		
		private static final long serialVersionUID = 1L;

		private String regex;
		
		private Long used;
		
		private LocalDateTime lastUsed;
		
		public RegexStats(String regex) {
			this.regex = regex;
			this.used = 1L;
			this.lastUsed = LocalDateTime.now();
		}
		
		public RegexStats use() {
			this.used += 1;
			this.lastUsed = LocalDateTime.now();
			return this;
		}
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
