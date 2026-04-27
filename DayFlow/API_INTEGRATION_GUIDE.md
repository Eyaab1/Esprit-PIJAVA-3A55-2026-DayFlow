# 🔌 External APIs Integration Guide

## Overview

Your DayFlow application now integrates **3 powerful AI APIs**:

1. **Groq AI** - AI-powered response suggestions (French)
2. **Google Perspective API** - Harmful content detection
3. **LibreTranslate** - English ↔ French translation

---

## 🎯 What Each API Does

### 1. Groq AI (Already Integrated)
- **Purpose**: Generate professional response suggestions for admins
- **Language**: French
- **Cost**: FREE (30 req/min, 14,400/day)
- **Status**: ✅ Already working

### 2. Google Perspective API (NEW)
- **Purpose**: Detect toxic, harmful, or inappropriate content
- **Detects**:
  - Toxicity
  - Severe toxicity
  - Threats
  - Insults
  - Profanity
  - Identity attacks
- **Cost**: FREE (1 req/sec, up to 1M requests/day)
- **Status**: ✅ Implemented

### 3. LibreTranslate (NEW)
- **Purpose**: Translate text between English and French
- **Use case**: Translate AI responses if needed
- **Cost**: FREE (public instance) or self-host
- **Status**: ✅ Implemented

---

## 🔑 How to Get API Keys

### Google Perspective API

#### Step 1: Create Google Cloud Project
1. Go to: https://console.cloud.google.com/
2. Click "Create Project"
3. Name it: "DayFlow-Moderation"
4. Click "Create"

#### Step 2: Enable Perspective API
1. Go to: https://console.cloud.google.com/apis/library
2. Search for "Perspective Comment Analyzer API"
3. Click on it
4. Click "Enable"

#### Step 3: Create API Key
1. Go to: https://console.cloud.google.com/apis/credentials
2. Click "Create Credentials" → "API Key"
3. Copy the API key (starts with `AIza...`)
4. (Optional) Click "Restrict Key" to limit to Perspective API only

#### Step 4: Add to application.properties
```properties
perspective.api.key=AIzaSyC-your-actual-key-here
```

**Documentation**: https://developers.perspectiveapi.com/s/docs-get-started

---

### LibreTranslate API

#### Option 1: Use Public Instance (FREE - No Key Needed)
```properties
libretranslate.api.url=https://libretranslate.com/translate
libretranslate.api.key=
```

**That's it!** No API key needed for public instance.

#### Option 2: Self-Host (Advanced)
1. Install Docker
2. Run:
   ```bash
   docker run -ti --rm -p 5000:5000 libretranslate/libretranslate
   ```
3. Update config:
   ```properties
   libretranslate.api.url=http://localhost:5000/translate
   libretranslate.api.key=
   ```

**Documentation**: https://github.com/LibreTranslate/LibreTranslate

---

## 📋 Complete Configuration

### Your `application.properties` should look like:

```properties
# Database
app.db.url=jdbc:postgresql://localhost:5432/dayflow
app.db.user=postgres
app.db.password=your_password

# Groq AI (Response Suggestions)
groq.api.key=gsk_your_groq_key_here
groq.api.url=https://api.groq.com/openai/v1/chat/completions
groq.model=llama-3.1-70b-versatile
groq.max.tokens=500
groq.temperature=0.7

# Google Perspective API (Content Moderation)
perspective.api.key=AIzaSyC-your-perspective-key-here

# LibreTranslate (Translation)
libretranslate.api.url=https://libretranslate.com/translate
libretranslate.api.key=
```

---

## 🎨 How It Works

### Content Moderation Flow:

```
User writes reclamation
        ↓
Click "OK" to submit
        ↓
Perspective API analyzes text
        ↓
    Is harmful?
    ↙        ↘
  YES         NO
   ↓           ↓
Show warning  Submit
   ↓
User chooses:
- Modify
- Submit anyway
- Cancel
```

### Warning Dialog Example:

```
⚠️ Contenu inapproprié détecté

Votre message contient du contenu potentiellement inapproprié

Insultes détectées. Langage inapproprié détecté.

Score de toxicité : 85%

Veuillez reformuler votre message de manière respectueuse.

[Modifier]  [Soumettre quand même]  [Annuler]
```

---

## 🧪 Testing the Features

### Test Content Moderation:

1. **Run your app**
2. **Login as a user**
3. **Go to "Mes Réclamations"**
4. **Click "Nouvelle réclamation"**
5. **Try these test messages**:

**Test 1: Clean message (should pass)**
```
L'application ne fonctionne pas correctement. Pouvez-vous m'aider?
```
✅ Should submit without warning

