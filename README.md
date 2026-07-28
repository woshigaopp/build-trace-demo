# BuildTrace

BuildTrace 是一个面向 DeepWisdom Atoms Demo Assessment 的 AI App Builder。用户用自然语言描述应用，Java 后端调用真实 LLM 生成可运行的单文件 Web 应用，React 工作台负责流式展示进度、沙箱预览、多轮修改、版本管理和刷新恢复。

- 在线 Demo：[https://buildtrace-atoms-demo.vercel.app](https://buildtrace-atoms-demo.vercel.app)
- 后端健康检查：[https://buildtrace-backend-production.up.railway.app/actuator/health](https://buildtrace-backend-production.up.railway.app/actuator/health)
- 技术栈：React 19、TypeScript、Vite、Spring Boot 3、Java 21、PostgreSQL、SSE
- 生产部署：Vercel、Railway、Neon、OpenAI-compatible LLM API

## 核心能力

- **真实 LLM 生成**：生产环境通过 OpenAI-compatible API 调用 `qwen-plus`，不是预制页面或固定动画。
- **可运行应用**：生成结果包含完整 HTML、内联 CSS 和 JavaScript，在受限 iframe 中直接运行。
- **真实交互**：生成应用可以包含表单、增删改、筛选、统计、主题切换等浏览器内交互。
- **多轮修改**：继续描述变更时，后端把当前完整 HTML 与新需求一起交给模型生成下一版本。
- **版本历史**：每次成功生成都会创建不可变版本；恢复旧版本会创建新版本，不覆盖历史。
- **持久化与隔离**：项目、对话、当前 HTML 和版本存入 PostgreSQL，并按本地访客 ID 隔离。
- **多种查看方式**：支持桌面/移动端预览、源码查看和新窗口运行。
- **明确 fallback**：没有配置模型 Key 时仍可体验产品流程，但界面会明确标记为本地 fallback。

## 产品边界

本项目要证明的是一条完整可信的 AI App Builder 主链路：

```text
需求描述 -> 模型生成 -> 完整性校验 -> 运行预览
        -> 多轮修改 -> 版本持久化 -> 回滚与刷新恢复
```

生成物目前是一个自包含的 HTML 文档，不是包含独立后端、数据库和依赖安装的多文件工程。平台持久化的是项目、对话、HTML 和版本；生成应用自身的业务数据保存在 iframe 的 JavaScript 内存中，刷新生成应用后会恢复其初始数据。

这是 6-8 小时时间盒内的主动范围选择：优先把生成、执行、迭代、持久化和恢复做成可验证闭环，而不是引入 WebContainer、任意 npm 安装和生成应用独立部署所需的大量运行时基础设施。

## 系统架构

```text
Browser
  |
  | React workspace / JSON / SSE
  v
Vercel ------------------------------------------+
  |                                               |
  | VITE_API_BASE_URL                             | sandboxed iframe
  v                                               v
Railway: Spring Boot 3 / Java 21             Generated HTML app
  |                    |
  | JPA                | OpenAI-compatible streaming API
  v                    v
Neon PostgreSQL      qwen-plus
```

前后端之间普通操作使用 JSON；生成过程是单向长响应，因此使用 SSE 而不是 WebSocket。SSE 足以表达阶段变化、增量 token、成功结果和失败事件，同时减少连接生命周期复杂度。

## 一次生成如何完成

1. 前端首次访问时生成随机 `X-Guest-Id` 并保存在浏览器本地，用它读取该访客的项目列表。
2. 用户提交需求后，前端先创建项目，再调用 `POST /api/projects/{id}/generate`。
3. `GenerationService` 先持久化用户消息，并读取项目的当前 HTML 作为多轮上下文。
4. `AiGenerationClient` 组合 system prompt 和 user prompt，调用 OpenAI-compatible `/chat/completions`，开启 `stream=true`。
5. 后端解析模型的流式 chunk，并向浏览器发送 `phase` 和 `token` SSE 事件。
6. 模型流正常结束后，`HtmlExtractor` 提取 HTML，检查文档起止标签、`body` 和最小完整度。
7. 只有校验成功，后端才在事务中创建版本、更新项目当前 HTML、写入助手消息。
8. 后端发送唯一的 `completed` 事件；前端收到它后才替换预览。流提前结束或收到 `generation-error` 都不会把半份 HTML 当作成功结果。

对应的 SSE 事件语义：

| 事件 | 含义 |
| --- | --- |
| `phase` | 读取上下文、调用模型、校验 HTML 等阶段状态 |
| `token` | 模型返回的增量文本，用于展示真实生成进度 |
| `completed` | 已校验并持久化的最终项目、版本、模型和耗时 |
| `generation-error` | 模型、解析、超时或保存过程中发生的失败 |

## LLM Prompt 构造

Prompt 设计分成稳定的运行时契约和每次变化的任务上下文，目标不是限制模型如何写界面，而是让生成物满足当前执行环境的硬约束。

### 1. System prompt：锁定输出与运行时契约

System prompt 要求模型：

- 只返回一份完整、自包含的 HTML 文档，不返回 Markdown 围栏和解释文字。
- CSS 和 JavaScript 必须内联，不依赖外部包、远程脚本或远程图片。
- 页面需要响应式、可访问并且真正可交互，每个请求的控件都必须工作。
- 优先提供确定性的示例数据，保证首次打开即可演示。
- 生成物运行在没有 same-origin 权限的 iframe 中，因此不能使用 `localStorage`、`sessionStorage`、IndexedDB、Cookie 或 Service Worker。
- 业务状态保存在当前页面内存中，表单必须阻止默认页面跳转。

这些约束来自真实浏览器验收。早期模型生成物使用了 `localStorage`，在不含 `allow-same-origin` 的 iframe 中触发 `SecurityError`。最终选择修正 Prompt 契约，而不是降低 iframe 隔离级别。

### 2. 首轮 user prompt：只携带产品需求

```text
Create this application:
{用户输入的需求}
```

首轮不附加额外代码上下文，减少 token 消耗，并让 system prompt 专注约束输出格式和运行环境。

### 3. 多轮 user prompt：需求加当前完整 HTML

```text
Update the current application according to the request.
Return the entire updated HTML.

Request:
{本轮修改需求}

Current HTML:
{当前版本的完整 HTML}
```

模型拿到当前可运行版本和本轮差异需求，返回新的完整 HTML。这样不需要在客户端合并不可靠的局部代码片段，也能保证每一个成功版本都可以独立运行和回滚。

当前方案的代价是 HTML 越大，多轮请求的 token 成本越高。产品化后应增加上下文长度限制、摘要、结构化 patch、自动修复和生成成本治理；在本次 Demo 范围内，完整文档重写更简单，也更容易验证。

### 4. 模型请求参数

Java 后端使用 Spring `WebClient` 发送兼容 OpenAI Chat Completions 的请求：

```json
{
  "model": "${APP_AI_MODEL}",
  "stream": true,
  "temperature": 0.2,
  "messages": [
    { "role": "system", "content": "<runtime contract>" },
    { "role": "user", "content": "<first build or update prompt>" }
  ]
}
```

`temperature=0.2` 用于降低生成结果的随机性。模型、API 地址、Key 和超时全部通过环境变量注入，业务代码不绑定具体厂商。生产环境使用千问，任何兼容相同流式协议的模型服务都可以替换。

此外提供本地专用的 `codex-cli` provider，用于在没有外部 API Key 时验证真实模型效果。它为每次生成创建空临时目录，以只读沙箱运行非交互 Codex 任务，读取最终输出后立即清理；该 provider 不用于公网部署。

## 数据模型与版本语义

平台包含三个核心实体：

| 实体 | 保存内容 |
| --- | --- |
| `Project` | 访客归属、名称、当前 HTML、创建和更新时间 |
| `Message` | 用户需求与平台生成结果消息，按时间排序 |
| `Version` | 版本号、生成 Prompt、完整 HTML 和创建时间 |

成功生成 v1、v2 时会分别保存两份完整 HTML。恢复 v1 不会删除 v2，也不会直接把当前指针倒退，而是复制 v1 的 HTML 创建新的 v3。这样历史保持只增不改，每次当前状态变化都有可追踪记录。

## 安全与失败边界

- 生成结果通过 `srcDoc` 放入 `sandbox="allow-scripts allow-forms"` 的 iframe。
- 不开启 `allow-same-origin`，生成代码不能读取父页面 Origin 和存储。
- 只有收到模型完整流并通过 HTML 校验后才创建版本。
- API Key、数据库密码和平台 Token 只配置在托管平台的加密环境变量中。
- CORS 生产环境只允许准确的 Vercel Origin。
- 游客 ID 提供 Demo 级数据隔离，不等同于身份认证或权限系统。

当前隔离适合评估 Demo，不是生产级任意代码沙箱。生产化还需要独立沙箱域名、CSP、出站网络限制、认证、限流、配额、数据保留策略和数据库迁移工具。

## 本地运行

要求：Java 21、Maven 3.9+、Node.js 20+。

启动后端：

```bash
cd backend
mvn spring-boot:run
```

启动前端：

```bash
cd frontend
npm ci
npm run dev
```

访问 `http://localhost:5173`。默认使用文件型 H2 数据库 `backend/data/buildtrace.mv.db`；未配置模型 Key 时使用界面明确标记的 fallback。

接入 OpenAI-compatible 模型：

```bash
APP_AI_PROVIDER=api \
APP_AI_BASE_URL=https://YOUR-PROVIDER/compatible-mode/v1 \
APP_AI_API_KEY=YOUR_KEY \
APP_AI_MODEL=qwen-plus \
APP_AI_TIMEOUT=180s \
mvn spring-boot:run
```

本地 Codex CLI 模式：

```bash
APP_AI_PROVIDER=codex-cli APP_AI_TIMEOUT=180s mvn spring-boot:run
```

完整环境变量见 [`.env.example`](.env.example)。不要把真实 Key 或数据库密码写入 `.env.example`、README、Issue 或 Git 提交。

## 测试与验收

```bash
cd backend && mvn test
cd frontend && npm run lint
cd frontend && npm run build
cd frontend && npm audit --audit-level=high
```

当前自动化结果：后端 5 个测试通过，前端 lint 和生产构建通过，高危依赖审计为 0。后端测试覆盖：

- 项目、消息和版本持久化。
- 版本递增和“恢复时创建新版本”。
- 不同访客之间的数据隔离。
- 从 Markdown 围栏和解释文本中提取完整 HTML。
- 拒绝空响应和不完整 HTML。
- 本地 Codex CLI 命令的隔离参数。

浏览器验收路径：

1. 创建应用并等待真实模型生成 v1。
2. 在 iframe 内执行新增、筛选、删除或状态切换等交互。
3. 提交修改需求并得到 v2。
4. 恢复 v1，确认平台创建新的 v3。
5. 刷新页面，确认对话、当前 HTML 和全部版本仍然存在。

## 生产部署

当前线上拓扑：

| 组件 | 平台 | 配置重点 |
| --- | --- | --- |
| 前端 | Vercel | `VITE_API_BASE_URL` 指向 Railway |
| Java 后端 | Railway | Dockerfile、模型变量、数据库变量、精确 CORS |
| PostgreSQL | Neon | JDBC SSL 连接，保存项目/消息/版本 |
| LLM | OpenAI-compatible API | `APP_AI_BASE_URL`、`APP_AI_API_KEY`、`APP_AI_MODEL` |

部署不需要 AWS 账号或自管理虚拟机。详细变量模板见 [`.env.example`](.env.example)，Railway 使用 [`backend/Dockerfile`](backend/Dockerfile) 和 [`backend/railway.toml`](backend/railway.toml)，Vercel 使用 [`frontend/vercel.json`](frontend/vercel.json)。

## 项目结构

```text
.
├── backend
│   ├── src/main/java/dev/buildtrace
│   │   ├── generation   # Prompt、模型流、HTML 校验、fallback
│   │   └── project      # 项目、消息、版本 API 与事务
│   ├── src/test         # 持久化、隔离、提取与 CLI 测试
│   ├── Dockerfile
│   └── railway.toml
├── frontend
│   ├── src
│   │   ├── api.ts       # JSON API、SSE 解析、完成语义
│   │   ├── App.tsx      # 项目和生成状态编排
│   │   └── components   # 对话、预览、版本和项目列表
│   └── vercel.json
├── docs
│   └── ai-development-report.md
└── .env.example
```

需求取舍、AI 协作方式、真实纠偏和验证记录见 [`docs/ai-development-report.md`](docs/ai-development-report.md)。
