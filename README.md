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

## 当前已实现：账户、商品、订单、预约与保养模块

数据库初始化脚本统一位于 `db/init/init.sql`，按账号/门店、业务表、工作流字段、管理员种子、完整性约束的顺序执行；当前模型包含 8 张业务表，并为审核、订单流程和保养记录补充了状态/时间/唯一性约束：

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

目前已实现账户注册/登录、加盟店审核、商品与库存、订单审批/配送/核销、预约、保养记录和评价。后端所有接口都通过 Service 层校验角色和数据归属，前端只通过 `/api` 访问后端。

## 加盟店审核（M1）

平台管理员登录后可调用：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/stores?status=PENDING` | 查看待审核加盟店 |
| PUT | `/api/admin/stores/{storeId}/approve` | 审核通过 |
| PUT | `/api/admin/stores/{storeId}/reject` | 审核拒绝，可提交 `reason` |

`init.sql` 会为本地测试写入管理员账号 `admin`，密码为 `Admin123!`。该账号仅用于本机开发验证，部署到真实环境前必须替换或删除。

## 已实现业务接口

| 模块 | 主要接口 |
| --- | --- |
| 商品与门店 | `GET /api/products`、`GET /api/stores`；管理员使用 `/api/admin/products` 新增、修改、上下架和调整库存 |
| 用户订单 | `POST/GET /api/orders`、`GET /api/orders/{id}` |
| 平台订单 | `/api/admin/orders` 审批、拒绝和配送，配送时生成 8 位数字核销码 |
| 门店订单 | `/api/store/orders` 查询门店订单，`GET /lookup` 查询核销码，`PUT /{id}/verify` 核销 |
| 用户预约 | `POST/GET /api/appointments`、`PUT /api/appointments/{id}/cancel` |
| 门店预约 | `/api/store/appointments` 查询、确认和拒绝 |
| 保养记录 | 用户 `GET /api/maintenance-records`；门店在 `/api/store/maintenance-records` 开始/完成服务 |
| 评价 | 用户 `POST/GET /api/reviews`；公开门店评价 `GET /api/stores/{storeId}/reviews` |

订单创建与库存扣减在同一事务中完成，扣库存使用带非负条件的原子更新；订单列表采用一次批量明细查询，不在订单循环中逐条查询明细。维修记录通过 `init.sql` 的唯一索引保证一个预约最多一条记录。
