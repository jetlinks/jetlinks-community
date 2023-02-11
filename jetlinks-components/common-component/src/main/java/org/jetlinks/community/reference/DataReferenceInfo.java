package org.jetlinks.community.reference;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class DataReferenceInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String dataId;

    private String referenceType;

    private String referenceId;

    private String referenceName;

}
