# Kitchenstaff Pro — démonstration Stripe test

Cette intégration utilise le véritable Checkout hébergé par Stripe, uniquement dans un environnement de test. Le backend fixe l'abonnement à **1 990 centimes EUR par mois, quantité 1, par établissement**, avec un prix hors TVA (`tax_behavior=exclusive`). Aucun montant, identifiant de prix ou propriétaire transmis par le navigateur ne choisit le prix facturé.

Le calcul automatique de TVA n'est pas activé dans cette démonstration. Il ne s'agit pas d'une implémentation fiscale prête à la commercialisation. Les menus et buffets restent hors périmètre.

**Un parcours réel sur le sandbox Stripe nécessite les clés de votre compte et un listener webhook actif.** Les tests automatisés avec Stripe simulé ne prouvent pas qu'un paiement a été effectué sur ce compte. Consigner séparément le résultat de la recette manuelle décrite ici.

## Modèle et API

| Table ajoutée | Rôle et relations |
| --- | --- |
| `establishments` | Nom et propriétaire ADMIN ; FK vers `users.id`, propriétaire unique : un établissement par ADMIN dans cette version. |
| `billing_subscriptions` | Une ligne par établissement ; statut, références Stripe, session Checkout, tentative idempotente et dates ; FK unique vers l'établissement. |
| `stripe_webhook_events` | Journal minimal des événements traités : identifiant Stripe en clé primaire, type, établissement et date ; aucun payload brut ni donnée bancaire. |

Les références Stripe sont sensibles à la casse dans le script MySQL. Les relations n'utilisent pas de suppression en cascade. Les tables `users`, `categories`, `items` et `tasks` restent inchangées. Ce modèle isole la **facturation** par propriétaire ; il ne transforme pas les tâches et utilisateurs existants en architecture multi-établissements.

| Endpoint | Droit et comportement |
| --- | --- |
| `POST /api/v1/billing/establishment` | ADMIN actif ; corps `{"name":"Cuisine de démonstration"}` ; crée son propre établissement, jamais celui d'un autre utilisateur. |
| `GET /api/v1/billing/subscription` | ADMIN actif ; lit son établissement et le statut persisté. `configured` décrit la configuration de test ; `confirmed=true` exige le statut `active`. |
| `POST /api/v1/billing/checkout` | ADMIN actif ; corps `{}` ; crée ou reprend un Checkout pour son établissement et renvoie son URL Stripe. |
| `POST /api/v1/stripe/webhook` | Public au niveau JWT pour permettre la livraison Stripe ; signature obligatoire sur le corps brut, événements test uniquement. |

Le retour `/billing/success` ne modifie rien en base. L'écran interroge le backend, lequel n'active l'abonnement qu'après un webhook vérifié et une lecture du statut courant auprès de Stripe. Le retour `/billing/cancel` indique un abandon de Checkout ; il ne résilie pas un abonnement existant.

Une tentative de Checkout est enregistrée avant l'appel Stripe et sert de clé d'idempotence. La même session ouverte est reprise après abandon ou erreur réseau. Un abonnement existant non terminé bloque la création d'un second abonnement. Une tentative sans identifiant de session et restée incertaine plus de 23 heures exige une réconciliation dans Stripe avant de recommencer.

Les événements sont dédupliqués par leur identifiant. Leur traitement et la mise à jour de l'abonnement partagent une transaction. Le serveur relit l'abonnement Stripe courant sous verrou local, pour éviter qu'un ancien événement ne rétablisse un ancien statut.

## 1. Préparer MySQL sans modifier les données existantes

**État de cette machine au 24 novembre 2026 : migration déjà appliquée à `kitchenstaff_db`.** Les trois tables de facturation sont présentes et vides ; les volumes existants sont restés 120/100/232/7. Les commandes de création ci-dessous servent à une autre installation et ne doivent pas être rejouées sur cette base. La migration et 19 tests de facturation ont également été validés sur une base MySQL distincte, avec réponses Stripe simulées.

