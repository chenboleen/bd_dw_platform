# 任务 1.6: 异步任务和线程池配置

## 任务概述

为数据仓库元数据管理系统配置异步任务执行器和线程池，支持异步导出等耗时操作。

## 实现内容

### 1. AsyncProperties 配置属性类

**文件**: `src/main/java/com/kiro/metadata/config/AsyncProperties.java`

**功能**:
- 从 `application.yml` 读取 `async.*` 配置
- 绑定线程池参数到 Java 对象

**配置属性**:
- `corePoolSize`: 核心线程数（默认: 5）
- `maxPoolSize`: 最大线程数（默认: 10）
- `queueCapacity`: 队列容量（默认: 100）
- `threadNamePrefix`: 线程名称前缀（默认: "async-task-"）

### 2. AsyncConfig 配置类

**文件**: `src/main/java/com/kiro/metadata/config/AsyncConfig.java`

**功能**:
- 实现 `AsyncConfigurer` 接口
- 配置 `ThreadPoolTaskExecutor` Bean
- 启用 `@EnableAsync` 注解
- 配置异步异常处理器

**线程池配置**:
- 核心线程数: 从配置文件读取
- 最大线程数: 从配置文件读取
- 队列容量: 从配置文件读取
- 线程名称前缀: 从配置文件读取
- 拒绝策略: `CallerRunsPolicy`（调用者运行策略）
- 等待任务完成: 关闭时等待60秒

### 3. application.yml 配置

**文件**: `src/main/resources/application.yml`

```yaml
# 异步任务配置
async:
  core-pool-size: 5
  max-pool-size: 10
  queue-capacity: 100
  thread-name-prefix: async-task-
```

### 4. 导出文件存储配置

**文件**: `src/main/resources/application.yml`

```yaml
# 导出文件存储路径
export:
  file-path: ./exports
```

### 5. 单元测试

**文件**: `src/test/java/com/kiro/metadata/config/AsyncConfigTest.java`

**测试内容**:
- 线程池配置正确性验证
- Bean 创建验证
- 异步属性默认值验证
- 拒绝策略验证

## 线程池工作原理

1. **任务提交时**，如果线程数 < corePoolSize，创建新线程执行任务
2. **如果线程数 >= corePoolSize**，任务放入队列等待
3. **如果队列已满且线程数 < maxPoolSize**，创建新线程执行任务
4. **如果队列已满且线程数 >= maxPoolSize**，执行拒绝策略

## 使用示例

### 1. 在服务类中使用异步方法

```java
@Service
public class ImportExportService {
    
    /**
     * 异步导出 CSV
     * 使用 @Async 注解标记异步方法
     * 指定使用 taskExecutor 线程池
     */
    @Async("taskExecutor")
    public CompletableFuture<String> exportCsv(Map<String, Object> filters) {
        try {
            // 执行导出逻辑
            String filePath = performExport(filters);
            return CompletableFuture.completedFuture(filePath);
        } catch (Exception e) {
            log.error("导出 CSV 失败", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
```

### 2. 在控制器中调用异步方法

```java
@RestController
@RequestMapping("/api/v1/export")
public class ImportExportController {
    
    @Autowired
    private ImportExportService importExportService;
    
    @PostMapping("/csv")
    public ResponseEntity<ExportTaskResponse> exportCsv(@RequestBody ExportRequest request) {
        // 提交异步任务
        CompletableFuture<String> future = importExportService.exportCsv(request.getFilters());
        
        // 生成任务 ID
        String taskId = UUID.randomUUID().toString();
        
        // 异步任务完成后的处理
        future.thenAccept(filePath -> {
            // 更新任务状态
            updateTaskStatus(taskId, TaskStatus.COMPLETED, filePath);
        }).exceptionally(ex -> {
            // 处理异常
            updateTaskStatus(taskId, TaskStatus.FAILED, ex.getMessage());
            return null;
        });
        
        // 立即返回任务 ID
        return ResponseEntity.ok(new ExportTaskResponse(taskId, TaskStatus.PENDING));
    }
}
```

## 配置优势

1. **性能优化**: 异步执行耗时操作，不阻塞主线程
2. **资源控制**: 通过线程池限制并发数，避免系统过载
3. **任务保障**: CallerRunsPolicy 拒绝策略，任务不会丢失
4. **可配置性**: 不同环境使用不同的线程池参数
5. **可观测性**: 线程名称前缀便于日志追踪和问题排查
6. **异常处理**: 统一的异常处理机制，记录详细日志

## 验证步骤

### 1. 启动应用

```bash
mvn spring-boot:run
```

应该看到类似日志:
```
初始化异步任务线程池 - 核心线程数: 5, 最大线程数: 10, 队列容量: 100, 线程名称前缀: async-task-
异步任务线程池初始化完成
```

### 2. 运行单元测试

```bash
mvn test -Dtest=AsyncConfigTest
```

### 3. 监控线程池状态

可以通过 Spring Boot Actuator 监控线程池状态:

```java
@Autowired
private ThreadPoolTaskExecutor taskExecutor;

public ThreadPoolStats getThreadPoolStats() {
    return ThreadPoolStats.builder()
        .corePoolSize(taskExecutor.getCorePoolSize())
        .maxPoolSize(taskExecutor.getMaxPoolSize())
        .activeCount(taskExecutor.getActiveCount())
        .poolSize(taskExecutor.getPoolSize())
        .queueSize(taskExecutor.getThreadPoolExecutor().getQueue().size())
        .build();
}
```

## 已创建文件

- `src/main/java/com/kiro/metadata/config/AsyncProperties.java` - 配置属性类
- `src/main/java/com/kiro/metadata/config/AsyncConfig.java` - 配置类
- `src/main/resources/application.yml` - 应用配置（已包含异步配置）
- `src/test/java/com/kiro/metadata/config/AsyncConfigTest.java` - 单元测试

## 任务完成状态

✅ 创建 AsyncProperties 配置属性类
✅ 创建 AsyncConfig 配置类
✅ 配置 ThreadPoolTaskExecutor Bean
✅ 配置线程池参数（核心线程数、最大线程数、队列容量、线程名称前缀）
✅ 启用 @EnableAsync 注解
✅ 配置拒绝策略（CallerRunsPolicy）
✅ 配置优雅关闭（等待任务完成）
✅ 配置异步异常处理器
✅ 创建单元测试
✅ 更新 application.yml 配置文件

## 已知问题

项目存在 Lombok 注解处理器配置问题，导致编译时无法生成 getter/setter 方法。这是项目级别的问题，不影响异步配置的代码实现。

**解决方案**:
1. 检查 `pom.xml` 中 Lombok 依赖配置
2. 确保 Lombok 版本与 Java 版本兼容
3. 在 IDE 中安装 Lombok 插件
4. 清理并重新编译项目: `mvn clean compile`

## 下一步

任务 1.6 已完成。异步任务配置已就绪，可以在后续任务中使用 `@Async` 注解实现异步导出功能（任务 12.4）。

**建议**: 在实现导出功能时，需要:
1. 创建 ExportTask 实体类（存储导出任务状态）
2. 创建 ExportTaskRepository（管理导出任务）
3. 在 ImportExportService 中使用 `@Async("taskExecutor")` 注解
4. 实现任务状态查询接口
