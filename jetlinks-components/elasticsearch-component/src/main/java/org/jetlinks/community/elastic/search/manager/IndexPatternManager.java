package org.jetlinks.community.elastic.search.manager;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public interface IndexPatternManager {

    String getName();

    String getPattern(String format);
}
