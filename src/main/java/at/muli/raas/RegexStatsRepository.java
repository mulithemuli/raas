package at.muli.raas;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.repository.MongoRepository;

import at.muli.raas.RaasApplication.ConditionalOnMongoUrl;
import at.muli.raas.RaasApplication.RegexStats;
import at.muli.raas.RegexStatsRepository.RegexStatsPO;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@ConditionalOnMongoUrl
public interface RegexStatsRepository extends MongoRepository<RegexStatsPO, String> {
	
	RegexStatsPO findTopByOrderByLastUsedDesc();
	
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	@NoArgsConstructor
	@Getter
	public class RegexStatsPO {
		
		@Id
		private String regex;
		
		private Long used;
		
		private Date lastUsed;
		
		public static RegexStatsPO fromRegexStats(RegexStats regexStats) {
			return new RegexStatsPO(regexStats.getRegex(), regexStats.getUsed(), Date.from(regexStats.getLastUsed().toInstant()));
		}
	}
}