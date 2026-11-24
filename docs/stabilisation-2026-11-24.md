# Stabilisation du 24 novembre 2026

Le lot corrige les autorisations HTTP, la propriété des tâches COMMIS, l'attribution CHEF, les transactions de conversion DTO et l'identité JWT. Le schéma JPA n'a pas été modifié.

## Configuration locale

L'application charge facultativement `.local/kitchenstaff.properties`, relatif au répertoire de lancement du backend. Ce dossier est exclu de Git. Les valeurs de connexion MySQL locales ont été conservées dans ce fichier privé et la clé JWT locale a été renouvelée. Les anciennes sessions doivent donc se reconnecter après redémarrage. Aucune valeur secrète n'est incluse dans ce document.

Les variables attendues sont `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` et `JWT_SECRET`. Elles peuvent être fournies par l'environnement ou par le fichier local privé. Pour une nouvelle installation, utiliser une clé aléatoire d'au moins 32 octets ; ne jamais copier une clé de démonstration dans Git. Lancer Maven/Java depuis la racine du backend, ou préciser explicitement l'emplacement de configuration externe.

La suppression des valeurs du fichier suivi ne les efface pas de l'historique Git. La clé JWT locale est renouvelée ; le mot de passe MySQL et les mots de passe des comptes existants n'ont pas été changés. Les renouveler s'ils ont été exposés. Ne pas ajouter `.local` à un commit, même avec `git add -f`.

Le seeder est désactivé par défaut. Son activation exige `DEMO_SEED=true` et `DEMO_PASSWORD` non vide, uniquement sur une base de démonstration explicitement choisie. Il ne modifie pas les mots de passe des comptes existants et ne produit pas à lui seul 100 lignes par table. Le jeu historique de tâches reste daté de juillet/août 2026.

`spring.jpa.hibernate.ddl-auto=update` reste inchangé pour préserver la configuration existante. Avant une validation de restauration, choisir `validate` par surcharge externe ; aucun démarrage de l'application contre MySQL n'a été réalisé pendant l'audit.

## Vérification

Utiliser Java 21, déclaré dans `pom.xml` :

```sh
JAVA_HOME="$(/usr/libexec/java_home -v 21)" mvn verify
```

Les tests de contexte et de persistance utilisent explicitement le profil `test`, H2 en mémoire et le seeder désactivé. Les tests de sécurité isolent la chaîne Spring Security ; les tests JWT vérifient réellement signature, expiration et identité. H2 ne remplace pas une recette MySQL complète.

Lors de cet audit, `mvn verify` a réussi : 52 tests, zéro échec, zéro erreur. Les dépendances ont été téléchargées dans un cache temporaire pour ne pas modifier le cache Maven utilisateur. La compilation et le packaging ont également réussi.

## Dump local

Un export de la base existante est conservé dans `.local/kitchenstaff-tfe-20261124.sql`, avec accès fichier privé et exclusion Git. Il contient structure, données et trois clés étrangères. Le dump n'a pas été restauré et le caractère entièrement synthétique des données n'a pas été certifié. Vérifier ces deux points avant de le remettre au jury. Les 7 catégories nécessitent une justification d'exception à la règle des 100 lignes.

## Commits proposés

- `fix(security): enforce task ownership and stable JWT identity`
- `fix(backend): isolate demo seeding and database tests`

Aucun commit ni push n'a été effectué pendant ce travail.