Le script [001-billing-test.sql](sql/001-billing-test.sql) contient uniquement trois créations de tables. Vérifier le serveur et la base sélectionnés, conserver un dump de sauvegarde, puis relever avant/après les quatre volumes existants. Le bilan précédent observait **120 utilisateurs, 7 catégories, 100 items et 232 tâches** ; ne pas remplacer une vérification actuelle par ces nombres historiques.

Configurer, si nécessaire, un chemin de connexion MySQL avec un mot de passe saisi interactivement ; ne jamais placer sa valeur dans la commande :

```sh
mysql_config_editor set --login-path=kitchenstaff_tfe --host=127.0.0.1 --port=3306 --user=UTILISATEUR_A_REMPLACER --password
```

Avant la migration, vérifier que les trois nouvelles tables sont absentes. Si elles existent déjà, inspecter leur structure ; ne pas les supprimer et ne pas relancer le script aveuglément :

```sh
mysql --login-path=kitchenstaff_tfe --database=kitchenstaff_db --execute="SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('establishments','billing_subscriptions','stripe_webhook_events');"
```

Depuis la racine du backend, exécuter une seule fois le script si les trois tables sont absentes :

```sh
mysql --login-path=kitchenstaff_tfe --database=kitchenstaff_db < docs/sql/001-billing-test.sql
```

Ne pas ajouter `--force` : arrêter à la première erreur. Le DDL MySQL n'est pas entièrement annulable par un rollback ; si l'exécution est partielle, examiner les tables créées et appliquer uniquement les créations manquantes après revue.

Les contrôles [002-billing-verification.sql](sql/002-billing-verification.sql) sont en lecture seule et n'affichent ni noms, ni emails, ni hashes de mots de passe, ni données bancaires :

```sh
mysql --login-path=kitchenstaff_tfe --database=kitchenstaff_db < docs/sql/002-billing-verification.sql
```

Après migration, utiliser `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` au lancement pour vérifier le schéma sans laisser Hibernate modifier les tables existantes. Ne pas utiliser le profil de test H2 comme configuration du serveur de démonstration.

## 2. Choisir le bon environnement Stripe

Dans le Dashboard Stripe, sélectionner un **sandbox** ou l'environnement de test du compte prévu. Conserver la même sélection pour les clés, le CLI et les événements. Les cartes de test ne déplacent pas de fonds. [Environnements et moyens de test Stripe](https://docs.stripe.com/testing)

Dans les clés API de cet environnement, récupérer une **clé secrète de test** commençant par `sk_test_`. Le code accepte aussi une clé restreinte `rk_test_` disposant des droits nécessaires à Checkout et à la lecture des abonnements. Il refuse toute clé live, même si Stripe est désactivé. Aucun réglage du projet ne permet d'activer le mode live.

Aucune clé publique `pk_`, aucun Stripe.js et aucun identifiant de prix créé manuellement ne sont nécessaires : Angular redirige vers l'URL Checkout fournie par le backend ; le produit et le prix récurrent sont définis côté serveur. Ne pas saisir de coordonnées bancaires dans Kitchenstaff.

## 3. Installer et connecter Stripe CLI

Sur ce Mac, si le CLI n'est pas installé :

```sh
brew install stripe
stripe --version
stripe login
```

