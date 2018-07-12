package at.muli.raas;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.repository.MongoRepository;

import at.muli.raas.RegexStatsRepository.RegexStatsPO;
import lombok.AllArgsConstructor;
import lombok.Getter;

@ConditionalOnMongoUrl
public interface RegexStatsRepository extends MongoRepository<RegexStatsPO, String> {
	
	RegexStatsPO findTopByOrderByLastUsedDesc();
	
	@AllArgsConstructor
	@Getter
	public class RegexStatsPO {
		
		@Id
		private String regex;
		
		private Long used;
		
		private Date lastUsed;
	}
}