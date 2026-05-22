# Platemate
## Introduction
The ultimate companion for modern home cooks and shared households:
- Manage your pantry stock in real-time
- Generate shared shopping lists automatically
- Coordinate meal plans with your group
- Reduce food waste and save money

## Technologies Used
- **Backend Framework**: Java 17, Spring Boot, Spring Web MVC
- **Data Persistence**: Spring Data JPA, H2 Database (local development), PostgreSQL (production/Supabase)
- **Mapping & OCR**: MapStruct (for DTO mapping), Google Cloud Vision API (for OCR list/receipt scanning)
- **Security & Build**: Spring Security, Gradle
- **Testing & Quality**: JUnit 5, JaCoCo, SonarQube
- **DevOps**: Docker, GitHub Actions, Google Cloud App Engine

## High-Level Components
PlateMate is structured into 4 main functional components that operate collaboratively under a group-centric model (where pantry inventory, shopping lists, and meal plans are scoped to shared groups/households):

1. **User & Group Management**:
   Handles user registration, login, profile data, group creation, and membership administration. Because PlateMate is collaborative, a group aggregates multiple users, and all other entities are linked to a specific group.
   - **Main Classes**: [UserService.java](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/server/src/main/java/ch/uzh/ifi/hase/soprafs26/service/UserService.java) and [GroupService.java](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/server/src/main/java/ch/uzh/ifi/hase/soprafs26/service/GroupService.java)
2. **Pantry Inventory Manager**:
   Allows household members to log, edit, and track ingredients currently in stock, including their quantities and expiration dates.
   - **Main Classes**: [PantryService.java](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/server/src/main/java/ch/uzh/ifi/hase/soprafs26/service/PantryService.java) and [IngredientService.java](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/server/src/main/java/ch/uzh/ifi/hase/soprafs26/service/IngredientService.java)
3. **Automated Shopping List Engine**:
   Automatically detects and populates shopping list items when inventory quantities run low or when upcoming meals require missing ingredients. It also integrates OCR to auto-detect shopping list items from scanned receipt/list images.
   - **Main Classes**: [ShoppingListService.java](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/server/src/main/java/ch/uzh/ifi/hase/soprafs26/service/ShoppingListService.java) and [ShoppingListAutoDetectService.java](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/server/src/main/java/ch/uzh/ifi/hase/soprafs26/service/ShoppingListAutoDetectService.java)
4. **Meal Planning & Recipe Book**:
   Coordinates schedules for weekly meals, referencing standard recipes or custom user-submitted recipes. If scheduled recipes require ingredients not present in the Pantry, the Shopping List Engine is notified to prompt the group to buy them.
   - **Main Classes**: [MealPlanService.java](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/server/src/main/java/ch/uzh/ifi/hase/soprafs26/service/MealPlanService.java) and [RecipeService.java](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/server/src/main/java/ch/uzh/ifi/hase/soprafs26/service/RecipeService.java)

## Launch & Deployment
Follow these steps to get started with the PlateMate server locally:

### 1. Prerequisites
- **Java SDK 17** must be installed.
- (Optional) Nix with `direnv` can be used to automatically configure the development environment as defined in [flake.nix](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/server/flake.nix) and [.envrc](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/server/.envrc).

### 2. External Dependencies & Database
- **Database**: By default, local development uses a file-based **H2 database**. There is **no need** to set up or run an external database locally.
  - The H2 console is enabled locally at `http://localhost:8080/h2-console`.
  - JDBC URL: `jdbc:h2:file:./data/platemate` (Username: `sa`, Password: leave empty).
- **OCR (Google Cloud Vision API)**: To run features that scan shopping lists/receipts, you must supply a Google Cloud service account key:
  - Set the `GOOGLE_APPLICATION_CREDENTIALS` environment variable pointing to your GCP service account JSON key file.

### 3. Local Commands
- **Build the application**:
  ```bash
  ./gradlew build
  ```
- **Run the server locally** (starts on port `8080`):
  ```bash
  ./gradlew bootRun
  ```
- **Run the test suite**:
  ```bash
  ./gradlew test
  ```

### 4. Releases & Deployment
Deployments and release packaging are automated using GitHub Actions workflows:
- **Google Cloud App Engine Deployment**: Every push to the `main` branch triggers the [main.yml](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/server/.github/workflows/main.yml) workflow, which runs tests, checks quality via SonarQube, and deploys the build artifact using [app.yaml](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/server/app.yaml) to Google Cloud App Engine.
- **Dockerization**: The [dockerize.yml](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/server/.github/workflows/dockerize.yml) workflow builds a containerized Docker image using the multi-stage [Dockerfile](file:///Users/karina/Local/UZH_study/SoPra/PlateMate/server/Dockerfile) and pushes it to Docker Hub on branch updates.

## Illustrations: In your client repository, briefly describe and illustrate the main user flow(s)
of your interface. How does it work (without going into too much detail)? Feel free to
include a few screenshots of your application.

## Roadmap: The top 2-3 features that new developers who want to contribute to your project
could add.

## Authors and acknowledgment.
Marc Honegger & Karina Litvinova

We also want to thank our former teammates: Dan Zolotov, Ceyda B. Dag & Kishore Sivapathasundaram

## License: Say how your project is licensed (see License guide3).
MIT License

Copyright (c) 2026 Marc Honegger & Karina Litvinova
