中文 | English (TBD)

# Cleaning ONNX=ON 压测指南

## 1. 目标
验证 JVM -> JNI -> ONNX Runtime 真推理链路在真实模型下的稳定性与延迟基线。

## 2. 运行前提
- 已安装 ONNX Runtime Java 依赖（项目内已引入）。
- 本地可访问 ONNX 模型文件。
- 设置环境变量：

```bash
export DATASENTRY_ONNX_BENCH_MODEL_PATH=/path/to/model.onnx
```

## 3. 执行命令
在仓库根目录执行：

```bash
mvn -q -pl datasentry-management -Dtest=OnnxL2BenchmarkTest test
```

## 4. 结果解读
输出包含：
- `avg / p50 / p95` 推理时延
- `qps` 吞吐
- `ops success/failure/fallback` 运行指标

建议门槛（可按业务调优）：
- `p95` 不高于历史基线 +10%
- `fallback` 持续为 0 或可解释
- `failure` 不出现持续增长

## 5. 无 ONNX 模型时怎么办
若当前环境只接云模型 API：
- 不阻塞上线，配置 `provider=CLOUD_API`。
- ONNX Provider 将保持不可用态，由路由自动走 Cloud Provider。
- 仍需通过 `metrics/summary` 观察 cloud 延迟与 fallback。

## 6. 联合验收建议
每次模型/阈值升级，至少同步提供：
1. 本次压测输出（包含 p95 与 qps）
2. `docs/CLEANING_MODEL_CONTRACT.md` 更新记录
3. `GET /api/datasentry/cleaning/metrics/summary` 截图或导出
