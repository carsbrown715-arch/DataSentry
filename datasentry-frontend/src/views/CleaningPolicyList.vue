<template>
  <BaseLayout>
    <div class="cleaning-policy-page">
      <main class="main-content">
        <div class="content-header">
          <div class="header-info">
            <h1 class="content-title">清理策略配置</h1>
            <p class="content-subtitle">管理清理策略、规则与优先级绑定</p>
          </div>
          <div class="header-actions">
            <el-button type="primary" :icon="Plus" size="large" @click="openPolicyDialog()">
              新增策略
            </el-button>
            <el-button :icon="Refresh" size="large" @click="loadAll">刷新</el-button>
          </div>
        </div>

        <el-tabs v-model="activeTab" class="content-tabs">
          <el-tab-pane label="策略" name="policies">
            <el-card>
              <el-table :data="policies" style="width: 100%" stripe v-loading="loadingPolicies">
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="name" label="策略名称" min-width="160" />
                <el-table-column prop="defaultAction" label="默认动作" width="160" />
                <el-table-column label="规则数" width="100">
                  <template #default="scope">
                    {{ scope.row.rules?.length || 0 }}
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="100">
                  <template #default="scope">
                    <el-tag :type="scope.row.enabled ? 'success' : 'info'" size="small">
                      {{ scope.row.enabled ? '启用' : '停用' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="240" fixed="right">
                  <template #default="scope">
                    <el-button size="small" type="primary" @click="openPolicyDialog(scope.row)">
                      编辑
                    </el-button>
                    <el-button size="small" type="danger" @click="deletePolicy(scope.row)">
                      删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-tab-pane>
          <el-tab-pane label="规则" name="rules">
            <el-card>
              <div class="rule-toolbar">
                <el-button type="primary" :icon="Plus" size="large" @click="openRuleDialog()">
                  新增规则
                </el-button>
              </div>
              <el-table :data="rules" style="width: 100%" stripe v-loading="loadingRules">
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="name" label="规则名称" min-width="160" />
                <el-table-column prop="ruleType" label="类型" width="120" />
                <el-table-column prop="category" label="类别" width="140" />
                <el-table-column prop="severity" label="严重度" width="120" />
                <el-table-column label="状态" width="100">
                  <template #default="scope">
                    <el-tag :type="scope.row.enabled ? 'success' : 'info'" size="small">
                      {{ scope.row.enabled ? '启用' : '停用' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="240" fixed="right">
                  <template #default="scope">
                    <el-button size="small" type="primary" @click="openRuleDialog(scope.row)">
                      编辑
                    </el-button>
                    <el-button size="small" type="danger" @click="deleteRule(scope.row)">
                      删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-tab-pane>
        </el-tabs>
      </main>

      <el-dialog
        v-model="policyDialogVisible"
        :title="policyDialogTitle"
        width="820px"
        :close-on-click-modal="false"
      >
        <el-form :model="policyForm" label-width="110px" label-position="left">
          <el-form-item label="策略名称">
            <el-input v-model="policyForm.name" placeholder="请输入策略名称" />
          </el-form-item>
          <el-form-item label="策略描述">
            <el-input v-model="policyForm.description" placeholder="请输入描述" />
          </el-form-item>
          <el-form-item label="默认动作">
            <el-select v-model="policyForm.defaultAction" placeholder="请选择">
              <el-option label="DETECT_ONLY" value="DETECT_ONLY" />
              <el-option label="SANITIZE_RETURN" value="SANITIZE_RETURN" />
              <el-option label="SANITIZE_WRITEBACK" value="SANITIZE_WRITEBACK" />
              <el-option label="REVIEW_THEN_WRITEBACK" value="REVIEW_THEN_WRITEBACK" />
              <el-option label="DELETE" value="DELETE" />
            </el-select>
          </el-form-item>
          <el-form-item label="启用状态">
            <el-switch v-model="policyForm.enabled" />
          </el-form-item>
          <el-divider>策略阈值配置</el-divider>
          <div class="threshold-row">
            <el-form-item label="Block 阈值">
              <el-input-number v-model="policyForm.blockThreshold" :min="0" :max="1" :step="0.05" />
            </el-form-item>
            <el-form-item label="Review 阈值">
              <el-input-number
                v-model="policyForm.reviewThreshold"
                :min="0"
                :max="1"
                :step="0.05"
              />
            </el-form-item>
            <el-form-item label="L3 启用">
              <el-switch v-model="policyForm.llmEnabled" />
            </el-form-item>
            <el-form-item label="L2 阈值">
              <el-input-number v-model="policyForm.l2Threshold" :min="0" :max="1" :step="0.05" />
            </el-form-item>
            <el-form-item label="Shadow 启用">
              <el-switch v-model="policyForm.shadowEnabled" />
            </el-form-item>
            <el-form-item label="Shadow 采样">
              <el-input-number
                v-model="policyForm.shadowSampleRatio"
                :min="0"
                :max="1"
                :step="0.05"
              />
            </el-form-item>
          </div>
          <el-divider>规则绑定与优先级</el-divider>
          <el-table :data="ruleSelections" style="width: 100%" height="280">
            <el-table-column width="60">
              <template #default="scope">
                <el-checkbox v-model="scope.row.selected" />
              </template>
            </el-table-column>
            <el-table-column prop="name" label="规则名称" min-width="160" />
            <el-table-column prop="ruleType" label="类型" width="120" />
            <el-table-column prop="category" label="类别" width="140" />
            <el-table-column label="优先级" width="140">
              <template #default="scope">
                <el-input-number
                  v-model="scope.row.priority"
                  :min="0"
                  :max="100"
                  :step="1"
                  size="small"
                  :disabled="!scope.row.selected"
                />
              </template>
            </el-table-column>
          </el-table>
        </el-form>
        <template #footer>
          <el-button @click="policyDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="savePolicy">保存</el-button>
        </template>
      </el-dialog>

      <el-dialog
        v-model="ruleDialogVisible"
        :title="ruleDialogTitle"
        width="640px"
        :close-on-click-modal="false"
      >
        <el-form :model="ruleForm" label-width="110px" label-position="left">
          <el-form-item label="规则名称">
            <el-input v-model="ruleForm.name" placeholder="请输入规则名称" />
          </el-form-item>
          <el-form-item label="规则类型">
            <el-select v-model="ruleForm.ruleType" placeholder="请选择">
              <el-option label="REGEX" value="REGEX" />
              <el-option label="LLM" value="LLM" />
            </el-select>
          </el-form-item>
          <el-form-item label="规则类别">
            <el-input v-model="ruleForm.category" placeholder="PII/SPAM/..." />
          </el-form-item>
          <el-form-item label="严重度">
            <el-input-number v-model="ruleForm.severity" :min="0" :max="1" :step="0.05" />
          </el-form-item>
          <el-form-item label="启用状态">
            <el-switch v-model="ruleForm.enabled" />
          </el-form-item>
          <el-form-item label="配置 JSON">
            <el-input
              v-model="ruleForm.configJson"
              type="textarea"
              :rows="5"
              placeholder='例如: {"pattern":"\\\\d{11}","flags":"g"}'
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="ruleDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveRule">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </BaseLayout>
</template>

<script setup>
  import { onMounted, reactive, ref } from 'vue';
  import { ElMessage, ElMessageBox } from 'element-plus';
  import { Plus, Refresh } from '@element-plus/icons-vue';
  import cleaningService from '@/services/cleaning';
  import BaseLayout from '@/layouts/BaseLayout.vue';

  const activeTab = ref('policies');
  const policies = ref([]);
  const rules = ref([]);
  const loadingPolicies = ref(false);
  const loadingRules = ref(false);

  const policyDialogVisible = ref(false);
  const policyDialogTitle = ref('新增策略');
  const policyForm = reactive({
    id: null,
    name: '',
    description: '',
    defaultAction: 'DETECT_ONLY',
    enabled: true,
    blockThreshold: 0.7,
    reviewThreshold: 0.4,
    llmEnabled: true,
    l2Threshold: 0.6,
    shadowEnabled: false,
    shadowSampleRatio: 0,
  });
  const ruleSelections = ref([]);

  const ruleDialogVisible = ref(false);
  const ruleDialogTitle = ref('新增规则');
  const ruleForm = reactive({
    id: null,
    name: '',
    ruleType: 'REGEX',
    category: '',
    severity: 0.8,
    enabled: true,
    configJson: '',
  });

  const loadPolicies = async () => {
    loadingPolicies.value = true;
    try {
      policies.value = await cleaningService.listPolicies();
    } catch (error) {
      ElMessage.error('加载策略失败');
    } finally {
      loadingPolicies.value = false;
    }
  };

  const loadRules = async () => {
    loadingRules.value = true;
    try {
      rules.value = await cleaningService.listRules();
    } catch (error) {
      ElMessage.error('加载规则失败');
    } finally {
      loadingRules.value = false;
    }
  };

  const loadAll = async () => {
    await Promise.all([loadPolicies(), loadRules()]);
  };

  const parseJsonSafe = value => {
    if (!value) {
      return {};
    }
    try {
      return JSON.parse(value);
    } catch (error) {
      return {};
    }
  };

  const openPolicyDialog = policy => {
    const isEdit = !!policy;
    policyDialogTitle.value = isEdit ? '编辑策略' : '新增策略';
    policyForm.id = isEdit ? policy.id : null;
    policyForm.name = isEdit ? policy.name : '';
    policyForm.description = isEdit ? policy.description || '' : '';
    policyForm.defaultAction = isEdit ? policy.defaultAction || 'DETECT_ONLY' : 'DETECT_ONLY';
    policyForm.enabled = isEdit ? !!policy.enabled : true;
    const config = isEdit ? parseJsonSafe(policy.configJson) : {};
    policyForm.blockThreshold = config.blockThreshold ?? 0.7;
    policyForm.reviewThreshold = config.reviewThreshold ?? 0.4;
    policyForm.llmEnabled = config.llmEnabled ?? true;
    policyForm.l2Threshold = config.l2Threshold ?? 0.6;
    policyForm.shadowEnabled = config.shadowEnabled ?? false;
    policyForm.shadowSampleRatio = config.shadowSampleRatio ?? 0;
    ruleSelections.value = rules.value.map(rule => {
      const binding = policy?.rules?.find(item => item.ruleId === rule.id);
      return {
        ruleId: rule.id,
        name: rule.name,
        ruleType: rule.ruleType,
        category: rule.category,
        selected: !!binding,
        priority: binding?.priority ?? 0,
      };
    });
    policyDialogVisible.value = true;
  };

  const savePolicy = async () => {
    if (!policyForm.name) {
      ElMessage.warning('请输入策略名称');
      return;
    }
    try {
      const payload = {
        name: policyForm.name,
        description: policyForm.description,
        enabled: policyForm.enabled ? 1 : 0,
        defaultAction: policyForm.defaultAction,
        config: {
          blockThreshold: policyForm.blockThreshold,
          reviewThreshold: policyForm.reviewThreshold,
          llmEnabled: policyForm.llmEnabled,
          l2Threshold: policyForm.l2Threshold,
          shadowEnabled: policyForm.shadowEnabled,
          shadowSampleRatio: policyForm.shadowSampleRatio,
        },
      };
      let policyId = policyForm.id;
      if (policyId) {
        await cleaningService.updatePolicy(policyId, payload);
      } else {
        const created = await cleaningService.createPolicy(payload);
        policyId = created?.id;
      }
      if (policyId) {
        const bindings = ruleSelections.value
          .filter(item => item.selected)
          .map(item => ({ ruleId: item.ruleId, priority: item.priority }));
        await cleaningService.updatePolicyRules(policyId, bindings);
      }
      policyDialogVisible.value = false;
      await loadPolicies();
      ElMessage.success('策略已保存');
    } catch (error) {
      ElMessage.error('保存策略失败');
    }
  };

  const deletePolicy = async policy => {
    try {
      await ElMessageBox.confirm(`确认删除策略「${policy.name}」?`, '删除确认', {
        type: 'warning',
      });
      await cleaningService.deletePolicy(policy.id);
      await loadPolicies();
      ElMessage.success('策略已删除');
    } catch (error) {
      if (error !== 'cancel' && error !== 'close') {
        ElMessage.error('删除策略失败');
      }
    }
  };

  const openRuleDialog = rule => {
    const isEdit = !!rule;
    ruleDialogTitle.value = isEdit ? '编辑规则' : '新增规则';
    ruleForm.id = isEdit ? rule.id : null;
    ruleForm.name = isEdit ? rule.name : '';
    ruleForm.ruleType = isEdit ? rule.ruleType : 'REGEX';
    ruleForm.category = isEdit ? rule.category : '';
    ruleForm.severity = isEdit ? (rule.severity ?? 0.8) : 0.8;
    ruleForm.enabled = isEdit ? !!rule.enabled : true;
    ruleForm.configJson = isEdit ? rule.configJson || '' : '';
    ruleDialogVisible.value = true;
  };

  const saveRule = async () => {
    if (!ruleForm.name || !ruleForm.ruleType || !ruleForm.category) {
      ElMessage.warning('请填写完整规则信息');
      return;
    }
    let config = {};
    if (ruleForm.configJson) {
      try {
        config = JSON.parse(ruleForm.configJson);
      } catch (error) {
        ElMessage.error('配置 JSON 格式不正确');
        return;
      }
    }
    const payload = {
      name: ruleForm.name,
      ruleType: ruleForm.ruleType,
      category: ruleForm.category,
      severity: ruleForm.severity,
      enabled: ruleForm.enabled ? 1 : 0,
      config,
    };
    try {
      if (ruleForm.id) {
        await cleaningService.updateRule(ruleForm.id, payload);
      } else {
        await cleaningService.createRule(payload);
      }
      ruleDialogVisible.value = false;
      await loadRules();
      await loadPolicies();
      ElMessage.success('规则已保存');
    } catch (error) {
      ElMessage.error('保存规则失败');
    }
  };

  const deleteRule = async rule => {
    try {
      await ElMessageBox.confirm(`确认删除规则「${rule.name}」?`, '删除确认', {
        type: 'warning',
      });
      await cleaningService.deleteRule(rule.id);
      await loadRules();
      await loadPolicies();
      ElMessage.success('规则已删除');
    } catch (error) {
      if (error !== 'cancel' && error !== 'close') {
        ElMessage.error('删除规则失败');
      }
    }
  };

  onMounted(() => {
    loadAll();
  });
</script>

<style scoped>
  .cleaning-policy-page {
    min-height: 100vh;
    padding: 2rem;
  }

  .main-content {
    max-width: 1200px;
    margin: 0 auto;
  }

  .content-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1.5rem;
  }

  .content-title {
    font-size: 1.75rem;
    font-weight: 600;
    color: #0f172a;
    margin: 0;
  }

  .content-subtitle {
    margin-top: 0.5rem;
    color: #64748b;
  }

  .header-actions {
    display: flex;
    gap: 0.75rem;
  }

  .content-tabs {
    background: white;
    border-radius: 12px;
    padding: 1rem;
  }

  .threshold-row {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 1rem;
    margin-bottom: 1rem;
  }

  .rule-toolbar {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 1rem;
  }
</style>
