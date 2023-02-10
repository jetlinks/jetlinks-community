package org.jetlinks.community.tdengine;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DetectTDengineOperations implements TDengineOperations {
    private final TDEngineDataWriter writer;
    private final TDEngineQueryOperations query;

    @Override
    public TDEngineDataWriter forWrite() {
        return writer;
    }

    @Override
    public TDEngineQueryOperations forQuery() {
        return query;
    }

    @Override
    public void dispose() {
        writer.dispose();
    }
}
