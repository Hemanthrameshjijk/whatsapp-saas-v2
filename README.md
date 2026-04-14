# 🚀 WhatsApp AI SaaS V2

> **Conversational Commerce Stabilized at V23 Baseline**

A comprehensive, enterprise-grade SaaS platform designed to automate hyper-local eCommerce through the power of Conversational AI. This project integrates the **WhatsApp Cloud API** with a high-performance **Spring Boot** backend and a sleek **Vite/React** dashboard to deliver a seamless shopping experience.

---

## ✨ Core Pillars

### 🤖 Intelligent Conversational Order Engine
- **LLM-Powered Autonomy**: Leverages Ollama/Groq for human-like interaction.
- **Product Discovery**: Dynamic catalog browsing and natural language item search.
- **Automated Checkout**: Full cart management and order placement via chat.
- **Real-Time Tracking**: Instant status updates for customers.

### 📈 Marketing & Growth Intelligence
- **Smart Abandoned Cart Recovery**: Automatically identifies high-intent shoppers and delivers personalized nudges/coupons.
- **Influencer & Attribution Manager**: Generate unique tracking links with deep-link support to WhatsApp products and measure viral acquisition.
- **Segmented Broadcasts**: Launch targeted promotional campaigns to specific customer groups.

### 🛡️ Enterprise Guardrails
- **Pre-emptive Filtering**: Built-in protection against off-topic discussions (Politics, Medical, Legal, Adult, Religion).
- **Competitor Shield**: Prevents LLM hallucinations or mentions of configured competitor brands.
- **Sentiment Awareness**: Detects frustrated customers and escalates to human agents.

### 🖥️ High-Fidelity Dashboard
- **Real-Time ROI Analytics**: Track revenue, order volume, and campaign performance via SSE-powered live feeds.
- **Hybrid Support**: Seamlessly transition between AI automation and manual human takeover.
- **Visual Intelligence**: Chart-driven insights for business growth.

---

## 🛠️ Technical Architecture

### **Backend (Java/Spring Boot)**
- **Framework**: Spring Boot 3.2.5 (Java 21)
- **Database**: PostgreSQL with Flyway Migrations (Stabilized at V23)
- **Security**: JWT-based Authentication & RBAC
- **Inference**: Ollama / Groq API Integration

### **Frontend (Vite/React)**
- **Styling**: Vanilla CSS with modern Glassmorphism aesthetics
- **State Management**: React Hooks + Context API
- **Visuals**: Lucide Icons + Recharts for data visualization
- **Connectivity**: SSE (Server-Sent Events) for real-time dashboard updates

---

## 🚀 Getting Started

### 1. Backend Setup
1. Configure your `.env` with Meta Cloud API tokens and Database credentials.
2. Ensure **PostgreSQL** is running.
3. Use your IDE or Maven to start the application:
   ```bash
   mvn spring-boot:run
   ```

### 2. Frontend Setup
1. Navigate to the `frontend` directory:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

