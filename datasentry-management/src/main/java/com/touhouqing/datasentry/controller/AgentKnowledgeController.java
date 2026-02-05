/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.touhouqing.datasentry.controller;

import com.touhouqing.datasentry.vo.PageResult;
import com.touhouqing.datasentry.dto.knowledge.agentknowledge.AgentKnowledgeQueryDTO;
import com.touhouqing.datasentry.dto.knowledge.agentknowledge.CreateKnowledgeDTO;
import com.touhouqing.datasentry.dto.knowledge.agentknowledge.UpdateKnowledgeDTO;
import com.touhouqing.datasentry.service.knowledge.AgentKnowledgeService;
import com.touhouqing.datasentry.vo.AgentKnowledgeVO;
import com.touhouqing.datasentry.vo.ApiResponse;
import com.touhouqing.datasentry.vo.PageResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent Knowledge Management Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/datasentry/agent-knowledge")
@CrossOrigin(origins = "*")
@AllArgsConstructor
public class AgentKnowledgeController {

	private final AgentKnowledgeService agentKnowledgeService;

	/**
	 * Query knowledge details by ID
	 */
	@GetMapping("/{id}")
	public ApiResponse<AgentKnowledgeVO> getKnowledgeById(@PathVariable("id") Long id) {
		try {
			AgentKnowledgeVO knowledge = agentKnowledgeService.getKnowledgeById(id);
			if (knowledge != null) {
				return ApiResponse.success("查询成功", knowledge);
			}
			else {
				return ApiResponse.error("知识不存在");
			}
		}
		catch (Exception e) {
			log.error("查询知识详情失败：{}", e.getMessage());
			return ApiResponse.error("查询知识详情失败：" + e.getMessage());
		}
	}

	/**
	 * Create knowledge,supporting file upload
	 */
	@PostMapping("/create")
	public ApiResponse<AgentKnowledgeVO> createKnowledge(@Valid CreateKnowledgeDTO createKnowledgeDto) {
		AgentKnowledgeVO knowledge = agentKnowledgeService.createKnowledge(createKnowledgeDto);
		return ApiResponse.success("创建知识成功，后台向量存储开始更新，请耐心等待...", knowledge);
	}

	/**
	 * Update knowledge
	 */
	@PutMapping("/{id}")
	public ApiResponse<AgentKnowledgeVO> updateKnowledge(@PathVariable("id") Long id,
			@RequestBody UpdateKnowledgeDTO updateKnowledgeDto) {
		AgentKnowledgeVO knowledge = agentKnowledgeService.updateKnowledge(id, updateKnowledgeDto);
		return ApiResponse.success("更新成功", knowledge);
	}

	@PutMapping("/recall/{id}")
	public ApiResponse<AgentKnowledgeVO> updateRecallStatus(@PathVariable Long id,
			@RequestParam(value = "isRecall") Boolean isRecall) {
		AgentKnowledgeVO agentKnowledgeVO = agentKnowledgeService.updateKnowledgeRecallStatus(id, isRecall);
		return ApiResponse.success("更新成功", agentKnowledgeVO);
	}

	/**
	 * Delete knowledge
	 */
	@DeleteMapping("/{id}")
	public ApiResponse<Boolean> deleteKnowledge(@PathVariable("id") Long id) {
		return agentKnowledgeService.deleteKnowledge(id) ? ApiResponse.success("删除操作已接收，等待后台删除相关资源...")
				: ApiResponse.error("删除失败");
	}

	@PostMapping("/query/page")
	public PageResponse<List<AgentKnowledgeVO>> queryByPage(@Valid @RequestBody AgentKnowledgeQueryDTO queryDTO) {
		try {
			PageResult<AgentKnowledgeVO> pageResult = agentKnowledgeService.queryByConditionsWithPage(queryDTO);
			return PageResponse.success(pageResult.getData(), pageResult.getTotal(), pageResult.getPageNum(),
					pageResult.getPageSize(), pageResult.getTotalPages());
		}
		catch (Exception e) {
			log.error("分页查询知识列表失败：{}", e.getMessage());
			return PageResponse.pageError("分页查询失败：" + e.getMessage());
		}
	}

	@PostMapping("/retry-embedding/{id}")
	public ApiResponse<AgentKnowledgeVO> retryEmbedding(@PathVariable Long id) {
		agentKnowledgeService.retryEmbedding(id);
		return ApiResponse.success("重试向量化操作成功，如果是文件解析需要花费点时间，请耐心等待...");
	}

}
