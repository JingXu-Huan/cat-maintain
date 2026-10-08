# 精简演示数据

`demo.sql` 当前版本为 `demo-v2`，适用于 MySQL 8.4 和 `db/init/init.sql` 创建的表结构。此目录不由 Docker 初始化或生产发布自动执行。

## 基础数据

| 数据 | 重置后数量 | 内容 |
| --- | ---: | --- |
| 账号 | 3 | 管理员 `admin`、车主 `car_owner`、加盟店 `car_store` |
| 门店 | 1 | 城东汽车养护店，已审核通过 |
| 商品 | 3 | 全合成机油、机油滤清器、空调滤芯 |
| 订单及明细 | 0 | 由演示操作生成 |
| 预约、保养记录、评价 | 0 | 由演示操作生成 |

三个账号均为 `ACTIVE`，可以直接登录。门店、商品和账号名称不带“测试”字样。管理员没有公开注册入口，由此维护脚本创建。

新账号的统一密码由导入者配置，保存在部署根目录的私有 `demo.env` 中，权限必须为 `600`，不提交到 Git：

```text
DEMO_PASSWORD='演示账号密码'
DEMO_PASSWORD_HASH='对应密码的 BCrypt 哈希'
```

## 导入和重置

在 Linux 服务器上执行；辅助脚本使用现有容器 `cat-maintain-mysql-1`，检查 Compose 项目和数据库名称，并与发布流程共用 `deploy.lock`。

### 补充缺失的基础数据

```sh
bash deploy/seed-demo.sh /home/ubuntu/cat-maintain
```

默认会先备份数据库，然后仅插入缺失的账号、门店和商品。不会覆盖已有密码、价格、库存、审核状态或演示操作产生的数据。重复执行不会重复插入。

### 清空业务数据并重建基础数据

```sh
bash /home/ubuntu/cat-maintain/maintenance/demo-data/deploy/seed-demo.sh \
  /home/ubuntu/cat-maintain --reset --no-backup
```

`--reset` 明确清空全部 9 张业务表，包括后来手工添加的数据；保留表结构和自增序列，并按上表重建基础数据。`--no-backup` 跳过备份，本次用户已要求使用此选项。

清理和重建在同一事务中完成，按外键依赖顺序删除，任何错误都会回滚。没有使用 `TRUNCATE`，也没有关闭外键检查。重置后旧登录会话失效，请刷新页面并用新账号登录。

辅助脚本默认从同目录的 `db/seed/demo.sql` 读取 SQL，也可在部署根目录参数后传入其他 SQL 文件路径。私有密码不进入 SQL、进程参数或日志。

直接通过 MySQL 客户端执行时，先在同一会话设置变量：

```sql
SET @demo_password_hash = '有效的 BCrypt 哈希';
SET @reset_demo_data = 1; -- 仅在需要清空时设置；默认 0 为补充模式
SOURCE db/seed/demo.sql;
```

客户端必须在出错时停止，禁止使用 `--force`。成功后脚本删除临时维护过程，并把重置变量恢复为 `0`。

## 演示顺序

1. 使用 `car_owner` 选购商品，选择城东汽车养护店并下单。
2. 使用 `admin` 审核订单并确认配送，生成核销码。
3. 使用 `car_owner` 预约到店服务。
4. 使用 `car_store` 确认预约，展示到店二维码并处理登记、核销和保养。
5. 使用 `car_owner` 查看保养记录并提交门店、商品评价。

## 服务器维护位置

- 维护副本：`/home/ubuntu/cat-maintain/maintenance/demo-data/`。
- 私有演示凭据：`/home/ubuntu/cat-maintain/demo.env`。
- 更新本目录的 SQL 和说明时，同时同步服务器维护副本。

此版本替代先前的大批场景数据。调整基础数据时，同步维护上表数量；完整历史场景由实际操作生成。

## 本次执行结果

2026-10-08 已按用户要求使用 `--reset --no-backup` 重置服务器 `3.18.233.115` 的 `cat_maintain` 数据库，没有新增备份。重置后共 3 个账号、1 家门店、3 款商品，其余业务表均为 0 条记录。

随后以补充模式再次执行，数量保持不变。已通过实际登录与角色权限接口验证三个账号均为 `ACTIVE` 并可登录；各角色订单列表为空，公开商品及门店名称、商品描述均无“测试”字样。
