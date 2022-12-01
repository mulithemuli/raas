package at.muli.raas.repository;

import at.muli.raas.RegexStatsRepository;
import at.muli.raas.data.RegexStats;

import java.util.Map;
import java.util.stream.Collectors;

public class RegexLoaderDB implements RegexLoader {

    private final RegexStatsRepository regexStatsRepository;

    public RegexLoaderDB(RegexStatsRepository regexStatsRepository) {
        this.regexStatsRepository = regexStatsRepository;
    }

    @Override
    public RegexStats loadLastUsedRegex() {
        return RegexStats.from(regexStatsRepository.findTopByOrderByLastUsedDesc());
    }

    @Override
    public Map<String, RegexStats> loadRegexUsages() {
        return regexStatsRepository.findAll().parallelStream().map(RegexStats::from).collect(Collectors.toConcurrentMap(RegexStats::getRegex, rs -> rs));
    }

    @Override
    public void saveLastUsedRegex(RegexStats regexStats) {
        // when the save fails ... it fails.
        new Thread(() -> regexStatsRepository.save(regexStats.to())).start();
    }
}
