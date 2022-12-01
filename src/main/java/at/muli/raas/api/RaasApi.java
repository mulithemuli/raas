package at.muli.raas.api;

import at.muli.raas.data.RegexStats;
import at.muli.raas.repository.RegexLoader;
import jakarta.annotation.PostConstruct;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
public class RaasApi {

    private static final Object LAST_USED_REGEX_LOCK = new Object();

    private final SimpMessagingTemplate wsMessagingTemplate;

    private final RegexLoader regexLoader;

    private RegexStats lastUsedRegex;

    private Map<String, RegexStats> regexUsages = new ConcurrentHashMap<>();

    public RaasApi(SimpMessagingTemplate wsMessagingTemplate,
                   RegexLoader regexLoader) {
        this.wsMessagingTemplate = wsMessagingTemplate;
        this.regexLoader = regexLoader;
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

}
