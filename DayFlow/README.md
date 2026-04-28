# DayFlow - AI Profile Analyzer + Auth Security

## Implemented in this iteration

- Desktop auth flow with login/register, forgot password, reset password
- Password hashing with BCrypt (`jbcrypt`) - no Spring runtime required
- Basic lockout policy (5 failed logins in 15 minutes)
- Google Sign-In integration for JavaFX desktop (OAuth2 auth code + localhost callback)
- AI Profile Analyzer service and profile history persistence
- JavaFX profile UI updated with AI analyzer card + history table + filtering/sorting
- Flyway migrations on app startup

## Configuration

Use `src/main/resources/application.properties` or environment variables:

- `APP_DB_URL`, `APP_DB_USER`, `APP_DB_PASSWORD`
- `APP_OAUTH_GOOGLE_CLIENTID`, `APP_OAUTH_GOOGLE_CLIENTSECRET`, `APP_OAUTH_GOOGLE_CALLBACKPORT`

Default OAuth callback URI used by app:

`http://localhost:8765/oauth/google/callback`

## Google Cloud Console Setup

1. Create OAuth Client ID of type **Desktop app** (or Web app with localhost redirect).
2. Add redirect URI:
   - `http://localhost:8765/oauth/google/callback`
3. Enable scopes used:
   - `openid`
   - `email`
   - `profile`
4. Copy client ID and secret to config.

## Run

```bash
mvn clean compile
mvn javafx:run
```

## Tests

```bash
mvn -Dtest=ProfileScoreServiceTest test
```

## Notes

- Forgot-password email is currently simulated in console output (`[MOCK-EMAIL]`).
- To move to real SMTP, replace mock sending in `PasswordResetService.requestReset()`.
