# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Superkiller Admin is a Spring Boot + Vue 3 admin management system with Maven multi-module structure. It provides a scaffolding platform with common enterprise features: user/role/menu management, notifications, file center, audit logs, scheduled tasks, and system monitoring.

## Build Commands

### Backend (sak-service)
```bash
cd sak-service
mvn spring-boot:run                    # Run locally
mvn clean package                       # Build distribution package
mvn clean package -pl sak-service       # Build from root
```

### Frontend (sak-page)
```bash
cd sak-page
corepack pnpm install                   # Install dependencies
corepack pnpm dev                       # Development server
corepack pnpm build                     # Production build
```

### Maven Multi-Module (from root)
```bash
mvn clean package                       # Build all modules
mvn clean package -pl sak-service      # Backend only
mvn clean package -pl sak-page          # Frontend only
mvn clean package -pl sak-page -Dfrontend.skip.install=true   # Skip pnpm install
mvn clean package -pl sak-page -Dfrontend.skip.build=true     # Skip frontend build
```

## Architecture

### Maven Modules
- **sak-service**: Spring Boot backend (Java 17)
- **sak-page**: Vue 3 admin frontend (Vite, TypeScript, Arco Design)
- **sak-mobile**: Mobile variant of the frontend

### Backend Structure (sak-service)
```
com.sak.service/
├── controller/     # REST API endpoints
├── service/        # Business logic
├── mapper/         # MyBatis-Plus data access
├── entity/         # Database entities
├── dto/            # Data transfer objects
├── vo/             # View objects
├── config/         # Spring configuration classes
├── security/       # JWT authentication & filters
├── filter/         # Servlet filters
├── handler/        # Exception handlers
├── job/            # Scheduled task implementations
├── websocket/      # WebSocket handlers
└── util/           # Utility classes
```

### Security Architecture
- JWT-based stateless authentication
- Access token (10min) + Refresh token (7 days)
- BCrypt password encoding
- Multi-level cache: Caffeine (local) + Redis
- Captcha validation on login
- MFA support (TOTP)

### Frontend Structure (sak-page)
```
src/
├── api/            # Backend API clients
├── router/         # Vue Router configuration
├── stores/          # Pinia state management
├── views/          # Page components
├── components/     # Reusable components
└── types/          # TypeScript type definitions
```

### Key API Endpoints
| Prefix | Description |
|--------|-------------|
| `/auth` | Login, logout, captcha, MFA |
| `/user` | Current user profile, avatar, password |
| `/system/users` | User management |
| `/system/roles` | Role management |
| `/system/menus` | Menu/permission management |
| `/system/config` | System configuration |
| `/system/notifications` | Notification management |
| `/system/exports` | Export records |
| `/system/files` | File center |
| `/system/monitor` | System monitoring |
| `/system/schedules` | Scheduled tasks |
| `/system/online-users` | Online user sessions |
| `/system/logs` | Operation logs |
| `/site-messages` | Site messages |
| `/ws/site-messages` | WebSocket for real-time messages |

## Database

- **Location**: `db/schema.sql` (DDL) and `db/seed.sql` (initial data)
- **Upgrade scripts**: `db/upgrade/*.sql` (incremental migrations)
- **ORM**: MyBatis-Plus with auto-convert underscore to camelCase

## Configuration

### Profile-based Configuration
- Default: `application.yml` with `spring.profiles.active=local`
- Environment-specific: `application-local.yml`, `application-aliyun.yml`

### Environment Variables
Key environment variables (see `docs/environment-variables.md`):
- `DB.HOST`, `DB.PORT`, `DB.USERNAME`, `DB.PASSWORD` - Database
- `REDIS.HOST`, `REDIS.PORT`, `REDIS.PASSWORD` - Redis
- `JWT.SECRET`, `JWT.AT.EXPIRE`, `JWT.RT.EXPIRE` - JWT settings
- `MAIL.HOST`, `MAIL.USERNAME`, `MAIL.PASSWORD` - Email
- `WXPUSHER.*` - WeChat push notifications
- `STORAGE.SHARE.*` - File sharing settings

### External Dependencies
- **ip2region**: Place `./ip2region/ip2region_v4.xdb` for IP geolocation in login logs

## Development Notes

### Adding Scheduled Task Types
Implement `ScheduledTaskDefinitionHandler` interface. On application startup, task type metadata auto-syncs to `sys_scheduled_task_type` table. See `docs/schedule-task-extension.md`.

### Notification Channels
Currently supports: in-app notifications, email, WxPusher (WeChat). Add new channels by implementing notification handlers.

### Multi-Level Cache
Default: 5min local (Caffeine) + 30min Redis. Configurable via `cache.multilevel.*`.

### Distribution Package
`mvn clean package` in sak-service produces:
```
target/sak-service-1.0.0-dist/
├── app/        # Business jar
├── lib/        # Dependencies
├── config/     # Config files
├── sql/        # SQL scripts
├── static/     # Static resources
└── bin/        # Startup scripts
```
