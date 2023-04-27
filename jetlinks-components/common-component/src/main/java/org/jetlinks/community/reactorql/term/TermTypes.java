package org.jetlinks.community.reactorql.term;

import org.jetlinks.core.metadata.DataType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @see org.jetlinks.community.utils.ReactorUtils#createFilter(List)
 */
public class TermTypes {
    private static final Map<String, TermTypeSupport> supports = new LinkedHashMap<>();

    static {
        for (FixedTermTypeSupport value : FixedTermTypeSupport.values()) {
            register(value);
        }
    }

    public static void register(TermTypeSupport support){
        supports.put(support.getType(),support);
    }

    public static List<TermType> lookup(DataType dataType) {

        return supports
            .values()
            .stream()
            .filter(support -> support.isSupported(dataType))
            .map(TermTypeSupport::type)
            .collect(Collectors.toList());
    }

    public static Optional<TermTypeSupport> lookupSupport(String type) {
        return Optional.ofNullable(supports.get(type));
    }
}
