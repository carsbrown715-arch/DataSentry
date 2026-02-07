中文 | English (TBD)

# Cleaning API P2（执行版）

本文档描述 P2 阶段数据清理能力的后端 API 合同与前端页面映射，作为联调与上线验收基线。

## 1. 基础信息
- Base Path：`/api/datasentry/cleaning`
- 响应包装：`ApiResponse<T>`
- 追踪头：`X-Trace-Id`（可选，未传则服务端生成）
- 在线清理鉴权头：`X-API-KEY`（必需，且 Agent 开启 API Key）

## 2. 在线清理（P0/P2）
- `POST /{agentId}/check`
  - 作用：检测文本风险，不做脱敏写回。
- `POST /{agentId}/sanitize`
  - 作用：检测 + 返回脱敏文本。
- 请求体：`CleaningCheckRequest`
  - 关键字段：`content`、`scene`、`metadata`。
- 返回体：`CleaningResponse`
  - 关键字段：`verdict`、`findings[]`、`sanitizedText`、`traceId`。

## 3. 批处理任务与预算（P1/P2）
- `POST /jobs`：创建任务。
- `GET /jobs/{jobId}`：查询任务。
- `GET /jobs`：任务列表（`agentId`/`datasourceId`/`enabled` 过滤）。
- `POST /jobs/{jobId}/runs`：启动一次运行实例。
- `GET /job-runs/{runId}`：查询运行实例。
- `GET /job-runs`：运行实例列表（`jobId`/`status` 过滤）。
- `POST /job-runs/{runId}/pause|resume|cancel`：运行控制。
- `GET /job-runs/{runId}/budget`：预算状态与成本视图。
- `GET /cost-ledger`：成本台账查询（`jobRunId`/`traceId`/`channel` 过滤）。

## 4. 价格目录与同步（P2-A）
- `POST /pricing/sync?reason=manual`：手动同步价格目录。
- `GET /pricing/catalog`：查询当前价格目录。
- 启动行为：应用启动自动执行一次价格初始化（冷启动保障）。

## 5. 观测与告警（P2）
- `GET /metrics/summary`：全局运行指标。
- `GET /alerts`：告警列表（含 ONNX/CLOUD 降级告警）。

## 6. DLQ 与回滚（P1/P2）
- `GET /dlq`：DLQ 列表（`status`/`jobRunId` 过滤）。
- `POST /dlq/{id}/retry`：单条重试。
- `POST /job-runs/{runId}/rollback`：创建回滚任务。
- `GET /rollbacks/{rollbackRunId}`：回滚状态查询。

## 7. 前端页面映射
- `CleaningJobManager.vue`：任务管理、运行控制、预算/台账、回滚入口。
- `CleaningOpsDashboard.vue`：指标与告警看板。
- `CleaningReviewWorkbench.vue`：人审工作台。
- `CleaningRealtime.vue`：实时 check/sanitize 调试入口。

## 8. 上线验收最小清单
- 后端：`mvn spotless:apply && mvn spring-javaformat:apply && mvn checkstyle:check && mvn test`。
- 前端：`npm run lint && npm run format && npm run build`。
- 冒烟：
  - `POST /{agentId}/check`
  - `POST /jobs/{jobId}/runs`
  - `GET /metrics/summary`
  - `POST /pricing/sync`
