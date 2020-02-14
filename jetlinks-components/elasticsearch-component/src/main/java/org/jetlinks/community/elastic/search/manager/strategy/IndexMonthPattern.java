package org.jetlinks.community.elastic.search.manager.strategy;

import org.jetlinks.community.elastic.search.enums.IndexPatternEnum;
import org.jetlinks.community.elastic.search.manager.IndexPatternManager;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Component
public class IndexMonthPattern implements IndexPatternManager {

    @Override
    public String getName() {
        return IndexPatternEnum.MONTH.getValue();
    }

    @Override
    public String getPattern(String format) {
        LocalDateTime localDateTime = LocalDateTime.now();
        return localDateTime.format(DateTimeFormatter.ofPattern(format));
    }
}
