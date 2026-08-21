# Exemples d'utilisation de l'API Kitchenstaff

L'API est accessible par défaut à l'adresse `http://localhost:8080`. Les exemples ci-dessous utilisent `curl`.

## Authentification

### Se connecter

`POST /api/v1/auth/login`

Cette route est publique. Elle permet d'obtenir un token JWT à partir d'un email et d'un mot de passe.

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@kitchenstaff.test",
    "password": "password"
  }'
```

Exemple de body JSON :

```json
{
  "email": "admin@kitchenstaff.test",
  "password": "password"
}
```

La réponse contient notamment les informations de l'utilisateur, son rôle et la valeur du token dans le champ `token`.

### Consulter l'utilisateur connecté

`GET /api/v1/auth/me`

```bash
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <token>"
```

Cette route renvoie les informations de l'utilisateur associé au token JWT.

## Utilisation du token JWT

Toutes les routes protégées nécessitent le header HTTP suivant :

```http
Authorization: Bearer <token>
```

Remplacez `<token>` par le token reçu lors de la connexion. Par exemple :

```bash
curl http://localhost:8080/api/v1/tasks \
  -H "Authorization: Bearer <token>"
```

## Tâches

### Consulter toutes les tâches

`GET /api/v1/tasks`

```bash
curl http://localhost:8080/api/v1/tasks \
  -H "Authorization: Bearer <token>"
```

### Filtrer les tâches par date

`GET /api/v1/tasks?date=YYYY-MM-DD`

```bash
curl "http://localhost:8080/api/v1/tasks?date=2026-08-21" \
  -H "Authorization: Bearer <token>"
```

La date doit respecter le format `YYYY-MM-DD`.

### Filtrer les tâches par statut

`GET /api/v1/tasks?status=A_FAIRE`

```bash
curl "http://localhost:8080/api/v1/tasks?status=A_FAIRE" \
  -H "Authorization: Bearer <token>"
```

Les statuts disponibles sont `A_FAIRE`, `EN_COURS`, `TERMINEE` et `ANNULEE`.

### Créer une tâche

`POST /api/v1/tasks`

```bash
curl -X POST http://localhost:8080/api/v1/tasks \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "taskDate": "2026-08-21",
    "itemId": 1,
    "assignedUserId": 3,
    "priority": "HAUTE",
    "quantity": 5.0,
    "comment": "Préparer les tomates pour le service"
  }'
```

Exemple de body JSON :

```json
{
  "taskDate": "2026-08-21",
  "itemId": 1,
  "assignedUserId": 3,
  "priority": "HAUTE",
  "quantity": 5.0,
  "comment": "Préparer les tomates pour le service"
}
```

Le champ `itemId` est obligatoire. Les priorités disponibles sont `BASSE`, `NORMALE`, `HAUTE` et `URGENTE`. Sans priorité précisée, la valeur par défaut est `NORMALE`.

### Modifier une tâche

`PUT /api/v1/tasks/{id}`

Cette route remplace les informations modifiables de la tâche. `taskDate` et `itemId` sont obligatoires.

```bash
curl -X PUT http://localhost:8080/api/v1/tasks/1 \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "taskDate": "2026-08-22",
    "itemId": 1,
    "assignedUserId": 3,
    "priority": "URGENTE",
    "quantity": 7.5,
    "comment": "Préparation prioritaire pour le service"
  }'
```

Exemple de body JSON :

```json
{
  "taskDate": "2026-08-22",
  "itemId": 1,
  "assignedUserId": 3,
  "priority": "URGENTE",
  "quantity": 7.5,
  "comment": "Préparation prioritaire pour le service"
}
```

### Démarrer une tâche

`PATCH /api/v1/tasks/{id}/start`

```bash
curl -X PATCH http://localhost:8080/api/v1/tasks/1/start \
  -H "Authorization: Bearer <token>"
```

### Terminer une tâche

`PATCH /api/v1/tasks/{id}/done`

```bash
curl -X PATCH http://localhost:8080/api/v1/tasks/1/done \
  -H "Authorization: Bearer <token>"
```

### Annuler une tâche

`PATCH /api/v1/tasks/{id}/cancel`

```bash
curl -X PATCH http://localhost:8080/api/v1/tasks/1/cancel \
  -H "Authorization: Bearer <token>"
```

### Supprimer une tâche

`DELETE /api/v1/tasks/{id}`

```bash
curl -X DELETE http://localhost:8080/api/v1/tasks/1 \
  -H "Authorization: Bearer <token>"
```

Une suppression réussie renvoie une réponse sans contenu avec le statut HTTP `204 No Content`.

## Items

### Consulter les items

`GET /api/v1/items`

```bash
curl http://localhost:8080/api/v1/items \
  -H "Authorization: Bearer <token>"
```

Cette route renvoie la liste des préparations disponibles.

## Utilisateurs

### Consulter les utilisateurs

`GET /api/v1/users`

```bash
curl http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer <token>"
```

Cette route est réservée au rôle `ADMIN`.

## Comptes de test

| Rôle | Email | Mot de passe |
| --- | --- | --- |
| `ADMIN` | `admin@kitchenstaff.test` | `password` |
| `CHEF` | `chef@kitchenstaff.test` | `password` |
| `COMMIS` | `commis@kitchenstaff.test` | `password` |

Ces comptes sont créés automatiquement au démarrage de l'application par les données de démonstration.

## Remarques sur les rôles

- `ADMIN` peut accéder aux tâches, aux items et à la gestion des utilisateurs.
- `CHEF` peut accéder aux tâches et aux items, mais pas aux routes de gestion des utilisateurs.
- `COMMIS` peut accéder aux tâches et aux items, mais pas aux routes de gestion des utilisateurs.
- La route de connexion est publique. Les autres routes présentées dans ce document nécessitent un token JWT valide.
- Un accès avec un rôle insuffisant est refusé par Spring Security.
