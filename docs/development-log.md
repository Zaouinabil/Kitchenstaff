# Journal de développement - Kitchenstaff

## Commit 1 - Initial Kitchenstaff project setup

Ce commit initialise le projet Spring Boot Kitchenstaff.  
Il configure le nom de l’application, la connexion à la base de données et ajoute un endpoint de test `/api/v1/health`.

---

## Commit 2 - Add core entities and repositories

Ce commit ajoute les entités principales du projet : User, Category, Item et Task.  
Il ajoute aussi les enums Role, TaskStatus et TaskPriority, ainsi que les repositories JPA.

---

## Commit 3 - Add category and item REST endpoints

Ce commit ajoute les endpoints REST pour gérer les catégories et les préparations.  
Il permet de créer et consulter les catégories comme Légumes, Sauces et Salades, ainsi que les préparations comme Tomates rondelles ou Mayonnaise.

---

## Commit 4 - Add task DTOs

Ce commit ajoute les DTO nécessaires à la gestion des tâches.  
TaskDto représente les données envoyées par l’API.  
CreateTaskRequest représente les données nécessaires pour créer une tâche.  
UpdateTaskRequest représente les données nécessaires pour modifier une tâche.

---

## Commit 5 - Add task service

Ce commit ajoute le service métier des tâches.  
Il permet de créer, rechercher, modifier, commencer, terminer, annuler et supprimer une tâche.  
Il transforme les entités Task en TaskDto afin d’exposer des données propres via l’API.

---

## Commit 6 - Add task REST controller

Ce commit ajoute le controller REST des tâches.  
Il expose les endpoints permettant de créer, consulter, modifier, commencer, terminer, annuler et supprimer une tâche.  
Ces endpoints représentent le cœur métier de l’application Kitchenstaff : suivre l’avancement de la mise en place du matin.

## Commit 7- Add dashboard progress endpoint

Ce commit ajoute un endpoint de tableau de bord pour suivre l’avancement de la mise en place.
Le dashboard calcule le nombre total de tâches, les tâches à faire, en cours, terminées et annulées.
Il calcule aussi un pourcentage de progression basé sur les tâches terminées.
Cet endpoint permet au chef d’avoir une vue rapide de l’état de la mise en place du jour.

## Commit 8 - Add global error handling

Ce commit améliore la gestion des erreurs de l’API.
Il ajoute une exception ResourceNotFoundException, un modèle ErrorResponse et un GlobalExceptionHandler.
Les ressources introuvables retournent maintenant une réponse 404 claire.
Les erreurs de validation retournent une réponse 400 avec un message compréhensible.
Cette amélioration rend l’API plus propre à tester avec Postman.
## Commit 9- Add initial kitchen prep data

Ce commit ajoute des données initiales pour faciliter les tests de l’application.
Au démarrage, l’application crée automatiquement les catégories principales : Légumes, Sauces et Salades.
Elle crée aussi plusieurs préparations réelles comme Tomates rondelles, Mayonnaise, Sauce tartare, Œufs cuits et Haricots cuits.
Ces données permettent de tester plus facilement la création des tâches sans devoir tout encoder manuellement dans Postman.

## Commit 10- Add user REST endpoints

Ce commit ajoute les premiers endpoints REST liés aux utilisateurs.
Il permet de créer un utilisateur, de lister les utilisateurs et de consulter un utilisateur par identifiant.
Les réponses utilisent UserDto afin de ne pas exposer le mot de passe dans l’API.
Cette étape prépare l’assignation des tâches à un membre de l’équipe.
## Commit 11 - Add user active status endpoint

Ce commit ajoute un endpoint permettant de désactiver un utilisateur.
L’utilisateur n’est pas supprimé de la base de données, mais son champ active passe à false.
Cette approche permet de conserver l’historique des tâches liées à cet utilisateur.

## Commit 12 - Add update user role endpoint

Ce commit ajoute un endpoint permettant de modifier le rôle d’un utilisateur.
Cette fonctionnalité prépare la future gestion des autorisations selon les rôles ADMIN, CHEF et COMMIS.
Elle sera utile lorsque la sécurité avec JWT sera ajoutée.

## Commit 13- Add reactivate user endpoint

Ce commit ajoute un endpoint permettant de réactiver un utilisateur désactivé.
Il complète la fonctionnalité de désactivation sans suppression définitive.
Cette approche permet de conserver l’historique des tâches tout en gardant la possibilité de réactiver un membre de l’équipe.


## Commit 14 - Add user update endpoint

Ce commit ajoute un endpoint permettant de modifier les informations principales d’un utilisateur.
Il permet de mettre à jour le nom et l’email sans exposer ni modifier le mot de passe.
Cette fonctionnalité complète les premières opérations de gestion des utilisateurs dans le back-office.

## Commit 15- Add task filters by status and user

Ce commit améliore la consultation des tâches en ajoutant des filtres optionnels.
L’API permet maintenant de filtrer les tâches par date, par statut et par utilisateur assigné.
Cette fonctionnalité est utile pour le chef, qui peut suivre les tâches terminées ou restantes, et pour les commis, qui peuvent consulter leurs tâches assignées.

## Commit 17 - Add task filter by category

Ce commit ajoute un filtre par catégorie dans la consultation des tâches.
L’API permet maintenant de récupérer les tâches liées à une catégorie précise, par exemple Légumes, Sauces ou Salades.
Cette fonctionnalité est utile pour le chef afin de suivre séparément les préparations de chaque zone de la cuisine froide.

## Commit 18- Add password encoding

Ce commit améliore la sécurité de la gestion des utilisateurs.
Les mots de passe ne sont plus enregistrés en clair dans la base de données.
Ils sont maintenant encodés avec BCrypt avant la sauvegarde.
Cette étape prépare l’authentification sécurisée qui sera ajoutée plus tard avec Spring Security et JWT.

## Commit 19 - Add authentication login endpoint

Ce commit ajoute un endpoint de connexion pour l’application.
L’utilisateur peut se connecter avec son email et son mot de passe.
Le service vérifie le mot de passe encodé avec BCrypt et refuse les identifiants incorrects.
Cette étape prépare l’ajout futur d’un token JWT pour sécuriser les routes de l’API.

## Commit 20 - Add auth response token placeholder

Ce commit prépare la réponse de connexion pour la future authentification JWT.
La réponse contient maintenant les champs token et tokenType.
Pour le moment, le token reste null, mais la structure est prête pour l’intégration du JWT dans une étape suivante.
