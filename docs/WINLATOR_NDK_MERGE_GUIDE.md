# Panduan Penggabungan StormRev Launcher ke Fork Winlator NDK

Dokumen ini memandu Anda menggabungkan UI & konfigurator **StormRev Launcher** (yang sudah siap pakai untuk MediaTek/ARM Mali) ke dalam repositori **fork Winlator NDK** milik Anda agar menghasilkan **1 APK tunggal** beserta runtime **Box64 + Wine + Vortek/VirGL + XServer**.

> **Catatan:** Repositori ini tidak menyertakan *source code* C/C++ Winlator maupun `imagefs.txz`. Anda perlu mengkloning atau mem-fork proyek *open-source* Winlator terlebih dahulu, lalu menyalin modul Kotlin & resource dari proyek ini ke dalamnya.

---

## 1. Berkas yang Disalin dari Proyek Ini ke Fork Winlator

Salin folder/berkas berikut ke `app/src/main/` di repositori Winlator Anda (sesuaikan deklarasi `package` dan `import R` agar cocok dengan `namespace` Winlator, misalnya `com.winlator`):

- `java/com/example/MainActivity.kt` (ubah nama menjadi `StormLauncherActivity.kt` agar tidak bentrok dengan `MainActivity` milik Winlator)
- `java/com/example/model/LauncherModels.kt`
- `java/com/example/data/*` (`GameFolderScanner.kt`, `HardwareAndControllerMonitor.kt`, `LauncherPreferencesRepository.kt`)
- `java/com/example/runtime/RuntimeContainerBridge.kt`
- `java/com/example/ui/*` (`LauncherViewModel.kt`, komponen, tema, dan semua layar Compose)
- `res/font/*` (`chakra_petch.ttf`, `plus_jakarta_sans.ttf`, `jetbrains_mono.ttf`)
- `res/drawable/img_storm_hero_banner.jpg` dan `res/drawable/img_controller_deck.jpg`
- Entri string di `res/values/strings.xml` dan warna/tema di `res/values/themes.xml`

---

## 2. Tambahkan Dukungan Jetpack Compose & DataStore di `app/build.gradle` Winlator

Aktifkan Kotlin + Compose di proyek Gradle Winlator:

```kotlin
android {
    defaultConfig {
        ndk {
            abiFilters += "arm64-v8a"
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
```

---

## 3. Implementasikan `RuntimeContainerBridge` Internal di Winlator

Ganti `ExternalPackageRuntimeBridge(application)` di `LauncherViewModel` dengan implementasi internal yang memanggil `ContainerManager` dan `XServerDisplayActivity` milik Winlator secara langsung:

```kotlin
class EmbeddedWinlatorBridge(private val context: Context) : RuntimeContainerBridge {

    override fun detectInstalledRuntime(): String = context.packageName

    override fun launchContainerWithPreset(
        graphicsConfig: GraphicsConfig,
        controllerProfile: ControllerProfile,
        hardware: DeviceHardwareProfile,
        gameDir: GameDirectoryStatus.Ready?
    ): RuntimeLaunchResult {
        // 1. Buat atau ambil Container Winlator via ContainerManager(context)
        // 2. Terapkan parameter MediaTek / Mali dari graphicsConfig:
        //    - graphicsDriver = graphicsConfig.winlatorDriver.id ("vortek" / "virgl" / "turnip")
        //    - dxwrapper      = graphicsConfig.dxWrapper.id ("dxvk-1.10.3" / "dxvk-sarek" / "wined3d")
        //    - screenSize     = "${graphicsConfig.scaledResolutionWidth()}x${graphicsConfig.resolutionScale.height}"
        //    - envVars        = graphicsConfig.toWinlatorMaliEnvString(hardware)
        //    - box64Preset    = "PERFORMANCE"
        // 3. Buat/perbarui shortcut ke NSUNSR.exe (atau launch_nsunsr.bat)
        // 4. Luncurkan XServerDisplayActivity milik Winlator dengan Intent:
        //    val intent = Intent(context, com.winlator.XServerDisplayActivity::class.java)
        //        .putExtra("container_id", container.id)
        //        .putExtra("shortcut_path", shortcutPath)
        //        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        //    context.startActivity(intent)
        return RuntimeLaunchResult.Launched("Winlator Embedded Runtime (${graphicsConfig.winlatorDriver.label})")
    }
}
```

---

## 4. Pastikan `imagefs.txz` & Build NDK via GitHub Actions

1. Pastikan aset runtime (`app/src/main/assets/imagefs.txz` beserta komponen Box64, Wine, Vortek/VirGL, DXVK 1.10.3/Sarek) sudah berada di direktori aset repo Winlator sebelum build.
2. Salin berkas `docs/winlator-ndk-build.yml` ke `.github/workflows/winlator-ndk-build.yml` di **repositori fork Winlator Anda** untuk membangun 1 APK gabungan secara otomatis.
