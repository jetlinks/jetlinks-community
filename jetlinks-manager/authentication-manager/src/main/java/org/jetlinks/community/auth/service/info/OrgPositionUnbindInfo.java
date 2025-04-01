package org.jetlinks.community.auth.service.info;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @author wangsheng
 */
@Getter
@Setter
@AllArgsConstructor(staticName = "of")
public class OrgPositionUnbindInfo implements Serializable {

    private static final long serialVersionUID = -8749814596871175566L;

    private List<String> userIds;

    private List<String> orgIds;
}
