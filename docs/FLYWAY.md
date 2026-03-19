# Flyway notes

## Why the app failed with `jpaSharedEM_entityManagerFactory`

That error is a **symptom**. The real failure is usually **earlier in the chain**:

1. **Flyway validation** fails (e.g. checksum mismatch on edited migrations).
2. `flywayInitializer` does not complete.
3. `entityManagerFactory` is not created.
4. JPA repositories cannot wire → `Cannot resolve reference to bean 'entityManagerFactory'`.

## Do not edit applied migrations

After a version (e.g. `V2__...sql`) has run on any database, **changing that file** changes its checksum and Flyway will refuse to start until you:

- Run **`flyway repair`** (updates checksums in `flyway_schema_history`), or  
- Restore the migration file to the **exact** bytes that were applied.

Use **new** migrations (e.g. `V11__...sql`) for data fixes instead of editing old ones.

## Configuration

Flyway must be under **`spring.flyway`** in `application.yml` (a top-level `flyway:` block is **ignored** by Spring Boot).

## Password reset

Passwords for all users are normalized by **`V10__reset_all_passwords_to_password.sql`** (plain text login: `password`).
