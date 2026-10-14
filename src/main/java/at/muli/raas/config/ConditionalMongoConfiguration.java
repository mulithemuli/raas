package at.muli.raas.config;

import at.muli.raas.ConditionalOnMongoUrl;
import at.muli.raas.RegexStatsRepository;
import at.muli.raas.repository.RegexLoader;
import at.muli.raas.repository.RegexLoaderDB;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnMongoUrl
@Configuration
@ImportAutoConfiguration({MongoAutoConfiguration.class, DataMongoAutoConfiguration.class})
public class ConditionalMongoConfiguration {

    @Bean
    public RegexLoader regexLoader(RegexStatsRepository regexStatsRepository) {
        return new RegexLoaderDB(regexStatsRepository);
    }
}
