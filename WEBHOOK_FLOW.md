# 🌊 The Brain of the Pipeline: Deep-Dive Decision Logic

This document explores the "Why" and "How" behind every decision made as a message flows through the system.

---

## 1. The Entry Filter (`WebhookController`)
**Decision**: *Is this a message I should process?*

-   **The Logic**: The controller iterates through `entry -> changes -> value`. It specifically looks for a `messages` array.
-   **Outcome**:
    -   **If `messages` exists**: It extracts `from`, `type`, and `text` and continues.
    -   **If `statuses` exists**: (e.g., Message Delivered/Read receipts) The system currently ignores these to save processing power.
    -   **Media Check**: It identifies the `type` (image, audio, document). Currently, it only flows `text` and `location` into the AI; others receive a "Media not supported" reply.

---

## 2. Multi-Tenant Routing (`TenantResolver`)
**Decision**: *Who owns this conversation?*

-   **The Logic**: Every business has a registered WhatsApp Number in the `Business` table.
-   **The Cache Decision**:
    -   First, it checks Redis for `biz:wa:{number}`.
    -   If not in Redis, it queries Postgres.
    -   **Why?** This prevents a single viral business from slowing down the database for all other tenants.

---

## 3. Customer Gatekeeping (`ConversationService`)
**Decision**: *Should I talk to this person or stay silent?*

-   **Blocked Status**: If `customer.isBlocked()` is true, the system returns `void` immediately. No reply is sent, and no AI is called.
-   **Human Handoff**: If `customer.requiresHuman()` is true, the message is **persisted** to the database (so the agent sees it) but the AI pipeline is terminated. This prevents the AI from "fighting" with a human agent.
-   **Business Hours**: Logic checks `LocalTime.now()` against `settings.getOpenTime()` and `getCloseTime()`. If closed, a "Closed" template is sent, and processing stops.

---

## 4. Session State & Stage (`SessionStore`)
**Decision**: *Where are we in the journey?*

When a message arrives, the `ConvStage` determines the AI's behavior:
-   **Stage: GREETING**: If the customer has no name and no history, we force a "May I know your name?" prompt.
-   **Stage: AWAITING_NAME**: If the next message is text, we treat it as a name extraction task, update the DB, and move to `BROWSING`.
-   **Stage: BROWSING**: The standard AI state where tool-calling is enabled.
-   **Stage: ORDER_CONFIRMED**: A transient state after a checkout, usually triggers a summary or a "thank you" logic.

---

## 5. Intent Classification (`GuardrailService`)
**Decision**: *Is this a valid shopping query?*

The system performs a **Keyword + Regex** scan:
-   **INTENT_MAP**:
    -   `ADULT`, `POLITICS`, `MEDICAL`: High-priority blocks.
    -   `ORDER_STATUS`: Triggers specific order lookup logic.
    -   `COMPLAINT`: Triggers human agent or support ticket tools.
-   **The "Word Boundary" Decision**: To prevent false positives (e.g., blocking the brand "Temple" because of "religion" keywords), it uses `\bkeyword\b` regex to ensure only exact matches trigger the guardrail.

---

## 6. Response Generation (`ModelGateway`)
**Decision**: *Should I act (Tool) or speak (Text)?*

The LLM (Claude/Llama) receives the full context and reaches a decision:
-   **Tool Call**: If the AI sees `ToolDefinition`, it evaluates if the user's intent matches one (e.g. `view_cart`). It returns a JSON object with function arguments.
-   **Text Content**: If no tool is exact, it generates a natural language reply.
-   **Decision Tie-break**: The system prompt instructs the AI: *"Always prioritize actions (adding to cart, checking status) over just talking about them."*

---

## 7. Persistence & Delivery (`ChatPersistenceService`)
**Decision**: *How do we ensure the message isn't lost?*

-   **The Async Problem**: Since the pipeline is `@Async`, a standard `@Transactional` won't work if called within the same class (Proxy bypass).
-   **The Solution**: We call a **separate bean** (`ChatPersistenceService`) which opens its own transaction. It saves both the `IN` and `OUT` messages atomically.
-   **SSE Notification**: Every successful reply triggers an SSE push. The Dashboard UI decides whether to display it based on the `businessId` of the logged-in user.

---

## 8. Memory Management (`SummarisationService`)
**Decision**: *Is the history getting too long?*

-   **Trigger**: Every 5 messages, the system evaluates the token count.
-   **Summarisation**: It asks a small model (Llama) to "condense everything above into 3 bullet points."
-   **Outcome**: The 20-message window is shifted, and the summary is injected into future prompts to maintain context without hitting token limits.
