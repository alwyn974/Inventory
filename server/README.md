# API d'Inventaire - Serveur Ktor

## Description

Cette application d'inventaire est développée avec Ktor et offre une API REST complète pour gérer un système d'inventaire avec authentification, gestion des rôles et stockage d'images via MinIO.

## Fonctionnalités

### Authentification et Autorisation
- Authentification JWT
- Système de rôles : ADMIN, MANAGER, USER, VIEWER
- Permissions granulaires par endpoint
- Gestion des utilisateurs

### Gestion d'Inventaire
- **Items** : Création, lecture, mise à jour, suppression
- **Catégories** : Organisation des items par catégories dynamiques
- **Tags** : Étiquetage flexible des items avec couleurs
- **Dossiers** : Organisation hiérarchique des items
- **Images** : Upload et gestion d'images via MinIO

### Stockage
- Base de données : PostgreSQL (production) / H2 (développement)
- Images : MinIO (compatible S3)

## Installation et Configuration

### Prérequis
- JDK 17+
- MinIO (ou instance locale)
- PostgreSQL (pour la production)

### Variables d'environnement
Copiez `.env.example` vers `.env` et configurez :

```bash
# Base de données
DATABASE_URL=jdbc:postgresql://localhost:5432/inventory
DATABASE_USER=inventory_user
DATABASE_PASSWORD=inventory_password

# MinIO
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET_NAME=inventory-images

# JWT
JWT_SECRET=your-secure-secret-key
```

### Démarrage rapide avec Docker Compose

```bash
# Démarrer MinIO et PostgreSQL
docker-compose up -d

# Construire et démarrer l'application
./gradlew :server:run
```

## API Endpoints

### Authentification

#### POST `/api/v1/auth/login`
Connexion utilisateur
```json
{
  "username": "admin",
  "password": "admin123"
}
```

#### GET `/api/v1/auth/me`
Informations de l'utilisateur connecté (JWT requis)

### Utilisateurs (ADMIN uniquement)

#### GET `/api/v1/users`
Liste tous les utilisateurs

#### POST `/api/v1/users`
Créer un nouvel utilisateur
```json
{
  "username": "newuser",
  "email": "user@example.com",
  "password": "password",
  "role": "USER"
}
```

#### PUT `/api/v1/users/{id}`
Mettre à jour un utilisateur

#### DELETE `/api/v1/users/{id}`
Supprimer un utilisateur

### Items

#### GET `/api/v1/items`
Liste tous les items

#### POST `/api/v1/items`
Créer un nouvel item
```json
{
  "name": "Ordinateur portable",
  "description": "Dell XPS 13",
  "quantity": 5,
  "minQuantity": 2,
  "categoryId": "uuid-category",
  "folderId": "uuid-folder",
  "tagIds": ["uuid-tag1", "uuid-tag2"]
}
```

#### GET `/api/v1/items/{id}`
Récupérer un item spécifique

#### PUT `/api/v1/items/{id}`
Mettre à jour un item

#### DELETE `/api/v1/items/{id}`
Supprimer un item

#### POST `/api/v1/items/{id}/image`
Upload d'image pour un item (multipart/form-data)

### Catégories

#### GET `/api/v1/categories`
Liste toutes les catégories

#### POST `/api/v1/categories`
Créer une nouvelle catégorie
```json
{
  "name": "Électronique",
  "description": "Appareils électroniques"
}
```

#### PUT `/api/v1/categories/{id}`
Mettre à jour une catégorie

#### DELETE `/api/v1/categories/{id}`
Supprimer une catégorie

### Tags

#### GET `/api/v1/tags`
Liste tous les tags

#### POST `/api/v1/tags`
Créer un nouveau tag
```json
{
  "name": "Urgent",
  "color": "#ff0000"
}
```

#### DELETE `/api/v1/tags/{id}`
Supprimer un tag

### Dossiers

#### GET `/api/v1/folders`
Liste tous les dossiers

#### POST `/api/v1/folders`
Créer un nouveau dossier
```json
{
  "name": "Bureau",
  "description": "Équipement de bureau",
  "parentFolderId": "uuid-parent"
}
```

#### PUT `/api/v1/folders/{id}`
Mettre à jour un dossier

#### DELETE `/api/v1/folders/{id}`
Supprimer un dossier

## Rôles et Permissions

### ADMIN
- Accès complet à toutes les fonctionnalités
- Gestion des utilisateurs

### MANAGER
- Gestion complète de l'inventaire
- Pas d'accès à la gestion des utilisateurs

### USER
- Création, lecture, mise à jour des items, catégories, tags, dossiers
- Pas de suppression

### VIEWER
- Lecture seule sur tous les éléments

## Authentification

L'API utilise JWT pour l'authentification. Après connexion, incluez le token dans l'en-tête :
```
Authorization: Bearer <votre-token-jwt>
```

## Compte par défaut

Un compte administrateur est créé automatiquement :
- Username: `admin`
- Password: `admin123`

**⚠️ Changez ce mot de passe en production !**

## Structure de réponse

### Succès
```json
{
  "message": "Operation successful"
}
```

### Erreur
```json
{
  "error": "ERROR_CODE",
  "message": "Description de l'erreur"
}
```

## Développement

### Lancer en mode développement
```bash
./gradlew :server:run --args="--development"
```

### Tests
```bash
./gradlew :server:test
```

## Production

1. Configurez PostgreSQL
2. Configurez MinIO avec des credentials sécurisés
3. Changez la clé JWT secrète
4. Utilisez HTTPS
5. Configurez les CORS selon vos besoins

## Support

Pour toute question ou problème, consultez la documentation ou créez une issue.
