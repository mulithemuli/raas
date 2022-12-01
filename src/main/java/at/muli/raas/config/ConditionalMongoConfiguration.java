package at.muli.raas.config;

import at.muli.raas.ConditionalOnMongoUrl;
import at.muli.raas.RegexStatsRepository;
import at.muli.raas.repository.RegexLoader;
import at.muli.raas.repository.RegexLoaderDB;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnMongoUrl
@Configuration
@ImportAutoConfiguration({MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
public class ConditionalMongoConfiguration {

    @Bean
    public RegexLoader regexLoader(RegexStatsRepository regexStatsRepository) {
        return new RegexLoaderDB(regexStatsRepository);
    }
}
