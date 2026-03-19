# Backend troubleshooting

## `Failed to start bean 'webServerStartStop'`

This is a **wrapper error**. Scroll up in the log for **`Caused by:`**.

### Most common: port already in use

Default port is **8080**. If another app (another Spring Boot run, Docker, IIS, etc.) uses it, Tomcat cannot bind.

**Fix (pick one):**

1. **Stop the process on 8080** (Windows PowerShell):
   ```powershell
   netstat -ano | findstr :8080
   taskkill /PID <pid_from_last_column> /F
   ```

2. **Run the API on another port** (must match Vite proxy if you use the React dev server):
   ```powershell
   $env:SERVER_PORT=8081; mvn spring-boot:run
   ```
   Then set `hrms-frontend/vite.config.ts` proxy `target` to `http://localhost:8081`.

### Other causes

- **Flyway validation failed** — see [FLYWAY.md](./FLYWAY.md); often shows as an error *before* Tomcat starts, but the final message can still mention web server failure.
- **PostgreSQL not running** — usually fails earlier on datasource/Flyway with a clear connection error.

Always read the **first** `Caused by:` with a concrete exception (`BindException`, `FlywayValidateException`, etc.).
