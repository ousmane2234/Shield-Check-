# ShieldCheck - Application Android Native

Système de sécurité automatisé pour téléphones volés utilisant Kotlin et Supabase.

## 🎯 Fonctionnalités Principales

- **Identification Automatique**: Récupération de l'IMEI via TelephonyManager
- **Mode Sécours**: Saisie manuelle avec stockage SharedPreferences
- **Surveillance Real-Time**: Écoute Supabase Realtime sur table `objets_voles`
- **Verrouillage Forcé**: Activation Device Admin pour `lockNow()`
- **Service de Premier Plan**: Surveillance continue sans interruption

## 📋 Architecture

```
com.shieldcheck.app/
├── MainActivity.kt                    # UI Compose
├── service/
│   └── DeviceMonitorService.kt       # Service Foreground + Realtime
├── receiver/
│   └── DeviceAdminReceiver.kt        # Gestion Device Admin
├── data/
│   ├── model/
│   │   └── StolenObject.kt           # Data class Supabase
│   └── repository/
│       └── StolenObjectRepository.kt # Intégration Supabase
└── util/
    └── DeviceIdentifier.kt           # Gestion IMEI
```

## 🔧 Installation et Configuration

### Prérequis
- Android Studio Giraffe+
- SDK Android 26+ (cible 34)
- Kotlin 1.9.10+
- Compte Supabase avec table `objets_voles`

### Étapes

1. **Cloner le repository**
   ```bash
   git clone https://github.com/ousmane2234/Shield-Check-.git
   cd Shield-Check-
   ```

2. **Configurer Supabase**
   - Éditez `app/src/main/java/com/shieldcheck/app/service/DeviceMonitorService.kt`
   - Remplacez:
     ```kotlin
     val supabaseUrl = "https://YOUR_PROJECT.supabase.co"
     val supabaseKey = "YOUR_ANON_KEY"
     ```

3. **Compiler et Lancer**
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## 📱 Permissions Requises

- `READ_PHONE_STATE` - Accès IMEI
- `INTERNET` - Connexion Supabase
- `FOREGROUND_SERVICE` - Service continu
- `POST_NOTIFICATIONS` - Alertes utilisateur
- `BIND_DEVICE_ADMIN` - Verrouillage
- `ACCESS_NETWORK_STATE` - Vérification réseau

## 🚀 Build Production

```bash
# APK Debug (développement)
./gradlew assembleDebug

# APK Release (production)
./gradlew assembleRelease

# Fichiers générés
# Debug: app/build/outputs/apk/debug/app-debug.apk
# Release: app/build/outputs/apk/release/app-release.apk
```

Consultez [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md) pour les détails complets.

## 📖 Utilisation

1. **Démarrage**: L'app récupère automatiquement l'IMEI
2. **Saisie manuelle**: Si nécessaire, formulaire de saisie
3. **Surveillance**: Le service écoute Supabase en continu
4. **Alerte**: Si correspondance détectée → Verrouillage immédiat

## 🔐 Sécurité

- Device Admin requis pour verrouillage
- Utilisation HTTPS pour Supabase
- IMEI stocké localement et de façon sécurisée
- Pas de transmission d'IMEI non chiffré

## 📊 Schéma Supabase

Table requise: `objets_voles`
```sql
- id (UUID)
- imei (TEXT)
- status (TEXT: 'vole', 'retrouve', 'archive')
- owner_name (TEXT)
- owner_email (TEXT)
- phone_model (TEXT)
- created_at (TIMESTAMP)
- updated_at (TIMESTAMP)
- description (TEXT, nullable)
```

## 🤝 Contribution

Les contributions sont bienvenues!

## 📄 Licence

Propriétaire - Tous droits réservés

---

**Auteur**: Ousmane  
**Année**: 2026  
**Version**: 1.0.0