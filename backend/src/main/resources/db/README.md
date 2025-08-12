# Database Setup Guide for NextSkill

This guide explains how to set up and use the **database** part of the NextSkill backend.

---

## 1. Overview

We use **PostgreSQL** as the database for NextSkill.
Flyway is used to manage database migrations (schema changes).

Folder structure:
```
backend/
  src/
    main/
      resources/
        db/
          migration/
            V1__init.sql    # First migration file
```

---

## 2. Installing PostgreSQL

1. **Download and Install**
   - Visit: https://www.postgresql.org/download/
   - Choose your OS and install.
   - During installation, set:
     - **Username**: `postgres` (or your choice)
     - **Password**: Remember this! (used in `application.properties`)
     - **Port**: `5432` (default)

2. **Verify Installation**
   - Open terminal / command prompt:
     ```bash
     psql --version
     ```

---

## 3. Creating the Database

1. Open **psql** (PostgreSQL terminal):
   ```bash
   psql -U postgres
   ```
   - Enter your password.

2. Create the database:
   ```sql
   CREATE DATABASE nextskill_db;
   ```

3. Connect to it:
   ```sql
   \c nextskill_db
   ```

4. Exit psql:
   ```sql
   \q
   ```

---

## 4. Flyway Migration

We use **Flyway** to apply SQL scripts automatically.

- Migration scripts go in:
  ```
  src/main/resources/db/migration/
  ```
- Naming convention:
  ```
  V1__description.sql
  V2__description.sql
  ```
  (Version starts with `V`, then a number, then two underscores `__`.)

Example: `V1__init.sql`

### Example `V1__init.sql`
```sql
CREATE TABLE resume (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    skills TEXT
);
```

---

## 5. application.properties Setup

Example:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/nextskill_db
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=none

spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
```

---

## 6. Running Migrations

When you run:
```bash
mvn spring-boot:run
```
Flyway:
- Checks the `db/migration` folder
- Runs new migrations in order
- Keeps track in the `flyway_schema_history` table

---

## 7. Common Commands

- **List databases**:
  ```sql
  \l
  ```
- **Switch database**:
  ```sql
  \c dbname
  ```
- **List tables**:
  ```sql
  \dt
  ```
- **Show table content**:
  ```sql
  SELECT * FROM tablename;
  ```

---

## 8. Notes for Team Members

If someone pulls the project:
1. Install PostgreSQL.
2. Create the `nextskill_db` database.
3. Set correct username/password in `application.properties`.
4. Run `mvn spring-boot:run` — Flyway will create/update tables.

---

**End of DB Setup Guide**