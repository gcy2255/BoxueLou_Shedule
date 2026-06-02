# 博学楼空闲教室查看系统

一个基于前后端分离的空闲教室查看系统，用于实时展示博学楼各教室的使用状态。

## 功能特性

- 🟢🟢 大屏展示所有教室状态（绿色空闲/红色占用）
- 🔍 支持按教室名称搜索
- 📊 点击教室查看完整周课表
- ⏰ 每分钟自动刷新状态

## 技术栈

- **前端**: Vue 3 + Vite
- **后端**: Spring Boot 3.2 + MyBatis-Plus
- **数据库**: MySQL 8.x

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8.0+

### 2. 配置数据库

编辑 `backend/src/main/resources/application.yml`，修改数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/classroom_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: root
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端会自动创建数据库表并导入Excel数据。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173

## 项目结构

```
博学楼课表/
├── backend/          # Spring Boot 后端
├── frontend/         # Vue 3 前端
├── *.xls            # Excel课表数据
├── 需求.md           # 需求文档
└── 开发文档.md       # 开发文档
```

## API接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/rooms` | 获取所有教室及当前状态 |
| GET | `/api/rooms/search?keyword=xxx` | 搜索教室 |
| GET | `/api/rooms/{id}` | 获取教室详情及课表 |
| GET | `/api/rooms/{id}/status` | 查询教室指定时间状态 |
| GET | `/api/time` | 获取当前时间信息 |

## 文档

- [需求文档](需求.md)
- [开发文档](开发文档.md)
