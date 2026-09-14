# UniAssist Cloud Backend Deployment Guide ☁️

This guide outlines how to host the **UniAssist Django REST Framework** backend online for free.

---

## 🏆 Option 1: Render.com (Recommended — Best Free Tier)

**Render** connects directly to your GitHub repository (`09kushal/UniAssist`) and automatically deploys your backend every time you push code.

### Prerequisites:
- A free account at [render.com](https://render.com) (Sign up with GitHub).

### Deployment Steps:

#### Method A: 1-Click Blueprint (Easiest)
1. Go to your [Render Dashboard](https://dashboard.render.com).
2. Click **New +** &rarr; **Blueprint**.
3. Select your repository: `09kushal/UniAssist`.
4. Render will automatically detect the [render.yaml](file:///Users/xxx/UniAssist/render.yaml) file we created for you.
5. Click **Apply**.
6. Render will automatically install dependencies, collect static assets, run database migrations, and launch your Gunicorn server.

---

#### Method B: Manual Web Service Setup
If you prefer setting it up manually:
1. In Render Dashboard, click **New +** &rarr; **Web Service**.
2. Select your repository: `09kushal/UniAssist`.
3. Configure the following settings:
   - **Name**: `uniassist-backend`
   - **Region**: Singapore (or nearest to you)
   - **Branch**: `main`
   - **Runtime**: `Python 3`
   - **Build Command**: `./build.sh`
   - **Start Command**: `cd backend && gunicorn uniassist.wsgi:application --bind 0.0.0.0:$PORT`
   - **Instance Type**: `Free`
4. Add the following **Environment Variables** in the Environment tab:
   - `PYTHON_VERSION`: `3.11.9`
   - `DEBUG`: `False`
   - `SECRET_KEY`: *(Generate or click random)*
   - `USE_SQLITE`: `True` *(For zero-config instant database, or connect a MySQL/PostgreSQL URL below)*
   - `CORS_ALLOW_ALL_ORIGINS`: `True`
5. Click **Create Web Service**.

Your live API will be available at:
```
https://uniassist-backend-nlyq.onrender.com/
```

### Django Admin Portal:
```
https://uniassist-backend-nlyq.onrender.com/admin/
```
* **Username / Email**: `admin@uniassist.com`
* **Password**: `Admin@12345`

---

## 🗄️ Database Options for Cloud Deployment

You have 3 free options for the backend database:

| Option | Cost | Setup | Recommended For |
| :--- | :--- | :--- | :--- |
| **1. SQLite (`USE_SQLITE=True`)** | Free | Built-in (Zero setup) | Instant deployment, portfolios & quick demos |
| **2. TiDB Serverless (MySQL Cloud)** | Free (5 GB Forever, No Card) | Set `DB_HOST`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`, `DB_PORT` | Full MySQL compatibility matching local environment |
| **3. Render Managed PostgreSQL** | Free (1 GB) | Set `DATABASE_URL` (Auto-injected by Render) | Production-grade relational storage |

### Connecting TiDB Serverless (Free Forever MySQL):
1. Sign up at [tidbcloud.com](https://tidbcloud.com) (Free, no credit card required).
2. Create a free **Serverless Cluster**.
3. Copy the host, port, user, and password into your Render Web Service Environment Variables (`DB_HOST`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`, `DB_PORT`).
4. UniAssist's [settings.py](file:///Users/xxx/UniAssist/backend/uniassist/settings.py) will automatically connect to it!

---

## 📱 Updating the Android Mobile App

Once your backend is live on Render (e.g., `https://uniassist-backend-nlyq.onrender.com/`):

1. Open `UniAssist/android/UniAssist` in Android Studio.
2. Open [ApiClient.java](file:///Users/xxx/UniAssist/android/UniAssist/app/src/main/java/com/kushal/uniassist/network/ApiClient.java).
3. Update `BASE_URL`:
   ```java
   private static final String BASE_URL = "https://uniassist-backend-nlyq.onrender.com/";
   ```
4. Rebuild the app or compile a new release APK.

---

## 🛡️ Optional Integrations (Production Secrets)

To enable video calls and email OTPs in the cloud, add these into your Render Environment Variables:

- **Gmail SMTP (OTP)**:
  - `EMAIL_HOST_USER`: `your-email@gmail.com`
  - `EMAIL_HOST_PASSWORD`: `your-16-character-google-app-password`
- **8x8 JaaS Encrypted Video**:
  - `JAAS_APP_ID`: `vpaas-magic-cookie-...`
  - `JAAS_API_KEY_ID`: `your-api-key-id`
  - `JAAS_PRIVATE_KEY_PATH`: `jaas_private.pk`
