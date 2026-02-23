# 修复项目保存返回值问题

## 日期
2026-02-23

## 问题描述

### 错误信息
```
org.hibernate.PropertyValueException: not-null property references a null or transient value: com.opster.module.project.entity.SubProject.projectId
```

### 根本原因
创建项目时,后端 `/project` POST 接口返回的是 `boolean` 类型,而不是保存后的 `Project` 对象。前端无法从响应中获取新生成的 `projectId`,导致保存子项目时 `projectId` 为 `null`。

**前端期望**:
```javascript
const res = await request.post('/project', projectData)
projectId = res.id  // 期望 res 是 Project 对象,包含 id 字段
```

**后端实际返回**:
```java
@PostMapping
public boolean save(@RequestBody Project project) {  // 返回 boolean,而不是 Project
    projectService.save(project);
    return true;  // 返回 true,前端无法获取 id
}
```

## 修改内容

### 1. ProjectController.java
**文件**: `opster-backend/src/main/java/com/opster/module/project/controller/ProjectController.java`

#### 修改 1: 新增项目接口 (第 58-61 行)

**修改前**:
```java
@PostMapping
public boolean save(@RequestBody Project project) {
    projectService.save(project);
    return true;
}
```

**修改后**:
```java
@PostMapping
public Project save(@RequestBody Project project) {
    return projectService.save(project);  // 返回保存后的 Project 对象(包含 id)
}
```

#### 修改 2: 修改项目接口 (第 66-69 行)

**修改前**:
```java
@PutMapping
public boolean update(@RequestBody Project project) {
    projectService.save(project);
    return true;
}
```

**修改后**:
```java
@PutMapping
public Project update(@RequestBody Project project) {
    return projectService.save(project);  // 返回更新后的 Project 对象
}
```

#### 修改 3: 删除项目接口 (第 74-77 行)

**修改前**:
```java
@DeleteMapping("/{id}")
public boolean remove(@PathVariable Integer id) {
    projectService.deleteById(id);
    return true;
}
```

**修改后**:
```java
@DeleteMapping("/{id}")
public void remove(@PathVariable Integer id) {
    projectService.deleteById(id);  // 删除操作不需要返回值
}
```

### 2. SubProjectServiceImpl.java
**文件**: `opster-backend/src/main/java/com/opster/module/project/service/impl/SubProjectServiceImpl.java`

#### 新增验证逻辑 (第 80-95 行)

**修改前**:
```java
@Override
public SubProject save(SubProject subProject) {
    return subProjectRepository.save(subProject);
}
```

**修改后**:
```java
@Override
public SubProject save(SubProject subProject) {
    // 验证必填字段
    if (subProject.getProjectId() == null) {
        throw new IllegalArgumentException("项目ID不能为空");
    }
    if (StrUtil.isBlank(subProject.getSubProjectName())) {
        throw new IllegalArgumentException("子项目名称不能为空");
    }
    if (StrUtil.isBlank(subProject.getGitUrl())) {
        throw new IllegalArgumentException("Git仓库地址不能为空");
    }

    return subProjectRepository.save(subProject);
}
```

同时添加了导入:
```java
import cn.hutool.core.util.StrUtil;
```

## 影响

### 正面影响
- ✅ 前端可以正确获取新创建项目的 `id`
- ✅ 子项目能够正确关联到项目
- ✅ 新增/更新项目时返回完整的对象,便于前端使用
- ✅ 添加了必填字段验证,提供更友好的错误提示

### 兼容性说明
- **破坏性变更**: `/project` POST 和 PUT 接口的返回值从 `boolean` 改为 `Project`
- **前端影响**: 如果前端有其他地方依赖这两个接口返回 `boolean`,需要相应调整
- **建议**: 检查前端所有调用 `/project` POST 和 PUT 的地方,确保正确处理返回的 `Project` 对象

## 测试建议

1. **新建项目测试**: 创建一个包含子项目的新项目,验证子项目能够正确保存
2. **更新项目测试**: 更新现有项目及子项目,验证数据正确更新
3. **错误处理测试**: 尝试保存缺少必填字段的子项目,验证错误提示是否正确
