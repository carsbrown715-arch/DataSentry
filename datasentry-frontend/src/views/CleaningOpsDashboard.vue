<template>
  <BaseLayout>
    <div class="cleaning-ops-page">
      <main class="main-content">
        <div class="content-header">
          <div class="header-info">
            <h1 class="content-title">清理运维看板</h1>
            <p class="content-subtitle">预算、成本、DLQ、计价同步与告警集中观测</p>
          </div>
          <div class="header-actions">
            <el-button :icon="Refresh" size="large" :loading="loadingAll" @click="loadAll">
              刷新
            </el-button>
          </div>
        </div>

        <el-row :gutter="16" class="metrics-row">
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-card shadow="hover" class="metric-card">
              <div class="metric-label">任务总数</div>
              <div class="metric-value">{{ metrics.totalRuns || 0 }}</div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-card shadow="hover" class="metric-card">
              <div class="metric-label">运行中任务</div>
              <div class="metric-value">{{ metrics.runningRuns || 0 }}</div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-card shadow="hover" class="metric-card warning">
              <div class="metric-label">硬预算超限</div>
              <div class="metric-value">{{ metrics.hardExceededRuns || 0 }}</div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-card shadow="hover" class="metric-card danger">
              <div class="metric-label">DLQ 待处理</div>
              <div class="metric-value">{{ metrics.readyDlq || 0 }}</div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="16" class="metrics-row">
          <el-col :xs="24" :sm="8">
            <el-card shadow="never" class="metric-card">
              <div class="metric-label">总成本</div>
              <div class="metric-value">¥ {{ formatCost(metrics.totalCost) }}</div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-card shadow="never" class="metric-card">
              <div class="metric-label">在线成本</div>
              <div class="metric-value">¥ {{ formatCost(metrics.onlineCost) }}</div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-card shadow="never" class="metric-card">
              <div class="metric-label">批处理成本</div>
              <div class="metric-value">¥ {{ formatCost(metrics.batchCost) }}</div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="16" class="metrics-row">
          <el-col :xs="24" :sm="12" :md="6">
            <el-card shadow="never" class="metric-card">
              <div class="metric-label">L2 Provider 状态</div>
              <div class="metric-value status-value">
                {{ metrics.l2ProviderStatus || 'UNKNOWN' }}
              </div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-card shadow="never" class="metric-card">
              <div class="metric-label">最近价格同步</div>
              <div class="metric-value status-value">
                {{ formatDate(metrics.lastPricingSyncTime) }}
              </div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-card shadow="never" class="metric-card warning">
              <div class="metric-label">价格同步失败次数</div>
              <div class="metric-value">{{ metrics.pricingSyncFailureCount || 0 }}</div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-card shadow="never" class="metric-card">
              <div class="metric-label">Webhook 成功 / 失败</div>
              <div class="metric-value webhook-value">
                {{ metrics.webhookPushSuccessCount || 0 }} /
                {{ metrics.webhookPushFailureCount || 0 }}
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="16" class="metrics-row">
          <el-col :xs="24" :sm="12" :md="6">
            <el-card shadow="never" class="metric-card">
              <div class="metric-label">ONNX 模型加载（成功/失败）</div>
              <div class="metric-value tiny-value">
                {{ metrics.onnxModelLoadSuccessCount || 0 }} /
                {{ metrics.onnxModelLoadFailureCount || 0 }}
              </div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-card shadow="never" class="metric-card">
              <div class="metric-label">ONNX 推理（成功/失败）</div>
              <div class="metric-value tiny-value">
                {{ metrics.onnxInferenceSuccessCount || 0 }} /
                {{ metrics.onnxInferenceFailureCount || 0 }}
              </div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-card shadow="never" class="metric-card warning">
              <div class="metric-label">ONNX 降级次数</div>
              <div class="metric-value">{{ metrics.onnxFallbackCount || 0 }}</div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-card shadow="never" class="metric-card">
              <div class="metric-label">ONNX 推理延迟（Avg/P95）</div>
              <div class="metric-value tiny-value">
                {{ Number(metrics.onnxInferenceAvgLatencyMs || 0).toFixed(2) }}ms /
                {{ metrics.onnxInferenceP95LatencyMs || 0 }}ms
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="16" class="metrics-row">
          <el-col :xs="24">
            <el-card shadow="never" class="metric-card">
              <div class="metric-label">ONNX Runtime / Model Signature</div>
              <div class="metric-value tiny-value mono-value">
                {{ metrics.onnxRuntimeVersion || 'N/A' }} |
                {{ metrics.onnxModelSignature || 'N/A' }}
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="16" class="metrics-row">
          <el-col :xs="24" :sm="12" :md="6">
            <el-card shadow="never" class="metric-card">
              <div class="metric-label">Cloud 推理（成功/失败）</div>
              <div class="metric-value tiny-value">
                {{ metrics.cloudInferenceSuccessCount || 0 }} /
                {{ metrics.cloudInferenceFailureCount || 0 }}
              </div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-card shadow="never" class="metric-card warning">
              <div class="metric-label">Cloud 降级次数</div>
              <div class="metric-value">{{ metrics.cloudFallbackCount || 0 }}</div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="12" :md="12">
            <el-card shadow="never" class="metric-card">
              <div class="metric-label">Cloud 推理延迟（Avg/P95）</div>
              <div class="metric-value tiny-value">
                {{ Number(metrics.cloudInferenceAvgLatencyMs || 0).toFixed(2) }}ms /
                {{ metrics.cloudInferenceP95LatencyMs || 0 }}ms
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-card class="panel" shadow="never">
          <template #header>
            <div class="panel-header">
              <span>价格目录同步</span>
              <div class="panel-actions">
                <el-button
                  size="small"
                  type="primary"
                  :loading="syncingPricing"
                  @click="syncPricing"
                >
                  立即同步
                </el-button>
              </div>
            </div>
          </template>
          <el-alert
            v-if="pricingSyncResult"
            :type="pricingSyncResult.success ? 'success' : 'warning'"
            :title="pricingSyncResult.message || '同步完成'"
            show-icon
            class="sync-result"
          />
          <el-table :data="pricingCatalog" stripe>
            <el-table-column prop="provider" label="Provider" min-width="130" />
            <el-table-column prop="model" label="Model" min-width="120" />
            <el-table-column prop="version" label="Version" width="100" />
            <el-table-column prop="inputPricePer1k" label="输入单价/1k" width="130" />
            <el-table-column prop="outputPricePer1k" label="输出单价/1k" width="130" />
            <el-table-column prop="currency" label="货币" width="80" />
            <el-table-column prop="updatedTime" label="更新时间" min-width="180" />
          </el-table>
        </el-card>

        <el-card class="panel" shadow="never">
          <template #header>
            <div class="panel-header">
              <span>告警列表</span>
            </div>
          </template>
          <el-table :data="alerts" stripe>
            <el-table-column prop="createdTime" label="时间" min-width="180" />
            <el-table-column prop="code" label="告警码" min-width="180" />
            <el-table-column label="级别" width="120">
              <template #default="scope">
                <el-tag :type="scope.row.level === 'WARN' ? 'warning' : 'info'" size="small">
                  {{ scope.row.level }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="描述" min-width="260" />
          </el-table>
        </el-card>

        <el-card class="panel" shadow="never">
          <template #header>
            <div class="panel-header">
              <span>DLQ 列表</span>
              <div class="panel-actions">
                <el-select v-model="dlqStatus" clearable placeholder="状态" style="width: 140px">
                  <el-option label="READY" value="READY" />
                  <el-option label="DONE" value="DONE" />
                  <el-option label="DEAD" value="DEAD" />
                </el-select>
                <el-button size="small" @click="loadDlq">查询</el-button>
              </div>
            </div>
          </template>
          <el-table v-loading="loadingDlq" :data="dlqRecords" stripe>
            <el-table-column prop="id" label="ID" width="90" />
            <el-table-column prop="jobRunId" label="Run ID" width="120" />
            <el-table-column prop="tableName" label="表" min-width="140" />
            <el-table-column prop="retryCount" label="重试" width="90" />
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column
              prop="errorMessage"
              label="错误"
              min-width="240"
              show-overflow-tooltip
            />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
                <el-button
                  size="small"
                  type="primary"
                  :disabled="scope.row.status !== 'READY'"
                  @click="retryDlq(scope.row.id)"
                >
                  重试
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card class="panel" shadow="never">
          <template #header>
            <div class="panel-header">
              <span>成本台账（最近 50 条）</span>
            </div>
          </template>
          <el-table :data="costLedgers" stripe>
            <el-table-column prop="createdTime" label="时间" min-width="180" />
            <el-table-column prop="channel" label="链路" width="100" />
            <el-table-column prop="detectorLevel" label="层级" width="110" />
            <el-table-column prop="jobRunId" label="Run ID" width="110" />
            <el-table-column prop="inputTokensEst" label="输入Token" width="120" />
            <el-table-column prop="costAmount" label="成本" width="120">
              <template #default="scope">¥ {{ formatCost(scope.row.costAmount) }}</template>
            </el-table-column>
            <el-table-column prop="provider" label="Provider" min-width="120" />
            <el-table-column prop="model" label="Model" min-width="120" />
          </el-table>
        </el-card>
      </main>
    </div>
  </BaseLayout>
</template>

<script setup>
  import { onMounted, reactive, ref } from 'vue';
  import { ElMessage } from 'element-plus';
  import { Refresh } from '@element-plus/icons-vue';
  import BaseLayout from '@/layouts/BaseLayout.vue';
  import cleaningService from '@/services/cleaning';

  const loadingAll = ref(false);
  const loadingDlq = ref(false);
  const syncingPricing = ref(false);

  const metrics = reactive({
    totalRuns: 0,
    runningRuns: 0,
    pausedRuns: 0,
    hardExceededRuns: 0,
    totalDlq: 0,
    readyDlq: 0,
    totalCost: 0,
    onlineCost: 0,
    batchCost: 0,
    lastPricingSyncTime: '',
    pricingSyncFailureCount: 0,
    webhookPushSuccessCount: 0,
    webhookPushFailureCount: 0,
    l2ProviderStatus: 'UNKNOWN',
    onnxModelLoadSuccessCount: 0,
    onnxModelLoadFailureCount: 0,
    onnxInferenceSuccessCount: 0,
    onnxInferenceFailureCount: 0,
    onnxFallbackCount: 0,
    onnxInferenceAvgLatencyMs: 0,
    onnxInferenceP95LatencyMs: 0,
    onnxRuntimeVersion: '',
    onnxModelSignature: '',
    cloudInferenceSuccessCount: 0,
    cloudInferenceFailureCount: 0,
    cloudFallbackCount: 0,
    cloudInferenceAvgLatencyMs: 0,
    cloudInferenceP95LatencyMs: 0,
  });

  const alerts = ref([]);
  const dlqRecords = ref([]);
  const costLedgers = ref([]);
  const pricingCatalog = ref([]);
  const pricingSyncResult = ref(null);
  const dlqStatus = ref('READY');

  const formatCost = value => {
    if (value === null || value === undefined || Number.isNaN(Number(value))) {
      return '0.0000';
    }
    return Number(value).toFixed(4);
  };

  const formatDate = value => {
    if (!value) {
      return '-';
    }
    return String(value).replace('T', ' ');
  };

  const loadMetrics = async () => {
    const summary = await cleaningService.getMetricsSummary();
    Object.assign(metrics, summary || {});
  };

  const loadAlerts = async () => {
    alerts.value = await cleaningService.listAlerts();
  };

  const loadDlq = async () => {
    loadingDlq.value = true;
    try {
      dlqRecords.value = await cleaningService.listDlq({ status: dlqStatus.value || undefined });
    } finally {
      loadingDlq.value = false;
    }
  };

  const loadCostLedgers = async () => {
    const list = await cleaningService.listCostLedger();
    costLedgers.value = (list || []).slice(0, 50);
  };

  const loadPricingCatalog = async () => {
    const list = await cleaningService.getPricingCatalog();
    pricingCatalog.value = (list || []).slice(0, 50);
  };

  const retryDlq = async id => {
    try {
      await cleaningService.retryDlq(id);
      ElMessage.success('已提交重试');
      await loadDlq();
    } catch (error) {
      ElMessage.error('重试失败');
    }
  };

  const syncPricing = async () => {
    syncingPricing.value = true;
    try {
      pricingSyncResult.value = await cleaningService.syncPricingNow('manual');
      if (pricingSyncResult.value?.success) {
        ElMessage.success('价格同步成功');
      } else {
        ElMessage.warning(pricingSyncResult.value?.message || '价格同步失败');
      }
      await Promise.all([loadPricingCatalog(), loadMetrics(), loadAlerts()]);
    } catch (error) {
      ElMessage.error('触发价格同步失败');
    } finally {
      syncingPricing.value = false;
    }
  };

  const loadAll = async () => {
    loadingAll.value = true;
    try {
      await Promise.all([
        loadMetrics(),
        loadAlerts(),
        loadDlq(),
        loadCostLedgers(),
        loadPricingCatalog(),
      ]);
    } catch (error) {
      ElMessage.error('加载运维数据失败');
    } finally {
      loadingAll.value = false;
    }
  };

  onMounted(() => {
    loadAll();
  });
</script>

<style scoped>
  .cleaning-ops-page {
    min-height: 100vh;
    padding: 2rem;
  }

  .main-content {
    max-width: 1320px;
    margin: 0 auto;
  }

  .content-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1.25rem;
  }

  .header-actions {
    display: flex;
    gap: 0.5rem;
  }

  .content-title {
    margin: 0;
    font-size: 1.75rem;
    font-weight: 600;
    color: #0f172a;
  }

  .content-subtitle {
    margin-top: 0.5rem;
    color: #64748b;
  }

  .metrics-row {
    margin-bottom: 1rem;
  }

  .metric-card {
    border-radius: 10px;
  }

  .metric-card.warning {
    border-left: 3px solid #f59e0b;
  }

  .metric-card.danger {
    border-left: 3px solid #ef4444;
  }

  .metric-label {
    color: #64748b;
    font-size: 13px;
    margin-bottom: 0.5rem;
  }

  .metric-value {
    color: #0f172a;
    font-size: 24px;
    font-weight: 600;
  }

  .status-value {
    font-size: 18px;
  }

  .webhook-value {
    font-size: 20px;
  }

  .tiny-value {
    font-size: 15px;
  }

  .mono-value {
    font-family: Menlo, Monaco, Consolas, 'Courier New', monospace;
    word-break: break-all;
    line-height: 1.4;
  }

  .panel {
    margin-top: 1rem;
  }

  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
  }

  .panel-actions {
    display: flex;
    gap: 0.5rem;
    align-items: center;
  }

  .sync-result {
    margin-bottom: 10px;
  }
</style>
