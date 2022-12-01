package at.muli.raas.repository;

import at.muli.raas.data.RegexStats;

import java.util.Map;

public interface RegexLoader {

    RegexStats loadLastUsedRegex();

    Map<String, RegexStats> loadRegexUsages();

    void saveLastUsedRegex(RegexStats regexStats);
}
