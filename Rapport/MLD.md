# **Modèle Logique de Données (MLD)**

##  1. Table users
Contient les informations de profil et d'authentification des utilisateurs.
* **`id`** (INT, Clé Primaire, Auto-incrémenté)
* **`login`** (VARCHAR, Unique, Non Null)
* **`pwd`** (VARCHAR, Non Null)
* **`role`** (VARCHAR, Par défaut 'user')


##  2. Table channel
Représente les différents salons de discussion (canaux) publics ou privés.
* **`id`** (INT, Clé Primaire, Auto-incrémenté)
* **`name`** (VARCHAR, Non Null)
* **`type`** (VARCHAR, Contrainte : doit être `'public'` ou `'private'`)
* **`creator_id`** (INT, Clé Étrangère)

> **Contrainte d'intégrité :** `#creator_id` est une clé étrangère en référence à `users(id)`.


##  3. Table channel_member
Table d'association matérialisant l'adhésion d'un utilisateur à un canal (liaison N,N). Elle permet notamment de restreindre l'accès aux canaux privés.
* **`channel_id`** (INT, Clé Étrangère)
* **`user_id`** (INT, Clé Étrangère)
* **`is_room_admin`** (BOOLEAN, Par défaut `false`)
=
> **Contraintes d'intégrité :**
> * Clé primaire composite sur (`#channel_id`, `#user_id`).
> * `#channel_id` est une clé étrangère en référence à `channel(id)`.
> * `#user_id` est une clé étrangère en référence à `users(id)`.


##  4. Table message
Stocke l'historique des messages persistants envoyés dans l'application.
* **`id`** (INT, Clé Primaire, Auto-incrémenté)
* **`content`** (TEXT, Non Null)
* **`created_at`** (TIMESTAMP, Par défaut l'heure courante)
* **`user_id`** (INT, Clé Étrangère)
* **`channel_id`** (INT, Clé Étrangère)

> **Contraintes d'intégrité :**
> * `#user_id` est une clé étrangère en référence à `users(id)`.
> * `#channel_id` est une clé étrangère en référence à `channel(id)`.