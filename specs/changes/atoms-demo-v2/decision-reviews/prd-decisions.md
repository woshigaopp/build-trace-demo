# Atoms Demo v2 PRD 决策记录

## 决策权限

用户在明确“最后一次补交、按更高标准提交”后回复“授权”。本次 v2 的产品决策由 AI 按通过评审所需的最小完整产品范围锁定；授权不包括伪造验证结果或跳过公网验收。

## 决策摘要

| ID | 决策 | 状态 | 决策者 |
|---|---|---|---|
| PDEC-001 | 使用邮箱密码注册登录和 JWT，不保留游客身份作为主流程 | locked | ai-authorized |
| PDEC-002 | 生成物限定为多文件 React 前端应用 | locked | ai-authorized |
| PDEC-003 | 使用 Sandpack 完成多文件编辑、编译和预览 | locked | ai-authorized |
| PDEC-004 | 模型返回结构化文件操作，不再每轮返回完整 HTML | locked | ai-authorized |
| PDEC-005 | 候选版本校验失败最多自动修复一次，成功后原子提交 | locked | ai-authorized |
| PDEC-006 | 成功和失败的消息、任务、状态、错误都完整持久化 | locked | ai-authorized |

## 决策详情

### PDEC-001

- 拒绝方案：浏览器生成 `guestId`、仅做一个无后端语义的登录页面。
- 原因：两者都不能证明身份、隔离和跨设备恢复，无法关闭评审反馈。
- 影响：新增账号对象；项目查询必须从 JWT principal 派生 owner。
- 验证：注册/登录/退出、两账号越权、过期/无 token 集成测试和公网流程。

### PDEC-002

- 拒绝方案：继续单 HTML；宣称生成任意带后端产品。
- 原因：单 HTML 已被明确拒绝；任意后端生成与当前运行/部署能力不匹配。
- 影响：版本成为文件快照，产品文案明确 React 范围。
- 验证：至少 `package.json`、入口文件、组件、样式多文件可见且可运行。

### PDEC-003

- 拒绝方案：`iframe srcDoc`、自建 WebContainer。
- 原因：Sandpack 官方提供可编辑多文件与浏览器预览；自建运行时对最后一次补交风险过高。
- 影响：前端引入 `@codesandbox/sandpack-react`，UI 必须暴露文件、代码和 preview 错误。
- 验证：真实生成文件在 Sandpack 编译，交互按钮可用，编辑保存后预览更新。

### PDEC-004

- 拒绝方案：每轮把完整旧 HTML 发给模型并要求完整重写。
- 原因：输出随项目增长，是当前不稳定的直接结构性原因。
- 影响：模型契约变为 `summary + operations[]`；服务端规范化路径并应用到候选快照。
- 验证：解析、非法路径、write/delete、缺入口文件和连续修改测试。

### PDEC-005

- 拒绝方案：无限重试、失败时写入部分文件、只在浏览器显示错误。
- 原因：无限重试不可控；部分写入会污染之后每一轮；临时错误无法审计。
- 影响：显式任务状态机，repair 只有一次，版本提交必须事务化。
- 验证：强制两次无效响应后 run=failed 且 currentVersion/files 未变化。

### PDEC-006

- 拒绝方案：只存成功 assistant 消息，状态只走 SSE。
- 原因：这是“部分 project 对话”反馈的直接根因。
- 影响：run 在调用模型之前落库，所有终态写 assistant 消息与错误。
- 验证：断流/失败后刷新详情仍能看到 prompt、状态、错误和顺序。

## 下游约束

- 实现不得重新引入 `X-Guest-Id`。
- 失败候选不得更新 `Project.currentVersionId`。
- 前端不得以截断预览冒充完整文件内容。
- README 只能记录真实执行过的测试和公网地址。

## PRD Local Audit

所有 source 已登记；PDEC 均有用户授权、拒绝方案、理由、影响和验证方式；没有 open 产品决策。允许进入工程设计与实现。
