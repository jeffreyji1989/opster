package com.opster.module.service.controller;

import com.opster.module.service.dto.StartScriptDTO;
import com.opster.module.service.dto.StartScriptVersionDTO;
import com.opster.module.service.service.ServiceStartScriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 启动脚本管理控制器
 */
@RestController
@RequestMapping("/api/service-start-script")
@RequiredArgsConstructor
public class ServiceStartScriptController {

    private final ServiceStartScriptService scriptService;

    /**
     * 查询所有脚本列表
     */
    @GetMapping("/list")
    public ResponseEntity<List<StartScriptDTO>> listScripts() {
        return ResponseEntity.ok(scriptService.listScripts());
    }

    /**
     * 获取脚本详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<StartScriptDTO> getScriptDetail(@PathVariable Integer id) {
        return ResponseEntity.ok(scriptService.getScriptDetail(id));
    }

    /**
     * 创建脚本
     */
    @PostMapping("/create")
    public ResponseEntity<StartScriptDTO> createScript(@RequestBody StartScriptDTO dto) {
        return ResponseEntity.ok(scriptService.createScript(dto));
    }

    /**
     * 更新脚本
     */
    @PostMapping("/update")
    public ResponseEntity<StartScriptDTO> updateScript(@RequestBody StartScriptDTO dto) {
        return ResponseEntity.ok(scriptService.updateScript(dto));
    }

    /**
     * 删除脚本
     */
    @PostMapping("/delete/{id}")
    public ResponseEntity<Void> deleteScript(@PathVariable Integer id) {
        scriptService.deleteScript(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 查询脚本的所有版本
     */
    @GetMapping("/{scriptId}/versions")
    public ResponseEntity<List<StartScriptVersionDTO>> getScriptVersions(@PathVariable Integer scriptId) {
        return ResponseEntity.ok(scriptService.getScriptVersions(scriptId));
    }

    /**
     * 激活指定版本
     */
    @PostMapping("/{scriptId}/activate/{versionId}")
    public ResponseEntity<Void> activateVersion(
            @PathVariable Integer scriptId,
            @PathVariable Integer versionId) {
        scriptService.activateVersion(scriptId, versionId);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取默认脚本
     */
    @GetMapping("/default")
    public ResponseEntity<StartScriptDTO> getDefaultScript() {
        StartScriptDTO dto = scriptService.getDefaultScript();
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }
}
