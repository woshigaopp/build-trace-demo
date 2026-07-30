# BuildTrace

BuildTrace 是一个面向 DeepWisdom Atoms Demo Assessment 的 AI React App Builder。用户注册账号后用自然语言创建应用；Java 后端调用真实 LLM，生成结构化多文件变更；浏览器中的 Sandpack 负责编译、编辑和运行；PostgreSQL 持久化账号、完整对话、生成任务和每个不可变代码版本。

- 在线 Demo：[https://buildtrace-atoms-demo.vercel.app](https://buildtrace-atoms-demo.vercel.app)
- 后端健康检查：[https://buildtrace-backend-production.up.railway.app/actuator/health](https://buildtrace-backend-production.up.railway.app/actuator/health)
- 技术栈：React 19、TypeScript、Vite、Sandpack、Spring Boot 3、Java 21、PostgreSQL、SSE、JWT
- 生产部署：Vercel、Railway、Neon、OpenAI-compatible LLM API

## Reviewer Quick Start

1. 打开在线 Demo，使用任意有效邮箱和至少 8 位密码注册。
2. 描述一个应用，等待生成状态到达成功；刷新页面不会丢失任务和对话。
3. 在“预览”中操作生成应用，在“代码”中浏览全部文件并手动修改保存。
4. 再发送一次修改需求，确认未提及的能力仍被保留。
5. 在“版本”中比较变更文件并恢复旧快照；恢复会创建新版本，不覆盖历史。

不需要预置测试账号，也不需要本地环境或额外配置。

## 本次补交解决的问题

| 首次反馈 | v2 处理方式 |
| --- | --- |
| 无注册登录 | BCrypt 密码哈希、JWT 登录态，所有权从服务端 principal 获取 |
| 增量开发偶尔永远不成功 | 持久化 run 状态、模型超时、一次自动修复、终态失败、服务重启恢复 |
| 部分项目对话不完整 | 用户消息在入队时保存，助手成功/失败消息和 run 绑定并完整恢复 |
| 只支持单一且不完整的 HTML | 生成并持久化 Vite React 多文件工程，完整文件树和代码编辑器 |
| Preview 只是单页展示 | Sandpack 在浏览器内真实编译并运行多文件 React 应用 |

## 核心能力

- 真实邮箱注册、登录、退出与账号级数据隔离。
- 真实 `qwen-plus` 流式调用；没有 API Key 时才使用界面明确标记的 fallback。
- `/App.jsx`、`/components/*`、`/styles.css` 等多文件 React 工程，而不是单 HTML 文本。
- 结构化 `write/delete` 文件操作，适合首轮生成和增量修改。
- 候选快照校验失败后自动修复一次；仍失败则进入可理解、可重试的终态。
- `queued/generating/validating/repairing/succeeded/failed/cancelled` 持久化状态机。
- 成功前不移动当前版本指针，失败永远不会污染上一个可运行版本。
- 完整文件树、代码编辑、手动保存、changed-file 比较和不可变版本恢复。
- 桌面/移动预览与生成应用内真实状态交互。
- 刷新、退出登录和重新登录后恢复项目、对话、run、文件和版本。

## 产品边界

生成物是浏览器内运行的 Vite React 前端应用，可以包含表单、增删改、筛选、统计、主题切换等真实交互。平台本身有 Java 后端和 PostgreSQL，但当前不为每个生成应用自动创建独立后端、数据库或公网部署。

这是明确的运行时边界，不会把“可预览的前端应用”包装成“任意全栈产品”。继续产品化时，可以在现有文件操作和版本协议上增加依赖白名单、隔离构建容器和一键发布。

## 系统架构

```text
Browser
  |
  | React workspace / Bearer JWT / JSON + SSE
  v
Vercel: BuildTrace UI ------------------------+
  |                                           |
  | VITE_API_BASE_URL                         | multi-file snapshot
  v                                           v
Railway: Spring Boot 3 / Java 21          Sandpack Vite runtime
  |                         |
  | JPA                     | OpenAI-compatible stream
  v                         v
Neon PostgreSQL          qwen-plus
```

普通 API 使用 JSON。生成使用 SSE，因为它是一次请求对应一条有序长响应，足以表达阶段、token、完成和失败，不需要 WebSocket 的双向连接生命周期。

## 一次生成如何完成

1. `POST /generate` 在事务中创建 `queued` run，并立即保存状态为 `accepted` 的用户消息。
2. 后端读取当前版本的完整文件 map，把用户需求和当前快照交给模型。
3. 模型按结构化协议返回完整文件写入或删除操作；流式片段同时通过 `token` 事件发送给前端。
4. 后端先在内存中的候选快照应用操作，不直接修改数据库当前版本。
5. 校验 schema、安全路径、文件数量/大小、必需脚手架、`package.json` 和 React 入口。
6. 首次校验失败时，把原输出和精确错误交给模型修复一次，再重新完整校验。
7. 只有候选快照合法，事务才创建不可变 Version、推进 `currentVersionId`、写入助手消息并把 run 标记为 `succeeded`。
8. 模型、超时、解析、修复或保存失败时，run 和助手失败消息被持久化，当前版本保持不变，界面提供使用原需求重试。
9. 服务重启时，遗留的非终态 run 会收敛为可重试失败，不会永久阻塞项目。

SSE 事件：

| 事件 | 含义 |
| --- | --- |
| `phase` | `generating/validating/repairing` 等可见阶段 |
| `token` | 模型返回的原始增量文本，仅用于真实进度反馈 |
| `completed` | 已校验、持久化并可恢复的最终项目和版本 |
| `generation-error` | 已持久化的终态失败、错误摘要和未受污染的项目 |

## LLM Prompt 与文件操作协议

Prompt 分为稳定的 system contract 和每轮任务上下文。它不规定视觉方案，而是锁定生成物必须满足的执行协议。

### System contract

模型只能返回一个 JSON 对象，不返回 Markdown 或解释：

```json
{
  "summary": "面向用户的简短中文说明",
  "operations": [
    {
      "type": "write",
      "path": "/App.jsx",
      "content": "该文件的完整新内容"
    },
    {
      "type": "delete",
      "path": "/components/Unused.jsx"
    }
  ]
}
```

关键约束：

- 只允许 `write` 和 `delete`；路径必须是安全的项目绝对路径。
- `write.content` 永远是完整文件内容，不是 diff、片段或省略号。
- 使用根目录 Vite React 脚手架：`/index.jsx`、`/App.jsx`、`/styles.css`；附加组件放入 `/components`，不能创建第二套 `/src` 应用。
- 增量需求只改必要文件，未被需求影响的能力和文件必须保留。
- 不添加外部依赖、远程脚本、网络请求或秘密；控件必须用 React state 真正工作。

### 首轮与增量上下文

首轮的当前文件为空，后端在应用操作前补齐标准 Vite React 脚手架。增量请求的 user prompt 是：

```text
Modify the current application while preserving behavior that the request does not change.

User request:
{本轮需求}

Current files JSON:
{当前不可变版本的完整文件 map}
```

这使模型明确看到当前事实，但返回内容只包含必要文件操作，避免每轮重写整个项目。

### 自动修复

第一次解析或候选校验失败后，repair prompt 携带原需求、当前文件、校验错误和被截断到安全长度的非法输出。模型只有一次修复机会。一次是有意的边界：它能处理常见格式错误，同时限制延迟、成本和无界重试。

生产请求参数使用 `stream=true`、`temperature=0.1`。模型、地址、Key 和超时均来自环境变量，业务代码不绑定特定厂商。

## 数据和安全语义

| 实体 | 持久化内容 |
| --- | --- |
| `User` | 规范化邮箱、BCrypt 密码哈希、创建时间 |
| `Project` | 账号归属、名称、当前版本指针、时间 |
| `GenerationRun` | prompt、模型、状态、attempt、错误、耗时、时间 |
| `Message` | 完整用户/助手对话、run 关联和成功/失败状态 |
| `Version` | 版本号、来源、摘要、完整文件 JSON 快照、时间 |

- 项目所有者完全来自已验证 JWT，客户端不能提交 `ownerId` 或伪造访客 Header。
- 密码只保存 BCrypt hash；JWT secret、模型 Key 和数据库凭证只存在于托管平台环境变量。
- 每个项目最多一个非终态生成 run，防止并发写版本指针。
- 文件路径、单文件大小、总大小和文件数都有服务端上限。
- Sandpack 运行生成代码；当前演示的生成协议禁止网络请求和秘密，但它不是面向不受信任依赖的生产级任意代码沙箱。

## 本地运行

要求 Java 21、Maven 3.9+、Node.js 20+。

```bash
# terminal 1
cd backend
mvn spring-boot:run

# terminal 2
cd frontend
npm ci
npm run dev
```

访问 `http://localhost:5173`。默认使用文件型 H2；未配置模型 Key 时使用明确标记的交互式 fallback。

真实 OpenAI-compatible 模型：

```bash
APP_AI_PROVIDER=api \
APP_AI_BASE_URL=https://YOUR-PROVIDER/compatible-mode/v1 \
APP_AI_API_KEY=YOUR_KEY \
APP_AI_MODEL=qwen-plus \
APP_AI_TIMEOUT=180s \
mvn spring-boot:run
```

本地也支持 `APP_AI_PROVIDER=codex-cli`，仅用于开发机验证，不部署到公网。完整变量见 [`.env.example`](.env.example)。

## 测试与验收

```bash
cd backend && mvn test
cd frontend && npm run lint
cd frontend && npm run build
cd frontend && npm audit --audit-level=high
```

自动化覆盖：注册/登录和跨账号拒绝、SSE 异步完成、连续五次增量生成、非法输出后修复、修复失败的版本原子性、中断 run 恢复、完整消息与 run 持久化、文件路径/脚手架校验、手动版本和不可变恢复。

真实浏览器验收覆盖：注册、退出、重新登录；七文件树；代码编辑与精确 dirty 状态；手动保存；changed-file 比较；恢复创建新版本；Sandpack 预览内点击改变状态；刷新后恢复全部数据；390×844 移动布局。

## 生产部署

| 组件 | 平台 | 关键配置 |
| --- | --- | --- |
| 前端 | Vercel | `VITE_API_BASE_URL` 指向 Railway |
| Java 后端 | Railway | Dockerfile、JWT、模型、数据库、精确 CORS |
| PostgreSQL | Neon | JDBC SSL 连接和持久化 |
| LLM | OpenAI-compatible API | Base URL、API Key、model、timeout |

不需要 AWS 或自管理虚拟机。Railway 使用 [backend/Dockerfile](backend/Dockerfile) 和 [backend/railway.toml](backend/railway.toml)，Vercel 使用 [frontend/vercel.json](frontend/vercel.json)。

## 项目结构

```text
.
├── backend/src/main/java/dev/buildtrace
│   ├── auth          # 注册登录、JWT、Spring Security
│   ├── generation    # Prompt、流式模型、操作解析、候选校验/修复
│   └── project       # 项目、run、消息、版本和恢复事务
├── frontend/src
│   ├── api.ts        # Bearer JSON API 和 SSE 完成语义
│   ├── App.tsx       # 账号、项目和 durable run 恢复
│   └── components    # Auth、对话、Sandpack、文件树、版本
├── specs/changes/atoms-demo-v2
│   └── ...           # 决策、前端契约和验证矩阵
└── docs/ai-development-report.md
```

实现取舍、首次反馈回流、AI 协作边界和实际验收记录见 [docs/ai-development-report.md](docs/ai-development-report.md)。
