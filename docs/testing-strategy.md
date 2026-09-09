# Stratégie de test de Kitchenstaff

Ce document décrit les tests manuels rapportés pendant le développement. Les endpoints et les règles d'accès ont été vérifiés dans le code Spring Boot. Le code Angular n'étant pas présent dans ce dépôt, les scénarios frontend s'appuient sur le [journal de développement](development-log.md) et les informations fournies pour le TFE. Cette vérification documentaire ne constitue pas une nouvelle exécution des tests.

## 1. Objectif des tests

Les tests réalisés pendant le développement servent à vérifier que les différentes parties de Kitchenstaff fonctionnent correctement ensemble. Ils permettent principalement de contrôler :

- le fonctionnement de l'API REST Spring Boot ;
- l'authentification avec un token JWT ;
- les autorisations accordées selon le rôle de l'utilisateur ;
- les opérations CRUD, notamment la création, la lecture, la modification et la suppression des tâches ;
- la communication entre le frontend Angular et le backend Spring Boot.

Les tests permettent également de détecter rapidement une erreur après une modification et de vérifier les principaux scénarios utilisés lors de la démonstration du TFE.

## 2. Tests de l'API avec Postman

Postman est principalement utilisé pour envoyer des requêtes directement à l'API, sans passer par Angular. Cela permet de vérifier les réponses HTTP, les données JSON et le comportement des routes protégées.

Les principaux endpoints testés sont :

| Méthode et endpoint | Vérification principale |
| --- | --- |
| `POST /api/v1/auth/login` | Connexion et réception du token JWT |
| `GET /api/v1/auth/me` | Récupération de l'utilisateur connecté |
| `GET /api/v1/tasks` | Consultation des tâches et utilisation des filtres disponibles |
| `POST /api/v1/tasks` | Création d'une tâche |
| `PUT /api/v1/tasks/{id}` | Modification d'une tâche existante |
| `DELETE /api/v1/tasks/{id}` | Suppression d'une tâche |
| `PATCH /api/v1/tasks/{id}/start` | Passage d'une tâche au statut « en cours » |
| `PATCH /api/v1/tasks/{id}/done` | Passage d'une tâche au statut « terminée » |
| `PATCH /api/v1/tasks/{id}/cancel` | Annulation d'une tâche |
| `GET /api/v1/items` | Consultation des préparations |
| `GET /api/v1/users` | Consultation des utilisateurs par un administrateur |

Pour les opérations de création et de modification, les données envoyées dans le corps de la requête sont contrôlées ainsi que le code HTTP et le contenu de la réponse. Pour une suppression, le résultat attendu est une réponse `204 No Content`.

La création doit retourner `201 Created`. Après une création, une modification ou un changement de statut, une nouvelle lecture permet de vérifier les données enregistrées. Les filtres acceptés par `GET /api/v1/tasks` sont `date` (au format `YYYY-MM-DD`), `status`, `assignedUserId` et `categoryId`. Ce contrôleur ne propose pas de paramètre de priorité ou de recherche textuelle.

## 3. Tests d'authentification

Plusieurs scénarios ont été vérifiés :

- **connexion valide** : une adresse e-mail et un mot de passe corrects doivent retourner un token JWT de type `Bearer` ;
- **mauvais mot de passe** : l'API doit refuser la connexion avec une réponse `401 Unauthorized` ;
- **requête sans token** : une route protégée doit refuser la requête. Le code actuel ne définit pas explicitement de réponse `401` pour ce cas ; il faut relever le code HTTP retourné dans Postman, sans le confondre avec le `401` prévu pour un mauvais mot de passe ;
- **requête avec Bearer Token** : le token est placé dans le header `Authorization` sous la forme `Bearer <token>` et permet d'accéder aux routes autorisées ;
- **accès interdit selon le rôle** : un utilisateur connecté qui ne possède pas le rôle nécessaire doit recevoir une réponse `403 Forbidden`.

Ces tests vérifient à la fois la génération du JWT lors du login et son contrôle par Spring Security lors des requêtes suivantes.

## 4. Tests du frontend Angular

