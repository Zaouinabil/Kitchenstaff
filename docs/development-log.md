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