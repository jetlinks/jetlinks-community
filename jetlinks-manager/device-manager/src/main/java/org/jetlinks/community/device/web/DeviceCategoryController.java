package org.jetlinks.community.device.web;

import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.api.crud.entity.TreeSupportEntity;
import org.jetlinks.community.device.entity.DeviceCategory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/device/category")
@Slf4j
@Tag(name = "设备分类目录")
public class DeviceCategoryController {


    static List<DeviceCategory> statics;


    static void rebuild(String parentId, List<DeviceCategory> children) {
        if (children == null) {
            return;
        }
        for (DeviceCategory child : children) {
            String id = child.getId();
            child.setId(parentId + "|" + id + "|");
            child.setParentId(parentId + "|");
            rebuild(parentId + "|" + id, child.getChildren());
        }
    }

    static {
        try {
            ClassPathResource resource = new ClassPathResource("device-category.json");
            String json = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            List<DeviceCategory> all = JSON.parseArray(json, DeviceCategory.class);

            List<DeviceCategory> root = TreeSupportEntity.list2tree(all, DeviceCategory::setChildren);

            for (DeviceCategory category : root) {
                String id = category.getId();

                category.setId("|" + id + "|");
                category.setParentId("|" + category.getParentId() + "|");
                rebuild("|" + id, category.getChildren());
            }

            statics = all;

        } catch (Exception e) {
            statics = new ArrayList<>();
            log.error(e.getMessage(), e);
        }
    }

    @GetMapping
    @Operation(summary = "获取全部分类目录")
    public Flux<DeviceCategory> getAllCategory() {
        return Flux.fromIterable(statics);
    }

    @GetMapping("/_tree")
    @Operation(summary = "获取全部分类目录(树结构)")
    public Flux<DeviceCategory> getAllCategoryTree() {
        return Flux.fromIterable(TreeSupportEntity.list2tree(statics, DeviceCategory::setChildren));
    }
}
