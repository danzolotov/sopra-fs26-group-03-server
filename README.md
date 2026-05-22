# PlateMate — Server

PlateMate is the Spring Boot backend server for the PlateMate application, designed to simplify pantry management, meal planning, recipe discovery, cooking, and grocery shopping for shared households and families.

---

## 📖 Introduction
PlateMate is a collaborative web application designed for shared households to organize grocery shopping, recipe management, and meal planning. The main goal is to simplify recipe organization, shopping lists, and pantry inventories while improving coordination among household members.

### Core Features
- **Collaborative Household Spaces**: Household members share a real-time collaborative workspace with synchronized access to recipes, pantry items, and shopping lists to coordinate shopping and prevent duplicate purchases.
- **Pantry & Shopping List Sync**: Maintain a live pantry inventory of available ingredients. Items marked as purchased on the shared shopping list are automatically transferred to the pantry.
- **Recipe & Meal Planning**: Browse a preset collection of recipes and schedule them on the meal planner. Required ingredients for planned recipes can be automatically added to the shopping list.
- **Handwritten List Recognition**: Supports handwritten list recognition using external APIs integrated directly within the Pantry management section, allowing users to digitize paper lists and transfer them to the pantry in one step.

---

## 🛠️ Technologies Used
- **Language & Runtime**: Java 17
- **Core Framework**: Spring Boot 4.0 (Spring Web, Spring Security, WebSocket)
- **Data & Persistence**: Spring Data JPA, Hibernate 7.1
- **Databases**: 
  - **Local Development**: In-Memory H2 Database
  - **Production**: PostgreSQL (hosted on Supabase)
- **APIs & Mapping**: Google Cloud Vision API (for OCR list scanning), MapStruct (for DTO mapping)
- **Build System**: Gradle 9.2
- **Testing & Quality**: JUnit 5, Mockito, Spring Security Test, JaCoCo (code coverage)
- **DevOps**: Docker, GitHub Actions, Google Cloud App Engine

---

## 🧩 High-Level Components
PlateMate is divided into 4 main high-level functional components. These components are strongly correlated through a **Group-Centric Domain Model** where pantry inventory, shopping lists, and meal plans are scoped to shared groups (households):

1. **User & Group Management**:
   Manages user authentication, registration, profiles, and household group creation/memberships. A group acts as the parent container that binds users together and scopes all other modules.
   - **Key Classes**: [UserService.java](src/main/java/ch/uzh/ifi/hase/soprafs26/service/UserService.java) and [GroupService.java](src/main/java/ch/uzh/ifi/hase/soprafs26/service/GroupService.java)

2. **Pantry Inventory Manager**:
   Maintains the live list of ingredients available in the household. It tracks items, units, and quantities. It is modified when ingredients are manually added/updated or when items are checked off the shopping list.
   - **Key Classes**: [PantryService.java](src/main/java/ch/uzh/ifi/hase/soprafs26/service/PantryService.java) and [IngredientService.java](src/main/java/ch/uzh/ifi/hase/soprafs26/service/IngredientService.java)

3. **Automated Shopping List Engine**:
   Manages the list of ingredients that the household needs to buy. It supports manual additions, automatic entry creation via OCR scanning of physical lists, and seamless migration of items to the Pantry when marked as bought.
   - **Key Classes**: [ShoppingListService.java](src/main/java/ch/uzh/ifi/hase/soprafs26/service/ShoppingListService.java) and [ShoppingListAutoDetectService.java](src/main/java/ch/uzh/ifi/hase/soprafs26/service/ShoppingListAutoDetectService.java)

4. **Meal Planning & Recipe Book**:
   Coordinates recipe storage and meal scheduling. When a recipe is added to the meal plan, the planning module calculates missing ingredients by comparing required recipe ingredients against current pantry stock and existing shopping list items.
   - **Key Classes**: [MealPlanService.java](src/main/java/ch/uzh/ifi/hase/soprafs26/service/MealPlanService.java) and [RecipeService.java](src/main/java/ch/uzh/ifi/hase/soprafs26/service/RecipeService.java)

---

## 🚀 Launch & Deployment
Getting started with the PlateMate backend server locally is straightforward:

### 1. Prerequisites
- **Java Development Kit (JDK) 17** must be installed.
- (Optional) Nix package manager with `direnv` can be used to load the preconfigured development shell defined in [flake.nix](flake.nix) and [.envrc](.envrc).

### 2. External Dependencies & Database Config
- **Database**: Local development runs on a file-based **H2 database** by default. There is **no need** to set up or configure an external database locally.
  - Access H2 Console: `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:file:./data/platemate` (Username: `sa`, Password: leave blank).
- **Google Vision API Key (Optional)**: For list/receipt OCR scanning, set the `GOOGLE_APPLICATION_CREDENTIALS` environment variable pointing to your GCP Service Account JSON key.

### 3. Local Development Commands
Run these commands from the `server` root directory:
- **Build the Project**:
  ```bash
  ./gradlew build
  ```
- **Run the Server locally** (starts on port `8080`):
  ```bash
  ./gradlew bootRun
  ```
- **Run the Test Suite**:
  ```bash
  ./gradlew test
  ```

### 4. Releases & Deployment
Deployments and release packaging are fully automated using GitHub Actions CI/CD workflows:
- **Google Cloud App Engine**: Every merge to the `main` branch triggers [main.yml](.github/workflows/main.yml) which builds the server, runs tests, checks quality with SonarQube, and deploys using [app.yaml](app.yaml) to Google Cloud.
- **Dockerization**: The [dockerize.yml](.github/workflows/dockerize.yml) workflow builds a containerized Docker image using the multi-stage [Dockerfile](Dockerfile) and pushes it to Docker Hub.

---

## Core User Flows
Here is how the main user flows connect across the PlateMate interface:
1. **Join/Create Household**: A user signs up and either creates a new household group or joins an existing one using their unique group token.
2. **Pantry Management**: Household members log the food items they have in stock. Users can also scan physical lists (digitized instantly via OCR) or input manually with autocomplete.
3. **Meal Scheduling & Sync**: The group selects recipes from the shared Recipe Book and schedules them. The Outstanding Ingredients sidebar immediately highlights what is missing. Clicking "Add to Shopping List" queues the required ingredients.
4. **Shopping & Replenishment**: When a user goes shopping, they check off items in the app's Shopping List. Checked-off items are instantly added to the Pantry stock, and their status updates in real-time for all other group members.

---

## 🗺️ Roadmap
The top features planned for new developers contributing to PlateMate:
- **Real-Time Meal Voting**: Implement a voting system within the shared household so members can vote on scheduled dinner options to simplify meal decisions.
- **AI-Powered Pantry Suggestions**: Recommend recipes automatically based on pantry ingredients that are close to their expiration dates.
- **Adding and Editing Recipes**: Allow users to manually create, update, or customize recipe details directly from the user interface.
- **Auto-Deduct Pantry Stock**: Automatically cross off and deduct ingredients from the pantry inventory when a scheduled recipe is marked as cooked.

---

## 👥 Authors & Acknowledgments
- **Marc Honegger** & **Karina Litvinova**

---

## 📄 License
This project is licensed under the MIT License. See [LICENSE](LICENSE) for more details.

Copyright (c) 2026 Marc Honegger & Karina Litvinova
