package at.muli.raas.config;

import at.muli.raas.data.RegexStats;
import at.muli.raas.repository.RegexLoader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RaasApplicationConfig {

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
}
