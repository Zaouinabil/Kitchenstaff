# Implémentation de la sécurité de Kitchenstaff

## Objectif de la sécurité

La sécurité de Kitchenstaff doit garantir que seules les personnes authentifiées peuvent accéder aux fonctionnalités protégées et que chaque utilisateur dispose uniquement des autorisations correspondant à son rôle.

Le backend utilise Spring Security et des tokens JWT. L’API fonctionne sans session côté serveur : chaque requête protégée doit donc contenir le token de l’utilisateur. Les mots de passe sont enregistrés sous une forme encodée et ne sont jamais placés dans le JWT.

## Authentification

L’authentification commence lorsque l’utilisateur saisit son adresse e-mail et son mot de passe dans l’application Angular.

Le flux de connexion est le suivant :

1. Angular envoie les identifiants au endpoint public `POST /api/v1/auth/login`.
2. Le backend recherche l’utilisateur à partir de son adresse e-mail.
3. Il vérifie que le compte est actif.
4. Il compare le mot de passe reçu avec le mot de passe encodé dans la base de données.
5. Si les identifiants sont corrects, le backend génère un token JWT et le renvoie au frontend.
6. Si les identifiants sont incorrects ou si le compte est désactivé, la connexion est refusée.

## Génération du token JWT

Le token JWT est généré par le backend après une authentification réussie. Il contient les informations utiles pour identifier l’utilisateur :

- son adresse e-mail comme sujet du token ;
- son identifiant ;
- son nom ;
- son rôle ;
- la date de création et la date d’expiration du token.

Le token est signé avec une clé secrète configurée dans le backend. Cette signature permet de détecter toute modification du contenu. Lorsqu’un token est reçu, Spring Boot vérifie sa signature et sa date d’expiration avant de l’accepter.

## Utilisation du token côté frontend

Après la connexion, Angular sauvegarde le token dans le `localStorage` du navigateur. Il peut ainsi le récupérer pour les appels suivants, y compris après un rafraîchissement de la page.

Pour chaque route protégée, Angular ajoute le token au header HTTP `Authorization` en respectant le format suivant :

```http
Authorization: Bearer <token>
```

Le préfixe `Bearer` est obligatoire et doit être suivi d’un espace puis du token. Le frontend doit supprimer le token enregistré lors de la déconnexion.

## Protection des endpoints

Spring Security contrôle les requêtes avant leur arrivée dans les controllers. Un filtre recherche le header `Authorization`, extrait le token après le préfixe `Bearer` et vérifie sa validité.

Si le token est valide, le backend récupère l’utilisateur correspondant à l’adresse e-mail contenue dans le token. Il vérifie également que ce compte existe toujours et qu’il est actif. L’utilisateur et son rôle sont ensuite ajoutés au contexte de sécurité pour autoriser ou refuser l’accès à l’endpoint demandé.

Les principales règles d’accès actuellement configurées sont :

- `/api/v1/auth/login` et `/api/v1/health` sont publics ;
- `/api/v1/users/**` est réservé au rôle `ADMIN` ;
- `/api/v1/categories/**` est accessible aux rôles `ADMIN` et `CHEF` ;
- `/api/v1/items/**` est accessible aux trois rôles ;
- `/api/v1/dashboard/**` est accessible aux rôles `ADMIN` et `CHEF` ;
- `/api/v1/tasks/**` est accessible aux trois rôles ;
- les autres routes nécessitent par défaut une authentification.

Une requête sans authentification valide vers une route protégée provoque normalement une erreur `401 Unauthorized`. Un utilisateur authentifié qui ne possède pas le rôle requis reçoit une erreur `403 Forbidden`.

## Gestion des rôles

Kitchenstaff définit trois rôles :

- `ADMIN` : accès complet, notamment à la gestion des utilisateurs et aux fonctions d’administration ;
- `CHEF` : gestion des tâches et suivi de la mise en place ;
- `COMMIS` : consultation et exécution des tâches.

Dans Spring Security, ces rôles sont représentés par des autorités préfixées par `ROLE_`, par exemple `ROLE_ADMIN`. Les autorisations sont vérifiées côté backend afin qu’une fonctionnalité ne soit pas protégée uniquement par son affichage dans Angular.

## Comptes de test

Les comptes suivants sont créés pour faciliter les tests et la démonstration :

| Rôle | Adresse e-mail | Mot de passe |
|---|---|---|
| `ADMIN` | `admin@kitchenstaff.test` | `password` |
| `CHEF` | `chef@kitchenstaff.test` | `password` |
| `COMMIS` | `commis@kitchenstaff.test` | `password` |

Ces identifiants sont uniquement destinés à un environnement local, de test ou de démonstration. Ils ne doivent pas être utilisés tels quels en production.

## Exemple de requête protégée

L’exemple suivant consulte les tâches en transmettant le JWT obtenu lors de la connexion :

```http
GET /api/v1/tasks HTTP/1.1
Host: localhost:8080
Authorization: Bearer <token>
Accept: application/json
```

Le même appel peut être réalisé avec `curl` :

```bash
curl http://localhost:8080/api/v1/tasks \
  -H "Authorization: Bearer <token>" \
  -H "Accept: application/json"
```

Avant de transmettre la requête au controller des tâches, Spring Security vérifie le JWT et confirme que l’utilisateur possède l’un des rôles autorisés.

## Difficultés rencontrées

### Gestion du token JWT

Le token doit être sauvegardé après la connexion, ajouté correctement à chaque requête protégée et supprimé lors de la déconnexion. Un token absent, mal formé ou expiré n’est pas accepté par le backend.

### Erreurs 401 Unauthorized

Une erreur `401 Unauthorized` indique généralement que l’utilisateur n’est pas authentifié. Elle peut être provoquée par l’absence du header `Authorization`, l’oubli du préfixe `Bearer`, un token invalide ou un token expiré.

### Erreurs 403 Forbidden

Une erreur `403 Forbidden` signifie généralement que l’utilisateur est bien authentifié, mais que son rôle ne lui permet pas d’accéder à la ressource demandée. Par exemple, un `COMMIS` ne peut pas accéder à la gestion des utilisateurs.

### Configuration CORS entre Angular et Spring Boot

Angular et Spring Boot peuvent fonctionner sur des adresses ou des ports différents pendant le développement. Le backend doit donc autoriser explicitement l’origine du frontend, les méthodes HTTP utilisées et les headers nécessaires, dont `Authorization`. Une configuration CORS incorrecte peut bloquer une requête dans le navigateur avant même son traitement par l’API.

## Limites et améliorations futures

L’implémentation actuelle fournit une base adaptée au projet, mais plusieurs améliorations pourraient renforcer la sécurité :

- utiliser des clés secrètes différentes et robustes selon l’environnement, sans les conserver dans le dépôt ;
- prévoir un mécanisme de renouvellement avec un refresh token ;
- permettre la révocation d’un token avant sa date d’expiration ;
- limiter les tentatives de connexion afin de réduire les attaques par force brute ;
- renforcer la journalisation et le suivi des tentatives d’accès refusées ;
- utiliser HTTPS en production ;
- étudier le stockage du token dans un cookie `HttpOnly`, `Secure` et correctement configuré plutôt que dans le `localStorage`, car une faille XSS pourrait exposer les données accessibles au JavaScript ;
- remplacer les comptes et mots de passe de démonstration par une gestion sécurisée propre à l’environnement de production.
