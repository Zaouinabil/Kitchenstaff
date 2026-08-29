# Modèle de base de données de Kitchenstaff

## Vue générale du modèle de données

Kitchenstaff utilise une base de données relationnelle MySQL pour conserver les informations nécessaires à la mise en place du matin. Spring Data JPA assure la correspondance entre les entités Java et les tables de la base de données.

Le modèle repose sur quatre tables principales :

- `users` pour les utilisateurs ;
- `categories` pour les catégories de préparations ;
- `items` pour les éléments à préparer ;
- `tasks` pour les tâches quotidiennes de mise en place.

Chaque table possède une clé primaire `id` générée automatiquement.

## Table users

La table `users` contient les comptes qui peuvent utiliser l’application.

Principaux champs :

- `id` : identifiant unique de l’utilisateur ;
- `name` : nom de l’utilisateur ;
- `email` : adresse e-mail, obligatoire et unique ;
- `password` : mot de passe enregistré de manière sécurisée par l’application ;
- `role` : niveau d’autorisation de l’utilisateur ;
- `active` : indique si le compte est actif.

Les rôles disponibles sont :

- `ADMIN` : administre l’application et les utilisateurs ;
- `CHEF` : organise et suit la mise en place ;
- `COMMIS` : consulte et réalise les tâches qui lui sont confiées.

## Table categories

La table `categories` permet de regrouper les items par famille de préparations, par exemple les légumes, les sauces ou les desserts.

Principaux champs :

- `id` : identifiant unique de la catégorie ;
- `name` : nom obligatoire et unique de la catégorie ;
- `description` : description facultative de la catégorie.

Une catégorie peut être associée à plusieurs items.

## Table items

La table `items` contient les préparations ou éléments qui peuvent faire l’objet d’une tâche de mise en place.

Principaux champs :

- `id` : identifiant unique de l’item ;
- `name` : nom de la préparation ;
- `unit` : unité utilisée pour mesurer la quantité, par exemple `kg`, `litre` ou `portion` ;
- `active` : indique si l’item peut encore être utilisé ;
- `category_id` : clé étrangère vers la catégorie de l’item.

Chaque item appartient obligatoirement à une catégorie. Une catégorie peut contenir plusieurs items.

## Table tasks

La table `tasks` représente le travail de mise en place à effectuer pour une date donnée.

Principaux champs :

- `id` : identifiant unique de la tâche ;
- `task_date` : date prévue pour la tâche ;
- `status` : état actuel de la tâche ;
- `priority` : niveau de priorité ;
- `quantity` : quantité à préparer, avec au maximum deux décimales ;
- `comment` : remarque facultative ;
- `user_id` : clé étrangère facultative vers l’utilisateur assigné ;
- `item_id` : clé étrangère obligatoire vers l’item concerné ;
- `created_at` : date et heure de création ;
- `updated_at` : date et heure de dernière modification.

Les statuts possibles sont :

- `A_FAIRE` ;
- `EN_COURS` ;
- `TERMINEE` ;
- `ANNULEE`.

Les priorités possibles sont :

- `BASSE` ;
- `NORMALE` ;
- `HAUTE` ;
- `URGENTE`.

## Relations entre les tables

Schéma textuel simplifié :

```text
Category 1---N Item
Item     1---N Task
User     1---N Task
```

Ces relations signifient que :

- une `Category` peut contenir plusieurs `Item` ;
- un `Item` appartient à une seule `Category` ;
- un `Item` peut être concerné par plusieurs `Task` ;
- une `Task` concerne obligatoirement un seul `Item` ;
- un `User` peut avoir plusieurs `Task` assignées ;
- une `Task` peut être assignée à un `User`, mais elle peut aussi rester temporairement sans utilisateur.

Dans la base de données, `items.category_id` relie un item à sa catégorie. Les champs `tasks.item_id` et `tasks.user_id` relient une tâche à son item et, si elle est attribuée, à son utilisateur.

## Règles métier principales

- Une adresse e-mail ne peut appartenir qu’à un seul utilisateur.
- Un utilisateur possède un rôle parmi `ADMIN`, `CHEF` et `COMMIS`.
- Un utilisateur ou un item peut être désactivé grâce au champ `active` sans être supprimé directement.
- Le nom d’une catégorie est obligatoire et unique.
- Un item doit toujours appartenir à une catégorie.
- Une tâche doit toujours concerner un item.
- L’assignation d’une tâche à un utilisateur est facultative.
- Une nouvelle tâche reçoit par défaut le statut `A_FAIRE` et la priorité `NORMALE`.
- Si aucune date de tâche n’est fournie lors de sa création, la date du jour est utilisée.
- Les dates de création et de mise à jour d’une tâche sont gérées automatiquement.

## Exemple de données de démonstration

L’exemple suivant illustre un jeu de données simple pour une mise en place du matin :

### Utilisateurs

| id | name | email | role | active |
|---:|---|---|---|---|
| 1 | Sophie | sophie@kitchenstaff.be | `CHEF` | true |
| 2 | Lucas | lucas@kitchenstaff.be | `COMMIS` | true |

### Catégorie

| id | name | description |
|---:|---|---|
| 1 | Légumes | Préparations de légumes pour le service |

### Items

| id | name | unit | active | category_id |
|---:|---|---|---|---:|
| 1 | Oignons émincés | kg | true | 1 |
| 2 | Carottes taillées | kg | true | 1 |

### Tâches

| id | task_date | status | priority | quantity | item_id | user_id | comment |
|---:|---|---|---|---:|---:|---:|---|
| 1 | 2026-08-29 | `EN_COURS` | `HAUTE` | 3.00 | 1 | 2 | Pour le service du midi |
| 2 | 2026-08-29 | `A_FAIRE` | `NORMALE` | 2.50 | 2 | 2 | Tailler en brunoise |

Dans cet exemple, les deux items appartiennent à la catégorie « Légumes » et les deux tâches sont assignées à Lucas. Sophie possède le rôle de chef et peut organiser le travail de mise en place.
