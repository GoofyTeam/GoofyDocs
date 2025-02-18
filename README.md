# GoofyDocs

**GoofyDocs** est un projet Java (Spring Boot) visant à implémenter un système de découpage intelligent de fichiers.  
Les objectifs du projet sont :

- Découper dynamiquement les fichiers (Content-Defined Chunking avec Rabin Fingerprinting)
- Détecter les doublons via le calcul d’empreintes (SHA-1, SHA-256 ou BLAKE3)
- Compresser à la volée chaque chunk (avec Zstd, LZ4 ou Snappy)
- Réaliser des tests de performance sur divers types de fichiers

> **Note :** Pour l'instant, le projet se présente sous forme d'un exemple minimal affichant "Hello, World!" à la racine de l'application.

---

## Prérequis

- [Java 17](https://adoptium.net/)
- [Maven](https://maven.apache.org/)
- [Docker](https://www.docker.com/) et [Docker Compose](https://docs.docker.com/compose/)
- (Optionnel) [Visual Studio Code](https://code.visualstudio.com/) avec l'extension [Language Support for Java™ by Red Hat](https://marketplace.visualstudio.com/items?itemName=redhat.java)

---

## Installation et utilisation

### En local (sans Docker)

1. **Compilation :**
   - Ouvre un terminal et rends-toi dans le dossier `java`
   - Exécute :
     ```bash
     mvn clean package
     ```
2. **Exécution de l'application :**
   - Toujours dans le dossier `java`, lance :
     ```bash
     mvn spring-boot:run
     ```
   - L'application sera accessible sur [http://localhost:8080/](http://localhost:8080/).

---

### Avec Docker

1. **Construire et lancer les conteneurs :**

   - Depuis la racine du projet (`GoofyDocs`), exécute :
     ```bash
     docker-compose up --build -d
     ```
   - Cette commande va :
     - Construire l'image de l'application (compilation via le Dockerfile)
     - Lancer le conteneur de l'application sur le port `8080`
     - Lancer un conteneur PostgreSQL sur le port `5432`

2. **Mise à jour en développement :**

   - Si vous modifiez le code (par exemple, le message "Hello, World!"), utilisez :
     ```bash
     make docker-update
     ```
   - Cette commande (définie dans le Makefile) va :
     - Recompiler le projet dans le conteneur en utilisant le code source monté dans `/source`
     - Copier le nouveau jar vers `/app/app.jar`
     - Redémarrer le conteneur pour appliquer les changements

3. **Arrêter et nettoyer les conteneurs :**
   ```bash
   docker-compose down
   ```
