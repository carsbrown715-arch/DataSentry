<div align="center">
  <p>中文 | <a href="./README-en.md">English</a></p>
  <h1>DataSentry</h1>
  <p>
    <strong>基于 <a href="https://github.com/alibaba/spring-ai-alibaba" target="_blank">Spring AI Alibaba</a> 的企业级智能数据分析师</strong>
  </p>
  <p>
     Text-to-SQL | Python 深度分析 | 智能报告 | MCP 服务器 | RAG 增强
  </p>

  <p>
    <a href="https://github.com/alibaba/spring-ai-alibaba"><img src="https://img.shields.io/badge/Spring%20AI%20Alibaba-1.1.2.0-blue" alt="Spring AI Alibaba"></a>
    <img src="https://img.shields.io/badge/Spring%20Boot-3.4.8+-green" alt="Spring Boot">
    <img src="https://img.shields.io/badge/Java-17+-orange" alt="Java">
    <img src="https://img.shields.io/badge/License-Apache%202.0-red" alt="License">
    <a href="https://deepwiki.com/TouHouQing/DataSentry"><img src="https://deepwiki.com/badge.svg" alt="Ask DeepWiki"></a>
  </p>

   <p>
    <a href="#-项目简介">项目简介</a> • 
    <a href="#-核心特性">核心特性</a> • 
    <a href="#-快速开始">快速开始</a> • 
    <a href="#-文档导航">文档导航</a> • 
    <a href="#-开源与致谢">开源与致谢</a> • 
    <a href="#-加入社区--贡献">加入社区</a>
  </p>
</div>

<br/>


<br/>

## 📖 项目简介

**DataSentry** 是一个基于 **Spring AI Alibaba Graph** 打造的企业级数据治理与智能分析平台。当前能力覆盖 **Text-to-SQL / Python 分析 / 智能报告**，并正在升级为 **可配置的数据清理与内容安全平台**，用于敏感信息检测与脱敏、垃圾内容识别、数据清理与审计，覆盖 **实时文本输入** 与 **数据库存量批处理** 两类场景。

系统采用高度可扩展的架构设计，**全面兼容 OpenAI 接口规范**的对话模型与 Embedding 模型，并支持**灵活挂载任意向量数据库**。无论是私有化部署还是接入主流大模型服务（如 Qwen, Deepseek），都能轻松适配，为企业提供灵活、可控的数据洞察服务。

同时，本项目原生支持 **MCP (Model Context Protocol)**，可作为 MCP 服务器无缝集成到 Claude Desktop 等支持 MCP 的生态工具中。

## ✨ 核心特性

| 特性 | 说明 |
| :--- | :--- |
| **可配置清理 Pipeline** | 按策略组合检测、脱敏、写回、审计、人审、回滚等动作。 |
| **双输入通道** | 实时文本与数据库批处理两种模式并行支持。 |
| **敏感信息与垃圾治理** | 内置常见 PII 规则 + 可扩展策略，支持误杀白名单与阈值控制。 |
| **写回与回滚** | 允许更新原表，提供备份与回滚策略（可配置）。 |
| **合规与审计** | 默认脱敏、审计记录、权限分权、可选人审链路。 |
| **企业级性能** | 批处理热点隔离、可观测性与限流能力。 |
| **智能数据分析** | StateGraph 驱动的 Text-to-SQL，支持复杂多表与多轮意图理解。 |
| **Python 深度分析** | 内置 Python 执行器，自动生成并执行统计分析与预测代码。 |
| **智能报告生成** | 结果自动汇总为包含 ECharts 的 HTML/Markdown 报告。 |
| **RAG 检索增强** | 语义检索业务元数据与术语库，提升 SQL 生成准确率。 |
| **多模型调度** | 内置模型注册表，支持运行时动态切换不同 LLM/Embedding。 |
| **MCP 服务器** | 遵循 MCP 协议，支持作为 Tool Server 对外提供能力。 |
| **API Key 管理** | 完善的 API Key 生命周期管理与细粒度权限控制。 |

## 🧭 目标能力（规划中）

- **策略驱动**：策略集/规则/绑定三层模型，支持场景化策略管理。
- **动作可配置**：检测仅返回、脱敏返回、写回、软删/硬删、人审后写回。
- **双模备份**：MetaDB 集中式与业务库就近式可选。
- **安全与合规**：全链路脱敏、最小权限、审计可追溯。

### 动作模式（规划中）

- `DETECT_ONLY`：仅检测返回，不写回。
- `SANITIZE_RETURN`：脱敏后返回，不写回。
- `SANITIZE_WRITEBACK`：脱敏后写回业务表。
- `REVIEW_THEN_WRITEBACK`：人审通过后写回。
- `DELETE`：软删/硬删（可配置、需严格备份与权限控制）。

### 输入通道（规划中）

- **实时文本**：API 请求携带文本，低延迟检测/脱敏。
- **数据库批处理**：对存量表按策略扫描、清理、审计与回滚。

## 🚀 快速开始

> 详细的安装和配置指南请参考 [📑 快速开始文档](docs/QUICK_START.md)。

### 1. 准备环境
- JDK 17+
- MySQL 5.7+
- Node.js 16+

### 2. 启动服务

```bash
# 1. 导入数据库
mysql -u root -p < datasentry-management/src/main/resources/sql/schema.sql

# 2. 启动后端
cd datasentry-management
./mvnw spring-boot:run

# 3. 启动前端
cd datasentry-frontend
npm install && npm run dev
```

### 3. 访问系统
打开浏览器访问 `http://localhost:3000`，开始创建您的第一个数据智能体！

## 📚 文档导航

| 文档 | 此文档包含的内容 |
| :--- | :--- |
| [快速开始](docs/QUICK_START.md) | 环境要求、数据库导入、基础配置、系统初体验 |
| [架构设计](docs/ARCHITECTURE.md) | 系统分层架构、StateGraph与工作流设计、核心模块时序图 |
| [开发者指南](docs/DEVELOPER_GUIDE.md) | 开发环境搭建、详细配置手册、代码规范、扩展开发(向量库/模型) |
| [高级功能](docs/ADVANCED_FEATURES.md) | API Key 调用、MCP 服务器配置、自定义混合检索策略、Python执行器配置 |
| [知识配置最佳实践](docs/KNOWLEDGE_USAGE.md) | 语义模型，业务知识，智能体知识的解释和使用 |

## 🧾 开源与致谢

- 本项目为 **DataSentry**，基于开源项目 **DataAgent**（Apache-2.0）二次开发。
- 已保留原始许可与版权声明，并在此基础上进行功能与品牌升级。

## 🤝 加入社区 & 贡献

- **贡献指南**: 欢迎社区贡献！请查阅 [开发者文档](docs/DEVELOPER_GUIDE.md) 了解如何提交 PR。
- **问题反馈**: 如有任何问题或建议，请通过 [GitHub Issues](https://github.com/TouHouQing/DataSentry/issues) 报告。
- **联系方式**: 微信:tohoqing备注DataSentry

## 📄 许可证

本项目采用 Apache License 2.0 许可证。
## Star 历史

[![Star History Chart](https://api.star-history.com/svg?repos=TouHouQing/DataSentry&type=Date)](https://star-history.com/#TouHouQing/DataSentry&Date)

## 贡献者名单

<a href="https://github.com/TouHouQing/DataSentry/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=TouHouQing/DataSentry" />
</a>

---

<div align="center">
    Made with ❤️ by DataSentry Team
</div>
