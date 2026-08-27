# Architecture technique de Kitchenstaff

## Vue générale de l’architecture

Kitchenstaff est une application web organisée selon une architecture client-serveur. Le frontend Angular constitue l’interface utilisée par les membres de la cuisine. Il communique avec une API REST développée avec Spring Boot. Cette API applique les règles métier et utilise une base de données MySQL pour conserver les informations.

Schéma simplifié :

```text
Utilisateur → Angular → API REST Spring Boot → Spring Data JPA → MySQL
```

Les responsabilités sont ainsi séparées : Angular gère l’affichage et les interactions, Spring Boot traite la logique métier et MySQL assure la persistance des données.

## Architecture backend

Le backend est développé avec Spring Boot. Il expose des routes REST qui permettent au frontend de consulter et de modifier les données de l’application.

Il est principalement organisé en plusieurs couches :

- les **controllers** reçoivent les requêtes HTTP et renvoient les réponses ;
- les **services** contiennent la logique métier de l’application ;
- les **repositories** fournissent l’accès aux données avec Spring Data JPA ;
- les **entities** représentent les données enregistrées dans MySQL ;
- les **DTO** transportent les données entre l’API et ses clients sans exposer directement les entités.

Les principales entités métier sont :

- `User` : représente un utilisateur de l’application ;
- `Category` : regroupe les préparations par catégorie ;
- `Item` : représente une préparation ou un élément de mise en place ;
- `Task` : représente une tâche à réaliser en cuisine.

## Architecture frontend

Le frontend est développé avec Angular. Il fournit les écrans permettant à l’utilisateur de se connecter, de consulter la mise en place et d’interagir avec les catégories, les items, les tâches et les utilisateurs selon ses autorisations.

Angular appelle l’API REST Spring Boot au moyen de requêtes HTTP. Il affiche ensuite les données reçues et transmet au backend les actions effectuées par l’utilisateur. La logique métier principale reste centralisée dans Spring Boot.

## Base de données

Les données de Kitchenstaff sont stockées dans une base de données relationnelle MySQL. Elles concernent notamment les utilisateurs, les catégories, les items et les tâches.

Spring Data JPA assure la communication entre le backend et MySQL. Les repositories permettent d’effectuer les opérations courantes de création, lecture, modification et suppression. JPA réalise la correspondance entre les entités Java (`User`, `Category`, `Item` et `Task`) et les tables de la base de données.

## Sécurité et JWT

Spring Security protège les routes de l’API. Lorsqu’un utilisateur se connecte avec des identifiants valides, le backend lui fournit un token JWT. Ce token permet à l’API d’identifier l’utilisateur lors des requêtes suivantes sans lui demander de se reconnecter à chaque action.

Pour accéder à une route protégée, Angular envoie le token dans le header HTTP suivant :

```http
Authorization: Bearer <token>
```

Le backend vérifie la validité du JWT et les autorisations associées à l’utilisateur avant de traiter la requête. Les mots de passe ne doivent pas être transmis avec chaque appel protégé.

## Communication frontend/backend

La communication entre Angular et Spring Boot repose sur l’API REST et le protocole HTTP. Le frontend envoie des requêtes telles que `GET`, `POST`, `PUT` ou `DELETE` selon l’opération souhaitée. Les données échangées sont principalement au format JSON.

Le déroulement général d’un appel est le suivant :

1. Angular envoie une requête HTTP à une route de l’API.
2. Pour une route protégée, le JWT est ajouté au header `Authorization`.
3. Spring Security contrôle le token et les droits de l’utilisateur.
4. Le controller transmet la demande au service concerné.
5. Le service applique la logique métier et utilise un repository si des données doivent être lues ou enregistrées.
6. Spring Data JPA communique avec MySQL.
7. Spring Boot renvoie une réponse HTTP, généralement en JSON, qu’Angular affiche à l’utilisateur.

## Organisation des rôles

Kitchenstaff distingue trois rôles :

- `ADMIN` : rôle d’administration, notamment pour la gestion globale et la gestion des utilisateurs ;
- `CHEF` : rôle responsable de l’organisation et du suivi de la mise en place ;
- `COMMIS` : rôle destiné à la consultation et à la réalisation des tâches attribuées.

Ces rôles permettent d’adapter l’accès aux fonctionnalités. Le contrôle des autorisations est effectué côté backend afin qu’une restriction ne dépende pas uniquement de l’affichage du frontend.

## Flux principal d’utilisation

1. L’utilisateur ouvre l’application Angular et saisit ses identifiants.
2. Angular transmet la demande de connexion à l’API Spring Boot.
3. Spring Boot vérifie les identifiants et renvoie un token JWT si la connexion est valide.
4. Angular conserve le token pour l’envoyer avec les prochaines requêtes protégées.
5. L’utilisateur consulte les catégories, les items et les tâches de mise en place disponibles selon son rôle.
6. Lorsqu’il réalise une action, Angular appelle la route REST correspondante avec le JWT.
7. Spring Security vérifie l’accès, puis Spring Boot exécute la logique métier.
8. Spring Data JPA lit ou met à jour les données dans MySQL.
9. Le résultat est renvoyé à Angular, qui actualise l’interface.
