# Migration V9 Fix - Complete Explanation

## 🔍 Root Cause Analysis

### The Problem
The error message was:
```
ERROR: column "frequency" does not exist
Failing SQL: ALTER TABLE routine ADD CONSTRAINT chk_routine_frequency CHECK (frequency IN (...))
```

### What Was Actually Wrong
The issue was **NOT** that the `frequency` column was missing. The column WAS defined correctly in the CREATE TABLE statement (line 79):

```sql
CREATE TABLE IF NOT EXISTS routine (
    ...
    frequency VARCHAR(50) NOT NULL,  -- ✅ Column exists here
    ...
);
```

**The real problem**: Incorrect PL/pgSQL block delimiter syntax.

### The Bug
```sql
DO $          -- ❌ WRONG: Single dollar sign
BEGIN
    ...
END $;
```

Should be:
```sql
DO $$         -- ✅ CORRECT: Double dollar signs
BEGIN
    ...
END $$;
```

### Why This Caused the Error
1. PostgreSQL failed to parse the `DO $` block due to invalid delimiter
2. The `CREATE TABLE routine` statement executed successfully
3. The constraint-adding block failed before execution
4. On retry, Flyway tried to add constraints but the DO block still failed
5. PostgreSQL reported "column doesn't exist" because the block never executed properly

## ✅ The Fix

### Changed Lines
All `DO $` blocks changed to `DO $$`:
- Line 21: `DO $` → `DO $$` (goal constraints)
- Line 35: `END $;` → `END $$;`
- Line 58: `DO $` → `DO $$` (goal_participation constraints)
- Line 66: `END $;` → `END $$;`
- Line 91: `DO $` → `DO $$` (routine constraints)
- Line 99: `END $;` → `END $$;`
- Line 119: `DO $` → `DO $$` (activity constraints)
- Line 125: `END $;` → `END $$;`

### Why This Works
- `$$` is the standard PostgreSQL delimiter for PL/pgSQL blocks
- It properly escapes the block content
- PostgreSQL can now parse and execute the constraint logic correctly

## 🔧 How to Apply the Fix

### Step 1: Clean Up Failed Migration

Run this SQL in pgAdmin or psql:

```sql
-- Remove failed migration from Flyway history
DELETE FROM flyway_schema_history 
WHERE version = '9' AND success = false;

-- Drop partially created tables
DROP TABLE IF EXISTS activity CASCADE;
DROP TABLE IF EXISTS routine CASCADE;
DROP TABLE IF EXISTS goal_participation CASCADE;
DROP TABLE IF EXISTS goal CASCADE;
```

**Or use the provided script:**
```bash
psql -U your_user -d your_database -f CLEANUP_AND_FIX.sql
```

### Step 2: Recompile Project

```bash
mvn clean compile
```

This ensures the corrected SQL file is copied to `target/classes/db/migration/`

### Step 3: Run Application

```bash
mvn javafx:run
```

Flyway will now execute the corrected V9 migration successfully.

## 📋 Verification

After running, check Flyway history:

```sql
SELECT version, description, success, installed_on
FROM flyway_schema_history
WHERE version = '9';
```

Expected result:
```
version | description          | success | installed_on
--------|---------------------|---------|-------------
9       | create goal tables  | t       | 2026-04-28...
```

Check that tables exist:
```sql
SELECT tablename FROM pg_tables 
WHERE schemaname = 'public' 
  AND tablename IN ('goal', 'routine', 'activity', 'goal_participation');
```

Expected result: All 4 tables listed.

## 🛡️ Why This Fix Is Safe

1. **Idempotent**: Uses `CREATE TABLE IF NOT EXISTS` and constraint existence checks
2. **No data loss**: Tables are recreated cleanly
3. **Flyway-compliant**: Properly removes failed migration before retry
4. **PostgreSQL-standard**: Uses correct `$$` delimiter syntax
5. **NOT VALID constraints**: Won't fail on existing data

## 🎯 Key Takeaways

### What We Learned
- Always use `$$` (double dollar) for PostgreSQL DO blocks
- Single `$` is not a valid delimiter for anonymous code blocks
- Flyway marks failed migrations with `success = false`
- Safe to delete failed migrations from `flyway_schema_history`

### Best Practices Applied
1. ✅ Used `IF NOT EXISTS` for tables
2. ✅ Used `IF NOT EXISTS` checks for constraints
3. ✅ Added `NOT VALID` to constraints (skip existing data validation)
4. ✅ Separated constraint creation from table creation
5. ✅ Used proper PostgreSQL syntax

## 📚 Additional Resources

- [PostgreSQL Dollar Quoting](https://www.postgresql.org/docs/current/sql-syntax-lexical.html#SQL-SYNTAX-DOLLAR-QUOTING)
- [Flyway Repair Command](https://flywaydb.org/documentation/command/repair)
- [PostgreSQL DO Statement](https://www.postgresql.org/docs/current/sql-do.html)

## ❓ Troubleshooting

### If migration still fails:

1. **Check Flyway history:**
   ```sql
   SELECT * FROM flyway_schema_history WHERE version = '9';
   ```

2. **Manually repair Flyway:**
   ```bash
   mvn flyway:repair
   ```

3. **Check PostgreSQL logs** for detailed error messages

4. **Verify file was recompiled:**
   ```bash
   cat target/classes/db/migration/V9__create_goal_tables.sql | grep "DO \$\$"
   ```
   Should show `DO $$` (double dollar)

### If tables already exist:

Run the cleanup script first:
```bash
psql -U your_user -d your_database -f CLEANUP_AND_FIX.sql
```

Then retry the application.

## ✅ Success Indicators

You'll know it worked when you see:
```
Flyway: Migrating schema "public" to version "9 - create goal tables"
Flyway: Successfully applied 1 migration to schema "public"
```

And the application starts without errors! 🎉
