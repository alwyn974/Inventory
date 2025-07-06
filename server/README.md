# Inventory API Server

A REST API server for managing inventory items, categories, tags, and folders built with Ktor and Kotlin.

## Features

- **Authentication**: JWT-based authentication system
- **Items Management**: Create, read, update, delete inventory items
- **Categories**: Organize items by categories
- **Tags**: Tag items for better organization
- **Folders**: Hierarchical folder structure for items
- **Image Upload**: Upload and manage item images with MinIO
- **Database**: PostgreSQL with Exposed ORM
- **API Documentation**: OpenAPI/Swagger documentation

## Tech Stack

- **Kotlin** - Programming language
- **Ktor** - Web framework
- **Exposed** - SQL framework and ORM
- **PostgreSQL** - Database
- **MinIO** - Object storage for images
- **JWT** - Authentication
- **Koin** - Dependency injection
- **Docker** - Containerization

## Getting Started

### Prerequisites

- JDK 11 or higher
- PostgreSQL database
- MinIO server (for image storage)
- Docker (optional)

### Environment Variables

Create a `.env` file in the server directory with the following variables:

```env
# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/inventory
DATABASE_USER=your_db_user
DATABASE_PASSWORD=your_db_password

# MinIO Configuration
MINIO_URL=http://localhost:9000
MINIO_ACCESS_KEY=your_minio_access_key
MINIO_SECRET_KEY=your_minio_secret_key
MINIO_BUCKET=inventory-images

# JWT Configuration
JWT_SECRET=your_jwt_secret_key
JWT_ISSUER=inventory-api
JWT_AUDIENCE=inventory-users

# Server Configuration
PORT=8080
```

### Running the Server

#### With Gradle

```bash
# Build and run
./gradlew :server:run

# Or build JAR and run
./gradlew :server:buildFatJar
java -jar server/build/libs/server-all.jar
```

#### With Docker

```bash
# Build and run with Docker Compose
docker-compose up --build
```

### API Documentation

Once the server is running, you can access:

- **API Documentation**: `http://localhost:8080/docs`
- **OpenAPI Spec**: `http://localhost:8080/openapi.json`
- **Health Check**: `http://localhost:8080/health`

## API Endpoints

### Authentication

- `POST /api/v1/auth/login` - User login
- `GET /api/v1/auth/me` - Get current user info

### Items

- `GET /api/v1/items` - List all items
- `GET /api/v1/items/{id}` - Get specific item
- `POST /api/v1/items` - Create new item
- `PATCH /api/v1/items/{id}` - Update item
- `DELETE /api/v1/items/{id}` - Delete item
- `POST /api/v1/items/{id}/image` - Upload item image

### Categories

- `GET /api/v1/categories` - List all categories
- `GET /api/v1/categories/{id}` - Get specific category
- `POST /api/v1/categories` - Create new category
- `PATCH /api/v1/categories/{id}` - Update category
- `DELETE /api/v1/categories/{id}` - Delete category

### Tags

- `GET /api/v1/tags` - List all tags
- `GET /api/v1/tags/{id}` - Get specific tag
- `POST /api/v1/tags` - Create new tag
- `PATCH /api/v1/tags/{id}` - Update tag
- `DELETE /api/v1/tags/{id}` - Delete tag

### Folders

- `GET /api/v1/folders` - List all folders
- `GET /api/v1/folders/{id}` - Get specific folder
- `POST /api/v1/folders` - Create new folder
- `PATCH /api/v1/folders/{id}` - Update folder
- `DELETE /api/v1/folders/{id}` - Delete folder

## Database Schema

The server uses PostgreSQL with the following main tables:

- `users` - User accounts
- `items` - Inventory items
- `categories` - Item categories
- `tags` - Item tags
- `folders` - Hierarchical folders
- `item_tags` - Many-to-many relationship between items and tags

## Image Storage

Images are stored in MinIO object storage. When an image is uploaded:

1. The image is validated for type and size
2. A unique filename is generated
3. The image is stored in MinIO
4. The image URL is saved in the database

## Security

- JWT tokens are required for all API endpoints (except login)
- Passwords are hashed using bcrypt
- CORS is configured for cross-origin requests
- Rate limiting is implemented to prevent abuse

## Development

### Project Structure

```
server/
├── src/main/kotlin/re/alwyn974/inventory/
│   ├── Application.kt              # Main application setup
│   ├── routes/                     # API route definitions
│   │   ├── AuthRoutes.kt
│   │   ├── ItemRoutes.kt
│   │   └── CategoryTagFolderRoutes.kt
│   ├── service/                    # Business logic services
│   │   ├── DatabaseFactory.kt
│   │   ├── JwtService.kt
│   │   ├── PasswordService.kt
│   │   └── S3Service.kt
│   └── model/                      # Data models and DTOs
├── src/main/resources/
│   ├── logback.xml                 # Logging configuration
│   └── scalar.html                 # API documentation template
└── build.gradle.kts                # Build configuration
```

### Building

```bash
# Build the project
./gradlew :server:build

# Run tests
./gradlew :server:test

# Generate fat JAR
./gradlew :server:buildFatJar
```

### Configuration

The server configuration is handled through environment variables and can be customized in `Application.kt`.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
