# GitHub Actions 与 go-server 部署

## 流水线

工作流位于 `.github/workflows/ci-deploy.yaml`。

| 触发方式 | 行为 |
| --- | --- |
| 推送到任意分支 | 运行后端集成测试、前端类型检查和生产构建 |
| 向 `master` 提交 PR | 运行相同的检查，不使用服务器密钥 |
| 推送到 `master` | 检查通过后自动部署 |
| 手动运行，勾选 `deploy` | 检查通过后部署所选版本；生产环境应限制允许部署的分支 |

后端使用 Java 21 和真实 MySQL 8.4，测试数据库由仓库已有的 `compose.yaml` 创建，运行结束后删除。前端使用 Node.js 24 和 `npm ci`，`npm run build` 同时执行 `vue-tsc` 与 Vite 构建。前后端分别保存构建产物，部署必须等待两项检查都通过。

## 运行结构

```text
浏览器 → 服务器 TCP 80 → Nginx
                         ├─ /         Vue 静态资源
                         └─ /api/     Spring Boot :8080 → MySQL :3306
```

仅 Nginx 发布宿主机端口。MySQL 和后端没有宿主机端口映射，前端不持有数据库凭据。生产环境使用独立的 `cat-maintain` Compose 项目与 `cat-maintain_mysql-data` 数据卷。

服务器目录默认使用 `/home/ubuntu/cat-maintain`：

```text
cat-maintain/
├─ .env                   数据库密码、管理员 BCrypt 哈希和 HTTP 端口
├─ admin-password         首次配置时生成的管理员密码，权限 600
├─ ssh-entrypoint.sh      专用部署密钥的受限入口
├─ releases/              按提交 SHA、运行 ID 和重试次数保存发布版本
├─ backups/               每次数据库升级前的 SQL 压缩备份
├─ current                当前成功发布的版本
└─ previous               上一个成功发布的版本
```

仓库的默认管理员为 `admin / Admin123!`。上线脚本在启动后端前把默认哈希替换为服务器 `.env` 中的 `ADMIN_PASSWORD_HASH`，不会覆盖后续修改过的密码。数据库密码和明文管理员密码只保存在服务器，不能进入仓库、构建产物或前端环境变量。

## 首次配置

1. 通过本机 SSH 配置的 `go-server` 登录服务器，确认 Docker、Docker Compose、`curl`、`flock` 和 `gzip` 可用。服务器必须能够访问 Docker Hub。
2. 创建 `/home/ubuntu/cat-maintain`，复制 `production.env.example` 为该目录的 `.env`，填写两组不同的随机数据库密码和真实 BCrypt 管理员密码哈希。`.env` 权限设为 `600`，目录权限设为 `700`。
3. 将 `ssh-entrypoint.sh` 放到部署根目录并赋予执行权限。为 CI 创建独立的 Ed25519 密钥，不使用个人 SSH 私钥。
4. 将 CI 公钥加入服务器 `~/.ssh/authorized_keys`，使用如下选项限制这把密钥：

   ```text
   restrict,command="/home/ubuntu/cat-maintain/ssh-entrypoint.sh" ssh-ed25519 <CI公钥> cat-maintain-actions
   ```

5. 在 GitHub 仓库创建 `production` Environment，并设置以下配置。建议只允许 `master` 部署到该环境。

   | 类型 | 名称 | 内容 |
   | --- | --- | --- |
   | Variable | `DEPLOY_HOST` | 服务器公网 IP 或 DNS 名，不能填写本机 SSH 别名 |
   | Variable | `DEPLOY_USER` | `ubuntu` |
   | Variable | `DEPLOY_PORT` | `22` |
   | Secret | `DEPLOY_SSH_KEY` | 专用 CI 私钥的完整内容 |
   | Secret | `DEPLOY_KNOWN_HOSTS` | 经现有 SSH 连接核实的服务器主机公钥 |

6. 允许服务器安全组入站 TCP 80。使用域名和 HTTPS 时，在入口增加 TLS 配置，并将后端 Session Cookie 的 `secure` 属性设为 `true`。

Windows 客户端使用 PowerShell；下方服务器命令在确认的 Ubuntu 环境中执行。

## 发布过程和故障处理

CI 把同一次运行产生的后端 JAR、前端静态资源、部署配置和数据库脚本打包，通过专用 SSH 密钥传输。部署开始前核对当前 `master` 的提交，防止较慢的旧流水线覆盖新版本。发布目录使用完整提交 SHA，加上运行 ID 和重试次数；服务器核对文件 SHA-256 后再激活版本。

服务器先构建运行镜像，等 MySQL 就绪并保存数据库备份，再暂停应用、执行仓库幂等 SQL、替换默认管理员密码并启动新版本。上线必须通过容器健康检查、`/api/health`、访问数据库的 `/api/products` 和首页检查。成功后才更新 `current` 和 `previous`。同一时间只能运行一个生产部署。

应用更新期间存在短暂维护窗口，数据库数据卷保持不变。数据库升级失败或新应用启动失败时尝试恢复上一个应用版本；SQL 已执行的结构变更不会自动撤销。破坏性的数据库变更需要单独制定迁移方案。备份和旧版本保留在服务器，维护时按实际磁盘空间清理，禁止使用 `docker compose down --volumes` 清理生产项目。

查看状态与日志：

```sh
cd /home/ubuntu/cat-maintain
export APP_VERSION=$(basename "$(readlink -f current)")
docker compose -p cat-maintain --env-file .env -f current/deploy/compose.production.yaml ps
docker compose -p cat-maintain --env-file .env -f current/deploy/compose.production.yaml logs --tail 100 backend web
curl --fail http://127.0.0.1/api/health
```

查看首次生成的管理员密码：

```powershell
ssh go-server 'cat /home/ubuntu/cat-maintain/admin-password'
```

手动恢复上一版应用：

```sh
cd /home/ubuntu/cat-maintain
previous_release=$(readlink -f previous)
export APP_VERSION=$(basename "$previous_release")
docker compose -p cat-maintain --env-file .env -f "$previous_release/deploy/compose.production.yaml" up -d --no-build --wait --wait-timeout 180 backend web
ln -sfn "$previous_release" current
```

SSH 连接报 `kex_exchange_identification` 时，失败发生在身份认证之前，应先检查连接路径；不能据此判断私钥错误。若本机 `ProxyCommand` 故障，可以临时使用 `ssh -o ProxyCommand=none go-server`，不必修改本机 SSH 配置。
