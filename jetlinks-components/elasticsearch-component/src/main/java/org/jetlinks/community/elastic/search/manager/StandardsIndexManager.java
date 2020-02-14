package org.jetlinks.community.elastic.search.manager;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public interface StandardsIndexManager {

    String getStandardsIndex(String index);

    boolean indexIsChange(String index);

    boolean indexIsUpdate(String index);

    boolean standardsIndexIsUpdate(String standardsIndex);

    void addStandardsIndex(String standardsIndex);

    public void registerIndexManager(String index, IndexManager indexManager);

}
