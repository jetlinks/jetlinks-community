package org.jetlinks.community.relation;

import org.jetlinks.core.things.relation.RelationManager;
import reactor.core.Disposable;

public class RelationManagerInitializer implements Disposable {

    private final RelationManager temp;
    private final RelationManager target;

    public RelationManagerInitializer(RelationManager manager) {
        temp = RelationManagerHolder.hold;
        target = manager;
        RelationManagerHolder.hold = manager;
    }

    @Override
    public void dispose() {
        if (RelationManagerHolder.hold == target) {
            RelationManagerHolder.hold = temp;
        }
    }
}
