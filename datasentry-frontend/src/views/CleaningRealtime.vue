<template>
  <BaseLayout>
    <div class="cleaning-realtime-page">
      <main class="main-content">
        <div class="content-header">
          <div class="header-info">
            <h1 class="content-title">清理实时调试</h1>
            <p class="content-subtitle">调用 check/sanitize 接口做在线链路联调与验收</p>
          </div>
          <div class="header-actions">
            <el-button :icon="Refresh" :loading="loadingAgents" @click="loadAgents">
              刷新 Agent
            </el-button>
          </div>
        </div>

        <el-alert
          type="warning"
          show-icon
          :closable="false"
          title="API Key 仅建议在可信本机环境使用，演示时请关闭投屏或隐藏敏感信息。"
          class="security-alert"
        />

        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel-header">
              <span>请求参数</span>
            </div>
          </template>

          <el-form :model="form" label-width="120px">
            <el-form-item label="Agent">
              <el-select
                v-model="form.agentId"
                filterable
                clearable
                placeholder="请选择 Agent"
                style="width: 100%"
              >
                <el-option
                  v-for="agent in agents"
                  :key="agent.id"
                  :label="agent.name || `Agent-${agent.id}`"
                  :value="agent.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="API Key">
              <el-input
                v-model="form.apiKey"
                type="password"
                show-password
                autocomplete="on"
                placeholder="请输入 Agent API Key"
              />
            </el-form-item>

            <el-form-item label="本机记住 Key">
              <el-switch v-model="form.rememberApiKey" @change="handleRememberChange" />
            </el-form-item>

            <el-form-item label="调用类型">
              <el-radio-group v-model="form.action">
                <el-radio-button label="check">check</el-radio-button>
                <el-radio-button label="sanitize">sanitize</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="Scene">
              <el-input v-model="form.scene" placeholder="可选：业务场景标识" />
            </el-form-item>

            <el-form-item label="Policy ID">
              <el-input-number v-model="form.policyId" :min="1" :step="1" />
            </el-form-item>

            <el-form-item label="Text">
              <el-input
                v-model="form.text"
                type="textarea"
                :autosize="{ minRows: 6, maxRows: 12 }"
                placeholder="请输入待检测文本"
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="submitRequest">
                执行 {{ form.action }}
              </el-button>
              <el-button @click="clearResult">清空结果</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel-header">
              <span>响应结果</span>
            </div>
          </template>

          <el-empty v-if="!responseData && !responseError" description="暂无结果" />

          <template v-else>
            <el-alert
              v-if="responseError"
              type="error"
              show-icon
              :closable="false"
              :title="responseError"
              class="response-error"
            />

            <el-descriptions v-if="responseData" :column="1" border>
              <el-descriptions-item label="Verdict">
                {{ responseData.verdict || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="Categories">
                <span v-if="!responseData.categories || responseData.categories.length === 0">
                  -
                </span>
                <template v-else>
                  <el-tag
                    v-for="category in responseData.categories"
                    :key="category"
                    type="warning"
                    class="category-tag"
                  >
                    {{ category }}
                  </el-tag>
                </template>
              </el-descriptions-item>
              <el-descriptions-item label="Sanitized Text">
                <pre class="result-text">{{ responseData.sanitizedText || '-' }}</pre>
              </el-descriptions-item>
            </el-descriptions>

            <div v-if="responseRaw" class="raw-panel">
              <h4>Raw JSON</h4>
              <pre class="result-text">{{ responseRaw }}</pre>
            </div>
          </template>
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
  import agentService from '@/services/agent';
  import cleaningService from '@/services/cleaning';

  const STORAGE_KEY = 'datasentry.cleaning.realtime.api_key';

  const loadingAgents = ref(false);
  const submitting = ref(false);
  const agents = ref([]);

  const responseData = ref(null);
  const responseRaw = ref('');
  const responseError = ref('');

  const form = reactive({
    agentId: undefined,
    apiKey: '',
    rememberApiKey: false,
    action: 'check',
    text: '',
    scene: '',
    policyId: undefined,
  });

  const loadAgents = async () => {
    loadingAgents.value = true;
    try {
      agents.value = await agentService.list();
    } catch (error) {
      agents.value = [];
      ElMessage.error('加载 Agent 失败');
    } finally {
      loadingAgents.value = false;
    }
  };

  const clearResult = () => {
    responseData.value = null;
    responseRaw.value = '';
    responseError.value = '';
  };

  const handleRememberChange = enabled => {
    if (!enabled) {
      localStorage.removeItem(STORAGE_KEY);
    } else if (form.apiKey) {
      localStorage.setItem(STORAGE_KEY, form.apiKey);
    }
  };

  const buildRequestPayload = () => {
    const payload = {
      text: form.text,
    };
    if (form.scene) {
      payload.scene = form.scene;
    }
    if (form.policyId) {
      payload.policyId = Number(form.policyId);
    }
    return payload;
  };

  const extractErrorMessage = error => {
    const message = error?.response?.data?.message || error?.message || '请求失败';
    if (error?.response?.status === 401) {
      return '鉴权失败：请检查 API Key 是否正确且 Agent 已启用 API Key。';
    }
    if (error?.response?.status === 404) {
      return 'Agent 不存在，请检查 Agent ID。';
    }
    if (error?.response?.status === 503) {
      return '清理功能已关闭，请联系管理员开启。';
    }
    return `请求失败：${message}`;
  };

  const submitRequest = async () => {
    if (!form.agentId) {
      ElMessage.error('请选择 Agent');
      return;
    }
    if (!form.apiKey) {
      ElMessage.error('请输入 API Key');
      return;
    }
    if (!form.text.trim()) {
      ElMessage.error('请输入待检测文本');
      return;
    }

    submitting.value = true;
    responseError.value = '';
    try {
      const payload = buildRequestPayload();
      const data =
        form.action === 'sanitize'
          ? await cleaningService.sanitize(Number(form.agentId), form.apiKey, payload)
          : await cleaningService.check(Number(form.agentId), form.apiKey, payload);

      responseData.value = data;
      responseRaw.value = JSON.stringify(data || {}, null, 2);
      if (form.rememberApiKey) {
        localStorage.setItem(STORAGE_KEY, form.apiKey);
      }
      ElMessage.success('调用成功');
    } catch (error) {
      responseData.value = null;
      responseRaw.value = '';
      responseError.value = extractErrorMessage(error);
      ElMessage.error('调用失败');
    } finally {
      submitting.value = false;
    }
  };

  onMounted(async () => {
    const cachedKey = localStorage.getItem(STORAGE_KEY);
    if (cachedKey) {
      form.apiKey = cachedKey;
      form.rememberApiKey = true;
    }
    await loadAgents();
  });
</script>

<style scoped>
  .cleaning-realtime-page {
    min-height: 100vh;
    padding: 2rem;
  }

  .main-content {
    max-width: 1100px;
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

  .security-alert {
    margin-bottom: 1rem;
  }

  .panel {
    margin-bottom: 1rem;
  }

  .panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
  }

  .category-tag {
    margin-right: 0.4rem;
    margin-bottom: 0.3rem;
  }

  .raw-panel {
    margin-top: 1rem;
  }

  .result-text {
    margin: 0;
    white-space: pre-wrap;
    word-break: break-word;
    font-family: Menlo, Consolas, Monaco, monospace;
    font-size: 13px;
    line-height: 1.5;
  }

  .response-error {
    margin-bottom: 1rem;
  }
</style>
