Matériel / environnement
Box Android TV SFR Connect TV V2 (SDMC DV8555, Amlogic S905X2, Android TV 12 en version **32bits**, build 20251222). 
Bootloader verrouillé : pas de ROM custom, on reste sur le firmware SFR.
Mode développeur débloqué (code dérivé du numéro de série via github.com/dotkl3m/dev_mode_unlock).

Caractéristiques principales

• Processeur (CPU) : Quad-core ARM Cortex-A53 (64 bits) cadencé jusqu'à 2 GHz.
• Processeur graphique (GPU) : Mali-G31 MP2.
• ~2 Go (1.94 Go effectifs)
• Vidéo : Décodage 4K Ultra HD avec prise en charge du HDR10 et HLG.
• Connectivité et interfaces : 
  - Compatible USB 3.0, 
  - Bluetooth (selon les modèles) 
  - réseaux Gigabit LAN 
  - Wi-Fi double bande.

# connecttv-activities

Mini-appli Android TV exposant 3 tuiles (Guide, TV, Replay) qui ouvrent directement
les sections correspondantes du launcher SFR Connect TV (`com.sfr.androidtv.launcher`),
pour les utiliser depuis un launcher tiers (Projectivy, etc.).

Projectivy n'affiche qu'une tuile par package : chaque tuile est donc un APK distinct,
produit par un product flavor (dimension `tile`). Un 4e flavor, `homekey`, n'est pas
une tuile : c'est un service d'accessibilité qui renvoie la touche Home vers Projectivy.

| Flavor    | Package                                    | Contenu                          |
|-----------|--------------------------------------------|----------------------------------|
| `guide`   | `fr.minskaya.connecttv.activities.guide`   | tuile `GuideActivity`            |
| `tv`      | `fr.minskaya.connecttv.activities.tv`      | tuile `TvActivity`               |
| `replay`  | `fr.minskaya.connecttv.activities.replay`  | tuile `ReplayActivity`           |
| `homekey` | `fr.minskaya.connecttv.activities.homekey` | service `HomeKeyService` (Home)  |

Le code des tuiles (`TrampolineActivity` et ses sous-classes) est commun dans `app/src/main`.
Chaque flavor a son manifeste `app/src/<flavor>/AndroidManifest.xml`, qui ne déclare
que son activité (ou son service), avec son label et sa bannière.

## Pourquoi un service pour la touche Home

- Le launcher SFR déclare son activité HOME avec une **priorité 2** (appli système).
  Android le choisit donc toujours, même si Projectivy est enregistré comme launcher
  par défaut : `set-home-activity` est accepté mais n'a aucun effet sur la touche Home.
- Nos tuiles ouvrent justement cette activité HOME de SFR : la touche Home y ramène
  sur place, et Retour ne revient jamais à Projectivy.
- L'option « Remplacer le launcher actuel » de Projectivy ferme toute fenêtre SFR,
  y compris celles ouvertes par nos tuiles : inutilisable avec elles.
- `HomeKeyService` n'intercepte que la touche Home et ouvre Projectivy. Les tuiles
  restent ouvertes.

**Effet de bord connu :** dès qu'un service d'accessibilité est actif (le nôtre ou celui
de Projectivy), la navigation dans les menus de l'appli SFR devient erratique (la liste
saute, il faut appuyer plusieurs fois pour descendre). Testé : sans service, c'est fluide ;
avec notre service sans filtrage des touches, ça saute quand même. C'est l'appli SFR qui
réagit à l'accessibilité active. Solution en évaluation.

## Mode d'emploi ADB

Toutes les commandes se lancent dans PowerShell, à la racine du projet.

### 1. Se connecter à la box

```powershell
adb connect connecttv:5555
```
Ouvre la connexion ADB réseau. La box doit avoir le débogage réseau actif (mode développeur).
Au premier essai, accepter la demande d'autorisation qui s'affiche sur la TV.

```powershell
adb devices
```
Vérifie que la box apparaît avec l'état `device` (et non `unauthorized` ou `offline`).

### 2. Construire et installer

