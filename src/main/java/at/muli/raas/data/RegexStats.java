package at.muli.raas.data;

import at.muli.raas.RegexStatsRepository;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

@Getter
public class RegexStats implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String regex;

    private Long used;

    private ZonedDateTime lastUsed;

    public RegexStats() {
        this.used = 0L;
    }

    public RegexStats(String regex) {
        this.regex = regex;
        this.used = 1L;
        this.lastUsed = ZonedDateTime.now();
    }

    public static RegexStats from(RegexStatsRepository.RegexStatsPO regexStatsPO) {
        RegexStats regexStats = new RegexStats();
        regexStats.regex = regexStatsPO.getRegex();
        regexStats.used = regexStatsPO.getUsed();
        regexStats.lastUsed = ZonedDateTime.ofInstant(regexStatsPO.getLastUsed().toInstant(), ZoneId.of("UTC"));
        return regexStats;
    }

    public RegexStatsRepository.RegexStatsPO to() {
        return new RegexStatsRepository.RegexStatsPO(regex, used, Date.from(lastUsed.toInstant()));
    }

    public RegexStats use() {
        this.used += 1;
        this.lastUsed = ZonedDateTime.now();
        return this;
    }
}
