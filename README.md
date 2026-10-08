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

界面按角色组织为工作台：游客浏览配件和门店；车主管理购物车、订单与保养；门店处理核销、预约和登记二维码；管理员审核门店、管理商品库存与配送。桌面使用侧边导航，手机使用可展开的导航菜单。界面组件、设计资源与验证说明见 [前端界面说明](docs/前端界面说明.md)。

生产构建：

```powershell
npm run build
```

## 当前已实现：账户、商品、购物车、订单、扫码登记、保养与评价

数据库初始化脚本统一位于 `db/init/init.sql`，当前模型包含 9 张业务表，并为审核、订单流程、到店登记、保养记录和商品评价补充了状态/时间/唯一性约束：

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
| `product_reviews` | 用户对已完成订单中购买商品的评价 |

Docker 数据卷已经存在时，修改初始化脚本不会自动重新执行；需要手动把新增脚本导入当前数据库，或在确认数据可删除后重建数据卷。

接口如下：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/register/user` | 用户注册，账号直接为 `ACTIVE` |
| POST | `/api/auth/register/store` | 加盟店注册，账号为 `PENDING`，等待平台审核 |
| POST | `/api/auth/login` | 登录并创建 HTTP Session |
| POST | `/api/auth/logout` | 注销当前 Session |
| GET | `/api/auth/me` | 获取当前登录账号 |

目前已实现账户注册/登录、加盟店审核、商品编辑与库存、按账号保存的购物车、订单审批/配送/核销、预约、用户扫码登记、保养记录、商品与门店评价。后端写操作通过 Service 层校验角色和数据归属；可售商品、加盟店和公开评价支持游客浏览，前端只通过 `/api` 访问后端。

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
| 商品与门店 | `GET /api/products`、`GET /api/stores`；管理员使用 `/api/admin/products` 新增、修改、删除、上下架和调整库存 |
| 用户订单 | `POST/GET /api/orders`、`GET /api/orders/{id}` |
| 平台订单 | `/api/admin/orders` 审批、拒绝和配送，配送时生成 8 位数字核销码 |
| 门店订单 | `/api/store/orders` 查询门店订单，`GET /lookup` 查询核销码，`PUT /{id}/verify` 核销 |
| 用户预约 | `POST/GET /api/appointments`、`PUT /api/appointments/{id}/cancel` |
| 门店预约 | `/api/store/appointments` 查询、确认和拒绝 |
| 保养记录 | 用户 `GET /api/maintenance-records`；门店在 `/api/store/maintenance-records` 开始/完成服务 |
| 评价 | 用户 `POST/GET /api/reviews`；公开门店评价 `GET /api/stores/{storeId}/reviews` |
| 商品评价 | 用户 `POST/GET /api/product-reviews`；公开商品评价 `GET /api/products/{productId}/reviews` |
| 扫码登记 | 用户 `POST /api/stores/{storeId}/check-ins`；门店资料 `GET /api/store/profile` |
| 凭据查预约 | 门店 `GET /api/store/orders/lookup-appointments?code=12345678` |

新订单在创建时检查库存、保存配件和工时费快照，在平台配送时扣减库存并生成核销码。配送的状态更新、全部商品扣库存和凭据生成在同一事务中完成，任一商品库存不足时全部回滚。重复配送不能再次扣库存。订单列表采用一次批量明细查询，不在订单循环中逐条查询明细。保养记录通过 `init.sql` 的唯一索引保证一个预约最多一条记录。

## 本次补齐的功能

任务书逐项对照、原有缺口、实现入口及验收步骤见 [功能实现对照](docs/功能实现对照.md)。

- 管理员可编辑已有商品的 SKU、名称、品牌、价格、工时费、库存与描述。
- 用户从配件列表加购，在购物车调整数量、移除商品、查看分项及合计金额；刷新后恢复当前账号的购物车。
- 用户预约时选择自己的订单，门店自动随订单匹配；评价时选择已完成订单与购买商品，无需手填数据库 ID。
- 门店展示登记二维码，用户扫码登录、选择已确认预约、输入 8 位核销码完成登记。门店可按核销码同时查看订单和预约。
- 用户与游客可在配件、门店列表中浏览公开评价。
- 商品、用户/门店/平台订单、预约和保养历史提供分页入口；门店完成保养时可录入里程和备注。

支付为任务书可选功能，当前未接入支付渠道。物流、短信和微信消息按任务书约定省略，不需要外部账号。

## 已有数据库升级

首次创建 Docker 数据卷时会自动执行初始化脚本。已有数据卷请先停止后端，再从项目根目录导入同一个脚本。下面使用默认开发账号；自定义 `.env` 时请同步替换数据库连接参数：

```powershell
Get-Content -Raw -Encoding utf8 .\db\init\init.sql | docker compose exec -T mysql mysql --default-character-set=utf8mb4 -ucatmaintain -pcatmaintain cat_maintain
```

升级会新增 `appointments.checked_in_at`、`orders.stock_deducted` 和 `product_reviews`，并保留原有数据。旧版本的订单已经在下单时扣库存，迁移会标记这些订单，配送时不再扣第二次；旧版本已拒绝订单的库存会返还一次。脚本可重复导入。数据库升级完成后再启动新后端。

## 手机扫码演示

1. 电脑与手机连接同一网络，按上文启动数据库、后端和前端。
2. 门店账号登录后，在“到店登记二维码”填写手机可访问的网站地址，例如 `http://192.168.1.100:5173`，重新生成二维码。
3. 手机使用相机、微信或浏览器扫码打开链接，登录购买账号，选择门店已确认的预约，输入订单中的核销码。
4. 门店刷新工作台后查看登记时间并开始、完成保养。用户可查看完整保养记录并评价。

Vite 开发服务器监听局域网地址；手机访问时需允许 Windows 防火墙的 TCP 5173 入站，后端请求仍由 Vite `/api` 代理转发。二维码中的 `localhost` 只能由当前电脑使用。二维码包含门店登记地址，不包含用户凭据。
