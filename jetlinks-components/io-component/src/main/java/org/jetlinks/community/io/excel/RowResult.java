package org.jetlinks.community.io.excel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RowResult<T> {

    private int rowIndex;

    private T result;

}
