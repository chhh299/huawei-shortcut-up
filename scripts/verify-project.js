const fs = require('fs');
const path = require('path');

console.log("=== Dual App Shortcut Launcher Verification Suite ===");

let passed = 0;
let failed = 0;

function assert(condition, message) {
    if (condition) {
        console.log(`  [PASS] ${message}`);
        passed++;
    } else {
        console.error(`  [FAIL] ${message}`);
        failed++;
    }
}

// 1. Check Project Structure & Gradle Config
console.log("\n1. Verifying Gradle Build Configuration:");
const rootBuild = fs.readFileSync('build.gradle.kts', 'utf8');
assert(rootBuild.includes('com.android.application'), "Root build.gradle.kts configures Android plugin");
assert(rootBuild.includes('org.jetbrains.kotlin.android'), "Root build.gradle.kts configures Kotlin plugin");

const appBuild = fs.readFileSync('app/build.gradle.kts', 'utf8');
assert(appBuild.includes('compileSdk = 34'), "app/build.gradle.kts sets compileSdk = 34");
assert(appBuild.includes('minSdk = 26'), "app/build.gradle.kts sets minSdk = 26");
assert(appBuild.includes('targetSdk = 34'), "app/build.gradle.kts sets targetSdk = 34");
assert(appBuild.includes('viewBinding = true'), "app/build.gradle.kts enables viewBinding");
assert(appBuild.includes('enableV1Signing = true'), "app/build.gradle.kts enables V1 signing");
assert(appBuild.includes('enableV2Signing = true'), "app/build.gradle.kts enables V2 signing");
assert(appBuild.includes('enableV3Signing = true'), "app/build.gradle.kts enables V3 signing");

// 2. Check AndroidManifest.xml
console.log("\n2. Verifying AndroidManifest.xml:");
const manifest = fs.readFileSync('app/src/main/AndroidManifest.xml', 'utf8');
assert(manifest.includes('android.permission.QUERY_ALL_PACKAGES'), "Declares QUERY_ALL_PACKAGES permission");
assert(manifest.includes('com.android.launcher.permission.INSTALL_SHORTCUT'), "Declares INSTALL_SHORTCUT permission");
assert(manifest.includes('<action android:name="android.intent.action.CREATE_SHORTCUT" />'), "Declares queries for ACTION_CREATE_SHORTCUT");
assert(manifest.includes('android:name=".ui.DispatchActivity"'), "Declares DispatchActivity");
assert(manifest.includes('android:excludeFromRecents="true"'), "DispatchActivity sets excludeFromRecents=true");
assert(manifest.includes('android:noHistory="true"'), "DispatchActivity sets noHistory=true");
assert(manifest.includes('android:theme="@style/Theme.DualAppShortcutLauncher.Translucent"'), "DispatchActivity uses Translucent theme");
assert(manifest.includes('android:name=".ui.MainActivity"'), "Declares MainActivity");

// 3. Check Themes and Resources
console.log("\n3. Verifying Themes & Resources:");
const themes = fs.readFileSync('app/src/main/res/values/themes.xml', 'utf8');
assert(themes.includes('Theme.DualAppShortcutLauncher.Translucent'), "Defines Translucent theme");
assert(themes.includes('android:windowAnimationStyle">@null'), "Translucent theme sets windowAnimationStyle=@null");
assert(themes.includes('android:windowIsTranslucent">true'), "Translucent theme sets windowIsTranslucent=true");

const strings = fs.readFileSync('app/src/main/res/values/strings.xml', 'utf8');
assert(strings.includes('title_slot_1') && strings.includes('title_slot_2'), "Defines Slot 1 and Slot 2 string resources");
assert(strings.includes('huawei_permission_title'), "Defines Huawei permission adaptation strings");

// 4. Check CI Workflow
console.log("\n4. Verifying GitHub Actions Workflow:");
const workflow = fs.readFileSync('.github/workflows/build-apk.yml', 'utf8');
assert(workflow.includes('actions/setup-java@v4'), "CI sets up Java");
assert(workflow.includes('java-version: \'17\''), "CI uses JDK 17");
assert(workflow.includes('KEYSTORE_BASE64'), "CI supports KEYSTORE_BASE64 secret");
assert(workflow.includes('./gradlew assembleRelease'), "CI builds Release APK");
assert(workflow.includes('apksigner verify'), "CI verifies APK signatures");
assert(workflow.includes('actions/upload-artifact@v4'), "CI uploads APK artifact");

// 5. Check Kotlin Source Code
console.log("\n5. Verifying Kotlin Source Code:");
const expectedSourceFiles = [
    'app/src/main/java/com/example/dualshortcut/data/model/TargetType.kt',
    'app/src/main/java/com/example/dualshortcut/data/model/LaunchTarget.kt',
    'app/src/main/java/com/example/dualshortcut/data/model/LaunchProfile.kt',
    'app/src/main/java/com/example/dualshortcut/data/repository/LauncherConfigRepository.kt',
    'app/src/main/java/com/example/dualshortcut/util/IntentSerializer.kt',
    'app/src/main/java/com/example/dualshortcut/util/HuaweiShortcutUtils.kt',
    'app/src/main/java/com/example/dualshortcut/util/ShortcutPickerHelper.kt',
    'app/src/main/java/com/example/dualshortcut/ui/DispatchRunner.kt',
    'app/src/main/java/com/example/dualshortcut/ui/DispatchActivity.kt',
    'app/src/main/java/com/example/dualshortcut/ui/AppPickerAdapter.kt',
    'app/src/main/java/com/example/dualshortcut/ui/MainActivity.kt'
];

for (const file of expectedSourceFiles) {
    assert(fs.existsSync(file), `Source file exists: ${file}`);
}

// 6. Check Test Files
console.log("\n6. Verifying Unit Test Suite:");
const expectedTestFiles = [
    'app/src/test/java/com/example/dualshortcut/data/model/LaunchProfileTest.kt',
    'app/src/test/java/com/example/dualshortcut/data/repository/LauncherConfigRepositoryTest.kt',
    'app/src/test/java/com/example/dualshortcut/util/IntentSerializerTest.kt',
    'app/src/test/java/com/example/dualshortcut/util/HuaweiShortcutUtilsTest.kt',
    'app/src/test/java/com/example/dualshortcut/ui/DispatchLogicTest.kt'
];

for (const file of expectedTestFiles) {
    assert(fs.existsSync(file), `Test file exists: ${file}`);
}

console.log(`\nVerification Summary: ${passed} Passed, ${failed} Failed`);
if (failed > 0) {
    process.exit(1);
}
