package org.jetlinks.community.elastic.search.index;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public interface IndexTemplateProvider {

    static String getIndexTemplate(String index) {
        return index.concat("_template");
    }
}
