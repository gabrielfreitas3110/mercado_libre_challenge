# 📦 Distributed Inventory Management Optimization

## 📌 Project Overview
This project is a **prototype improvement of an existing inventory management system**, designed to operate in a **distributed environment**.

The main goals are:
- ✅ Optimize **inventory consistency** across multiple stores
- ⚡ Reduce **update latency** for stock synchronization
- 💰 Lower **operational costs** while maintaining **security** and **observability**

The system simulates a retail chain scenario where each store has its own local database that synchronizes with a central database. In the current legacy system, inconsistencies and delays (15-minute syncs) cause poor user experience and sales losses.

This prototype proposes and implements a **distributed backend architecture** and **inventory API** to address these issues.

---

## 🚀 Features
- RESTful API with core **inventory operations** (`GET`, `POST`, `PUT`, `DELETE`)
- Simulated **data persistence** using in-memory storage or JSON/CSV files
- Basic **fault-tolerance mechanisms**
- Handling of **concurrent updates** with consistency strategies
- Clear **API documentation** via Swagger/OpenAPI
- Support for future deployment in **AWS (cloud-native architecture)**

---

## 🛠️ Tech Stack
- **Backend:** Java (Spring Boot) or Node.js (Express/NestJS)
- **Database (simulation):** H2 / SQLite / Local JSON files
- **Testing:** JUnit (Java) or Jest (Node.js)
- **API Documentation:** Swagger / OpenAPI

### ☁️ Optional AWS Services (cloud prototype)
- **API Gateway** → expose REST APIs
- **Lambda / ECS Fargate** → serverless or containerized backend
- **DynamoDB / Aurora Serverless** → low-latency distributed database
- **SQS / Kafka (MSK)** → asynchronous updates and event processing
- **CloudWatch & X-Ray** → monitoring and observability

---

## 📂 Project Structure
```bash
.
├── src/               # Source code
├── tests/             # Unit tests
├── data/              # JSON/CSV sample inventory files
├── run.md             # How to execute locally
├── prompts.md         # AI prompts used (if applicable)
└── README.md          # Project documentation
