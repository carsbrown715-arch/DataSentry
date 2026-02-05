<div align="center">
  <p><a href="./README.md">中文</a> | English</p>
  <h1>DataSentry</h1>
  <p>
    <strong>Enterprise-grade Intelligent Data Analyst powered by <a href="https://github.com/alibaba/spring-ai-alibaba" target="_blank">Spring AI Alibaba</a></strong>
  </p>
  <p>
     Text-to-SQL | Python Deep Analysis | Intelligent Reports | MCP Server | RAG Enhancement
  </p>

  <p>
    <a href="https://github.com/alibaba/spring-ai-alibaba"><img src="https://img.shields.io/badge/Spring%20AI%20Alibaba-1.1.0.0-blue" alt="Spring AI Alibaba"></a>
    <img src="https://img.shields.io/badge/Spring%20Boot-3.4.8+-green" alt="Spring Boot">
    <img src="https://img.shields.io/badge/Java-17+-orange" alt="Java">
    <img src="https://img.shields.io/badge/License-Apache%202.0-red" alt="License">
    <a href="https://deepwiki.com/TouHouQing/DataSentry"><img src="https://deepwiki.com/badge.svg" alt="Ask DeepWiki"></a>
  </p>

   <p>
    <a href="#-introduction">Introduction</a> •
    <a href="#-core-features">Core Features</a> •
    <a href="#-quick-start">Quick Start</a> •
    <a href="#-documentation">Documentation</a> •
    <a href="#-open-source--attribution">Open Source</a> •
    <a href="#-community--contribution">Community</a>
  </p>
</div>

<br/>

<br/>

## Introduction

**DataSentry** is an enterprise-grade data governance and intelligent analysis platform built on **Spring AI Alibaba Graph**. It currently delivers **Text-to-SQL / Python analysis / intelligent reporting**, and is being upgraded into a **configurable data cleaning and content safety platform** for sensitive data detection and masking, spam/content cleanup, and auditable write-back — covering **real-time text input** and **database batch processing**.

The system adopts a highly extensible architecture design, **fully compatible with OpenAI API specifications** for chat models and embedding models, and supports **flexible integration with any vector database**. Whether for private deployment or integration with mainstream LLM services (such as Qwen, Deepseek), it can be easily adapted to provide flexible and controllable data insight services for enterprises.

Additionally, this project natively supports **MCP (Model Context Protocol)**, enabling seamless integration as an MCP server into MCP-compatible ecosystem tools such as Claude Desktop.

## Core Features

| Feature | Description |
| :--- | :--- |
| **Configurable Cleaning Pipeline** | Compose detection, masking, write-back, audit, review, and rollback actions by policy. |
| **Dual Input Channels** | Support both real-time text and database batch processing. |
| **Sensitive & Spam Governance** | Built-in PII rules with extendable policies, allowlist and threshold control to reduce false positives. |
| **Write-back & Rollback** | Update source tables with backup and rollback strategies (configurable). |
| **Compliance & Audit** | End-to-end masking, audit trails, delegated permissions, optional human review. |
| **Enterprise Performance** | Batch hot-spot isolation, observability, and throttling. |
| **Intelligent Data Analysis** | StateGraph-based Text-to-SQL for complex multi-table and multi-turn queries. |
| **Python Deep Analysis** | Built-in Python executor for automated statistical analysis and prediction. |
| **Intelligent Report Generation** | Auto-generated HTML/Markdown reports with ECharts visuals. |
| **RAG Retrieval Enhancement** | Semantic retrieval on business metadata and terminology to improve SQL accuracy. |
| **Multi-Model Orchestration** | Runtime switching between LLM and Embedding models. |
| **MCP Server** | MCP-compliant tool server capabilities. |
| **API Key Management** | Fine-grained API Key lifecycle and permission control. |

## Roadmap (Planned)

- **Policy-driven governance**: policy set / rule / binding model for scenario-based management.
- **Action configurability**: detect-only, mask-return, write-back, soft/hard delete, review-then-writeback.
- **Dual-mode backup**: MetaDB centralized vs business DB co-located backup.
- **Security & compliance**: masking by default, least privilege, auditable changes.

### Action Modes (Planned)

- `DETECT_ONLY`: detect and return only, no write-back.
- `SANITIZE_RETURN`: mask and return, no write-back.
- `SANITIZE_WRITEBACK`: mask then write back to the source table.
- `REVIEW_THEN_WRITEBACK`: human review before write-back.
- `DELETE`: soft/hard delete (configurable, strict backup and permission control).

### Input Channels (Planned)

- **Real-time text**: API-based low-latency detection/masking.
- **Database batch**: scan existing tables with policy-based cleanup, audit, and rollback.

## Quick Start

> For detailed installation and configuration guide, please refer to [Quick Start Guide](docs/QUICK_START.md).

### 1. Prerequisites
- JDK 17+
- MySQL 5.7+
- Node.js 16+

### 2. Start Services

```bash
# 1. Import database
mysql -u root -p < datasentry-management/src/main/resources/sql/schema.sql

# 2. Start backend
cd datasentry-management
./mvnw spring-boot:run

# 3. Start frontend
cd datasentry-frontend
npm install && npm run dev
```

### 3. Access the System
Open your browser and visit `http://localhost:3000` to start creating your first DataSentry agent!

## Documentation

| Document | Contents |
| :--- | :--- |
| [Quick Start](docs/QUICK_START.md) | Environment requirements, database import, basic configuration, getting started |
| [Architecture Design](docs/ARCHITECTURE.md) | System layered architecture, StateGraph and workflow design, core module sequence diagrams |
| [Developer Guide](docs/DEVELOPER_GUIDE.md) | Development environment setup, detailed configuration manual, coding standards, extension development (vector DB/models) |
| [Advanced Features](docs/ADVANCED_FEATURES.md) | API Key invocation, MCP server configuration, custom hybrid retrieval strategies, Python executor configuration |
| [Knowledge Configuration Best Practices](docs/KNOWLEDGE_USAGE.md) | Explanation and usage of semantic models, business knowledge, and agent knowledge |

## Open Source & Attribution

- **DataSentry** is a derivative work based on **DataAgent** (Apache-2.0).
- We keep the original license and notices, and build new features on top of it.

## Community & Contribution

- **Contribution Guide**: Contributions from the community are welcome! Please refer to the [Developer Documentation](docs/DEVELOPER_GUIDE.md) to learn how to submit a PR.
- **Issue Feedback**: If you have any questions or suggestions, please report them via [GitHub Issues](https://github.com/TouHouQing/DataSentry/issues).
- **Contact**: WeChat: tohoqing (please note "DataSentry" when adding).

## License

This project is licensed under the Apache License 2.0.

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=TouHouQing/DataSentry&type=Date)](https://star-history.com/#TouHouQing/DataSentry&Date)

## Contributors

<a href="https://github.com/TouHouQing/DataSentry/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=TouHouQing/DataSentry" />
</a>

---

<div align="center">
    Made with ❤️ by DataSentry Team
</div>
