<template>
  <BaseLayout>
    <div class="cleaning-job-page">
      <main class="main-content">
        <div class="content-header">
          <div class="header-info">
            <h1 class="content-title">清理任务管理</h1>
            <p class="content-subtitle">创建清理任务、启动运行并查看预算状态</p>
          </div>
          <div class="header-actions">
            <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建任务</el-button>
            <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
          </div>
        </div>

        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel-header">
              <span>任务列表</span>
              <el-form inline>
                <el-form-item label="Agent">
                  <el-select v-model="filters.agentId" clearable filterable style="width: 180px">
                    <el-option
                      v-for="agent in agents"
                      :key="agent.id"
                      :label="agent.name || `Agent-${agent.id}`"
                      :value="agent.id"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="状态">
                  <el-select v-model="filters.enabled" clearable style="width: 120px">
                    <el-option label="启用" :value="1" />
                    <el-option label="停用" :value="0" />
                  </el-select>
                </el-form-item>
                <el-button @click="loadJobs">查询</el-button>
              </el-form>
            </div>
          </template>

          <el-table :data="jobs" stripe v-loading="loadingJobs">
            <el-table-column prop="id" label="ID" width="90" />
            <el-table-column prop="agentId" label="Agent" width="100" />
            <el-table-column prop="datasourceId" label="数据源" width="100" />
            <el-table-column prop="tableName" label="表" min-width="160" />
            <el-table-column prop="targetConfigType" label="目标模式" width="120" />
            <el-table-column label="预算" min-width="220">
              <template #default="scope">
                <span>{{ scope.row.budgetEnabled === 1 ? '启用' : '关闭' }}</span>
                <span v-if="scope.row.budgetEnabled === 1">
                  / {{ scope.row.budgetSoftLimit }} - {{ scope.row.budgetHardLimit }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="scope">
                <el-button size="small" type="primary" @click="createRun(scope.row)">
                  启动Run
                </el-button>
                <el-button size="small" @click="selectJob(scope.row)">查看Run</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card shadow="never" class="panel" v-if="selectedJob">
          <template #header>
            <div class="panel-header">
              <span>运行列表（Job #{{ selectedJob.id }}）</span>
              <el-form inline>
                <el-form-item label="状态">
                  <el-select v-model="runStatusFilter" clearable style="width: 140px">
                    <el-option label="QUEUED" value="QUEUED" />
                    <el-option label="RUNNING" value="RUNNING" />
                    <el-option label="PAUSED" value="PAUSED" />
                    <el-option label="SUCCEEDED" value="SUCCEEDED" />
                    <el-option label="FAILED" value="FAILED" />
                  </el-select>
                </el-form-item>
                <el-button @click="loadRuns">查询</el-button>
              </el-form>
            </div>
          </template>

          <el-table :data="runs" stripe v-loading="loadingRuns">
            <el-table-column prop="id" label="Run ID" width="110" />
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column prop="totalScanned" label="扫描" width="100" />
            <el-table-column prop="totalFlagged" label="命中" width="100" />
            <el-table-column prop="actualCost" label="成本" width="120" />
            <el-table-column prop="budgetStatus" label="预算状态" width="140" />
            <el-table-column prop="budgetMessage" label="预算信息" min-width="180" />
            <el-table-column label="操作" width="360" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="loadBudget(scope.row)">预算</el-button>
                <el-button size="small" type="primary" @click="createRollback(scope.row)">
                  回滚
                </el-button>
                <el-button size="small" type="warning" @click="pauseRun(scope.row)">暂停</el-button>
                <el-button size="small" type="success" @click="resumeRun(scope.row)">
                  恢复
                </el-button>
                <el-button size="small" type="danger" @click="cancelRun(scope.row)">取消</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-dialog v-model="createDialogVisible" title="新建清理任务" width="760px">
          <el-form :model="createForm" label-width="130px">
            <el-form-item label="Agent">
              <el-select
                v-model="createForm.agentId"
                filterable
                style="width: 100%"
                placeholder="请选择 Agent"
              >
                <el-option
                  v-for="agent in agents"
                  :key="agent.id"
                  :label="agent.name || `Agent-${agent.id}`"
                  :value="agent.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="数据源">
              <el-select
                v-model="createForm.datasourceId"
                filterable
                style="width: 100%"
                placeholder="请选择数据源"
              >
                <el-option
                  v-for="ds in datasources"
                  :key="ds.id"
                  :label="ds.name || `Datasource-${ds.id}`"
                  :value="ds.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="表名">
              <el-select
                v-model="createForm.tableName"
                filterable
                clearable
                style="width: 100%"
                :loading="loadingTables"
                placeholder="请选择表"
              >
                <el-option
                  v-for="table in tableOptions"
                  :key="table"
                  :label="table"
                  :value="table"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="策略">
              <el-select
                v-model="createForm.policyId"
                filterable
                clearable
                style="width: 100%"
                placeholder="请选择策略"
              >
                <el-option
                  v-for="policy in policies"
                  :key="policy.id"
                  :label="`${policy.name} (#${policy.id})`"
                  :value="policy.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="主键列">
              <el-select
                v-model="createForm.pkColumns"
                multiple
                filterable
                clearable
                collapse-tags
                collapse-tags-tooltip
                style="width: 100%"
                :loading="loadingColumns"
                placeholder="请选择主键列"
              >
                <el-option
                  v-for="column in tableColumns"
                  :key="`pk-${column}`"
                  :label="column"
                  :value="column"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="目标列">
              <el-select
                v-model="createForm.targetColumns"
                multiple
                filterable
                clearable
                collapse-tags
                collapse-tags-tooltip
                style="width: 100%"
                :loading="loadingColumns"
                placeholder="请选择目标列"
              >
                <el-option
                  v-for="column in tableColumns"
                  :key="`target-${column}`"
                  :label="column"
                  :value="column"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="目标模式">
              <el-select v-model="createForm.targetConfigType" style="width: 180px">
                <el-option label="COLUMNS" value="COLUMNS" />
                <el-option label="JSONPATH" value="JSONPATH" />
              </el-select>
            </el-form-item>

            <el-form-item v-if="createForm.targetConfigType === 'JSONPATH'" label="JSONPath 配置">
              <div class="jsonpath-panel">
                <div class="jsonpath-toolbar">
                  <el-button
                    size="small"
                    type="primary"
                    plain
                    :disabled="createForm.targetColumns.length === 0"
                    @click="fillJsonPathTemplates"
                  >
                    模板填充
                  </el-button>
                </div>
                <div v-if="createForm.targetColumns.length === 0" class="jsonpath-empty">
                  请先选择目标列
                </div>
                <div v-else class="jsonpath-list">
                  <div
                    v-for="column in createForm.targetColumns"
                    :key="column"
                    class="jsonpath-item"
                  >
                    <span class="jsonpath-column">{{ column }}</span>
                    <el-input v-model="jsonPathMappings[column]" placeholder="例如：$.text.body" />
                  </div>
                </div>
              </div>
            </el-form-item>

            <el-divider content-position="left">高级配置</el-divider>

            <el-form-item label="运行模式">
              <el-select v-model="createForm.mode" style="width: 220px">
                <el-option label="DRY_RUN" value="DRY_RUN" />
                <el-option label="WRITEBACK" value="WRITEBACK" />
              </el-select>
            </el-form-item>

            <el-form-item label="写回模式">
              <el-select v-model="createForm.writebackMode" style="width: 220px">
                <el-option label="NONE" value="NONE" />
                <el-option label="UPDATE" value="UPDATE" />
                <el-option label="SOFT_DELETE" value="SOFT_DELETE" />
              </el-select>
            </el-form-item>

            <el-form-item label="人审策略">
              <el-select v-model="createForm.reviewPolicy" style="width: 220px">
                <el-option label="NEVER" value="NEVER" />
                <el-option label="ALWAYS" value="ALWAYS" />
                <el-option label="ON_RISK" value="ON_RISK" />
              </el-select>
            </el-form-item>

            <el-form-item label="whereSql">
              <el-input
                v-model="createForm.whereSql"
                type="textarea"
                :autosize="{ minRows: 2, maxRows: 4 }"
                placeholder="可选：例如 status = 'ACTIVE'"
              />
            </el-form-item>

            <el-alert
              v-if="createForm.whereSql && isRiskyWhereSql(createForm.whereSql)"
              type="warning"
              :closable="false"
              show-icon
              class="where-sql-alert"
              title="whereSql 检测到高风险关键词（DROP/DELETE/TRUNCATE/UPDATE...），请确认仅用于筛选条件。"
            />

            <el-form-item label="在线 Fail-Closed">
              <el-switch v-model="createForm.onlineFailClosedEnabled" />
            </el-form-item>

            <el-form-item label="在线 Token 上限">
              <el-input-number v-model="createForm.onlineRequestTokenLimit" :min="1" :step="100" />
            </el-form-item>

            <el-form-item label="写回映射(JSON)">
              <el-input
                v-model="createForm.writebackMappingJson"
                type="textarea"
                :autosize="{ minRows: 3, maxRows: 8 }"
                placeholder='例如：{"masked_phone":"phone"}'
              />
            </el-form-item>

            <el-form-item label="预算启用">
              <el-switch v-model="createForm.budgetEnabled" />
            </el-form-item>
            <el-form-item v-if="createForm.budgetEnabled" label="预算阈值">
              <div class="budget-row">
                <el-input-number v-model="createForm.budgetSoftLimit" :min="0" :step="1" />
                <span>-</span>
                <el-input-number v-model="createForm.budgetHardLimit" :min="0" :step="1" />
              </div>
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="createDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitCreate">创建</el-button>
          </template>
        </el-dialog>

        <el-dialog v-model="budgetDialogVisible" title="预算快照" width="520px">
          <el-descriptions v-if="budgetView" :column="1" border>
            <el-descriptions-item label="Run ID">{{ budgetView.runId }}</el-descriptions-item>
            <el-descriptions-item label="Job ID">{{ budgetView.jobId }}</el-descriptions-item>
            <el-descriptions-item label="预算阈值">
              {{ budgetView.budgetSoftLimit }} - {{ budgetView.budgetHardLimit }}
              {{ budgetView.budgetCurrency }}
            </el-descriptions-item>
            <el-descriptions-item label="预估成本">
              {{ budgetView.estimatedCost }}
            </el-descriptions-item>
            <el-descriptions-item label="实际成本">
              {{ budgetView.actualCost }}
            </el-descriptions-item>
            <el-descriptions-item label="预算状态">
              {{ budgetView.budgetStatus }}
            </el-descriptions-item>
            <el-descriptions-item label="预算信息">
              {{ budgetView.budgetMessage }}
            </el-descriptions-item>
          </el-descriptions>
        </el-dialog>

        <el-dialog v-model="rollbackDialogVisible" title="回滚任务" width="520px">
          <el-descriptions v-if="rollbackRun" :column="1" border>
            <el-descriptions-item label="Rollback Run ID">
              {{ rollbackRun.id }}
            </el-descriptions-item>
            <el-descriptions-item label="原 Run ID">
              {{ rollbackRun.jobRunId }}
            </el-descriptions-item>
            <el-descriptions-item label="状态">{{ rollbackRun.status }}</el-descriptions-item>
            <el-descriptions-item label="目标/成功/失败">
              {{ rollbackRun.totalTarget || 0 }} / {{ rollbackRun.totalSuccess || 0 }} /
              {{ rollbackRun.totalFailed || 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">
              {{ rollbackRun.createdTime }}
            </el-descriptions-item>
          </el-descriptions>
          <template #footer>
            <el-button @click="rollbackDialogVisible = false">关闭</el-button>
            <el-button type="primary" :loading="rollbackLoading" @click="refreshRollback">
              刷新状态
            </el-button>
          </template>
        </el-dialog>
      </main>
    </div>
  </BaseLayout>
</template>

<script setup>
  import { onMounted, reactive, ref, watch } from 'vue';
  import { ElMessage } from 'element-plus';
  import { Plus, Refresh } from '@element-plus/icons-vue';
  import BaseLayout from '@/layouts/BaseLayout.vue';
  import cleaningService from '@/services/cleaning';
  import agentService from '@/services/agent';
  import datasourceService from '@/services/datasource';

  const loadingJobs = ref(false);
  const loadingRuns = ref(false);
  const loadingTables = ref(false);
  const loadingColumns = ref(false);

  const jobs = ref([]);
  const runs = ref([]);
  const agents = ref([]);
  const datasources = ref([]);
  const policies = ref([]);
  const tableOptions = ref([]);
  const tableColumns = ref([]);

  const selectedJob = ref(null);
  const runStatusFilter = ref('');

  const filters = reactive({
    agentId: undefined,
    enabled: undefined,
  });

  const createDialogVisible = ref(false);
  const createForm = reactive({
    agentId: undefined,
    datasourceId: undefined,
    tableName: '',
    policyId: undefined,
    pkColumns: [],
    targetColumns: [],
    targetConfigType: 'COLUMNS',
    mode: 'DRY_RUN',
    writebackMode: 'NONE',
    reviewPolicy: 'NEVER',
    whereSql: '',
    onlineFailClosedEnabled: true,
    onlineRequestTokenLimit: 4000,
    writebackMappingJson: '{}',
    budgetEnabled: true,
    budgetSoftLimit: 10,
    budgetHardLimit: 50,
  });

  const jsonPathMappings = reactive({});

  const budgetDialogVisible = ref(false);
  const budgetView = ref(null);

  const rollbackDialogVisible = ref(false);
  const rollbackRun = ref(null);
  const rollbackLoading = ref(false);

  const resetJsonPathMappings = () => {
    Object.keys(jsonPathMappings).forEach(key => {
      delete jsonPathMappings[key];
    });
  };

  const buildDefaultJsonPath = column => `$.${column}`;

  const detectDefaultPkColumns = (columns, tableName) => {
    if (!columns || columns.length === 0) {
      return [];
    }
    const lowerMap = new Map(columns.map(column => [column.toLowerCase(), column]));

    if (lowerMap.has('id')) {
      return [lowerMap.get('id')];
    }

    const normalizedTable = (tableName || '').toLowerCase();
    if (normalizedTable) {
      const tableIdKey = `${normalizedTable}_id`;
      if (lowerMap.has(tableIdKey)) {
        return [lowerMap.get(tableIdKey)];
      }
    }

    if (lowerMap.has('uuid')) {
      return [lowerMap.get('uuid')];
    }

    return [];
  };

  const syncJsonPathMappings = () => {
    const nextMappings = {};
    createForm.targetColumns.forEach(column => {
      nextMappings[column] = jsonPathMappings[column] || buildDefaultJsonPath(column);
    });
    resetJsonPathMappings();
    Object.assign(jsonPathMappings, nextMappings);
  };

  const fillJsonPathTemplates = () => {
    if (createForm.targetColumns.length === 0) {
      ElMessage.warning('请先选择目标列');
      return;
    }
    const nextMappings = {};
    createForm.targetColumns.forEach(column => {
      nextMappings[column] = buildDefaultJsonPath(column);
    });
    resetJsonPathMappings();
    Object.assign(jsonPathMappings, nextMappings);
    ElMessage.success('已按模板填充 JSONPath');
  };

  const loadAgents = async () => {
    try {
      agents.value = await agentService.list();
    } catch (error) {
      agents.value = [];
    }
  };

  const loadDatasources = async () => {
    try {
      datasources.value = await datasourceService.getAllDatasource();
    } catch (error) {
      datasources.value = [];
    }
  };

  const loadPolicies = async () => {
    try {
      policies.value = await cleaningService.listPolicies();
      if (!createForm.policyId && policies.value.length > 0) {
        createForm.policyId = policies.value[0].id;
      }
    } catch (error) {
      policies.value = [];
    }
  };

  const resetTableAndColumns = () => {
    createForm.tableName = '';
    tableOptions.value = [];
    tableColumns.value = [];
    createForm.pkColumns = [];
    createForm.targetColumns = [];
    resetJsonPathMappings();
  };

  const loadTables = async datasourceId => {
    if (!datasourceId) {
      tableOptions.value = [];
      return;
    }
    loadingTables.value = true;
    try {
      tableOptions.value = await datasourceService.getDatasourceTables(Number(datasourceId));
    } catch (error) {
      tableOptions.value = [];
      ElMessage.error('加载数据表失败');
    } finally {
      loadingTables.value = false;
    }
  };

  const loadColumns = async (datasourceId, tableName) => {
    if (!datasourceId || !tableName) {
      tableColumns.value = [];
      return;
    }
    loadingColumns.value = true;
    try {
      const columns = await datasourceService.getTableColumns(Number(datasourceId), tableName);
      tableColumns.value = columns;
      const existingPkColumns = createForm.pkColumns.filter(column => columns.includes(column));
      const autoDetectedPkColumns =
        existingPkColumns.length > 0 ? [] : detectDefaultPkColumns(columns, tableName);
      createForm.pkColumns =
        existingPkColumns.length > 0 ? existingPkColumns : autoDetectedPkColumns;
      if (existingPkColumns.length === 0 && autoDetectedPkColumns.length > 0) {
        ElMessage.info(`已为你自动预选主键列：${autoDetectedPkColumns.join(', ')}`);
      }
      createForm.targetColumns = createForm.targetColumns.filter(column =>
        columns.includes(column),
      );
      syncJsonPathMappings();
    } catch (error) {
      tableColumns.value = [];
      ElMessage.error('加载字段失败');
    } finally {
      loadingColumns.value = false;
    }
  };

  const loadJobs = async () => {
    loadingJobs.value = true;
    try {
      jobs.value = await cleaningService.listJobs({
        agentId: filters.agentId,
        enabled: filters.enabled,
      });
    } catch (error) {
      ElMessage.error('加载任务失败');
    } finally {
      loadingJobs.value = false;
    }
  };

  const loadRuns = async () => {
    if (!selectedJob.value) {
      return;
    }
    loadingRuns.value = true;
    try {
      runs.value = await cleaningService.listRuns({
        jobId: selectedJob.value.id,
        status: runStatusFilter.value || undefined,
      });
    } catch (error) {
      ElMessage.error('加载运行实例失败');
    } finally {
      loadingRuns.value = false;
    }
  };

  const loadAll = async () => {
    await Promise.all([loadAgents(), loadDatasources(), loadPolicies(), loadJobs()]);
    if (selectedJob.value) {
      await loadRuns();
    }
  };

  watch(
    () => createForm.datasourceId,
    async datasourceId => {
      resetTableAndColumns();
      if (datasourceId) {
        await loadTables(datasourceId);
      }
    },
  );

  watch(
    () => createForm.tableName,
    async tableName => {
      tableColumns.value = [];
      createForm.pkColumns = [];
      createForm.targetColumns = [];
      resetJsonPathMappings();
      if (createForm.datasourceId && tableName) {
        await loadColumns(createForm.datasourceId, tableName);
      }
    },
  );

  watch(
    () => createForm.targetColumns,
    () => {
      if (createForm.targetConfigType === 'JSONPATH') {
        syncJsonPathMappings();
      }
    },
    { deep: true },
  );

  watch(
    () => createForm.targetConfigType,
    targetConfigType => {
      if (targetConfigType === 'JSONPATH') {
        syncJsonPathMappings();
      }
    },
  );

  const openCreateDialog = async () => {
    createDialogVisible.value = true;
    if (policies.value.length === 0) {
      await loadPolicies();
    }
  };

  const buildJsonPathTargetConfig = () => {
    const mapping = {};
    for (const column of createForm.targetColumns) {
      const path = (jsonPathMappings[column] || '').trim();
      if (!path) {
        ElMessage.error(`目标列 ${column} 的 JSONPath 不能为空`);
        return null;
      }
      mapping[column] = path;
    }
    return mapping;
  };

  const isRiskyWhereSql = whereSql => {
    if (!whereSql) {
      return false;
    }
    return /\b(drop|delete|truncate|update|alter|create)\b/i.test(whereSql);
  };

  const parseWritebackMapping = () => {
    const raw = (createForm.writebackMappingJson || '').trim();
    if (!raw) {
      return undefined;
    }
    try {
      const parsed = JSON.parse(raw);
      if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
        return parsed;
      }
      ElMessage.error('写回映射必须是 JSON 对象');
      return null;
    } catch (error) {
      ElMessage.error('写回映射 JSON 格式不合法');
      return null;
    }
  };

  const submitCreate = async () => {
    if (
      !createForm.agentId ||
      !createForm.datasourceId ||
      !createForm.tableName ||
      !createForm.policyId
    ) {
      ElMessage.error('请先填写 Agent、数据源、表和策略');
      return;
    }
    if (createForm.pkColumns.length === 0) {
      ElMessage.error('请至少选择一个主键列');
      return;
    }
    if (createForm.targetColumns.length === 0) {
      ElMessage.error('请至少选择一个目标列');
      return;
    }

    let targetConfig;
    if (createForm.targetConfigType === 'JSONPATH') {
      targetConfig = buildJsonPathTargetConfig();
      if (!targetConfig) {
        return;
      }
    }

    const writebackMapping = parseWritebackMapping();
    if (writebackMapping === null) {
      return;
    }

    if (isRiskyWhereSql(createForm.whereSql)) {
      ElMessage.warning('whereSql 包含高风险关键词，请确认仅用于筛选条件。');
    }

    try {
      await cleaningService.createJob({
        agentId: Number(createForm.agentId),
        datasourceId: Number(createForm.datasourceId),
        tableName: createForm.tableName,
        policyId: Number(createForm.policyId),
        pkColumns: createForm.pkColumns,
        targetColumns: createForm.targetColumns,
        targetConfigType: createForm.targetConfigType,
        targetConfig,
        mode: createForm.mode,
        writebackMode: createForm.writebackMode,
        reviewPolicy: createForm.reviewPolicy,
        whereSql: createForm.whereSql || undefined,
        onlineFailClosedEnabled: createForm.onlineFailClosedEnabled ? 1 : 0,
        onlineRequestTokenLimit: Number(createForm.onlineRequestTokenLimit),
        writebackMapping,
        budgetEnabled: createForm.budgetEnabled ? 1 : 0,
        budgetSoftLimit: createForm.budgetSoftLimit,
        budgetHardLimit: createForm.budgetHardLimit,
      });
      createDialogVisible.value = false;
      ElMessage.success('任务创建成功');
      await loadJobs();
    } catch (error) {
      ElMessage.error('创建任务失败，请检查输入');
    }
  };

  const selectJob = async job => {
    selectedJob.value = job;
    await loadRuns();
  };

  const createRun = async job => {
    try {
      await cleaningService.createRun(job.id);
      ElMessage.success('已创建运行实例');
      if (selectedJob.value?.id === job.id) {
        await loadRuns();
      }
    } catch (error) {
      ElMessage.error('创建运行实例失败');
    }
  };

  const pauseRun = async run => {
    try {
      await cleaningService.pauseRun(run.id);
      await loadRuns();
    } catch (error) {
      ElMessage.error('暂停失败');
    }
  };

  const resumeRun = async run => {
    try {
      await cleaningService.resumeRun(run.id);
      await loadRuns();
    } catch (error) {
      ElMessage.error('恢复失败（可能硬预算超限）');
    }
  };

  const cancelRun = async run => {
    try {
      await cleaningService.cancelRun(run.id);
      await loadRuns();
    } catch (error) {
      ElMessage.error('取消失败');
    }
  };

  const createRollback = async run => {
    rollbackLoading.value = true;
    try {
      const result = await cleaningService.createRollback(run.id);
      if (!result) {
        ElMessage.error('创建回滚任务失败');
        return;
      }
      rollbackRun.value = result;
      rollbackDialogVisible.value = true;
      ElMessage.success(`回滚任务已创建：#${result.id}`);
    } catch (error) {
      ElMessage.error('创建回滚任务失败');
    } finally {
      rollbackLoading.value = false;
    }
  };

  const refreshRollback = async () => {
    if (!rollbackRun.value?.id) {
      return;
    }
    rollbackLoading.value = true;
    try {
      rollbackRun.value = await cleaningService.getRollback(rollbackRun.value.id);
    } catch (error) {
      ElMessage.error('刷新回滚状态失败');
    } finally {
      rollbackLoading.value = false;
    }
  };

  const loadBudget = async run => {
    try {
      budgetView.value = await cleaningService.getBudget(run.id);
      budgetDialogVisible.value = true;
    } catch (error) {
      ElMessage.error('加载预算快照失败');
    }
  };

  onMounted(() => {
    loadAll();
  });
</script>

<style scoped>
  .cleaning-job-page {
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

  .header-actions {
    display: flex;
    gap: 0.5rem;
  }

  .panel {
    margin-top: 1rem;
  }

  .panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
  }

  .budget-row {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }

  .jsonpath-panel {
    width: 100%;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 12px;
    background: #f8fafc;
  }

  .jsonpath-toolbar {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 10px;
  }

  .jsonpath-empty {
    color: #64748b;
  }

  .jsonpath-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .jsonpath-item {
    display: grid;
    grid-template-columns: 160px 1fr;
    gap: 10px;
    align-items: center;
  }

  .jsonpath-column {
    color: #0f172a;
    font-weight: 500;
  }

  .where-sql-alert {
    margin-bottom: 12px;
  }
</style>
