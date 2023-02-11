package org.jetlinks.community.notify.wechat.corp.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Setter;
import org.jetlinks.community.notify.wechat.corp.CorpDepartment;

import java.util.Collections;
import java.util.List;

@Setter
public class GetDepartmentResponse extends ApiResponse {


    @JsonProperty
    @JsonAlias("department")
    private List<CorpDepartment> department;

    public List<CorpDepartment> getDepartment() {
        return department == null ? Collections.emptyList() : department;
    }
}
