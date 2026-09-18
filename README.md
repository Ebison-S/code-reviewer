# Lens AI Reviewer

An event-driven AI code reviewer that automatically analyzes GitHub Pull Requests. Built with Java Spring Boot, Kafka, and Python, it processes webhooks, evaluates code diffs using an LLM, and securely posts actionable Markdown feedback directly to your PRs via a GitHub App integration.

## 🏗️ System Architecture

1. **Webhook Reception:** A GitHub App sends a webhook payload to the Spring Boot API Gateway whenever a Pull Request is opened or updated (`synchronize`).
2. **Message Broker (Kafka):** The Gateway validates the payload and publishes the PR metadata to the `pr-events` Kafka topic.
3. **AI Analysis:** A Python worker consumes the event, retrieves the code diff, and uses LangChain to evaluate the changes for bugs, edge cases, and optimizations.
4. **Result Publication:** The structured JSON review is published back to the `review-results` Kafka topic.
5. **Dynamic Formatting:** A Java consumer reads the JSON payload via a `JsonNode` parser, formatting the nested comments into a clean Markdown checklist.
6. **Bot Authentication:** The consumer dynamically generates an RS256 JWT using a private `.pem` key, exchanging it for a temporary Installation Token to post the comment natively as the `lens[bot]` GitHub App.

## 🛠️ Tech Stack

* **Backend / API Gateway:** Java 17, Spring Boot 3
* **AI Service:** Python, LangChain, OpenAI API (or local LLM)
* **Message Broker:** Apache Kafka, Zookeeper
* **Database:** MySQL 8.0
* **Infrastructure:** Docker, Docker Compose
* **Authentication:** GitHub Apps (JJWT API)

## ⚙️ Prerequisites

* [Docker & Docker Compose](https://docs.docker.com/get-docker/?utm_source=gemini)
* A registered [GitHub App](https://docs.github.com/en/apps?utm_source=gemini) installed on your repositories
* An LLM API Key (e.g., OpenAI)

## 🚀 Getting Started

**1. Clone the Repository**

```bash
git clone https://github.com/Beast0E4/lens-ai-reviewer.git
cd lens-ai-reviewer

```

**2. Configure the GitHub App**

* Navigate to your GitHub App settings and generate a private key.
* Download the `.pem` file and convert it to PKCS#8 format for Java:
```bash
openssl pkcs8 -topk8 -inform PEM -outform PEM -in your-downloaded-key.pem -out github-app.pem -nocrypt

```


* Place `github-app.pem` inside `api-service/src/main/resources/`. *(Ensure `*.pem` is in your `.gitignore`)*.

**3. Environment Variables**
Create a `.env` file in the root directory (and any specific service directories as needed) with the following parameters:

```env
# GitHub Auth
GITHUB_APP_ID=your_github_app_id
WEBHOOK_SECRET=your_github_webhook_secret

# AI Configuration
OPENAI_API_KEY=your_openai_api_key

# Database
MYSQL_USER=user
MYSQL_PASSWORD=password
MYSQL_DATABASE=lens_db

```

**4. Deploy the Stack**
Spin up the entire microservices architecture using Docker Compose:

```bash
docker-compose up --build -d

```

* `lens-api-service` will be available on `http://localhost:8080`.
* `lens-kafka` will bind to port `9092`.
* `lens-mysql` will bind to port `3306` (or `3307` mapped).

**5. Route Webhooks (Local Development)**
Use a tool like [ngrok](https://ngrok.com/?utm_source=gemini) to expose your local gateway to the internet and paste the generated URL into your GitHub App's Webhook URL field.

```bash
ngrok http 8080

```

## 📝 Usage

Once running, the application requires zero manual intervention. Simply push a new commit or open a new Pull Request on any repository where the GitHub App is installed.

The pipeline will trigger automatically, and `lens[bot]` will post its findings directly in the PR conversation within a few seconds.