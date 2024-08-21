package org.jetlinks.community.form.type;

import org.jetlinks.community.spi.Provider;

import java.sql.JDBCType;
import java.util.Set;

public interface FieldTypeProvider {

    Provider<FieldTypeProvider> supports = Provider.create(FieldTypeProvider.class);

    String getProvider();

    default String getProviderName() {
        return getProvider();
    }

    default int getDefaultLength() {
        return 255;
    }

    Set<JDBCType> getSupportJdbcTypes();

    FieldType create(FieldTypeSpec configuration);

    static FieldType createType(FieldTypeSpec spec) {
        return supports
                .getNow(spec.getName())
                .create(spec);
    }
}
