# Spécification Technique : Architecture REST

## Principes Généraux de l'API

* **Format des données :** Toutes les requêtes avec corps (Body) et toutes les réponses s'échangent exclusivement au format **JSON** avec un encodage **UTF-8**.
* **Architecture Sans état :** Conformément aux contraintes REST.
* **Authentification Basic Auth :** L'authentification se fait à la volée. Le client doit transmettre ses identifiants à **chaque requête** via l'en-tête HTTP : 
  `Authorization: Basic <jeton_base64>`.
* **Sécurisation des données :** Toutes les routes `GET` forcent la désactivation du cache navigateur (`cache: 'no-store'`) pour éviter l'affichage de données sensibles après une déconnexion locale.

---

##  1. Ressource : Authentification (`/api/login`)

| Méthode | Endpoint | Description | Code Succès | Codes Erreur |
|:---:|---|---|:---:|---|
| **POST** | `/api/login` | Vérifie les identifiants en base de données et renvoie les informations publiques de l'utilisateur pour initialiser l'interface client. | `200 OK` | `400 Bad Request`<br>`401 Unauthorized` |

**Exemple de corps (Login) :**

```json
{
  "login": "jean",
  "pwd": "jean123"
}
```

##  2. Ressource : Canaux (`/api/channels`)

| Méthode | Endpoint | Description | Code Succès | Codes Erreur |
|:---:|---|---|:---:|---|
| **GET** | `/api/channels` | Récupère les canaux accessibles. Liste les canaux de type public ainsi que les canaux privés dont l'utilisateur authentifié est membre. | `200 OK` | `401 Unauthorized`<br>`500 Internal Error` |
| **POST** | `/api/channels` | Crée un nouveau canal. L'utilisateur authentifié (via le header Basic) est automatiquement défini comme créateur et administrateur du salon. | `201 Created` | `400 Bad Request`<br>`401 Unauthorized`<br>`500 Internal Error` |
| **POST** | `/api/channels/members` | Invite un utilisateur à rejoindre un canal privé existant en envoyant le `login` du membre à ajouter. | `201 Created` | `400 Bad Request`<br>`401 Unauthorized`<br>`403 Forbidden`<br>`404 Not Found`<br>`500 Internal Error` |

**Exemple de corps (Création d'un canal) :**

```json
{
  "name": "Projet-Secret",
  "type": "private"
}
```

**Exemple de corps (Invitation d'un membre à un canal privé) :**

```json
{
  "channelId": 1,
  "login": "paul"
}
```

##  3. Ressource : Messages (`/api/messages`)

La gestion des droits y est stricte : l'accès en lecture/écriture est bloqué pour les non-membres des canaux privés, et la modification/suppression est réservée aux auteurs ou aux utilisateurs possédant le rôle admin.

| Méthode | Endpoint | Description | Code Succès | Codes Erreur |
|:---:|---|---|:---:|---|
| **GET** | `/api/messages?channelId=X` | Récupère l'historique des messages du canal spécifié. L'accès est bloqué par un `403` si le canal est privé et que le requérant n'est pas membre. | `200 OK` | `400 Bad Request`<br>`401 Unauthorized`<br>`403 Forbidden`<br>`404 Not Found` |
| **POST** | `/api/messages` | Publie un nouveau message. L'ID de l'auteur est automatiquement et sécuritairement extrait du jeton d'authentification Basic. | `201 Created` | `400 Bad Request`<br>`401 Unauthorized`<br>`403 Forbidden`<br>`404 Not Found` |
| **PUT** | `/api/messages/{id}` | Modifie le contenu textuel d'un message existant. Action strictement réservée à l'auteur du message ou à un administrateur du système. | `200 OK` | `400 Bad Request`<br>`401 Unauthorized`<br>`403 Forbidden`<br>`404 Not Found` |
| **DELETE** | `/api/messages/{id}` | Supprime définitivement un message du système. Action strictement réservée à l'auteur du message ou à un administrateur du système. | `204 No Content` | `400 Bad Request`<br>`401 Unauthorized`<br>`403 Forbidden`<br>`404 Not Found` |

**Exemple de corps (Création d'un message) :**

```json
{
  "channelId": 1,
  "content": "Ceci est un test d'envoi via l'API REST."
}
```

**Exemple de corps (Modification d'un message) :**

```json
{
  "content": "Message corrigé après relecture."
}
```

