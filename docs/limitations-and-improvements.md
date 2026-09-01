# Limites actuelles et améliorations futures de Kitchenstaff

## Fonctionnalités déjà réalisées

Kitchenstaff dispose actuellement des fonctionnalités principales nécessaires à la gestion de la mise en place du matin dans une cuisine horeca :

- une authentification sécurisée à l'aide de jetons JWT ;
- une gestion des utilisateurs selon trois rôles : `ADMIN`, `CHEF` et `COMMIS` ;
- la gestion des tâches de préparation ;
- la création, la modification et la suppression de tâches ;
- le changement de statut des tâches afin de suivre leur avancement ;
- des filtres par date, statut et priorité ;
- une fonction de recherche permettant de retrouver rapidement une tâche ;
- un tableau de bord récapitulatif (*dashboard summary*) donnant une vue d'ensemble des tâches ;
- des données de démonstration réalistes, adaptées au fonctionnement d'une cuisine professionnelle.

Ces fonctionnalités constituent une base fonctionnelle pour organiser le travail de l'équipe et suivre la progression de la mise en place.

## Limites actuelles du projet

Même si l'application répond à ses objectifs principaux, elle présente encore plusieurs limites :

- l'interface utilisateur reste simple et pourrait être rendue plus moderne et intuitive ;
- aucune notification en temps réel n'est encore envoyée lors de la création ou de la modification d'une tâche ;
- l'application ne conserve pas encore d'historique détaillé des modifications et des actions réalisées par les utilisateurs ;
- les tâches ne peuvent pas encore être exportées au format PDF ou Excel ;
- la gestion des stocks et des ingrédients n'est pas intégrée ;
- la séparation des permissions entre les différents rôles reste perfectible ;
- l'application a principalement été testée dans un environnement local et n'a pas encore été validée dans des conditions réelles de production.

Ces limites ne remettent pas en cause le fonctionnement actuel de Kitchenstaff, mais elles définissent les principaux axes de progression du projet.

## Améliorations futures possibles

Plusieurs évolutions pourraient être envisagées afin d'enrichir l'application :

- améliorer le design et l'expérience utilisateur (UI/UX), notamment pour une utilisation rapide sur tablette ou smartphone ;
- ajouter des notifications pour informer l'équipe lors de l'attribution ou de la création de nouvelles tâches ;
- mettre en place un historique des actions afin de connaître l'auteur et la date de chaque modification ;
- proposer des statistiques avancées sur les tâches, les délais de réalisation et la charge de travail ;
- permettre l'export des tâches aux formats PDF et Excel ;
- intégrer une gestion des stocks, des ingrédients et des seuils d'alerte ;
- ajouter un planning hebdomadaire pour anticiper et répartir les préparations ;
- améliorer la gestion des rôles avec des permissions plus précises et configurables ;
- déployer l'application en ligne afin de la tester dans un environnement proche des conditions réelles ;
- compléter les tests automatisés du backend et du frontend, notamment avec des tests d'intégration et des tests de bout en bout.

Ces améliorations pourraient être développées progressivement en fonction des besoins des utilisateurs et des priorités du projet.

## Conclusion

Kitchenstaff fournit déjà une solution fonctionnelle pour centraliser, organiser et suivre les tâches de mise en place dans une cuisine horeca. Le projet couvre les besoins essentiels définis pour cette première version, tout en laissant plusieurs possibilités d'évolution.

Les limites identifiées constituent des pistes concrètes pour poursuivre le développement de l'application. À terme, ces améliorations permettraient de rendre Kitchenstaff plus complet, plus agréable à utiliser et mieux adapté à un déploiement dans un environnement professionnel.
