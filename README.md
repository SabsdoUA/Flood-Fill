# Flood Fill

Webová hra typu flood fill postavená na `Spring Boot` a `React + TypeScript`. Projekt obsahuje backend pre autentifikáciu, správu herného stavu, rebríček a komentáre, a frontend vo forme single-page aplikácie.

<p align="left">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot 3.5" />
  <img src="https://img.shields.io/badge/Spring_Security-OAuth2-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" alt="Spring Security OAuth2" />
  <img src="https://img.shields.io/badge/React-18-20232A?style=for-the-badge&logo=react&logoColor=61DAFB" alt="React 18" />
  <img src="https://img.shields.io/badge/TypeScript-5-3178C6?style=for-the-badge&logo=typescript&logoColor=white" alt="TypeScript 5" />
  <img src="https://img.shields.io/badge/Vite-6-646CFF?style=for-the-badge&logo=vite&logoColor=white" alt="Vite 6" />
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL 16" />
  <img src="https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white" alt="Redis 7" />
  <img src="https://img.shields.io/badge/Maven-3.9-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white" alt="Maven 3.9+" />
  <img src="https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker" />
  <img src="https://img.shields.io/badge/Cloud_Run-GCP-4285F4?style=for-the-badge&logo=googlecloud&logoColor=white" alt="Google Cloud Run" />
</p>

## Obsah

