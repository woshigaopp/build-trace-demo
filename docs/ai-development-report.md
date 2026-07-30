# AI-Assisted Development Report

## 1. 目标与范围

题目的核心不是复刻完整 Atoms，而是在时间盒内交付一个用户可以注册、真实生成、继续修改、浏览完整代码、运行预览并恢复历史的产品。首次提交已证明模型、Java 后端、数据库和公开部署链路可用，但评审明确指出四个产品级缺口：没有注册登录、增量生成可能永久失败、对话不完整、代码管理只有单一 HTML。

v2 因此没有继续增加装饰功能，而是把 P0 重定义为系统属性：真实身份、多文件工程、持久化生成状态、失败可恢复和原子版本提交。生成范围锁定为 Vite React 前端应用；不声称自动生成并部署任意后端服务。

## 2. 人与 AI 的职责

人给出题目、首次评审反馈和“最后一次补交”的优先级，并授权在范围内自主决策。AI 负责读取现有实现、把反馈还原成需求和契约、实现前后端、编写确定性测试、进行真实浏览器验收、部署和整理可复核文档。

这里使用 AI Coding 方法论的目的不是限制模型如何写代码，而是把关键判断和实际执行结果显式留下：为什么身份必须服务端派生，为什么失败不能移动当前版本，为什么修复只做一次，为什么生成物只承诺 React 前端。实现并未机械运行重型 workflow，但保留了范围决策、前端契约、验证矩阵和验收证据。

## 3. 关键工程决策

### 身份与所有权

使用 Spring Security、BCrypt 和 JWT 实现邮箱注册登录。每个项目查询都使用认证 principal 的 user ID，客户端不再发送 `X-Guest-Id`，也不能在 payload 中选择 owner。旧数据库列为了兼容 v1 schema 被保留，但匿名旧数据不会暴露给新账号。

### 多文件生成与运行

LLM 不再返回完整 HTML，而是返回结构化 `write/delete` 操作。后端在内存候选快照应用操作并校验安全路径、大小、脚手架、`package.json` 和 React 入口。浏览器使用 Sandpack 的 Vite React runtime 编译和运行 `/App.jsx`、`/components/*`、`/styles.css` 等完整工程文件。

### 增量稳定性

每次请求先持久化 run 和用户消息，状态依次经过 `queued/generating/validating/repairing`。首次非法输出会把精确错误交给模型自动修复一次。候选合法后才在同一事务中创建 Version 并推进 `currentVersionId`；两次都失败时持久化错误和助手失败消息，旧版本完全不变。进程重启时遗留非终态 run 会收敛为可重试失败，避免项目永久卡住。

### 代码和版本管理

Sandpack 提供编译、预览和 CodeMirror 编辑能力，BuildTrace 自己管理文件树、保存、版本比较和恢复。手动保存只提交实际发生变化的文件内容，忽略 `package.json` 的纯格式化差异。恢复旧版本会复制文件快照创建新版本，历史只增不改。

## 4. 真实纠偏

1. 首次真实千问调用在服务端已经成功，但 HTTP 流没有结束。原因不是模型，而是 Spring Security 拒绝了 Servlet 的 `ASYNC` 二次分发。安全配置只放行 `ASYNC/ERROR` dispatcher，外部 `/api/**` 请求仍要求 JWT；随后增加 MockMvc SSE 回归测试。
2. 最初把文件放在 `/src` 并给 `package.json` 使用部署式 `vite --host 0.0.0.0`。Sandpack 自带模板文件与它重复，且命令在浏览器 runtime 中解析失败。最终对齐 Sandpack 官方 Vite React 根目录脚手架和 `dev: vite`，文件树只显示真实持久化的七个文件。
3. 编辑器最初通过比较 Sandpack 文件 map 判断 dirty，运行时对 `package.json` 的格式化导致页面刚加载“保存版本”就可点。修复为结合 Sandpack 官方 `editorState` 和语义文件比较，只在真实代码变化时启用保存。
4. 手动只改 `App.jsx` 后，版本差异曾同时显示 `package.json`。原因仍是 JSON 格式化差异。保存现在只覆盖语义变化的路径，版本比较对 `package.json` 做 JSON 语义比较，实际 changed-file 只剩 `/App.jsx`。
5. 前端流提前断开不能等同于任务失败或成功。SSE 客户端只有收到 `completed` 才宣布成功；断线后重新读取 durable run。服务端即使无法继续向浏览器发送 token，也会继续执行并持久化终态。
6. 公开部署后，项目详情稳定需要 16 秒。生产证据表明普通 PostgreSQL `TEXT` 字段被 `@Lob` 按 Large Object/OID 协议读取，每个内容字段都产生额外远端往返；并行读取第一次部署还触发了 `Large Objects may not be used in auto-commit mode`。最终移除错误的 `@Lob` 映射，用一次性、带标记的事务迁移把历史 OID 解引用为 UTF-8 TEXT，再并行加载独立集合。历史数据完整保留，详情降到约 1.2-1.3 秒。

这些问题横跨模型协议、Servlet dispatch、浏览器编译器、编辑器状态和数据库事务，无法仅靠“代码能编译”发现。

## 5. 验证证据

自动化测试实际覆盖：

- 注册、登录、错误密码、未认证拒绝和两账号项目隔离。
- SSE 请求经过 Spring Security async dispatch 后正常 200 并包含 `completed`。
- 连续五次增量操作全部进入 `succeeded`，生成五个不可变版本和完整十条对话。
- 首次非法输出、修复成功时 attempt 为 2；修复仍失败时 run 终态失败且当前版本不变。
- 服务重启恢复遗留 run，不改变当前文件并写入可重试失败消息。
- 文件路径穿越、删除必需脚手架、空操作和非法 JSON 被拒绝。
- 手动保存、版本递增、比较数据和恢复不覆盖历史。
- 前端 ESLint、TypeScript 和 Vite production build。

真实模型只调用一次以保护用户额度：Java 后端通过 OpenAI-compatible API 调用 `qwen-plus`，35 秒内正常关闭 SSE，返回五个文件；用户消息为 `accepted`，助手消息和 run 为 `succeeded`，attempt 为 1，并创建唯一不可变版本。

真实浏览器验收实际完成：

- 注册、退出、登录，项目和账号正确恢复。
- 代码树显示 `App.jsx`、`components/MetricCard.jsx`、`index.html`、`index.jsx`、`package.json`、`styles.css`、`vite.config.js`。
- 初始“已保存”按钮禁用；修改 `App.jsx` 后启用；保存后生成新版本并恢复禁用状态。
- Sandpack 预览中点击“完成一个任务”，数字从 3 变为 4。
- 比较 v3 和 v4 时只显示真实变化的 `/App.jsx`。
- 恢复 v1 创建 v3，旧版本仍然存在；退出再登录后完整对话、v1-v4 和当前预览都恢复。
- 390×844 移动端主流程无控件重叠，预览和移动/桌面切换可用。
- 公开 Railway 健康检查为 `UP`，Vercel 精确 CORS 生效；另一账号读取项目返回 404；最终详情响应 1.32 秒并返回七文件、两版本和两条完整消息。

## 6. 后续演进

继续产品化时，优先增加受控依赖白名单和 lockfile、隔离构建域、编译结果服务端验签、限流与配额、Flyway 迁移、token/成本观测和生成应用独立发布。当前实现刻意不宣称这些尚未交付的能力。
