# Kitchenstaff

Kitchenstaff est une application Web/API développée dans le cadre de mon TFE.
Elle permet d’organiser la mise en place du matin dans une cuisine horeca, principalement pour la cuisine froide.

Le projet est inspiré d’une expérience réelle dans un restaurant à Knokke.
L’objectif est d’aider le chef et l’équipe de cuisine à mieux répartir, suivre et terminer les préparations avant le service.

---

## Objectif du projet

L’application ne gère pas les réservations, les commandes ou le service en salle.

Elle se concentre uniquement sur la préparation avant le service :

- création des préparations ;
- organisation des tâches du jour ;
- assignation des tâches aux membres de l’équipe ;
- suivi de l’avancement ;
- consultation d’un dashboard global.

---

## Contexte métier

Dans une cuisine horeca, la mise en place du matin est une étape importante.
Elle permet de préparer les ingrédients nécessaires avant le début du service.

Exemples de préparations :

- tomates rondelles ;
- tomates au four ;
- oignons ;
- choux blanc ;
- choux rouge ;
- carottes râpées ;
- œufs cuits ;
- haricots cuits ;
- citrons ;
- mayonnaise ;
- sauce tartare ;
- vinaigrette ;
- sauce moules.

Le projet se concentre principalement sur la cuisine froide.

---

## Acteurs

L’application contient trois rôles principaux :

### ADMIN

L’administrateur peut gérer les utilisateurs.

### CHEF

Le chef peut gérer les catégories, les préparations et les tâches.

### COMMIS

Le commis peut consulter les tâches qui lui sont assignées et mettre à jour leur statut.

---

## Fonctionnalités principales

- authentification par email et mot de passe ;
- encodage sécurisé des mots de passe avec BCrypt ;
- génération d’un token JWT après connexion ;
- protection des routes avec Spring Security ;
- gestion des rôles ADMIN, CHEF et COMMIS ;
- gestion des utilisateurs ;
- gestion des catégories ;
- gestion des préparations ;
- gestion des tâches ;
- changement de statut d’une tâche ;
- assignation d’une tâche à un utilisateur ;
- filtres sur les tâches ;
- dashboard global ;
- gestion centralisée des erreurs.

---

## Technologies utilisées

### Backend

- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security
- JWT
- Lombok
- Validation
- MySQL

### Outils

- IntelliJ IDEA
- Postman
- Git
- GitHub
- MySQL Workbench ou phpMyAdmin

---

## Structure du projet

```text
src/main/java/be/kitchenstaff
│
├── config
├── controller
├── dto
├── entity
├── enums
├── exception
├── repository
└── service