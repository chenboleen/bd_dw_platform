# ==========================================
# 前端 Dockerfile - 多阶段构建
# 阶段1: Node.js 构建
# 阶段2: Nginx 运行
# ==========================================

# 阶段1: 构建阶段
FROM docker.io/library/node:18-alpine AS builder

LABEL description="数据仓库元数据管理系统 - 前端构建阶段"

WORKDIR /build

# 优先复制 package.json，利用 Docker 层缓存
COPY package.json ./
RUN npm install --prefer-offline

# 复制源代码并构建
COPY . .
RUN npm run build

# ==========================================
# 阶段2: Nginx 运行阶段
FROM docker.io/library/nginx:1.25-alpine AS runtime

LABEL maintainer="Kiro Team"
LABEL description="数据仓库元数据管理系统 - 前端运行镜像"
LABEL version="1.0.0"

# 安装 curl 用于健康检查
RUN apk add --no-cache curl

# 删除默认 Nginx 配置
RUN rm -rf /etc/nginx/conf.d/default.conf

# 复制自定义 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 从构建阶段复制静态文件
COPY --from=builder /build/dist /usr/share/nginx/html

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:80/ || exit 1

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]
