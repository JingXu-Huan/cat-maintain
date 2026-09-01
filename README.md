# 汽车保养平台

本项目根据 `docs/Java企业级项目实践任务书及计划书+2023T计算机.docx` 初始化，题目为“汽车保养平台的设计与实现”。任务书要求使用 Java Web、数据库和可选的 Vue 前端；当前基础工程采用 Spring Boot + MyBatis + MySQL + Vue 3。

## 目录

- 后端：项目根目录，Java 21 + Spring Boot 4 + MyBatis。
- 前端：`front`，Vue 3 + TypeScript + Vite，开发服务器端口为 `5173`。
- 数据库：`compose.yaml`，Docker MySQL 8.4，宿主机端口为 `3307`。

## 前后端边界

前端禁止直接连接 MySQL 或其他数据库。`front` 只通过 `/api` 调用 Spring Boot 接口，Vite 开发代理将请求转发到 `http://localhost:8080`；数据库驱动、连接地址、账号密码、事务和持久化逻辑只能存在于后端。前端环境变量也不得存放数据库凭据。

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

项目只保留一份默认配置：`src/main/resources/application.yaml`。Spring Boot 会自动加载该文件，默认连接本机 Docker MySQL：

```powershell
.\mvnw.cmd spring-boot:run
```

健康接口：`http://localhost:8080/api/health`

后端测试与本地运行使用同一份默认配置和真实 MySQL（不是 H2），执行前请保证 Docker 容器健康：

```powershell
.\mvnw.cmd test
```

测试会连接本机 `3307` 的 `cat_maintain` 数据库，并执行 `SELECT 1` 和表结构检查验证连接与基础模型。

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

数据库初始化脚本位于 `db/init/001_account.sql`、`db/init/002_business.sql` 和 `db/init/003_workflow.sql`，当前基础模型包含 8 张表，并为加盟店审核和订单流程补充了状态时间字段：

| 表 | 用途 |
| --- | --- |
| `accounts` | 用户、加盟店和平台账号 |
| `merchant_stores` | 加盟店资料及审核关联 |
| `products` | 汽车零配件、销售价、保养工时费和库存 |
| `orders` | 用户订单、目标门店、审批状态和数字核销凭据 |
| `order_items` | 订单中的商品快照、数量和金额 |
| `appointments` | 用户到店预约及车辆信息 |
| `maintenance_records` | 门店实际保养过程和结果 |
| `reviews` | 用户对订单/门店的评价 |

Docker 数据卷已经存在时，修改初始化脚本不会自动重新执行；需要手动把新增脚本导入当前数据库，或在确认数据可删除后重建数据卷。

接口如下：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/register/user` | 用户注册，账号直接为 `ACTIVE` |
| POST | `/api/auth/register/store` | 加盟店注册，账号为 `PENDING`，等待平台审核 |
| POST | `/api/auth/login` | 登录并创建 HTTP Session |
| POST | `/api/auth/logout` | 注销当前 Session |
| GET | `/api/auth/me` | 获取当前登录账号 |

目前已实现账户注册、登录、Session 接口和加盟店审核接口。商品/库存、订单审批与配送、数字凭据核销、预约、保养记录和评价接口仍将在后续模块中实现；对应数据库表已先按业务关系建立。

## 加盟店审核（M1）

平台管理员登录后可调用：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/stores?status=PENDING` | 查看待审核加盟店 |
| PUT | `/api/admin/stores/{storeId}/approve` | 审核通过 |
| PUT | `/api/admin/stores/{storeId}/reject` | 审核拒绝，可提交 `reason` |

`003_workflow.sql` 会为本地测试写入管理员账号 `admin`，密码为 `Admin123!`。该账号仅用于本机开发验证，部署到真实环境前必须替换或删除。
