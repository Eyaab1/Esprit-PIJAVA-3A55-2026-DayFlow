# 🔧 LibreTranslate API Error Fix

## Error

```
❌ Erreur de traduction : LibreTranslate API error (code 400): 
{"error":"Visit https://portal.libretranslate.com..."}
```

## Root Cause

The public LibreTranslate instance at `https://libretranslate.com` is:
- Rate limited
- Requires registration
- Or temporarily down

## ✅ Solution: Use Alternative Instance

I've updated your configuration to use an alternative free instance.

### Updated Configuration:

```properties
# application.properties
libretranslate.api.url=https://translate.argosopentech.com/translate
libretranslate.api.key=
```

---

## 🔄 How to Apply Fix

### Option 1: Restart Application (Recommended)

```bash
# Stop current app (Ctrl+C)
# Restart
mvn javafx:run
```

The new configuration will be loaded automatically.

---

## 🌐 Alternative LibreTranslate Instances

If the current one doesn't work, try these alternatives:

### Instance 1: Argos Open Tech (Current)
```properties
libretranslate.api.url=https://translate.argosopentech.com/translate
```
- ✅ Free
- ✅ No API key needed
- ✅ Good uptime

### Instance 2: LibreTranslate Official (Original)
```properties
libretranslate.api.url=https://libretranslate.com/translate
```
- ⚠️ May require API key
- ⚠️ Rate limited

### Instance 3: Self-Hosted (Best for Production)
```bash
# Run your own instance with Docker
docker run -ti --rm -p 5000:5000 libretranslate/libretranslate
```

Then update config:
```properties
libretranslate.api.url=http://localhost:5000/translate
```

---

## 🧪 Test After Fix

1. **Restart application**
2. **Go to**: Admin → Réclamations
3. **Click**: "👁 Voir" on any reclamation
4. **Click**: "🌐 Traduire le contenu en français"
5. **Verify**: Translation works without error

---

## 🐛 If Still Not Working

### Check 1: Internet Connection
```bash
# Test if you can reach the API
curl https://translate.argosopentech.com/translate
```

### Check 2: Try Another Instance
Update `application.properties`:
```properties
# Try this one
libretranslate.api.url=https://libretranslate.de/translate
```

### Check 3: Disable Translation Feature (Temporary)
If translation is not critical, you can temporarily disable it by commenting out the translate button code.

---

## 📝 Alternative: Use Google Translate API

If LibreTranslate continues to have issues, you could switch to Google Translate API:

```properties
# Google Translate (requires API key)
google.translate.api.key=YOUR_KEY_HERE
```

But this requires:
1. Google Cloud account
2. Enable Translation API
3. Get API key
4. Update code to use Google Translate

---

## ✅ Summary

**What I Changed:**
- ✅ Updated `application.properties` to use alternative instance
- ✅ Changed URL from `libretranslate.com` to `translate.argosopentech.com`

**What You Need to Do:**
1. ✅ Restart your application
2. ✅ Test translation feature
3. ✅ If still not working, try another instance from the list above

---

## 🎯 Quick Fix Command

```bash
# Stop app (Ctrl+C)
# Restart
mvn javafx:run
```

**Translation should now work!** 🌐✨
