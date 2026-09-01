# 汽车保养平台

本项目根据 `docs/Java企业级项目实践任务书及计划书+2023T计算机.docx` 初始化，题目为“汽车保养平台的设计与实现”。任务书要求使用 Java Web、数据库和可选的 Vue 前端；当前基础工程采用 Spring Boot + MyBatis + MySQL + Vue 3。

## 目录

- 后端：项目根目录，Java 21 + Spring Boot 4 + MyBatis。
- 前端：`front`，Vue 3 + TypeScript + Vite，开发服务器端口为 `5173`。
- 数据库：`compose.yaml`，Docker MySQL 8.4，宿主机端口为 `3307`。

## 启动 MySQL

在项目根目录执行：

```powershell
docker compose up -d
docker compose ps
```

默认创建：

- 容器：`cat-maintain-mysql`
- 数据库：`cat_maintain`
- 用户：`catmaintain`
- 密码：`catmaintain`
- 宿主机连接：`localhost:3307`

需要自定义值时，复制 `.env.example` 为 `.env` 后再执行 Compose。`.env` 不应提交到 Git。

## 启动后端

默认 `dev` 配置只启动 Web 层和健康接口，不依赖数据库；连接 Docker MySQL 时使用 `local` profile：

```powershell
$env:SPRING_PROFILES_ACTIVE = 'local'
.\mvnw.cmd spring-boot:run
```

健康接口：`http://localhost:8080/api/health`

后端测试使用真实 MySQL（不是 H2），执行前请保证 Docker 容器健康：

```powershell
.\mvnw.cmd test
```

测试 profile 位于 `src/test/resources/application-test.yaml`，默认连接本机 `3307` 的 `cat_maintain` 数据库，并执行 `SELECT 1` 验证连接。

## 启动前端

```powershell
Set-Location .\front
npm install
npm run dev
```

前端通过 Vite 将 `/api` 请求代理到 `http://localhost:8080`。打开 `http://localhost:5173` 可以检查后端连接，并直接体验账户注册、登录和注销。

生产构建：

```powershell
npm run build
```

## 当前已实现：账户模块

数据库初始化脚本位于 `db/init/001_account.sql`，创建 `accounts` 和 `merchant_stores` 两张表。Docker 数据卷已经存在时，修改初始化脚本不会自动重新执行；需要手动把脚本导入当前数据库，或在确认数据可删除后重建数据卷。

接口如下：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/register/user` | 用户注册，账号直接为 `ACTIVE` |
| POST | `/api/auth/register/store` | 加盟店注册，账号为 `PENDING`，等待平台审核 |
| POST | `/api/auth/login` | 登录并创建 HTTP Session |
| POST | `/api/auth/logout` | 注销当前 Session |
| GET | `/api/auth/me` | 获取当前登录账号 |

加盟店审核模块尚未实现，因此加盟店注册成功后暂时不能登录；这是当前业务约束，不是前端占位状态。商品、库存、订单、配送凭据、预约、核销和保养记录将在后续模块中继续实现。