La connexion ouvre un parcours d'autorisation dans le navigateur. Choisir uniquement le compte/sandbox de démonstration. Si l'accès CLI est désactivé, l'administrateur du compte peut l'autoriser dans les paramètres **MCP and CLI access** du Dashboard. [Installation officielle](https://github.com/stripe/stripe-cli#installation), [connexion CLI](https://docs.stripe.com/cli/login)

Dans un premier terminal, lancer le transfert des événements suivants, sans option `--live` :

```sh
stripe listen \
  --events checkout.session.completed,checkout.session.expired,customer.subscription.created,customer.subscription.updated,customer.subscription.deleted,invoice.paid,invoice.payment_failed \
  --events-from @self \
  --forward-to http://localhost:8080/api/v1/stripe/webhook
```

Garder ce terminal ouvert. Il fournit un secret de signature `whsec_` pour ce listener : le copier uniquement dans la configuration locale du backend. Un secret de webhook créé dans le Dashboard n'est pas interchangeable avec celui du listener CLI. Le transfert local ne nécessite pas d'endpoint public enregistré. [Référence `stripe listen`](https://docs.stripe.com/cli/listen)

Le SDK utilisé est `com.stripe:stripe-java:34.1.0`, avec la version API `2026-10-28.endive`. Les événements attendus sont des **snapshots**. Pour un endpoint HTTPS enregistré dans le Dashboard, choisir ce format, cette version API et la même liste d'événements. Le listener CLI utilise par défaut la version du compte ; configurer la version de test correspondante si nécessaire. Ne pas sélectionner `--latest` sans vérifier la version reçue. [Réception des webhooks](https://docs.stripe.com/webhooks), [SDK Java 34.1.0](https://github.com/stripe/stripe-java/releases/tag/v34.1.0)

## 4. Configurer et lancer le backend

Les variables sont :

| Variable | Valeur attendue |
| --- | --- |
| `STRIPE_ENABLED` | `true` pour cette démonstration ; défaut `false`. |
| `STRIPE_SECRET_KEY` | Clé secrète du même environnement de test Stripe ; aucune valeur réelle dans Git. |
| `STRIPE_WEBHOOK_SECRET` | Secret de signature du listener actif ou de l'endpoint HTTPS sélectionné. |
| `APP_PUBLIC_URL` | `http://localhost:4200` pour la démonstration locale. |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `validate` après migration. |

Les variables existantes `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` et `JWT_SECRET` restent nécessaires, ou sont fournies par le fichier privé `.local/kitchenstaff.properties` déjà mis en place. Le backend doit être lancé depuis sa racine pour charger ce fichier. Un fichier `.env` est ignoré par Git mais n'est **pas chargé automatiquement** par cette application.

Dans un deuxième terminal **zsh**, saisir les secrets sans les afficher ni les inscrire littéralement dans l'historique :

```sh
cd '/Users/nabilzaoui/Downloads/Projet Intelij/Kitchenstaff'
read -rs 'STRIPE_SECRET_KEY?Clé secrète Stripe TEST : '
printf '\n'
read -rs 'STRIPE_WEBHOOK_SECRET?Secret du webhook TEST : '
printf '\n'
export STRIPE_SECRET_KEY STRIPE_WEBHOOK_SECRET
export STRIPE_ENABLED=true
export APP_PUBLIC_URL=http://localhost:4200
export SPRING_JPA_HIBERNATE_DDL_AUTO=validate
JAVA_HOME="$(/usr/libexec/java_home -v 21)" mvn spring-boot:run
```

Dans IntelliJ, fournir les mêmes variables dans la configuration d'exécution locale, sans partager la configuration contenant leurs valeurs. Ne pas coller de clé dans une capture, une issue, une conversation ou un commit. Le mode désactivé laisse les fonctionnalités précédentes disponibles ; le paiement annonce alors qu'il n'est pas configuré.

Dans un troisième terminal, lancer Angular :

```sh
cd /Users/nabilzaoui/kitchenstaff-frontend
CI=true npm start
```

`CI=true` évite le cache natif Angular problématique observé sur cette machine ; il ne désactive pas les contrôles TypeScript. Pour compiler : `CI=true npm run build`.

## 5. Recette manuelle à consigner

Utiliser un ADMIN de démonstration actif. Depuis `/pro`, créer son établissement si nécessaire puis utiliser le bouton de souscription. Les tests suivants sont à exécuter sur le sandbox configuré, puis à marquer réellement réussis ou échoués dans le dossier de recette.

| Scénario | Manipulation | Résultat à constater |
| --- | --- | --- |
| Succès | Sur Checkout test uniquement : carte `4242 4242 4242 4242`, date future, CVC de test. | Checkout terminé ; webhook livré ; `/billing/success` affiche le statut `active` confirmé par le serveur ; MySQL contient le même statut. |
| Annulation de Checkout | Revenir à Kitchenstaff depuis Checkout avant de payer. | Page `/billing/cancel` ; aucune activation artificielle. Le prochain essai reprend la session ouverte. |
| Refus | Dans Checkout test : carte `4000 0000 0000 0002`. | Refus visible chez Stripe ; Kitchenstaff ne devient pas `active` à cause de l'URL. Un réessai reste possible tant que la session est ouverte. |
| Retour falsifié | Ouvrir directement `/billing/success` avant toute souscription. | Pas d'abonnement actif ; seul le statut backend est affiché. |
| Signature invalide | Envoyer la requête ci-dessous lorsque Stripe est configuré. | HTTP 400 ; aucun événement traité ni changement d'abonnement. |
| Même événement répété | Renvoyer deux fois un événement réellement reçu et associé à cet établissement. | Réponses réussies ; une seule ligne pour son identifiant ; statut inchangé après la répétition. |
| Utilisateur non autorisé | Se connecter CHEF ou COMMIS et tenter `POST /api/v1/billing/checkout` avec son JWT via l'outil de recette. | HTTP 403. Sans JWT : HTTP 401. Aucun appel Checkout autorisé. |
| Propriétaire distinct | Se connecter avec un autre ADMIN. | Il ne voit ni ne souscrit pour l'établissement du premier ADMIN ; aucun identifiant de propriétaire n'est accepté du navigateur. |
| Persistance | Rejouer le script de vérification MySQL et recharger la page. | Statut conservé en base ; quatre volumes historiques inchangés par la facturation. |

Les deux numéros cités sont les cartes publiques de simulation Stripe, à saisir uniquement sur Checkout test ; utiliser une date future et un CVC de test. [Cartes Stripe de test](https://docs.stripe.com/testing)

Requête volontairement invalide, sans secret réel :

```sh
curl -i http://localhost:8080/api/v1/stripe/webhook \
  -H 'Content-Type: application/json' \
  -H 'Stripe-Signature: invalid' \
  --data '{}'
```

Pour vérifier l'idempotence, relever dans Stripe/CLI un identifiant `evt_...` du paiement Kitchenstaff. Le listener doit rester actif. Remplacer la valeur symbolique ci-dessous par cet identifiant, puis lancer deux fois la commande, toujours sans `--live` :

```sh
stripe events resend IDENTIFIANT_EVT_DU_PAIEMENT
```

Le CLI actuel peut retransmettre un événement à son listener local. Si un endpoint HTTPS est utilisé, ajouter `--webhook-endpoint=IDENTIFIANT_ENDPOINT_TEST`. [Renvoi d'événements](https://docs.stripe.com/cli/events/resend)

Pour ce seul identifiant, vérifier `COUNT(*) = 1` dans `stripe_webhook_events`. Le nombre total d'événements peut dépasser un pour un paiement : les événements Checkout, abonnement et facture ont des identifiants distincts. Un simple `stripe trigger` non relié à l'établissement Kitchenstaff ne prouve pas le parcours d'abonnement et ne doit pas activer un compte.

Pour répéter une démonstration de souscription déjà active, utiliser un autre ADMIN de démonstration ou résilier l'abonnement **dans le Dashboard Stripe test**, puis attendre son webhook `customer.subscription.deleted`. Ne pas supprimer des lignes MySQL pour remettre l'écran à zéro.

## Vérification automatisée et preuve MySQL

Les tests Java et Angular se lancent sans clés Stripe réelles :

```sh
JAVA_HOME="$(/usr/libexec/java_home -v 21)" mvn verify
```

```sh
npm test -- --watch=false
CI=true npm run build
```

Les tests automatiques isolent le réseau Stripe pour vérifier le prix, les permissions, les signatures, la réservation de Checkout, la déduplication et les statuts. Une exécution avec H2 valide la logique relationnelle ; elle ne remplace pas la recette Stripe en sandbox ni la vérification MySQL. Les résultats effectivement exécutés sont consignés dans le bilan du lot ; cette procédure ne transforme pas les scénarios manuels en tests déjà passés.

Après un vrai Checkout test confirmé, la requête de statut du script `002-billing-verification.sql` doit montrer `active`, une référence abonnement présente et des événements traités. Conserver cette preuve, en masquant les identifiants sensibles dans les captures destinées au jury. Ne pas exporter le contenu bancaire de Stripe : Kitchenstaff n'en stocke aucun.

Les trois nouvelles tables représentent une vraie relation établissement/abonnement et un journal d'événements. Elles peuvent contenir très peu de lignes dans une démonstration ; ne pas inventer 100 paiements ou événements Stripe pour atteindre un quota. Documenter cette situation et faire valider l'exception par le professeur, ou préparer un jeu de données de test distinct clairement identifié. Le dump antérieur à Stripe ne contient pas ces nouvelles tables : produire et restaurer un nouveau dump de remise après la recette, avec revue des données avant partage.

## Scénario de défense — 2 minutes

Préparer avant le passage : backend, frontend et listener démarrés ; environnement Stripe test visible ; ADMIN connecté ; établissement de démonstration sans abonnement actif ; aucun secret affiché dans les terminaux partagés.

| Temps | Démonstration et phrase utile |
| --- | --- |
| 0:00–0:20 | Ouvrir Kitchenstaff Pro : « L'abonnement est de 19,90 € HTVA par mois et par établissement. Cette démonstration utilise uniquement Stripe test. » Montrer le nom de l'établissement et le statut initial. |
| 0:20–0:45 | Cliquer Souscrire : « Le serveur impose le prix et vérifie le rôle ADMIN ; les coordonnées bancaires sont saisies chez Stripe. » Montrer Checkout avec sa mention de test. |
| 0:45–1:15 | Saisir la carte de succès de test et confirmer. Revenir à Kitchenstaff. |
| 1:15–1:40 | Montrer le statut confirmé : « Le retour navigateur n'active rien ; le webhook signé déclenche la mise à jour en base. » Montrer la requête MySQL préparée avec statut et références présentes, sans données privées. |
| 1:40–2:00 | Passer brièvement en EN/NL ; montrer le résultat préparé de refus COMMIS ou d'événement rejoué : « Les permissions restent côté API et une notification répétée n'est traitée qu'une fois. » |

Si le webhook tarde, expliquer le statut en attente et actualiser. Ne pas annoncer un succès non confirmé ni modifier le statut à la main.

## Limites assumées

- Un établissement par ADMIN ; pas de transfert de propriété ni d'administration de plusieurs établissements.
- La facturation est isolée par propriétaire, tandis que les données cuisine gardent leur modèle existant.
- Aucun portail client, remboursement, résiliation depuis Angular, prorata, changement de formule ou calcul fiscal automatique.
- Les tâches existantes ne sont pas bloquées par un abonnement inactif : la démonstration de paiement ne supprime aucune fonctionnalité cuisine.
- Les statuts Stripe non actifs sont affichés ; `trialing` n'est pas assimilé à un paiement confirmé et aucun essai gratuit n'est créé par ce parcours.
- La disponibilité du réseau et du listener est nécessaire ; en cas de résultat réseau incertain, le même Checkout est repris. Une tentative ancienne non résolue demande une vérification dans Stripe.
- Des produits/prix et parfois des Customers de test supplémentaires peuvent être créés au fil des nouvelles souscriptions. Aucun nettoyage automatique du compte Stripe n'est effectué.
- La mise en production et l'activation live ne font pas partie de ce travail ; le code les refuse.

Message de commit proposé : `feat(billing): add test-only Stripe subscriptions with verified webhooks`.
