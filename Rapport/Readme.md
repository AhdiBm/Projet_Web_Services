#  Polytech Chat - Journal de Bord

Ce projet consiste en un service de journal de bord et de messagerie en ligne persistant (canaux publics et privés), inspiré d'applications comme Slack ou Discord.

---

## Architecture du Projet

Le projet respecte une architecture logicielle multicouche découplant totalement la persistance, la logique métier et l'interface utilisateur.

```text
[ Client Graphique (SPA) ] 
       │ (Requêtes HTTP + En-tête Basic Auth)
       ▼
[ Couche Contrôleur (Servlets) ] <───> [ Sérialisation (Jackson JSON) ]
       │ (Appels DAO)
       ▼
[ Couche Persistance (DAOs) ] 
       │ (JDBC)
       ▼
[ Base de Données (PostgreSQL) ]

```

1. **Client (Front-end) :** Une Single Page Application propre en HTML5/CSS3 et JavaScript moderne.
2. **Contrôleur (Back-end) :** Servlets Jakarta (`HttpServlet`) interceptant les requêtes et manipulant les ressources.
3. **Persistance (DAO) :** Implémentation JDBC (`ChannelDAOJDBC`, `MessageDAOJDBC`, `UserDAOJDBC`) exécutant les requêtes SQL vers le serveur relationnel.
4. **Base de Données :** Système de Gestion de Base de Données PostgreSQL assurant la cohérence et la persistance des données.

---

##  Choix de Conception Technologiques

### 1. Architecture Sans état

Pour respecter scrupuleusement les contraintes du style architectural REST, le serveur ne maintient aucune session mémoire. Chaque requête HTTP est isolée et contient intrinsèquement l'intégralité des informations nécessaires à son traitement.

### 2. Authentification HTTP Basic sécurisée applicativement

L'authentification est réalisée à la volée grâce à l'en-tête standardisé `Authorization: Basic <token_base64>` transmis par le client à chaque appel.

* Côté serveur, une méthode utilitaire extrait et décode le jeton Base64 afin de valider l'identité du demandeur directement en base de données avant d'exécuter la logique métier.

### 3. Contrôle d'accès et Sécurité métier

* **Canaux Privés :** Une jointure SQL s'assure qu'un canal privé n'est visible et accessible en lecture/écriture que par son créateur ou par les utilisateurs explicitement enregistrés comme membres dans la table d'association `channel_member`.
* **Intégrité des Messages :** La modification (`PUT`) ou la suppression (`DELETE`) d'un message valide dynamiquement les droits : seul l'auteur d'origine du message ou un utilisateur possédant le rôle `admin` est autorisé à altérer la ressource.

---

##  Prérequis et Instructions de Déploiement

### Prérequis

* **Java Development Kit (JDK 17 ou supérieur)**
* **Apache Tomcat (v11.x.x)**
* **PostgreSQL** monté avec le script SQL d'initialisation fourni

### 1. Compilation du Back-end

Placez-vous à la racine du projet et compilez les classes Java directement vers le dossier des classes déployées de Tomcat :

```powershell
javac -cp "tomcat/lib/*;tomcat/webapps/JournalAPI/WEB-INF/lib/*" -d tomcat/webapps/JournalAPI/WEB-INF/classes src/dto/*.java src/dao/*.java src/controleur/*.java

```

### 2. Lancement du Serveur

Exécutez le script de démarrage Tomcat :

```powershell
.\tomcat\bin\startup.bat

```

---

##  Guide de Test du Système

### Option A : Test via l'Interface Web

1. Ouvrez votre navigateur et accédez à l'adresse : `http://localhost:8080/JournalAPI/`
2. Connectez-vous avec l'un des comptes de test (ex: `jean` / `jean123` ou `paul` / `paul456` ou `admin_api` / `rootpwd`).
3. **Scénario Public :** Créez un canal public. Envoyez des messages, modifiez-les ou supprimez-les. Basculez sur l'autre compte pour voir les messages s'actualiser en temps réel toutes les 3 secondes.
4. **Scénario Privé :** Connectez-vous avec `paul`, créez un canal `private`. Déconnectez-vous et connectez-vous avec `jean` : le canal privé est invisible pour Jean, garantissant la parfaite étanchéité des droits.

### Option B : Test de l'API REST via Bruno

Une collection d'API **Bruno** est disponible dans le dossier `Journal de Bord API/`.

> ⚠️ **Important :** L'architecture étant Stateless, les requêtes Bruno s'auto-identifient. Pour chaque requête exécutée (hors Login), veillez à aller dans l'onglet **Auth**, sélectionner **Basic Auth**, et saisir les identifiants d'un utilisateur valide (Username/Password).

* **POST | `api/login` :** Permet de valider la conformité des identifiants (Body JSON attendu avec `login` et `pwd`).
* **GET | `api/channels` :** Récupère la liste des canaux filtrés selon l'utilisateur configuré dans l'onglet *Auth*.
* **POST | `api/channels` :** Crée un canal public ou privé (Body JSON avec `name` et `type`).
* **POST | `api/channels/members` :** Invite un utilisateur à rejoindre un canal privé existant (Body JSON avec `channelId` et `login`).
* **GET | `api/messages?channelId=X` :** Récupère l'historique d'une room (renvoie un code `403 Forbidden` si le canal est privé et que l'utilisateur configuré n'est pas membre).
* **POST | `api/messages` :** Publie un message (Body JSON avec `channelId` et `content`).
* **PUT | `api/messages/{id}` :** Modifie un message (Body JSON avec `content`). Échoue si l'utilisateur *Auth* n'est pas l'auteur.
* **DELETE | `api/messages/{id}` :** Supprime définitivement un message du système.


## Comptes de Test Suggerés

- `jean` / `jean123`
- `paul` / `paul456`
- `admin_api` / `rootpwd`


## Fichiers utiles

- Dossier de la collection Bruno : `Journal de Bord API/`
- Script SQL d'initialisation : `database.sql`

---