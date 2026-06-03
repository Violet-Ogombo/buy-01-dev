# 🛍️ Buy-01 Showcase Setup Guide

This guide walks through running the **Buy-01 backend** on your showcase computer so the live Angular app at GitHub Pages connects to it.

> **Frontend URL:** https://violet-ogombo.github.io/buy-01-dev/
> 
> The frontend is permanently hosted on GitHub. You only need Docker running locally for the API/database.

---

## Prerequisites

| Requirement | Version |
|-------------|---------|
| Docker Desktop | Latest (≥ 4.x) |
| Git | Any |
| ~4 GB RAM free | For all services |
| ~3 GB disk free | For Docker images |

---

## Step 1 — Clone the Repository

```bash
git clone https://github.com/Violet-Ogombo/buy-01-dev.git
cd buy-01-dev
```

---

## Step 2 — Start the Backend (Showcase Mode)

This starts **only** the core services (no Jenkins, no SonarQube):

```bash
docker compose -f docker-compose.yml -f docker-compose.showcase.yml up -d
```

Wait ~60–90 seconds for all services to become healthy. You can monitor progress:

```bash
docker ps
```

All services should show `healthy` status before proceeding.

---

## Step 3 — Trust the Self-Signed SSL Certificate ⚠️

This is a **required one-time step**. Because the local Nginx uses a self-signed certificate, your browser will initially block API calls.

1. Open **https://localhost** in your browser (Chrome, Firefox, or Safari)
2. You will see a security warning — this is expected
3. Click **"Advanced"** → **"Proceed to localhost (unsafe)"** (Chrome)  
   Or click **"Accept the Risk and Continue"** (Firefox)
4. You should see an Nginx page or the app — that's fine, just confirming the cert is trusted

> [!IMPORTANT]
> If you skip this step, the GitHub Pages app will silently fail to connect to the backend.

---

## Step 4 — Open the App

Navigate to: **https://violet-ogombo.github.io/buy-01-dev/**

The app will now connect to your local backend at `https://localhost/api`.

---

## Stopping the Backend

```bash
docker compose -f docker-compose.yml -f docker-compose.showcase.yml down
```

To also remove stored data (products, users, media):

```bash
docker compose -f docker-compose.yml -f docker-compose.showcase.yml down -v
```

---

## Troubleshooting

### "Cannot connect" or blank page after login
- Make sure you completed **Step 3** (trust the SSL cert)
- Check that Docker services are healthy: `docker ps`
- Open browser DevTools → Console to see if there are CORS or connection errors

### Services show "unhealthy" or "starting"
- Wait another 60 seconds — Eureka/Kafka take time to initialize
- Run `docker compose logs discovery-server` to check status

### Port conflicts (80 or 443 already in use)
- Stop any other web servers or apps using those ports (e.g., MAMP, Apache)
- On Mac: `sudo lsof -i :443` to find what's using the port

### Resetting everything (fresh start)
```bash
docker compose -f docker-compose.yml -f docker-compose.showcase.yml down -v
docker compose -f docker-compose.yml -f docker-compose.showcase.yml up -d
```

---

## Architecture Overview

```
[Browser]
   │
   ├── Angular App ──── https://violet-ogombo.github.io/buy-01-dev/  (GitHub CDN)
   │
   └── API Calls ──────► https://localhost/api  (your local machine)
                              │
                         nginx-reverse-proxy (port 443, SSL)
                              │
                         api-gateway (port 8080)
                         ├── identity-service (auth/users)
                         ├── product-service (products)
                         └── media-service (images)
                              │
                         mongodb + kafka (data & events)
```
