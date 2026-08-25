# Scénario de démonstration

Ce scénario permet de présenter les principales fonctionnalités de Kitchenstaff dans un ordre simple et logique.

## Comptes de test

| Rôle | Adresse e-mail | Mot de passe |
| --- | --- | --- |
| ADMIN | `admin@kitchenstaff.test` | `password` |
| CHEF | `chef@kitchenstaff.test` | `password` |
| COMMIS | `commis@kitchenstaff.test` | `password` |

## Préparation de l'environnement

1. Démarrer MySQL et vérifier que la base de données Kitchenstaff est accessible.
2. Démarrer le backend Spring Boot et vérifier qu'il se connecte correctement à MySQL.
3. Démarrer le frontend Angular et ouvrir l'application dans le navigateur.

## Déroulement de la démonstration

1. Se connecter avec le compte CHEF : `chef@kitchenstaff.test` / `password`.
2. Consulter la liste des tâches du jour afin de présenter la mise en place à réaliser.
3. Utiliser le filtre par date pour afficher les tâches d'une autre journée, puis revenir à la date du jour.
4. Utiliser le filtre par statut pour distinguer les tâches à faire, en cours et terminées.
5. Utiliser le filtre par priorité pour faire ressortir les tâches les plus urgentes.
6. Utiliser la recherche pour retrouver rapidement une tâche à partir de son nom ou d'un mot-clé.
7. Créer une nouvelle tâche de test en complétant les informations demandées, notamment sa date, son statut et sa priorité.
8. Modifier cette tâche afin de montrer que ses informations peuvent être corrigées ou mises à jour.
9. Démarrer une tâche et vérifier que son statut passe à « En cours ».
10. Terminer la tâche et vérifier que son statut passe à « Terminée ».
11. Supprimer la tâche de test afin de montrer la gestion complète du cycle de vie d'une tâche.
12. Consulter le dashboard summary et vérifier que les totaux affichés correspondent à l'état actuel des tâches.
13. Se déconnecter de l'application.

## Points à expliquer au jury

- **Rôle du chef :** organiser la mise en place, créer et attribuer les tâches, définir leurs priorités et suivre leur avancement.
- **Rôle du commis :** consulter les tâches qui lui sont destinées et mettre à jour leur statut pendant la préparation.
- **Utilité de la mise en place du matin :** préparer les ingrédients, le matériel et les postes avant le service pour travailler plus efficacement et éviter les oublis.
- **Sécurité JWT :** authentifier les utilisateurs, sécuriser les échanges avec l'API et limiter les actions selon le rôle de la personne connectée.
- **Filtres et suivi des tâches :** retrouver rapidement les informations utiles et visualiser l'avancement global de la préparation.
- **Lien avec l'expérience réelle en cuisine :** l'application répond à un besoin concret observé sur le terrain, où la coordination, les priorités et le suivi sont essentiels avant le début du service.
