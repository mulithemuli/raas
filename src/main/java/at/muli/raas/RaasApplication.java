package at.muli.raas;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.Getter;

@SpringBootApplication
@RestController
public class RaasApplication {

	private static final Object LAST_USED_REGEX_LOCK = new Object();
	
	private String lastUsedRegex;
	
	private Map<String, RegexUsage> regexUsages = new ConcurrentHashMap<>();
	
	public static void main(String[] args) {
		SpringApplication.run(RaasApplication.class, args);
	}
	
	@RequestMapping(value = "lastUsedRegex", method = RequestMethod.POST)
	public void updateLastUsedRegex(@RequestParam("regex") String regex) {
		synchronized (LAST_USED_REGEX_LOCK) {
			lastUsedRegex = regex;
		}
		regexUsages.merge(regex, new RegexUsage(regex), (o, n) -> o.use()); 
	}
	
	@RequestMapping(value = "lastUsedRegex", method = RequestMethod.GET)
	public String getLastUsedRegex() {
		return lastUsedRegex;
	}
	
	@RequestMapping(value = "mostUsedRegex", method = RequestMethod.GET)
	public List<RegexUsage> getMostUsedRegex() {
		return regexUsages.values().parallelStream()
				.sorted(Comparator
						.comparing(RegexUsage::getUsed).reversed()
						.thenComparing(RegexUsage::getLastUsed))
				.collect(Collectors.toList());
	}
	
	@Getter
	public static class RegexUsage implements Serializable {
		
		private static final long serialVersionUID = 1L;

		private String regex;
		
		private Long used;
		
		private LocalDateTime lastUsed;
		
		public RegexUsage(String regex) {
			this.regex = regex;
			this.used = 1L;
			this.lastUsed = LocalDateTime.now();
		}
		
		public RegexUsage use() {
			this.used += 1;
			this.lastUsed = LocalDateTime.now();
			return this;
		}
	}
}
