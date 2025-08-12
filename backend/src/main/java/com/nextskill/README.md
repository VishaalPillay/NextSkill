# NextSkill Java Backend Code Overview

This folder contains the main Java source code for the **NextSkill backend**, built with **Spring Boot**.  
Below is a breakdown of the important files and their roles.

---

## 1. `BackendApplication.java`
- **Purpose:**  
  The main entry point for the Spring Boot backend application.
- **What it does:**  
  - Starts the embedded Tomcat server.
  - Loads all configurations.
  - Initializes the application context and beans.
- **Example:**  
  Think of this as the **"ON" button** for the backend.

---

## 2. `controller/` Folder
Contains all the **API endpoints** for the backend.

### `ResumeController.java`
- **Purpose:**  
  Handles requests related to resume uploading and processing.
- **What it does:**  
  - Accepts resume uploads from the frontend.
  - Passes the file to a service for processing.
  - Sends responses (success/error) back to the client.
- **Example:**  
  Like a **reception desk** — receives requests and sends them to the right department.

---

## 3. `model/` Folder
Contains **Entity** classes that map Java objects to database tables.

### `Resume.java`
- **Purpose:**  
  Represents a resume record in the database.
- **What it does:**  
  - Defines table fields like `id`, `name`, `filePath`, etc.
  - Uses **JPA annotations** like `@Entity` and `@Table`.
- **Example:**  
  Like a **blueprint** for how resume data is stored.

---

## 4. `repository/` Folder
Contains **DAO (Data Access Object)** interfaces for database operations.

### `ResumeRepository.java`
- **Purpose:**  
  Handles database CRUD operations for resumes.
- **What it does:**  
  - Extends `JpaRepository` so we get built-in methods like `save()`, `findAll()`, `deleteById()`.
- **Example:**  
  Like a **library assistant** — knows exactly how to find and store books (data).

---

## 5. `service/` Folder
Contains **business logic** for the application.

### `ResumeService.java`
- **Purpose:**  
  Contains the main logic for processing resumes.
- **What it does:**  
  - Validates files.
  - Stores files in a location.
  - Calls repository to save details to the database.
- **Example:**  
  Like the **operations team** — actually does the work after the controller gives instructions.

---

## Summary Flow
1. **Frontend** sends a request to **ResumeController**.
2. Controller calls **ResumeService**.
3. Service talks to **ResumeRepository**.
4. Repository saves/fetches data from **PostgreSQL**.
5. Response is sent back to the **frontend**.

---

## Tip for New Developers
- Always **add new features** by creating:
  - A controller for endpoints.
  - A service for logic.
  - A repository for database calls.
  - A model/entity for table mapping.
- Keep files organized in `controller/`, `service/`, `repository/`, and `model/` folders.

