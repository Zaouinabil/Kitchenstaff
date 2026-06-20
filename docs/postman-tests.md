# Tests API avec Postman

Ce document présente les principaux tests réalisés avec Postman pour vérifier le bon fonctionnement de l’API Kitchenstaff.

---

## 1. Vérifier que l’API fonctionne

### Requête

GET /api/v1/health

### Résultat attendu

Kitchenstaff API is running

---

## 2. Connexion utilisateur

### Requête

POST /api/v1/auth/login

### Body

```json
{
  "email": "admin@kitchenstaff.test",
  "password": "password"
}


---

# 3. Ajouter une ligne dans le README

Dans ton `README.md`, ajoute une petite section :

```markdown
## Documentation des tests API

Les tests principaux de l’API sont décrits dans le fichier :

```text
docs/api/postman-tests.md


---

# 4. Ajouter au journal de développement

Dans :

```text
docs/development-log.md