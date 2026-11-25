# NEXUS — AI Developer Workspace

## Project overview

NEXUS is a JavaFX desktop application that brings everyday developer tasks into one workspace. It combines a native terminal, a visual repository explorer, an API workbench, an AI assistant and live system monitoring.

| Module | Features |
| --- | --- |
| Terminal | Persistent shell sessions, terminal tabs, command output blocks and command history. |
| RepoGraph | Directory browsing, source previews, Git branches, working-tree status, commit history and diffs. |
| API Studio | HTTP requests, authentication, collections, environment variables, response assertions and basic OpenAPI import. |
| AI assistant | Contextual assistance and developer tools through LangChain4j, with permission checks for tool execution. |
| History and monitoring | Command records and analytics, plus CPU, memory, storage and network measurements. |

## Team members

**Team 8 · CSE 309: Advanced Java Programming · SRM University AP**

| Member | Registration number |
| --- | --- |
| Ch. Rohit | AP24110010986 |
| M. NagaSai Rishi | AP24110011126 |
| K. Bhavanadithya | AP24110010929 |

## Individual contributions

| Member | Responsibilities |
| --- | --- |
| Ch. Rohit | Application startup, navigation, terminal execution, command history and live system monitoring. |
| M. NagaSai Rishi | Repository exploration, assistant presentation, file and project services, conversation models and shared view infrastructure. |
| K. Bhavanadithya | API request execution and validation, environment handling, AI tool execution, permission checks and the JDBC service. |

The project report allocates 25 of the 75 application Java files to each member. Interface integration and the final demonstration are shared responsibilities. The report provides the detailed file allocation.

## How to run

### 1\. Prerequisites

*   A JDK supporting Java 21. The project compiles with release target 21.
*   Apache Maven 3.9 or later.
*   A desktop environment for JavaFX and a native shell for terminal features.
*   Internet access for the initial dependency download and online API or AI requests.

Confirm that Java and Maven are available:

```
java -version
mvn -version
```

Set `JAVA_HOME` to your JDK installation and ensure Maven reports the intended Java version. Maven downloads JavaFX and the other project dependencies automatically.

### 2\. Open the project directory

From the repository root:

```
cd "Project Codes"
```

Run the following commands from this directory, which contains `pom.xml`.

### 3\. Compile and launch

```
mvn clean compile
mvn javafx:run
```

The application can open without an AI key or MySQL. To explore Git branches and commit history, select a local Git repository with commits. Online API requests require access to the selected endpoint.

### 4\. Build an executable JAR

```
mvn package
java -jar target/nexus-workspace-1.0.0-SNAPSHOT.jar
```

The packaged JAR includes dependencies and the JavaFX native libraries selected for the build platform. Build it on the operating system where it will run.

## AI configuration

For live AI responses, create a local configuration file at:

*   **Windows:** `%USERPROFILE%\.nexus\config.properties`
*   **Linux/macOS:** `~/.nexus/config.properties`

Example configuration for an OpenAI-compatible Groq endpoint:

```
ai.api.key=YOUR_API_KEY
ai.base.url=https://api.groq.com/openai/v1
ai.model.name=YOUR_PROVIDER_MODEL_ID
```

Use a model supported by your provider account, then restart the application. If no base URL or model is configured, the implementation defaults to `https://api.openai.com/v1` and `gpt-4o-mini`.

The application also accepts `NEXUS_AI_API_KEY`, `NEXUS_AI_BASE_URL` and `NEXUS_AI_MODEL`. When set, the corresponding `OPENAI_API_KEY`, `OPENAI_BASE_URL` and `OPENAI_MODEL_NAME` variables take precedence. Store real credentials locally, outside the repository.

## Data storage and JDBC configuration

Command history, API collections, API history, environment settings and audit logs are stored under the user's `.nexus` directory. Conversation messages remain in memory.

`DatabaseService` includes MySQL connection and schema-creation code. It is not called by the normal application services, so MySQL is not required to launch the workspace. Configuring credentials alone does not enable database persistence.

For development of the JDBC integration, the same local configuration file supports:

```
db.host=localhost
db.port=3306
db.database=nexus_db
db.user=YOUR_MYSQL_USER
db.password=YOUR_MYSQL_PASSWORD
```

When invoked, the service attempts to create the database and tables; the database account needs the corresponding permissions. Environment overrides are `NEXUS_DB_HOST`, `NEXUS_DB_PORT`, `NEXUS_DB_NAME`, `NEXUS_DB_USER` and `NEXUS_DB_PASSWORD`.

## Source organization

```
Project Codes/
├── pom.xml
├── README.md
├── .gitignore
└── src/main/
    ├── java/com/nexus/
    │   ├── app/          # Application entry points
    │   ├── config/       # Application constants
    │   ├── controller/   # UI behavior and event handling
    │   ├── service/      # Shell, Git, HTTP, AI and storage logic
    │   ├── model/        # Data models and enums
    │   ├── component/    # Reusable JavaFX components
    │   └── util/         # View loading and rendering helpers
    └── resources/        # FXML views, styles and icons
```

## Build output

Maven creates `target/` when compiling or packaging. This folder contains generated classes and JAR files and is already excluded by `.gitignore`. It is not required in GitHub; the complete `src/main/` tree and `pom.xml` allow the project to be rebuilt.

## Project documentation

See the [project report](../A08_Project_Report.pdf) for architecture, individual file allocations, source explanations, screenshots and implementation limitations. Watch the [video demonstration](../Video%20Demonstration.mp4) for the project walkthrough. Return to the [submission overview](../README.md) for the repository contents.