**Test 2: Toxic message (should warn)**
```
Votre application est nulle et vous êtes des idiots!
```
⚠️ Should show warning dialog

**Test 3: Threat (should warn)**
```
Je vais vous détruire si vous ne réglez pas ce problème!
```
⚠️ Should show warning dialog

### Test Translation:

```java
LibreTranslateService translator = new LibreTranslateService();

// English to French
String french = translator.translateToFrench("Hello, how are you?");
// Result: "Bonjour, comment allez-vous?"

// French to English
String english = translator.translateToEnglish("Bonjour, comment allez-vous?");
// Result: "Hello, how are you?"
```

---

## 📊 API Limits & Costs

| API | Free Tier | Rate Limit | Cost After Free |
|-----|-----------|------------|-----------------|
| **Groq** | ✅ Forever | 30/min, 14.4K/day | N/A (always free) |
| **Perspective** | ✅ 1M req/day | 1 req/sec | $1 per 1K requests |
| **LibreTranslate** | ✅ Public instance | Varies | Self-host for unlimited |

---

## 🔒 Security & Privacy

### Data Sent to APIs:

**Perspective API**:
- ✅ Only reclamation text content
- ❌ No user personal data
- ❌ No images
- ❌ No database IDs

**LibreTranslate**:
- ✅ Only text to translate
- ❌ No user data
- ✅ Can self-host for complete privacy

**Groq AI**:
- ✅ Only reclamation content for context
- ❌ No user personal data

### Best Practices:
1. ✅ Never send passwords or tokens to APIs
2. ✅ Strip personal info before API calls
3. ✅ Use HTTPS for all API calls
4. ✅ Store API keys in environment variables (production)
5. ✅ Monitor API usage regularly

---

## 🐛 Troubleshooting

### "Perspective API key not configured"
**Solution**: Add your API key to `application.properties`

### "Perspective API error (code 403)"
**Solution**: 
1. Check API is enabled in Google Cloud Console
2. Verify API key is correct
3. Check API key restrictions

### "Perspective API error (code 429)"
**Solution**: Rate limit exceeded. Wait 1 second and retry.

### "LibreTranslate API error"
**Solution**: 
1. Check internet connection
2. Try alternative instance: `https://translate.argosopentech.com/translate`
3. Or self-host

### Translation not working
**Solution**: LibreTranslate public instance might be down. Use self-hosted or wait.

---

## 🎯 Features Implemented

### Content Moderation:
- ✅ Automatic toxicity detection
- ✅ Warning dialog with scores
- ✅ User can modify or submit anyway
- ✅ Graceful fallback if API fails
- ✅ Multiple toxicity categories

### Translation Service:
- ✅ English ↔ French translation
- ✅ Ready to use (if needed)
- ✅ Can be integrated anywhere

### Integration Points:
- ✅ Reclamation submission (moderation)
- ✅ Admin responses (AI suggestions)
- ✅ Future: Translate AI responses if needed

---

## 📈 Future Enhancements

### Possible Improvements:
1. **Auto-translate AI responses** from English to French
2. **Language detection** before translation
3. **Moderation logs** - Track flagged content
4. **Admin dashboard** - View moderation statistics
5. **Custom toxicity thresholds** - Adjust sensitivity
6. **Batch moderation** - Check multiple texts at once
7. **User reputation** - Track users with repeated violations

---

## 📞 Support & Resources

### Google Perspective API:
- Dashboard: https://console.cloud.google.com/
- Docs: https://developers.perspectiveapi.com/
- Pricing: https://developers.perspectiveapi.com/s/about-the-api-faqs

### LibreTranslate:
- Public Instance: https://libretranslate.com/
- GitHub: https://github.com/LibreTranslate/LibreTranslate
- API Docs: https://libretranslate.com/docs

### Groq AI:
- Dashboard: https://console.groq.com/
- Docs: https://console.groq.com/docs
- API Keys: https://console.groq.com/keys

---

## ✅ Setup Checklist

- [ ] Get Google Perspective API key
- [ ] Add Perspective key to application.properties
- [ ] Test content moderation with toxic message
- [ ] Verify LibreTranslate works (no key needed)
- [ ] Test translation (optional)
- [ ] Update team documentation
- [ ] Share API keys securely with team
- [ ] Monitor API usage
- [ ] Test all features end-to-end

---

## 🎉 Success!

Your application now has:
- ✅ AI-powered response suggestions (Groq)
- ✅ Automatic harmful content detection (Perspective)
- ✅ Translation capabilities (LibreTranslate)

**All integrated and ready to use!** 🚀
