package org.jetlinks.community.elastic.search.timeseries;

import org.elasticsearch.action.admin.indices.alias.Alias;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public interface IndexAliasProvider {

    static Alias getIndexAlias(String index) {
        return new Alias(index.concat("_alias"));
    }
}
