package org.jetlinks.community.rule.engine.service.terms;


import com.alibaba.fastjson.JSONException;
import org.hswebframework.ezorm.core.FeatureId;
import org.hswebframework.ezorm.core.param.SqlTerm;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.metadata.RDBSchemaMetadata;
import org.hswebframework.ezorm.rdb.metadata.RDBTableMetadata;
import org.hswebframework.ezorm.rdb.metadata.TableOrViewMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.TermFragmentBuilder;
import org.hswebframework.ezorm.rdb.operator.dml.query.NativeSelectColumn;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeviceAlarmTermBuilderTest {

    @Test
    void createFragments() {
        DeviceAlarmTermBuilder termBuilder = new DeviceAlarmTermBuilder();
        RDBColumnMetadata metadata = Mockito.mock(RDBColumnMetadata.class);
        SqlTerm term = SqlTerm.of("select * from table");
        term.setValue("name=c and age=test order by test");
        List<String> options = new ArrayList<>();

        options.add("not");
        term.setOptions(options);

        TableOrViewMetadata viewMetadata = Mockito.mock(TableOrViewMetadata.class);
        RDBSchemaMetadata schemaMetadata = Mockito.mock(RDBSchemaMetadata.class);
        Mockito.when(metadata.getOwner())
            .thenReturn(viewMetadata);
        Mockito.when(viewMetadata.getSchema())
            .thenReturn(schemaMetadata);
//        RDBTableMetadata rdbTableMetadata = new RDBTableMetadata();
        RDBTableMetadata rdbTableMetadata = Mockito.mock(RDBTableMetadata.class);
        RDBColumnMetadata rdbColumnMetadata = Mockito.mock(RDBColumnMetadata.class);
        Mockito.when(schemaMetadata.getTable(Mockito.anyString()))
            .thenReturn(Optional.of(rdbTableMetadata));

        termBuilder.createFragments("test",metadata,term);


        Mockito.when(rdbTableMetadata.getColumn(Mockito.anyString()))
            .thenReturn(Optional.of(rdbColumnMetadata));

        TermFragmentBuilder fragmentBuilder = Mockito.mock(TermFragmentBuilder.class);
        Mockito.when(rdbTableMetadata.findFeature(Mockito.any(FeatureId.class)))
            .thenReturn(Optional.of(fragmentBuilder));
        Mockito.when(rdbColumnMetadata.getFullName(Mockito.anyString()))
            .thenReturn("ccc");
        PrepareSqlFragments of = PrepareSqlFragments.of();
        of.addSql("sss");
        Mockito.when(fragmentBuilder.createFragments(Mockito.anyString(),Mockito.any(RDBColumnMetadata.class),Mockito.any(Term.class)))
            .thenReturn(of);
        termBuilder.createFragments("test",metadata,term);

        term.setValue("['name=测试 and age=test']");
        Executable executable = ()->termBuilder.createFragments("test",metadata,term);
        assertThrows(JSONException.class,executable);

        term.setValue(1);
        Executable executable1 = ()->termBuilder.createFragments("test",metadata,term);
        assertThrows(UnsupportedOperationException.class,executable1);
    }

    @Test
    void alarmTermBuilder() {
        TableOrViewMetadata tableOrViewMetadata = Mockito.mock(TableOrViewMetadata.class);
        Term term = new Term();
        term.setValue(NativeSelectColumn.of("select * from table"));
        SqlFragments termFragments = DeviceAlarmTermBuilder.builder.createTermFragments(tableOrViewMetadata, term);
        assertNotNull(termFragments);
    }
}