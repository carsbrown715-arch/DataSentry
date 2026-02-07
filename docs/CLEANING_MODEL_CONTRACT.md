中文 | English (TBD)

# Cleaning L2 模型契约（ONNX / CLOUD_API）

本文档定义清理 L2 检测的输入输出契约，用于算法/后端联调与上线变更评审。

## 1. ONNX 契约

### 1.1 输入契约
| 项 | 约束 |
|---|---|
| 输入节点数量 | 至少 1 个（当前取第一个输入节点） |
| 输入类型 | `STRING` 或 `FLOAT` |
| STRING 形状 | 支持 `[N]` 或 `[N, M]`（按单条文本推理） |
| FLOAT 形状 | 推荐 `[1, D]`；若 D 缺失则后端按默认 `16` 维构造特征 |
| 特征构造 | 文本长度、数字占比、特殊字符占比、命中词特征 + hash bucket |

### 1.2 输出契约
| 项 | 约束 |
|---|---|
| 输出节点数量 | 至少 1 个（遍历输出节点，取首个可解析数值） |
| 输出类型 | 数值型张量（`float/double/int/long` 及二维形式） |
| 评分范围 | 若不在 `[0,1]`，后端自动做 sigmoid 归一化 |
| 阈值判定 | `max(providerThreshold, policyThreshold)` |

### 1.3 运行时约束
- 模型路径由 `spring.ai.alibaba.datasentry.cleaning.l2.onnx-model-path` 配置。
- 若模型不可用，ONNX Provider 自动降级为不可用态，由路由层走备用 Provider。
- 启动时记录：`onnxRuntimeVersion`、`onnxModelSignature`。

## 2. Cloud API 契约

### 2.1 请求体
`POST {cloudApi.url}`，JSON：
- `text`: 待检测文本
- `category`: 规则类别（可选）
- `threshold`: 阈值
- `model`: 模型名（可选）

### 2.2 鉴权
- 支持自定义 Header 名、前缀与 Key：
  - `authHeader`（默认 `Authorization`）
  - `authPrefix`（可选）
  - `apiKey`

### 2.3 响应体兼容字段
后端按优先级读取 score：
- `score`
- `riskScore` / `risk_score`
- `data.score` / `data.riskScore`
- `result.score` / `result.riskScore`
- `predictions[0].score`

label 兼容字段：
- `label` / `decision` / `result.label` / `data.label`

若仅有 label：
- `RISK/SUSPICIOUS/BLOCK` -> `1.0`
- `SAFE/PASS/ALLOW` -> `0.0`

## 3. 工程化约束（P2++）
- `CloudApiL2DetectionProvider` 使用单例 `HttpClient` Bean，启用连接复用。
- Shadow 异步执行必须使用独立线程池（`DiscardPolicy`）。
- 预算超限：`Batch=PAUSE`，`Online=SMART_FAIL_CLOSED`。

## 4. 变更流程（必须）
模型升级前必须同步更新：
1. 本文档中的输入/输出契约。
2. `OnnxL2BenchmarkTest` 压测结果（ONNX=ON）。
3. `metrics/summary` 指标基线（加载/推理/降级）。
