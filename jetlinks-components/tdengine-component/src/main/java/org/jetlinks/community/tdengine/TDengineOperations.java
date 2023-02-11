package org.jetlinks.community.tdengine;

import reactor.core.Disposable;

public interface TDengineOperations extends Disposable {

    TDEngineDataWriter forWrite();

    TDEngineQueryOperations forQuery();

}
