# 🚀 SkillForge AI

<div align="center">

![SkillForge AI Banner](https://img.shields.io/badge/SkillForge-AI%20Learning%20Platform-6366f1?style=for-the-badge&logo=openai&logoColor=white)

**An intelligent, full-stack AI learning platform and engineering interview simulator powered by Google Gemini and Spring Boot.**

[![Java](https://img.shields.io/badge/Java-17-orange.svg?style=flat-square&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen.svg?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-blue.svg?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Gemini AI](https://img.shields.io/badge/AI%20Engine-Gemini--3.1--Flash--Lite-blueviolet.svg?style=flat-square&logo=google&logoColor=white)](https://ai.google.dev/)
[![YouTube Data API](https://img.shields.io/badge/YouTube%20API-v3-red.svg?style=flat-square&logo=youtube&logoColor=white)](https://developers.google.com/youtube/v3)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)

[Key Features](#-key-features) • [Tech Stack](#-tech-stack) • [Architecture](#-architecture) • [Getting Started](#-getting-started) • [API Reference](#-api-reference) • [Configuration](#-configuration)

</div>

---

## 📖 Overview

**SkillForge AI** is a comprehensive educational acceleration platform designed to bridge the gap between learning and landing top-tier engineering roles. It crafts personalized, step-by-step technical roadmaps, curates real-time video resources from YouTube, provides automated video transcript summaries, and runs realistic, stateful technical interviews tailored to specific tech giants.

Whether you're beginning your software journey or preparing for high-stakes interviews at companies like Google, Amazon, or Microsoft, SkillForge AI provides personalized guidance 24/7.

---

## ✨ Key Features

### 🗺️ 1. Dynamic AI Learning Roadmaps
- **Adaptive Level Selection**: Tailors paths across *Beginner*, *Intermediate*, and *Advanced* tiers.
- **Sequential Milestones**: Breaks complex topics into structured, bite-sized stages with time estimates.
- **Vetted Recommendations**: Surfaces recommended documentation, practice platforms (e.g., LeetCode, Kaggle), and free online courses.

### 📺 2. Real-Time Resource & YouTube Integration
- **Automated YouTube Curation**: Enriches each roadmap milestone with playlists and video guides via the YouTube Data API v3.
- **Trending Tutorials**: Fetches active video discussions and high-rated developer walkthroughs.

### 🎥 3. YouTube Video Summarizer & Knowledge Extractor
- **Automatic Caption Ingestion**: Pulls real-time video transcripts directly via YouTube's caption infrastructure.
- **Multimodal Summary Modes**: Generates structured summaries, bullet points, key takeaways, and visual conceptual outlines using Gemini AI.

### 🎙️ 4. Realistic AI Interview Simulator
Simulates multi-round technical interviews with **company-specific interviewer personas**:
- **Company Personalities**: *Google* (algorithmic rigor), *Microsoft* (system design & OOP), *Amazon* (Leadership Principles), *Flipkart* (high-scale e-commerce), *TCS*, *Infosys*, *Wipro*, *Zomato*, and *Tech Startups*.
- **6 Interview Modes**:
  1. **DSA Round**: Algorithmic problem-solving with dynamic challenges and test cases.
  2. **System Design**: High-level architecture, caching, database sharding, and latency trade-offs.
  3. **Project Deep Dive**: AI-led defense of custom project architecture, bottlenecks, and design decisions.
  4. **Debugging Challenge**: Real-world incident response and broken code troubleshooting.
  5. **Aptitude Assessment**: Quantitative, logical, and verbal multiple-choice testing.
  6. **HR / Behavioral Round**: Structured behavioral evaluation using the STAR framework.

### 💻 5. In-Browser Monaco Code Editor & Compiler
- Integrated **Monaco Editor** with syntax highlighting for multiple languages (Java, Python, C++, JavaScript).
- Sandboxed code execution simulation capturing `stdout`, `stderr`, and execution exit codes.

### 📊 6. Learner Analytics & Skill Radar Dashboard
- Tracks past search history and bookmarked learning materials.
- Visualizes readiness across core competencies with **Chart.js Radar Charts**.
- Computes aggregated interview performance metrics and past submission scores.

### 🔐 7. Authentication & Security
- **OAuth2 Login**: Google OAuth2 authentication.
- **Custom Credentials**: Standard email and password authentication with profile customization.
- **CSRF & Session Control**: Secured with Spring Security 6.

---

## 🛠️ Tech Stack

### Backend
- **Framework**: [Spring Boot 3.2.4](https://spring.io/projects/spring-boot) (Java 17)
- **Security**: Spring Security & Spring OAuth2 Client
- **Persistence**: Spring Data JPA & Hibernate
- **Database**: PostgreSQL
- **REST & Serialization**: Jackson & org.json
- **Build Tool**: Apache Maven

### Frontend
- **Interface**: Modern Glassmorphism UI (HTML5, Vanilla CSS3, JavaScript ES6+)
- **Code Editor**: Monaco Editor (VS Code core editor engine)
- **Visuals & Charts**: Chart.js (Skill Radar & score analytics)
- **Markdown Rendering**: Marked.js
- **Iconography**: FontAwesome 6

### AI & External Services
- **Generative AI**: [Google Gemini API](https://ai.google.dev/) (`gemini-3.1-flash-lite`)
- **Video Metadata**: [Google YouTube Data API v3](https://developers.google.com/youtube/v3)

---

## 📂 Project Structure

```
SkillForge-AI/
├── .github/                       # GitHub configuration & workflows
├── src/
│   └── main/
│       ├── java/com/studyrec/
│       │   ├── StudyRecommenderApplication.java   # Spring Boot entry point
│       │   ├── config/
│       │   │   └── SecurityConfig.java            # OAuth2 & security filter chain
│       │   ├── controller/
│       │   │   ├── AuthController.java            # Login, signup & profile endpoints
│       │   │   ├── ChatController.java            # Interactive AI study mentor chat
│       │   │   ├── CustomErrorController.java     # Error handling
│       │   │   ├── InterviewController.java       # Interview simulator sessions & compiler
│       │   │   ├── RecommendationController.java  # Roadmap & YouTube resource recommendations
│       │   │   └── VideoSummaryController.java    # YouTube transcript summarizer
│       │   ├── dto/
│       │   │   ├── ChatRequest.java
│       │   │   └── RecommendationRequest.java
│       │   ├── model/                             # JPA Entity models (User, Sessions, Feedback, etc.)
│       │   ├── repository/                        # Spring Data JPA repositories
│       │   └── service/
│       │       ├── CompilerService.java           # Code runner & syntax evaluator
│       │       ├── CustomOAuth2UserService.java   # Google OAuth2 profile handler
│       │       ├── GeminiService.java             # Google Gemini API client
│       │       ├── InterviewService.java          # Persona-based interview orchestration
│       │       ├── VideoSummaryService.java       # Video transcript summarizer
│       │       ├── YouTubeService.java            # YouTube Data API client
│       │       └── YouTubeTranscriptService.java  # Direct YouTube caption scraper
│       └── resources/
│           ├── static/                            # Web frontend assets
│           │   ├── css/
│           │   │   ├── login.css
│           │   │   └── style.css                  # Glassmorphic dark/light UI
│           │   ├── js/
│           │   │   ├── app.js                     # Core application logic & Chart.js integration
│           │   │   └── login.js                   # Auth logic
│           │   ├── index.html                     # Primary dashboard & simulator
│           │   └── login.html                     # Auth interface
│           └── application.properties             # Spring Boot properties
├── .gitignore
├── pom.xml                                        # Maven dependency definitions
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites
Ensure you have the following installed on your machine:
- **Java Development Kit (JDK)**: Version 17 or higher
- **Apache Maven**: Version 3.8+ (or use an embedded Maven wrapper)
- **PostgreSQL**: Version 13+ running locally or in Docker
- **Google Gemini API Key**: Obtainable from [Google AI Studio](https://aistudio.google.com/)
- **YouTube Data API v3 Key**: Obtainable from [Google Cloud Console](https://console.cloud.google.com/)

---

### Step 1: Clone the Repository
```bash
git clone https://github.com/Amitesh-cyber/SkillForge-AI.git
cd SkillForge-AI
```

---

### Step 2: Set Up PostgreSQL Database
Open PostgreSQL (`psql` or pgAdmin) and create a database named `studydb`:
```sql
CREATE DATABASE studydb;
```

---

### Step 3: Configure Environment Variables or `application.properties`
Set up your environment variables or customize `src/main/resources/application.properties`:

#### Option A: Set Environment Variables
```bash
# Windows PowerShell
$env:GEMINI_API_KEY="your_gemini_api_key"
$env:YOUTUBE_API_KEY="your_youtube_api_key"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="your_postgres_password"

# Linux / macOS / Bash
export GEMINI_API_KEY="your_gemini_api_key"
export YOUTUBE_API_KEY="your_youtube_api_key"
export SPRING_DATASOURCE_USERNAME="postgres"
export SPRING_DATASOURCE_PASSWORD="your_postgres_password"
```

#### Option B: Edit `application.properties` Directly
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/studydb
spring.datasource.username=postgres
spring.datasource.password=your_password

gemini.api.key=YOUR_GEMINI_API_KEY
youtube.api.key=YOUR_YOUTUBE_API_KEY

server.port=8200
```

---

### Step 4: Build and Run the Application
Use Maven to compile and start the Spring Boot service:
```bash
mvn clean spring-boot:run
```

---

### Step 5: Launch the Application
Once the application starts, open your web browser and navigate to:
```
http://localhost:8200
```

---

## 📡 API Reference

### 🎓 Recommendations & Roadmaps
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/recommend` | Generate a customized learning roadmap with curated YouTube videos |
| `GET` | `/api/history` | Retrieve user search history |
| `POST` | `/api/save` | Save a resource to the user's library |
| `GET` | `/api/saved` | Fetch all saved resources |
| `DELETE` | `/api/save/{id}` | Delete a saved resource |

### 🤖 AI Mentor Chat
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/chat` | Send a query to the contextual AI mentor |

### 🎥 Video Summarizer
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/video/summarize` | Extract captions and generate an AI summary for a YouTube video |

### 🎙️ Interview Simulator & Code Execution
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/interview/session/start` | Initialize a new interview session with company persona & mode |
| `POST` | `/api/interview/session/{id}/chat` | Converse with the AI technical interviewer |
| `POST` | `/api/interview/session/{id}/run-code` | Execute code snippet in the sandbox environment |
| `POST` | `/api/interview/session/{id}/submit-code` | Submit solution for evaluation and test results |
| `POST` | `/api/interview/session/{id}/complete` | Finalize session and receive detailed feedback and scorecard |
| `GET` | `/api/interview/aptitude/questions` | Generate dynamic aptitude and technical MCQs |
| `GET` | `/api/interview/history` | Retrieve past interview sessions and scores |
| `GET` | `/api/interview/session/{id}` | Retrieve comprehensive detail of a specific interview round |

### 👤 Authentication & Profile
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/signup` | Register a new user |
| `POST` | `/api/auth/login` | Log in with email and password |
| `GET` | `/api/auth/me` | Fetch currently authenticated user via OAuth2 |
| `GET` | `/api/auth/profile/{email}` | Retrieve user profile data |
| `PUT` | `/api/auth/profile` | Update user profile |

---

## ⚙️ Configuration Reference

| Property Key | Default Value | Description |
|---|---|---|
| `server.port` | `8200` | Application HTTP port |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/studydb` | PostgreSQL JDBC connection URL |
| `spring.datasource.username` | `postgres` | Database username |
| `spring.datasource.password` | `123456` | Database password |
| `spring.jpa.hibernate.ddl-auto` | `update` | Automatically update database schema |
| `gemini.api.key` | Environment / Config | Google Gemini API Key |
| `gemini.api.url` | `https://generativelanguage.googleapis.com/...` | Gemini API endpoint |
| `youtube.api.key` | Environment / Config | YouTube Data API v3 Key |
| `youtube.application.name` | `SkillForgeAI` | Application identifier for YouTube API |

---

## 🤝 Contributing

Contributions, bug reports, and feature requests are welcome!

1. **Fork** the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a **Pull Request**

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).

---

<div align="center">
  <sub>Developed with ❤️ by <a href="https://github.com/Amitesh-cyber">Amitesh-cyber</a>. Powered by Gemini AI.</sub>
</div>
