-- PHASE 1 - JALON 3 : STRUCTURE DE LA BASE DE DONNÉES

DROP TABLE IF EXISTS message CASCADE;
DROP TABLE IF EXISTS channel_member CASCADE;
DROP TABLE IF EXISTS channel CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Table des utilisateurs
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    login VARCHAR(50) UNIQUE NOT NULL,
    pwd VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'user'
);

-- Table des canaux
CREATE TABLE channel (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('public', 'private')),
    creator_id INT REFERENCES users(id) ON DELETE SET NULL
);

-- Table d'association pour l'appartenance des membres aux canaux
CREATE TABLE channel_member (
    channel_id INT REFERENCES channel(id) ON DELETE CASCADE,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    is_room_admin BOOLEAN DEFAULT false,
    PRIMARY KEY (channel_id, user_id)
);

-- Table des messages
CREATE TABLE message (
    id SERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    channel_id INT REFERENCES channel(id) ON DELETE CASCADE
);


-- ALIMENTATION EN DONNÉES JOUETS (3 utilisateurs, 2 canaux, 10 messages)

INSERT INTO users (login, pwd, role) VALUES 
('jean', 'jean123', 'user'),
('paul', 'paul456', 'user'),
('admin_api', 'rootpwd', 'admin');

INSERT INTO channel (name, type, creator_id) VALUES 
('General', 'public', 1),       -- Créé par jean
('Projet-Secret', 'private', 2); -- Créé par paul

INSERT INTO channel_member (channel_id, user_id, is_room_admin) VALUES 
(1, 1, false), -- jean dans General
(1, 2, false), -- paul dans General
(1, 3, false); -- admin_api dans General

INSERT INTO channel_member (channel_id, user_id, is_room_admin) VALUES 
(2, 2, true),  -- paul est membre et admin du canal privé
(2, 3, false); -- admin_api est simple membre (jean n'y est pas)

INSERT INTO message (content, user_id, channel_id) VALUES 
-- 7 messages dans le canal public
('Hello tout le monde !', 1, 1),
('Salut Jean, ça va ?', 2, 1),
('Oui et toi ? Prêt pour le projet de Web Services ?', 1, 1),
('Carrément, l’environnement de travail est prêt !', 2, 1),
('Des admins en ligne pour vérifier le serveur ?', 1, 1),
('Oui jean, je surveille le bon fonctionnement.', 3, 1),
('Super, merci admin.', 1, 1),

-- 3 messages dans le canal privé
('Voici les documents confidentiels du projet.', 2, 2),
('Attention, Jean ne doit pas y avoir accès.', 2, 2),
('Entendu, c''est bien noté.', 3, 2);