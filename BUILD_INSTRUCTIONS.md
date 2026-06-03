# ShieldCheck - Instructions de Build Complet

## Configuration Requise

- **Android SDK**: 26 (Android 8.0) minimum
- **Target SDK**: 34 (Android 14)
- **Kotlin**: 1.9.10+
- **Gradle**: 8.1.0+
- **Java**: 11+

## Configuration Supabase

Avant de compiler l'application, vous devez configurer vos identifiants Supabase:

1. Ouvrez `app/src/main/java/com/shieldcheck/app/service/DeviceMonitorService.kt`
2. Remplacez les valeurs suivantes:
   ```kotlin
   val supabaseUrl = "YOUR_SUPABASE_URL"
   val supabaseKey = "YOUR_SUPABASE_ANON_KEY"
   ```

## Instructions de Build

### 1. Préparer l'Environnement

```bash
# Assurez-vous que Android SDK est installé et configuré
# Configurez le fichier local.properties:
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

### 2. Nettoyer et Construire

```bash
# Nettoyer les builds précédents
./gradlew clean

# Construire le projet
./gradlew build

# Ou construire directement l'APK pour le déploiement
./gradlew assembleDebug      # Pour le développement
./gradlew assembleRelease    # Pour la production
```

### 3. Générer l'APK de Production

Pour créer une APK signée pour le déploiement en production:

```bash
# Générer la clé de signature (une seule fois)
keytool -genkey -v -keystore shieldcheck-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias shieldcheck

# Configurer le fichier de signature (créer app/keystore.properties)
STORE_FILE=../shieldcheck-key.jks
STORE_PASSWORD=your_password
KEY_ALIAS=shieldcheck
KEY_PASSWORD=your_password

# Compiler la version signée
./gradlew assembleRelease
```

### 4. Localiser l'APK Généré

L'APK compilée se trouvera à:
- **Debug**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release**: `app/build/outputs/apk/release/app-release.apk`

### 5. Déployer sur les Appareils

```bash
# Installer sur un appareil connecté (via USB avec débogage activé)
adb install app/build/outputs/apk/debug/app-debug.apk

# Ou installer la version de production
adb install app/build/outputs/apk/release/app-release.apk

# Vérifier l'installation
adb shell pm list packages | grep shieldcheck
```

## Architecture de l'Application

### Composants Principaux

1. **MainActivity**: Interface utilisateur Compose
   - Affichage de l'IMEI
   - Saisie manuelle de l'IMEI si nécessaire
   - État de surveillance en temps réel

2. **DeviceMonitorService**: Service de premier plan
   - Écoute Realtime sur la table `objets_voles`
   - Détection des correspondances IMEI
   - Verrouillage du dispositif automatique

3. **DeviceAdminReceiver**: Gestionnaire Device Admin
   - Activation/désactivation de l'accès administrateur
   - Verrouillage forcé du téléphone

4. **DeviceIdentifier**: Utilitaire de gestion IMEI
   - Récupération via TelephonyManager
   - Fallback UUID en cas d'échec
   - Stockage dans SharedPreferences

5. **StolenObjectRepository**: Intégration Supabase
   - Connexion à la table `objets_voles`
   - Streaming en temps réel via Realtime
   - Requêtes de données

## Permissions Requises

Les permissions suivantes sont demandées à l'installation:

- `READ_PHONE_STATE`: Accès à l'IMEI
- `INTERNET`: Communication avec Supabase
- `FOREGROUND_SERVICE`: Exécution du service de surveillance
- `POST_NOTIFICATIONS`: Notifications système
- `BIND_DEVICE_ADMIN`: Contrôle administrateur du appareil
- `ACCESS_NETWORK_STATE`: Vérification de la connectivité

## Fonctionnement de la Surveillance

1. **Au démarrage**:
   - L'application récupère l'IMEI du téléphone
   - Si non disponible, affiche un formulaire de saisie manuelle
   - Démarre le `DeviceMonitorService`

2. **En fonctionnement**:
   - Le service établit une connexion Realtime avec Supabase
   - Écoute les modifications sur la table `objets_voles`
   - Compare les IMEI reçus avec l'IMEI du dispositif

3. **En cas de correspondance**:
   - Si l'IMEI correspond et le statut est `'vole'`:
     - Affiche une notification
     - Verrouille immédiatement le téléphone via `devicePolicyManager.lockNow()`
     - Enregistre l'événement dans les logs

## Désactivation du Device Admin

Si vous devez désactiver l'accès Device Admin:

```bash
adb shell dpm remove-active-admin com.shieldcheck.app/.receiver.DeviceAdminReceiver
```

## Troubleshooting

### L'IMEI n'est pas récupéré
- Vérifiez que la permission `READ_PHONE_STATE` est accordée
- Utilisez la saisie manuelle pour enregistrer l'IMEI manuellement

### Le Service ne démarre pas
- Assurez-vous que les permissions sont accordées
- Vérifiez que Device Admin est activé
- Consultez les logs avec `adb logcat`

### Connexion Supabase échouée
- Vérifiez que l'URL et la clé Supabase sont correctes
- Vérifiez la connectivité Internet du téléphone
- Consulter les logs pour les erreurs de connexion

## Logs et Debugging

Pour consulter les logs en temps réel:

```bash
adb logcat | grep ShieldCheck
# ou
adb logcat | grep "DeviceMonitorService"
```

## Notes de Sécurité

- **Ne partagez jamais votre clé Supabase** en production
- Utilisez les **policies Row Level Security** de Supabase pour sécuriser les accès
- Assurez-vous que la table `objets_voles` n'est accessible qu'en lecture
- Signez l'APK avec une clé privée sécurisée avant le déploiement

## Support et Assistance

Pour toute question ou problème, consultez:
- Documentation Android: https://developer.android.com
- Documentation Supabase: https://supabase.com/docs
- Kotlin Documentation: https://kotlinlang.org/docs