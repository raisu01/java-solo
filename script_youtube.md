# 🎬 Script YouTube — Remote Terminal en Java (JavaFX)
> **Durée estimée :** 12–18 minutes  
> **Public cible :** Étudiants / développeurs Java débutants/intermédiaires  
> **Ton :** Pédagogique, dynamique, conversationnel

---

## ⏱️ INTRO — [0:00 – 0:45]

> 🎥 *À l'écran : démo rapide de l'appli en action (client qui tape une commande, le résultat s'affiche en temps réel)*

**[Paroles]**

« Bonjour à tous ! Dans cette vidéo, on va vous présenter notre projet de Java Avancé.

Le but était de créer un logiciel de contrôle à distance d'ordinateur en Java — similaire à SSH — où un client peut envoyer des commandes à un serveur distant qui les exécute et renvoie le résultat en temps réel.

On va vous expliquer le code chacun notre partie, et on terminera par une démonstration live du projet qui tourne. »

---

## ⏱️ PRÉSENTATION DU PROJET — [0:45 – 1:30]

> 🎥 *À l'écran : schéma d'architecture (tableau/slide) — Client ↔ Réseau TCP ↔ Serveur ↔ Shell OS*

**[Paroles]**

« Le projet est composé de deux programmes qui communiquent via **TCP/IP** sur le port 9999.

Le **Serveur** écoute les connexions entrantes. Dès qu'il reçoit une commande, il l'exécute sur son propre système d'exploitation et renvoie le résultat.

Le **Client** se connecte au serveur, envoie des commandes, et affiche le résultat en temps réel dans un terminal graphique.

Pour construire ça, on a utilisé quatre concepts clés : les **Sockets**, le **multithreading**, l'**exécution de processus système**, et une interface graphique **JavaFX**.

Notre groupe s'est réparti le travail en trois parties — le serveur, le client, et l'interface — qu'on va vous présenter dans cet ordre. »

---

## ⏱️ STRUCTURE DU PROJET — [1:30 – 2:00]

> 🎥 *À l'écran : arborescence des fichiers dans l'IDE*

**[Paroles]**

« Le projet est organisé en 4 packages. `common` pour les constantes partagées, `server` pour la logique serveur, `client` pour le client console, et `gui` pour les interfaces graphiques.

On va commencer par le serveur, ensuite le client, et enfin l'interface. »

---

> 🎙️ **MEMBRE 1 — La partie Serveur**

---

## ⏱️ PARTIE 1 — Protocol.java — [3:00 – 4:00]

> 🎥 *À l'écran : affichage de `Protocol.java` dans l'IDE*

**[Paroles]**

« On commence par le plus simple : `Protocol.java`.

C'est une classe **final** avec uniquement des constantes — son constructeur est privé, donc on ne peut pas l'instancier. C'est le pattern "classe utilitaire" en Java.

Elle définit :
- Le **port par défaut** : 9999 — c'est le port sur lequel le serveur écoute.
- L'**encodage** : CP850 — c'est important pour que les caractères spéciaux comme les accents s'affichent correctement dans le terminal Windows.
- Le **timeout de connexion** : 30 secondes — si un client reste inactif trop longtemps, il est déconnecté automatiquement.
- Le **nombre maximum de clients** : 3 — le serveur n'accepte pas plus de 3 connexions simultanées.

L'idée d'avoir tout ça dans une seule classe, c'est que si je veux changer le port ou le timeout, je le change à un seul endroit, et ça se propage partout. C'est le principe **DRY — Don't Repeat Yourself**. »

---

## ⏱️ PARTIE 2 — Server.java — [4:00 – 6:30]

> 🎥 *À l'écran : affichage de `Server.java`, scroll progressif sur le code*

**[Paroles]**

« Passons au cœur du projet : `Server.java`.

**L'interface ServerListener**

La première chose qu'on voit, c'est cette interface interne `ServerListener`. Elle définit un contrat entre la logique réseau et l'interface graphique. Quand quelque chose se passe sur le réseau — un client qui se connecte, une commande reçue, un log — le serveur appelle les méthodes de ce listener.

Ça nous permet de **séparer complètement** la logique réseau de l'interface graphique. La classe `Server` ne sait rien de JavaFX, et c'est très bien ainsi.

**Les attributs**

On a un `ServerSocket` qui va écouter les connexions, un `ExecutorService` — c'est le pool de threads — et une liste `activeHandlers` de type `CopyOnWriteArrayList`. Ce dernier type est important : c'est une liste **thread-safe**, parce qu'on va la modifier depuis plusieurs threads en même temps.

**La méthode start()**

Ici on crée le `ServerSocket` sur le port 9999, on initialise le **pool de threads** avec `newFixedThreadPool(3)`.

Un pool de threads, c'est un ensemble fixe de threads qui sont toujours prêts à travailler. Au lieu de créer un nouveau thread à chaque connexion — ce qui est coûteux — on en a 3 qui tournent en permanence et prennent en charge les clients à tour de rôle. Quand un client se connecte, on appelle `pool.execute()` et le pool lui assigne automatiquement un thread libre. C'est efficace et ça évite de surcharger le système.

Ensuite on lance un thread en arrière-plan qui tourne en boucle.

Dans cette boucle, `serverSocket.accept()` est un appel **bloquant** : il attend jusqu'à ce qu'un client se connecte. Quand ça arrive, on crée un `ClientHandler` pour gérer ce client, et on le soumet au pool avec `pool.execute()`.

Le thread accept est marqué `setDaemon(true)`, ce qui signifie qu'il s'arrêtera automatiquement quand le programme principal se termine.

**La méthode stop()**

On met `running = false`, on ferme le `ServerSocket` — ce qui va débloquer l'appel `accept()` et provoquer une `IOException` qu'on ignore proprement — et on appelle `pool.shutdownNow()` pour arrêter tous les threads en cours. »

---

---

> 🎙️ **MEMBRE 2 — La partie Client**

## ⏱️ PARTIE 3 — ClientHandler.java — [6:30 – 9:00]

> 🎥 *À l'écran : affichage de `ClientHandler.java`*

**[Paroles]**

« Maintenant regardons `ClientHandler`. C'est la classe qui gère **un client spécifique**.

Elle implémente l'interface `Runnable` — c'est une interface Java qui n'a qu'une seule méthode : `run()`. En faisant ça, on dit au pool de threads "cet objet sait comment s'exécuter". Quand on appelle `pool.execute(handler)`, le pool appelle automatiquement `handler.run()` dans un de ses threads. Tout le travail d'un client — lire ses messages, exécuter ses commandes, gérer sa déconnexion — est encapsulé dans cet objet.

**Le constructeur**

À la création, on récupère l'adresse IP et le port du client pour former un identifiant unique — ça nous servira dans les logs.

**La méthode run()**

C'est ici que se passe toute la communication. On ouvre deux flux à partir du socket.

Le **`BufferedReader`** c'est le flux de lecture — il accumule les données dans un buffer mémoire et nous donne une méthode `readLine()` pour recevoir les commandes du client ligne par ligne.

Le **`PrintWriter`** c'est le flux d'écriture — c'est lui qui envoie les résultats vers le client. On le crée avec `autoFlush = true`, ce qui signifie que chaque `println()` est envoyé immédiatement sur le réseau sans appel manuel à `flush()`.

Les deux utilisent l'encodage CP850 de notre protocole.

Ensuite, boucle simple : on lit les messages ligne par ligne. Si le client envoie "EXIT", on sort de la boucle. Sinon, on notifie le listener et on appelle `executeCommand()`.

On gère deux cas d'erreur : la `SocketTimeoutException` qui arrive si le client est inactif depuis 30 secondes, et les `IOException` générales. Le bloc `finally` ferme toujours le socket et notifie la déconnexion.

**La méthode executeCommand() — la partie intéressante**

C'est ici qu'on exécute vraiment la commande sur le système. On détecte d'abord le système d'exploitation : si c'est Windows, on utilise `cmd.exe /c`, sinon `/bin/sh -c`.

On utilise `ProcessBuilder` avec `redirectErrorStream(true)` — ça fusionne la sortie standard et la sortie d'erreur en un seul flux, pratique pour tout afficher dans le terminal.

Ensuite, on lance un thread séparé pour lire la sortie du processus **caractère par caractère** et l'envoyer au client en temps réel — c'est ce qui donne l'effet de terminal live.

En parallèle, la boucle principale gère l'entrée interactive : si le client envoie quelque chose pendant que le processus tourne, on le pipe directement dans le processus. Ça permet d'interagir avec des commandes qui demandent une saisie, comme `python` ou `ftp`. »

---

## ⏱️ PARTIE 4 — Client.java — [9:00 – 10:30]

> 🎥 *À l'écran : affichage de `Client.java`*

**[Paroles]**

« Le client console est beaucoup plus simple. Il demande l'adresse IP du serveur, ouvre un `Socket`, et c'est parti.

L'architecture est classique en réseau : **deux flux indépendants**.

Un **thread de réception** tourne en arrière-plan en lisant caractère par caractère depuis le serveur — là encore, on lit `char` par `char` et pas ligne par ligne, pour afficher la sortie en temps réel, sans attendre les fins de ligne.

La **boucle principale** lit le clavier et envoie les commandes. Si l'utilisateur tape "EXIT", on sort proprement.

Ce client console sert de base — la version JavaFX qu'on va voir maintenant reprend exactement la même logique réseau, mais avec une interface graphique. »

---

---

> 🎙️ **MEMBRE 3 — L'interface JavaFX**

## ⏱️ PARTIE 5 — Interface JavaFX — [10:30 – 13:00]

> 🎥 *À l'écran : code JavaFxServer + JavaFxClient en alternance avec les fenêtres graphiques*

**[Paroles]**

« Les deux interfaces graphiques utilisent le même thème sombre — les couleurs sont inspirées du thème **Catppuccin Mocha**, très populaire dans les terminaux.

**JavaFxServer**

La classe implémente `Server.ServerListener` — c'est notre pont entre le réseau et l'UI. Chaque callback comme `onClientConnected` ou `onCommandReceived` appelle `Platform.runLater()` pour mettre à jour l'interface depuis le bon thread — c'est obligatoire en JavaFX, les mises à jour UI doivent se faire sur le JavaFX Application Thread.

L'interface a trois zones :
- Une **barre supérieure** avec le bouton Démarrer/Arrêter et un indicateur de statut coloré — vert quand actif, rouge quand arrêté.
- Un **panneau gauche** avec la liste des clients connectés.
- Un **panneau central** avec le journal en temps réel de toutes les connexions et commandes.

**JavaFxClient**

Le client a deux écrans. D'abord un **écran de connexion** où l'utilisateur entre l'adresse IP et le port. La tentative de connexion se fait dans un thread séparé pour ne pas figer l'UI.

Une fois connecté, on bascule vers l'**écran terminal** — une `TextArea` en lecture seule, une barre de saisie avec un champ texte, et le bouton "Envoyer".

Détail sympa : j'ai implémenté la **navigation dans l'historique des commandes** avec les flèches ↑ et ↓ — exactement comme dans un vrai terminal. »

---

## ⏱️ DÉMO LIVE — [13:00 – 16:00]

> 🎥 *À l'écran : les deux fenêtres côte à côte — démarrer le serveur, puis le client*

**[Paroles]**

« Alright, place à la démo ! Je vais lancer les deux applications côte à côte.

**[Action : lancer JavaFxServer]**

« Voilà le serveur. Je clique sur "Démarrer" — l'indicateur passe au vert, le log confirme que le serveur écoute sur le port 9999. »

**[Action : lancer JavaFxClient]**

« Et voilà le client. Je laisse `localhost` comme adresse — je suis sur la même machine pour la démo — et je clique Connexion. »

**[Côté serveur]**

« On voit immédiatement dans le journal : "Nouveau client connecté" avec l'IP et le port. La liste des clients à gauche s'est mise à jour. »

**[Action : taper une commande dans le client, ex: `dir` ou `ls`]**

« Je tape `dir`... et le résultat s'affiche en temps réel dans le terminal ! Côté serveur, la commande est loguée dans le journal.

Regardez la fluidité — c'est parce qu'on lit caractère par caractère, pas ligne par ligne.

**[Taper une autre commande, ex: `whoami` ou `echo Hello`]**

Et voilà `whoami`... parfait. Chaque commande est tracée.

**[Action : taper EXIT]**

Je tape `EXIT`, le client se déconnecte proprement, et le serveur retire le client de la liste. »

---

## ⏱️ POINTS CLÉS À RETENIR — [16:00 – 17:00]

> 🎥 *À l'écran : slide récapitulatif ou retour sur le code*

**[Paroles]**

« Pour résumer ce qu'on a vu :

**1. L'architecture Client-Serveur TCP** — `ServerSocket` côté serveur, `Socket` côté client. Simple et robuste.

**2. Le multithreading** — un thread pour accepter les connexions, un thread par client pour la communication, un thread pour lire la sortie des processus. Chaque couche est indépendante.

**3. La séparation des responsabilités** — l'interface `ServerListener` découple complètement la logique réseau de l'interface graphique.

**4. JavaFX et le thread UI** — toutes les mises à jour graphiques passent par `Platform.runLater()`.

**5. L'encodage** — utiliser CP850 pour la compatibilité avec les terminaux Windows est un détail qui peut vous sauver beaucoup de maux de tête avec les caractères spéciaux. »

---

## ⏱️ CONCLUSION — [17:00 – 18:00]

> 🎥 *À l'écran : fenêtres de l'appli en arrière-plan*

**[Paroles]**

« Et voilà pour ce projet ! C'est un bon exercice pour comprendre comment fonctionne la communication réseau en Java, le multithreading, et comment brancher une interface graphique sur une logique asynchrone.

Si vous voulez aller plus loin, vous pouvez ajouter :
- De l'**authentification** à la connexion
- Du **chiffrement TLS** pour sécuriser les échanges
- La possibilité d'**envoyer des fichiers**
- Un système de **sessions multiples** avec des onglets dans le client

Si vous avez des questions sur n'importe quelle partie du code, posez-les en commentaire — je réponds à tout.

Si la vidéo vous a plu, un **like** ça m'aide vraiment à continuer à produire ce genre de contenu. Et **abonnez-vous** pour ne pas rater les prochains projets.

À la prochaine ! »

---

## 📋 CHECKLIST AVANT TOURNAGE

- [ ] IDE ouvert avec le projet, police agrandie (16-18px minimum)
- [ ] Deux fenêtres côte à côte prêtes pour la démo (serveur + client)
- [ ] Schéma d'architecture préparé (draw.io, Excalidraw, ou tableau)
- [ ] Résolution d'enregistrement : 1920×1080 minimum
- [ ] Micro testé — pas de fond sonore
- [ ] Commandes de démo préparées : `dir` / `ls`, `whoami`, `echo Hello World`, `EXIT`

---

## 🎬 TIMESTAMPS (pour la description YouTube)

```
0:00 - Introduction
0:45 - Présentation du projet
1:30 - Structure des fichiers
2:00 - [Membre 1] Protocol.java & Server.java — Sockets & pool de threads
6:30 - [Membre 2] ClientHandler.java & Client.java — exécution des commandes
9:30 - [Membre 3] Interface JavaFX serveur + client
13:00 - DÉMO LIVE
16:00 - Conclusion
```
