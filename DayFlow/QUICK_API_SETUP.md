# ⚡ Quick API Setup - 5 Minutes

## 🎯 What You Need

1. ✅ **Groq API** (already have it!)
2. 🆕 **Google Perspective API** (for content moderation)
3. 🆕 **LibreTranslate** (no key needed!)

---

## 🚀 Step-by-Step Setup

### Step 1: Google Perspective API (3 minutes)

1. **Go to**: https://console.cloud.google.com/
2. **Create project**: "DayFlow-Moderation"
3. **Enable API**: Search "Perspective Comment Analyzer API" → Enable
4. **Create key**: APIs & Services → Credentials → Create API Key
5. **Copy the key** (starts with `AIza...`)

### Step 2: Update application.properties

Open: `src/main/resources/application.properties`

Add this line:
```properties
perspective.api.key=AIzaSyC-paste-your-key-here
```

### Step 3: LibreTranslate (Already Configured!)

No action needed! Uses free public instance.

---

## ✅ Test It Works

### Test 1: Content Moderation

1. Run your app
2. Login as user
3. Create new reclamation
4. Type: "Vous êtes des idiots!"
5. Click OK
6. **Should see warning dialog** ⚠️

### Test 2: Clean Content

1. Type: "L'application ne fonctionne pas"
2. Click OK
3. **Should submit without warning** ✅

---

## 📋 Your Complete Config

```properties
# Database
app.db.url=jdbc:postgresql://localhost:5432/dayflow
app.db.user=postgres
app.db.password=your_password

# Groq AI
groq.api.key=gsk_your_key_here
groq.api.url=https://api.groq.com/openai/v1/chat/completions
groq.model=llama-3.3-70b-versatile
groq.max.tokens=500
groq.temperature=0.7

# Google Perspective API (NEW!)
perspective.api.key=AIzaSyC-your-key-here

# LibreTranslate (FREE - No key needed!)
libretranslate.api.url=https://libretranslate.com/translate
libretranslate.api.key=
```

---

## 🎯 What Happens Now

### When User Submits Reclamation:

```
User types message
       ↓
Clicks "OK"
       ↓
Perspective API checks for toxicity
       ↓
   Harmful?
   ↙      ↘
 YES       NO
  ↓         ↓
Warning   Submit
```

### Warning Example:

```
⚠️ Contenu inapproprié détecté

Insultes détectées.
Score de toxicité : 85%

Veuillez reformuler votre message.

[Modifier] [Soumettre quand même] [Annuler]
```

---

## 🐛 Troubleshooting

**"API key not configured"**
→ Add key to application.properties

**"API error 403"**
→ Enable Perspective API in Google Cloud Console

**"API error 429"**
→ Rate limit (1 req/sec). Wait and retry.

---

## 📊 API Limits

| API | Limit | Cost |
|-----|-------|------|
| Groq | 30/min | FREE |
| Perspective | 1/sec | FREE (1M/day) |
| LibreTranslate | Varies | FREE |

---

## ✅ Done!

You now have:
- ✅ AI response suggestions
- ✅ Automatic content moderation
- ✅ Translation ready

**See `API_INTEGRATION_GUIDE.md` for full documentation!** 📚
