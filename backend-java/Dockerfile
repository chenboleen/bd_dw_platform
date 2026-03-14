# ==========================================
# 后端 Dockerfile - 多阶段构建
# 阶段1: Maven 构建
# 阶段2: JRE 运行
# ==========================================

# 阶段1: 构建阶段
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

LABEL maintainer="Kiro Team"
LABEL description="数据仓库元数据管理系统 - 后端构建阶段"

WORKDIR /build

# 优先复制 pom.xml，利用 Docker 层缓存加速依赖下载
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并构建
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==========================================
# 阶段2: 运行阶段
FROM eclipse-temurin:17-jre-alpine AS runtime

LABEL maintainer="Kiro Team"
LABEL description="数据仓库元数据管理系统 - 后端运行镜像"
LABEL version="1.0.0"

# 安装必要工具（curl 用于健康检查）
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

# 创建非 root 用户运行应用（安全最佳实践）
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# 创建导出文件目录
RUN mkdir -p /app/exports /app/logs && \
    chown -R appuser:appgroup /app

# 从构建阶段复制 JAR 文件
COPY --from=builder /build/target/*.jar app.jar
RUN chown appuser:appgroup app.jar

# 切换到非 root 用户
USER appuser

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM 参数优化
ENV JAVA_OPTS="-Xms256m -Xmx512m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/heap-dump.hprof \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=Asia/Shanghai"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