```powershell
.\gradlew.bat assembleDebug
```
Construit les 4 APK (3 tuiles + service Home) dans `app\build\outputs\apk\<flavor>\debug\`.
Pour un seul : `.\gradlew.bat assembleTvDebug`, `assembleHomekeyDebug`, etc.

```powershell
adb install -r app\build\outputs\apk\guide\debug\app-guide-debug.apk
adb install -r app\build\outputs\apk\tv\debug\app-tv-debug.apk
adb install -r app\build\outputs\apk\replay\debug\app-replay-debug.apk
```
Installe les 3 tuiles (`-r` remplace une version déjà installée). Elles apparaissent
dans Projectivy : Catégories → Apps TV → Gérer les applications.

```powershell
adb install -r app\build\outputs\apk\homekey\debug\app-homekey-debug.apk
```
Installe le service Home. Il reste inactif tant qu'il n'est pas activé (étape 4).

```powershell
adb shell cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER --brief
```
Liste les applis visibles par les launchers : les 3 `fr.minskaya.connecttv.activities.*`
doivent y figurer.

### 3. Déclarer Projectivy comme launcher

```powershell
adb shell cmd package set-home-activity com.spocky.projengmenu/.ui.home.MainActivity
```
Enregistre Projectivy comme launcher par défaut (rôle HOME). Sans effet visible sur la
touche Home à cause de la priorité du launcher SFR, mais Projectivy est alors le launcher
officiel (démarrage, écran d'accueil par défaut de certaines applis).

```powershell
adb shell dumpsys role | Select-String -Context 0,1 "android.app.role.HOME"
```
Vérifie : la ligne suivante doit afficher `holders=com.spocky.projengmenu`.

### 4. Activer le service d'accessibilité

Choisir **un seul** des deux services ; deux services qui filtrent les touches se gênent.

```powershell
adb shell settings put secure enabled_accessibility_services fr.minskaya.connecttv.activities.homekey/fr.minskaya.connecttv.HomeKeyService
adb shell settings put secure accessibility_enabled 1
```
Active **notre** service : Home ouvre Projectivy, les tuiles restent ouvertes.
Conséquence : navigation erratique dans les menus de l'appli SFR (voir plus haut).

```powershell
adb shell dumpsys accessibility | Select-String "Bound services","Enabled services"
```
Vérifie que le service choisi est bien lié (`Bound services` non vide).
Après une réinstallation de l'APK `homekey`, refaire cette vérification ; réactiver si besoin.

```powershell
adb logcat -s HomeKeyService:D
```
Facultatif : affiche chaque touche vue par notre service (Ctrl-C pour arrêter).

### 5. Revenir à l'état initial

```powershell
adb shell settings delete secure enabled_accessibility_services
adb shell settings put secure accessibility_enabled 0
```
Désactive tout service d'accessibilité. Home ramène de nouveau au launcher SFR ;
la navigation dans l'appli SFR redevient fluide.

```powershell
adb shell cmd package set-home-activity com.sfr.androidtv.launcher/com.altice.androidtv.launcher.ui.LauncherActivity
```
Rend le rôle HOME au launcher SFR, comme à la sortie d'usine. Projectivy reste installé
et utilisable comme une appli.

```powershell
adb uninstall fr.minskaya.connecttv.activities.homekey
```
Supprime le service Home.

```powershell
adb uninstall fr.minskaya.connecttv.activities.guide
adb uninstall fr.minskaya.connecttv.activities.tv
adb uninstall fr.minskaya.connecttv.activities.replay
```
Supprime les 3 tuiles

```powershell
adb shell cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN
```
Vérifie : doit afficher `com.sfr.androidtv.launcher/com.altice.androidtv.launcher.ui.LauncherActivity`.

```powershell
adb disconnect connecttv:5555
```
Ferme la connexion ADB.

## Divers

Si `gradlew.bat` / `gradle-wrapper.jar` manquent, Android Studio les régénère
(File > Sync), ou `gradle wrapper --gradle-version 8.9`.

## Ajouter une tuile
1. Une sous-classe de `TrampolineActivity` dans `app/src/main/java/fr/minskaya/connecttv/`.
2. Un flavor dans `app/build.gradle.kts` (`dimension = "tile"`, `applicationIdSuffix` propre).
3. `app/src/<flavor>/AndroidManifest.xml` sur le modèle des existants, et une bannière
   `app/src/main/res/drawable/banner_<flavor>.png`.

Autres actions disponibles : `com.sfr.androidtv.VOD`, `com.sfr.androidtv.SEARCH`.
Deep links possibles (ACTION_VIEW) : `gen8://alert/settings`, `gen8://alert/tv/channel?play=<id>`.