Les vérifications frontend décrites pour le TFE sont principalement manuelles, dans le navigateur. Les scénarios à rejouer sont :

- la connexion ;
- la déconnexion ;
- l'affichage des tâches ;
- la création d'une tâche ;
- la modification d'une tâche ;
- la suppression d'une tâche ;
- le changement de statut : démarrer, terminer ou annuler ;
- les filtres par date, statut et priorité ;
- la recherche dans les tâches ;
- l'affichage du dashboard et de ses totaux ;
- le rafraîchissement de la page, afin de vérifier que le token conservé dans le `localStorage` permet de garder la session et de recharger les données.

Ces vérifications servent aussi à confirmer que les appels HTTP d'Angular atteignent bien Spring Boot, que le token est envoyé dans les requêtes protégées et que l'interface est mise à jour après une action.

## 5. Tests des rôles

Kitchenstaff définit trois rôles :

- `ADMIN` : peut notamment accéder à la gestion des utilisateurs ;
- `CHEF` : peut accéder au dashboard et aux fonctions d'organisation prévues par l'application ;
- `COMMIS` : peut consulter les préparations et utiliser les endpoints liés aux tâches, mais ne peut pas accéder à la gestion des utilisateurs ni au dashboard.

Plusieurs comptes de test, un pour chaque rôle, ont été utilisés pour comparer les réponses et vérifier les différences d'autorisation. Par exemple, `GET /api/v1/users` est réservé à `ADMIN`, tandis que les endpoints `/api/v1/tasks/**` sont accessibles aux trois rôles authentifiés. Le dashboard est réservé à `ADMIN` et `CHEF`.

## 6. Problèmes découverts grâce aux tests

Les tests ont permis d'identifier et de corriger ou mieux comprendre plusieurs problèmes :

- des erreurs `401 Unauthorized` liées à l'authentification, notamment aux identifiants incorrects ; l'absence, la validité, l'expiration et le format d'envoi du token font aussi partie des points à contrôler en cas de refus d'accès ;
- des erreurs `403 Forbidden` lorsqu'un compte authentifié ne possédait pas le rôle demandé ;
- un problème CORS qui empêchait le navigateur d'autoriser certains appels entre Angular et Spring Boot ;
- une `LazyInitializationException` Hibernate pendant le chargement et la transformation des tâches en DTO ;
- le port `8080` déjà utilisé par une autre instance du backend ;
- un blocage de la page Angular sur « Chargement des tâches... » parce que l'état de chargement n'était pas correctement terminé ;
- un problème de données affichées selon la date sélectionnée, qui a nécessité de vérifier la date envoyée au backend et la cohérence des tâches retournées pour cette date.

Ces problèmes montrent l'intérêt de tester séparément l'API, puis l'application complète avec Angular.

## 7. Limites des tests actuels

La stratégie actuelle présente encore certaines limites :

- une partie importante des tests est réalisée manuellement ;
- Postman est l'outil principalement utilisé pour contrôler l'API ;
- les scénarios doivent être rejoués manuellement après certaines modifications ;
- la couverture par des tests automatisés reste limitée et pourrait être développée dans une future version.

Les tests actuels conviennent pour valider les fonctions principales du TFE, mais ils garantissent moins facilement l'absence de régression qu'une suite automatisée complète.

Le dépôt contient déjà un test JUnit `contextLoads()` avec `@SpringBootTest`, qui vérifie le chargement du contexte Spring. Il ne couvre pas à lui seul les endpoints, les opérations CRUD ou les autorisations. Il n'a pas été exécuté lors de cette mise à jour documentaire.

## 8. Améliorations futures

La stratégie de test pourrait être renforcée avec :

- des tests unitaires avec JUnit ;
- des tests Spring Boot pour les services et les controllers ;
- des tests d'intégration couvrant l'API, Spring Security, JPA et la base de données de test ;
- des tests Angular pour les composants, les services HTTP et les formulaires ;
- l'automatisation des tests afin de les exécuter régulièrement et après chaque modification importante.

Ces améliorations permettraient de détecter plus rapidement les régressions et de rendre les futures évolutions de Kitchenstaff plus sûres.
