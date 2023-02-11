package org.jetlinks.community.reference;

import lombok.Getter;
import org.hswebframework.web.exception.I18nSupportException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@Getter
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DataReferencedException extends I18nSupportException {

    private final String dataType;
    private final String dataId;

    private final List<DataReferenceInfo> referenceList;


    public DataReferencedException(String dataType,
                                   String dataId,
                                   List<DataReferenceInfo> referenceList) {
        this.dataType = dataType;
        this.dataId = dataId;
        this.referenceList = referenceList;

        super.setI18nCode("error.data.referenced");
    }

}
