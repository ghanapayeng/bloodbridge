# 🩸 BloodBridge Project Changes & Knowledge Base

> **Notice for AI Agents & Developers**:  
> This directory (`changes/`) contains a complete, structured history of all architectural decisions, code changes, modules, database entities, APIs, frontend pages, and deployment setups created in BloodBridge till date.  
> Whenever an AI agent or engineer interacts with this codebase, this folder serves as the single source of truth for understanding the full project evolution.

---

## 📂 Documentation Directory Map

| Document | Purpose & Description |
| :--- | :--- |
| **[00_SYSTEM_ARCHITECTURE.md](file:///home/ghanapayeng/Desktop/bloodbridge/changes/00_SYSTEM_ARCHITECTURE.md)** | High-level system design, Spring Boot 4.1.1 stack, Java 25, Security & CSRF model, data flows. |
| **[01_TIMELINE_AND_COMMITS.md](file:///home/ghanapayeng/Desktop/bloodbridge/changes/01_TIMELINE_AND_COMMITS.md)** | Chronological log of all commits (`6fc3f83`, `aef2491`, `77b1897`) and working directory updates. |
| **[02_BACKEND_MODULES_AND_APIS.md](file:///home/ghanapayeng/Desktop/bloodbridge/changes/02_BACKEND_MODULES_AND_APIS.md)** | Deep-dive into all 14 backend packages, 37+ REST endpoints, DTOs, business services, and logic. |
| **[03_DATABASE_SCHEMA_AND_ENTITIES.md](file:///home/ghanapayeng/Desktop/bloodbridge/changes/03_DATABASE_SCHEMA_AND_ENTITIES.md)** | Complete database schema, 14 JPA entities, table structures, relationships, and status enums. |
| **[04_FRONTEND_UI_AND_WORKFLOWS.md](file:///home/ghanapayeng/Desktop/bloodbridge/changes/04_FRONTEND_UI_AND_WORKFLOWS.md)** | Complete frontend breakdown: 11 HTML pages, modular CSS stylesheets, client JS controllers, and WebRTC. |
| **[05_TESTING_AND_VERIFICATION.md](file:///home/ghanapayeng/Desktop/bloodbridge/changes/05_TESTING_AND_VERIFICATION.md)** | Test suite reference covering fraud detection, smart matching, eligibility, and integration testing. |
| **[06_AI_AGENT_MANIFEST.md](file:///home/ghanapayeng/Desktop/bloodbridge/changes/06_AI_AGENT_MANIFEST.md)** | **Quickstart guide for AI agents**: conventions, do's & don'ts, common gotchas, and extension recipes. |

---

## 🚀 Quick Snapshot of the System

- **Application Name**: BloodBridge (Intelligent Emergency Blood Donation & Hospital Network)
- **Live Production URL**: [https://bloodbridge-wzdp.onrender.com](https://bloodbridge-wzdp.onrender.com)
- **Production Infrastructure**:
  - **Hosting**: Render ([bloodbridge-wzdp.onrender.com](https://bloodbridge-wzdp.onrender.com)) running multi-stage Docker container.
  - **Database**: TiDB Cloud (distributed MySQL-compatible cloud cluster).
- **Primary Tech Stack**:
  - **Backend**: Spring Boot 4.1.1, Java 25, Spring Data JPA, Spring Security (Session-based + CSRF), Hibernate ORM.
  - **Database Support**: TiDB Cloud (production) / MySQL 8.x / H2 (in-memory dev/test).
  - **Frontend**: Pure Vanilla JavaScript (ES6+), Semantic HTML5, Custom Design System CSS (no external JS UI framework dependency).
  - **Real-Time Communication**: In-app request-linked chat & WebRTC audio/video signaling relay (`/api/chat`, `/api/call`).
  - **DevOps**: Multi-stage [Dockerfile](file:///home/ghanapayeng/Desktop/bloodbridge/Dockerfile) (Maven 3.9 + Eclipse Temurin 25 JDK -> Temurin 25 JRE).
- **Core Domain Capabilities**:
  1. Smart Donor Matching with Haversine distance & blood compatibility matrices.
  2. Automated 90-day cooldown & medical eligibility validation.
  3. Real-time Emergency Request broadcasts with rule-based fraud detection.
  4. Inter-Hospital Blood Bag Transfer logistics with cold-chain temperature logs.
  5. Hospital Blood Bank Inventory with batch expiry tracking and shortage alarms.
  6. 7-Day AI/Heuristic Blood Demand Forecasting.
  7. Digital verifiable donor certificates with public verification tokens.
  8. Full audit logging and admin telemetry.
