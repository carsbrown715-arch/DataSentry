import axios from 'axios';
import type { ApiResponse } from './common';

export interface CleaningPolicyRuleItem {
  ruleId: number;
  priority?: number;
}

export interface CleaningPolicyView {
  id: number;
  name: string;
  description?: string;
  enabled?: number;
  defaultAction?: string;
  configJson?: string;
  createdTime?: string;
  updatedTime?: string;
  rules?: CleaningPolicyRuleItem[];
}

export interface CleaningRule {
  id?: number;
  name: string;
  ruleType: string;
  category: string;
  severity?: number;
  enabled?: number;
  configJson?: string;
  createdTime?: string;
  updatedTime?: string;
}

export interface CleaningPolicyRequest {
  name: string;
  description?: string;
  enabled?: number;
  defaultAction?: string;
  config?: Record<string, unknown>;
}

export interface CleaningRuleRequest {
  name: string;
  ruleType: string;
  category: string;
  severity?: number;
  enabled?: number;
  config?: Record<string, unknown>;
}

export interface CleaningReviewTask {
  id: number;
  jobRunId: number;
  agentId: number;
  datasourceId: number;
  tableName: string;
  pkJson: string;
  pkHash: string;
  columnName?: string;
  verdict?: string;
  categoriesJson?: string;
  sanitizedPreview?: string;
  actionSuggested?: string;
  writebackPayloadJson?: string;
  beforeRowJson?: string;
  status: string;
  reviewer?: string;
  reviewReason?: string;
  version: number;
  createdTime?: string;
  updatedTime?: string;
}

export interface CleaningReviewDecisionRequest {
  version: number;
  reason?: string;
  reviewer?: string;
}

export interface CleaningReviewBatchRequest {
  taskIds?: number[];
  jobRunId?: number;
  filter?: string;
  reason?: string;
  reviewer?: string;
}

export interface CleaningReviewBatchResult {
  total: number;
  success: number;
  failed: number;
  conflict: number;
  stale: number;
}

const API_BASE_URL = '/api/datasentry/cleaning';

class CleaningService {
  async listPolicies(): Promise<CleaningPolicyView[]> {
    const response = await axios.get<ApiResponse<CleaningPolicyView[]>>(`${API_BASE_URL}/policies`);
    return response.data.data || [];
  }

  async createPolicy(payload: CleaningPolicyRequest): Promise<CleaningPolicyView | null> {
    const response = await axios.post<ApiResponse<CleaningPolicyView>>(
      `${API_BASE_URL}/policies`,
      payload,
    );
    return response.data.data || null;
  }

  async updatePolicy(
    policyId: number,
    payload: CleaningPolicyRequest,
  ): Promise<CleaningPolicyView | null> {
    const response = await axios.put<ApiResponse<CleaningPolicyView>>(
      `${API_BASE_URL}/policies/${policyId}`,
      payload,
    );
    return response.data.data || null;
  }

  async deletePolicy(policyId: number): Promise<void> {
    await axios.delete<ApiResponse<void>>(`${API_BASE_URL}/policies/${policyId}`);
  }

  async updatePolicyRules(policyId: number, rules: CleaningPolicyRuleItem[]): Promise<void> {
    await axios.put<ApiResponse<void>>(`${API_BASE_URL}/policies/${policyId}/rules`, { rules });
  }

  async listRules(): Promise<CleaningRule[]> {
    const response = await axios.get<ApiResponse<CleaningRule[]>>(`${API_BASE_URL}/rules`);
    return response.data.data || [];
  }

  async createRule(payload: CleaningRuleRequest): Promise<CleaningRule | null> {
    const response = await axios.post<ApiResponse<CleaningRule>>(`${API_BASE_URL}/rules`, payload);
    return response.data.data || null;
  }

  async updateRule(ruleId: number, payload: CleaningRuleRequest): Promise<CleaningRule | null> {
    const response = await axios.put<ApiResponse<CleaningRule>>(
      `${API_BASE_URL}/rules/${ruleId}`,
      payload,
    );
    return response.data.data || null;
  }

  async deleteRule(ruleId: number): Promise<void> {
    await axios.delete<ApiResponse<void>>(`${API_BASE_URL}/rules/${ruleId}`);
  }

  async listReviews(params?: {
    status?: string;
    jobRunId?: number;
    agentId?: number;
  }): Promise<CleaningReviewTask[]> {
    const response = await axios.get<ApiResponse<CleaningReviewTask[]>>(`${API_BASE_URL}/reviews`, {
      params,
    });
    return response.data.data || [];
  }

  async approveReview(
    taskId: number,
    payload: CleaningReviewDecisionRequest,
  ): Promise<CleaningReviewTask | null> {
    const response = await axios.post<ApiResponse<CleaningReviewTask>>(
      `${API_BASE_URL}/reviews/${taskId}/approve`,
      payload,
    );
    return response.data.data || null;
  }

  async rejectReview(
    taskId: number,
    payload: CleaningReviewDecisionRequest,
  ): Promise<CleaningReviewTask | null> {
    const response = await axios.post<ApiResponse<CleaningReviewTask>>(
      `${API_BASE_URL}/reviews/${taskId}/reject`,
      payload,
    );
    return response.data.data || null;
  }

  async batchApprove(
    payload: CleaningReviewBatchRequest,
  ): Promise<CleaningReviewBatchResult | null> {
    const response = await axios.post<ApiResponse<CleaningReviewBatchResult>>(
      `${API_BASE_URL}/reviews/batch-approve`,
      payload,
    );
    return response.data.data || null;
  }

  async batchReject(
    payload: CleaningReviewBatchRequest,
  ): Promise<CleaningReviewBatchResult | null> {
    const response = await axios.post<ApiResponse<CleaningReviewBatchResult>>(
      `${API_BASE_URL}/reviews/batch-reject`,
      payload,
    );
    return response.data.data || null;
  }
}

export default new CleaningService();