1. [Prehľad](#prehľad)
2. [Hlavné funkcie](#hlavné-funkcie)
3. [Architektúra projektu](#architektúra-projektu)
4. [Technologický stack](#technologický-stack)
5. [Požiadavky](#požiadavky)
6. [Konfigurácia](#konfigurácia)
7. [Lokálne spustenie](#lokálne-spustenie)
8. [Používanie aplikácie](#používanie-aplikácie)
9. [API](#api)
10. [Build a testovanie](#build-a-testovanie)
11. [Nasadenie](#nasadenie)
12. [Štruktúra repozitára](#štruktúra-repozitára)
13. [Príspevky](#príspevky)
14. [Licencia](#licencia)

## Prehľad

Flood Fill je hra, v ktorej hráč mení farbu počiatočnej oblasti v ľavom hornom rohu a snaží sa zjednotiť celú mriežku v obmedzenom počte ťahov. Aplikácia podporuje registráciu používateľov, prihlásenie cez email aj Google OAuth, ukladanie výhier do rebríčka a pridávanie komentárov.

Frontend je distribuovaný ako SPA a pri produkčnom builde sa vkladá do Spring Boot aplikácie ako statický obsah.

## Hlavné funkcie

- Hra na mriežkach veľkosti `12x12`, `15x15` a `18x18`
- Generovanie náhodnej hracej plochy na backende
- Výpočet limitu ťahov na základe serverovej heuristiky
- Registrácia a prihlásenie používateľov
- Overenie emailu po registrácii
- Obnova hesla pomocou emailového tokenu
- Prihlásenie cez Google OAuth 2.0
- Rebríček výhier podľa veľkosti hracej plochy
- Komentáre a hodnotenie hry
- Docker build pripravený pre Google Cloud Run

## Architektúra projektu

Projekt je rozdelený na dve hlavné časti:

- `backend` v `Spring Boot`
- `frontend` v `React + TypeScript`

Backend je modulovaný podľa domén:

- `authentication` pre registráciu, prihlásenie, OAuth a reset hesla
- `game` pre hernú logiku a správu herného stavu
- `leaderboard` pre evidenciu výhier
- `feedback` pre komentáre a hodnotenia
- `infrastructure` pre spoločné webové a logovacie komponenty

Aktuálna verzia hry používa na herné operácie HTTP endpointy.

## Technologický stack

### Backend

<p>
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot 3.5" />
  <img src="https://img.shields.io/badge/Security-OAuth2-6DB33F?style=flat-square&logo=springsecurity&logoColor=white" alt="Spring Security OAuth2" />
  <img src="https://img.shields.io/badge/JPA-Hibernate-59666C?style=flat-square&logo=hibernate&logoColor=white" alt="JPA Hibernate" />
  <img src="https://img.shields.io/badge/Validation-Spring-6DB33F?style=flat-square&logo=spring&logoColor=white" alt="Spring Validation" />
  <img src="https://img.shields.io/badge/Actuator-Observability-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Actuator" />
  <img src="https://img.shields.io/badge/Mail-SMTP-0A66C2?style=flat-square&logo=maildotru&logoColor=white" alt="SMTP Mail" />
</p>

- `Java 21`
- `Spring Boot 3.5.x`
- `Spring Web`
- `Spring Security`
- `Spring OAuth2 Client`
- `Spring Data JPA`
- `Spring Data Redis`
- `Spring Validation`
- `Spring Actuator`
- `Spring Retry`
- `Spring AOP`
- `Spring Mail`
- `Flyway`
- `PostgreSQL`
- `Redis`
- `Caffeine`
- `Lombok`

### Frontend

<p>
  <img src="https://img.shields.io/badge/React-18-20232A?style=flat-square&logo=react&logoColor=61DAFB" alt="React 18" />
  <img src="https://img.shields.io/badge/TypeScript-5-3178C6?style=flat-square&logo=typescript&logoColor=white" alt="TypeScript 5" />
  <img src="https://img.shields.io/badge/Vite-6-646CFF?style=flat-square&logo=vite&logoColor=white" alt="Vite 6" />
</p>

- `React 18`
- `TypeScript 5`
- `Vite 6`

### Build a runtime

<p>
  <img src="https://img.shields.io/badge/Maven-build-C71A36?style=flat-square&logo=apachemaven&logoColor=white" alt="Maven" />
  <img src="https://img.shields.io/badge/npm-package_manager-CB3837?style=flat-square&logo=npm&logoColor=white" alt="npm" />
  <img src="https://img.shields.io/badge/Docker-containerized-2496ED?style=flat-square&logo=docker&logoColor=white" alt="Docker" />
</p>

- `Maven`
- `npm`
- `Docker`

## Požiadavky

Pred lokálnym spustením je potrebné mať nainštalované:

<p>
  <img src="https://img.shields.io/badge/JDK-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="JDK 21" />
  <img src="https://img.shields.io/badge/Maven-3.9+-C71A36?style=flat-square&logo=apachemaven&logoColor=white" alt="Maven 3.9+" />
  <img src="https://img.shields.io/badge/Node.js-20+-339933?style=flat-square&logo=nodedotjs&logoColor=white" alt="Node.js 20+" />
  <img src="https://img.shields.io/badge/PostgreSQL-required-4169E1?style=flat-square&logo=postgresql&logoColor=white" alt="PostgreSQL required" />
  <img src="https://img.shields.io/badge/Redis-required-DC382D?style=flat-square&logo=redis&logoColor=white" alt="Redis required" />
</p>

- `JDK 21`
- `Maven 3.9+`
- `Node.js 20+`
- `npm`
- `PostgreSQL`
- `Redis`
- `Docker` (voliteľne, pre jednoduchšie lokálne spustenie infraštruktúry)

## Konfigurácia

Konfigurácia backendu je v súbore `src/main/resources/application.yaml`, frontendový dev server používa `frontend/vite.config.ts`. Projekt je riadený najmä cez environment premenné pre databázu, Redis, OAuth a email.

Pre lokálny aj produkčný beh nastavte vlastné hodnoty. Na citlivé alebo produkčné defaulty v repozitári sa nespoliehajte.

### Minimálne premenné pre lokálny beh

```bash
PORT=8080

POSTGRES_HOST=127.0.0.1
POSTGRES_PORT=5433
DB_NAME=postgres
DB_USER=postgres
DB_PASSWORD=<set-locally>

REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=<set-locally>

APP_BASE_URL=http://localhost:8080
APP_FRONTEND_URL=http://localhost:5173
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173

VITE_BACKEND_URL=http://localhost:8080
VITE_DEV_PORT=5173
```

### Voliteľné premenné

```bash
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
OAUTH2_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google

MAIL_USERNAME=...
MAIL_PASSWORD=...
MAIL_FROM=...

APP_MAIL_ALLOW_LOG_FALLBACK=true
SERVER_ADDRESS=0.0.0.0
SPRING_PROFILES_ACTIVE=dev
```

### Dôležité poznámky ku konfigurácii

- Ak premennú `PORT` nenastavíte, backend sa pri lokálnom spustení nespustí na `8080`, ale na náhodnom voľnom porte, pretože `server.port` má default `0`.
- Lokálna konfigurácia implicitne očakáva PostgreSQL na porte `5433`.
- Predvolená Redis konfigurácia očakáva heslo. Ak použijete ukážkový Docker príkaz nižšie, nastaví Redis s heslom `root`.
- Frontendový dev server používa Vite proxy pre `/auth`, `/oauth2`, `/secured`, `/logout` a `/api`.
- Ak `VITE_BACKEND_URL` nenastavíte, Vite proxy smeruje na nasadenú Cloud Run inštanciu, nie na lokálny backend.
- Registrácia, overenie emailu a obnova hesla vyžadujú funkčný SMTP server. Na lokálny vývoj môžete použiť `APP_MAIL_ALLOW_LOG_FALLBACK=true`, aby sa obsah emailov zapisoval do logu pri zlyhaní SMTP.
- Produkčný Docker image nastavuje `SPRING_PROFILES_ACTIVE=gcp`.

## Lokálne spustenie

### 1. Spustenie databázy

Príklad pre PostgreSQL:

```bash
docker run -d --name floodfill-postgres -p 5433:5432 -e POSTGRES_PASSWORD=root postgres:16
```

Príklad pre Redis:

```bash
docker run -d --name floodfill-redis -p 6379:6379 redis:7-alpine redis-server --requirepass root
```

Poznámka: repozitár aktuálne neobsahuje `docker-compose.yml`, preto je potrebné infraštruktúru spustiť samostatne.

### 2. Spustenie backendu

Pred spustením nastavte aspoň `PORT=8080` a základné databázové/Redis premenné.

```bash
mvn spring-boot:run
```

Alternatívne:

```bash
mvn clean package
java -jar target/gamestudio-5.0.0-SNAPSHOT.jar
```

Ak je `PORT=8080`, backend bude dostupný na:

```text
http://localhost:8080
```

### 3. Spustenie frontendu

```bash
cd frontend
npm ci
npm run dev
```

Frontend bude pri predvolenej konfigurácii dostupný na:

```text
http://localhost:5173
```

### 4. Overenie behu

Po štarte backendu a frontendu otvorte:

```text
http://localhost:5173
```

Ak chcete testovať backend priamo, použite napríklad `GET /secured/user`, `GET /api/leaderboard` alebo herné endpointy pod `/api/game`.

## Používanie aplikácie

### Registrácia a prihlásenie

- Používateľ sa môže zaregistrovať cez formulár.
- Po registrácii backend odošle verifikačný email.
- Prihlásenie funguje cez email a heslo alebo cez Google OAuth.
- Endpoint `/secured/user` vracia textovú informáciu o aktuálne prihlásenom používateľovi.
- Emailová registrácia a prihlásenie aktuálne akceptujú iba adresy `@gmail.com`.

### Hra

- Pri spustení novej hry klient vytvorí alebo použije lokálne uložené `gameId`.
- Nová hra sa štartuje cez backend endpoint `/api/game/{gameId}/start`.
- Obnovenie hry používa endpoint `/api/game/{gameId}/resume`.
- Každý ťah sa vykonáva cez `/api/game/{gameId}/move`.
- Server vracia mriežku, počet vykonaných ťahov, limit ťahov a stav hry.

### Rebríček

- Rebríček je dostupný cez `GET /api/leaderboard`.
- Výhra sa zapisuje cez `POST /api/leaderboard/win?size=12|15|18&gameId=...`.
- Výhru môže zapísať iba prihlásený používateľ a iba pre skutočne vyhranú hru patriacu danému používateľovi.

### Komentáre

- Komentáre sú dostupné cez `GET /api/feedback`.
- Pridanie komentára vyžaduje prihláseného používateľa.
- Hodnotenie musí byť v rozsahu `1-5`.
- Text komentára môže mať najviac `150` znakov.

## API

Nižšie je stručný prehľad aktuálne implementovaných endpointov.

### Autentifikácia

#### `POST /auth/register`

Registrácia nového používateľa.

Poznámka: endpoint aktuálne akceptuje iba adresy `@gmail.com`.

Príklad requestu:

```json
{
  "email": "user@gmail.com",
  "nickname": "PlayerOne",
  "password": "Secret123"
}
```

#### `POST /auth/login`

Prihlásenie používateľa.

Poznámka: endpoint aktuálne akceptuje iba adresy `@gmail.com`.

Príklad requestu:

```json
{
  "email": "user@gmail.com",
  "password": "Secret123"
}
```

#### `GET /auth/verify-email?token=...`

Potvrdí email a presmeruje používateľa späť na frontend.

#### `POST /auth/resend-verification`

Znovu odošle verifikačný email.

```json
{
  "email": "user@gmail.com"
}
```

#### `POST /auth/forgot-password`

Pošle email na obnovu hesla.

```json
{
  "email": "user@gmail.com"
}
```

#### `GET /auth/validate-reset-token?token=...`

Overí platnosť reset tokenu.

#### `POST /auth/reset-password`

Nastaví nové heslo.

```json
{
  "token": "reset-token",
  "newPassword": "Newpass123"
}
```

#### `GET /secured/user`

Vráti textový stav používateľa, napríklad:

```text
Prihlásený používateľ: PlayerOne
```

### Hra

#### `POST /api/game/{gameId}/start`

Vytvorí novú hru.

```json
{
  "size": 12
}
```

#### `POST /api/game/{gameId}/resume`

Načíta existujúcu hru alebo vytvorí novú pre dané `gameId`.

```json
{
  "size": 12
}
```

#### `POST /api/game/{gameId}/move`

Vykoná ťah.

```json
{
  "color": "BLUE"
}
```

Príklad odpovede:

```json
{
  "gameId": "f9e7c0b4-1d6d-4dc3-a6f3-1e4bb1b5dc8e",
  "grid": [["RED", "BLUE"], ["GREEN", "BLUE"]],
  "movesTaken": 3,
  "moveLimit": 12,
  "status": "ACTIVE",
  "won": false,
  "error": null
}
```

### Rebríček

#### `GET /api/leaderboard`

Voliteľné query parametre:

- `page`, predvolene `0`
- `size`, predvolene `50`, maximum `100`

Príklad odpovede:

```json
[
  {
    "name": "PlayerOne",
    "smallWins": 3,
    "mediumWins": 1,
    "largeWins": 0,
    "totalPoints": 5
  }
]
```

#### `POST /api/leaderboard/win?size=12&gameId=...`

Zapíše výhru prihlásenému používateľovi.

### Komentáre

#### `GET /api/feedback`

Vráti zoznam komentárov.

Príklad položky:

```json
{
  "id": 1,
  "user": "PlayerOne",
  "rating": 5,
  "comment": "Výborná hra",
  "createdAt": "2026-04-27T09:00:00Z",
  "createdDate": "2026-04-27"
}
```

#### `POST /api/feedback`

Pridá komentár a hodnotenie.

```json
{
  "rating": 5,
  "comment": "Výborná hra"
}
```

## Build a testovanie

### Backend

Spustenie testov:

```bash
mvn test
```

Build projektu:

```bash
mvn clean package
```

Testovacia sada backendu pokrýva doménovú logiku, kontroléry, validáciu DTO, infraštruktúrne komponenty aj architektonické pravidlá.

### Frontend

Inštalácia závislostí:

```bash
cd frontend
npm ci
```

Produkčný build:

```bash
npm run build
```

Náhľad produkčného buildu:

```bash
npm run preview
```

Frontend momentálne nemá samostatný testovací skript; v repozitári je definovaný build a preview workflow.

## Nasadenie

Projekt obsahuje `Dockerfile` a konfiguračný súbor `deploy/cloudrun.env.yaml` pre Google Cloud Run.

<p>
  <img src="https://img.shields.io/badge/Docker-multi--stage_build-2496ED?style=flat-square&logo=docker&logoColor=white" alt="Docker multi-stage build" />
  <img src="https://img.shields.io/badge/Google_Cloud_Run-supported-4285F4?style=flat-square&logo=googlecloud&logoColor=white" alt="Google Cloud Run" />
  <img src="https://img.shields.io/badge/Spring_Profile-gcp-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring profile gcp" />
</p>

### Build Docker image

```bash
docker build -t floodfill .
```

Docker build prebieha vo viacerých krokoch:

- frontend sa zostaví v `node:20-alpine`
- výsledný frontend build sa skopíruje do `src/main/resources/static`
- backend sa zabalí do JAR cez Maven
- finálny runtime image používa `eclipse-temurin:21-jre`

### Spustenie kontajnera lokálne

```bash
docker run --rm -p 8080:8080 -e PORT=8080 -e SPRING_PROFILES_ACTIVE=gcp floodfill
```

### Poznámky k nasadeniu

- Produkčný image predvolene nastavuje `SPRING_PROFILES_ACTIVE=gcp`.
- Kontajner očakáva externé služby pre PostgreSQL a Redis a príslušné environment premenné.
- Frontend build je vložený do `src/main/resources/static`.
- Pri nasadení je potrebné dodať všetky citlivé premenné cez bezpečný secrets management.
- Samotný `deploy/cloudrun.env.yaml` nestačí pre produkčný štart. `DB_PASSWORD`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `MAIL_PASSWORD` a `APP_REMEMBER_ME_KEY` musia prísť zo Secret Managera, inak Cloud Run revízia spadne ešte počas inicializácie `DataSource`/Flyway.

### Cloud Run deploy so secretmi

PowerShell skript `deploy/deploy-cloudrun.ps1` nasadí službu s:

- `deploy/cloudrun.env.yaml` pre ne-citlivé premenné,
- `--set-secrets` pre citlivé hodnoty zo Secret Managera,
- Cloud SQL a VPC nastaveniami pre aktuálny projekt.

Pred spustením overte, že v projekte existujú secret-y:

- `DB_PASSWORD`
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `MAIL_PASSWORD`
- `APP_REMEMBER_ME_KEY`

Nasadenie:

```powershell
./deploy/deploy-cloudrun.ps1
```

## Štruktúra repozitára

```text
.
├── deploy/
│   └── cloudrun.env.yaml
├── frontend/
│   ├── src/
│   │   └── main.tsx
│   ├── package.json
│   └── vite.config.ts
├── src/
│   ├── main/
│   │   ├── java/sk/tuke/gamestudio/
│   │   │   ├── authentication/
│   │   │   ├── feedback/
│   │   │   ├── game/
│   │   │   ├── infrastructure/
│   │   │   ├── leaderboard/
│   │   │   └── FloodFillApplication.java
│   │   └── resources/
│   │       ├── application.yaml
│   │       └── db/migration/
│   └── test/
├── Dockerfile
└── pom.xml
```

## Príspevky

Ak chcete prispieť:

1. vytvorte si vlastný branch
2. urobte zmeny v samostatných, zmysluplných commitoch
3. overte `mvn test` a `cd frontend && npm run build`
4. otvorte pull request s jasným popisom zmien

Pri zmenách API alebo konfigurácie aktualizujte aj tento `README.md`.

## Licencia

V repozitári sa aktuálne nenachádza samostatný súbor `LICENSE`. Licenčné podmienky preto nie sú v tejto chvíli explicitne určené.
