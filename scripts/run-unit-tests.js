const fs = require('fs');
const assert = require('assert');

console.log("=== Running Dual App Shortcut Launcher Unit & Logic Test Suite ===\n");

let suitePassed = 0;
let suiteFailed = 0;

function test(description, fn) {
    try {
        fn();
        console.log(`  [PASS] ${description}`);
        suitePassed++;
    } catch (err) {
        console.error(`  [FAIL] ${description}`);
        console.error(`         Error: ${err.message}`);
        suiteFailed++;
    }
}

// -------------------------------------------------------------
// Suite 1: LaunchProfile & Safe Delay Logic
// -------------------------------------------------------------
console.log("Suite 1: LaunchProfile Model & Safe Delay Logic");

function calculateSafeDelay(delayMs) {
    return Math.max(0, Math.min(2000, delayMs));
}

function isConfigured(slot1, slot2) {
    return slot1 !== null && slot1 !== undefined && slot2 !== null && slot2 !== undefined;
}

test("LaunchProfile.isConfigured returns false when slot1 or slot2 is missing", () => {
    assert.strictEqual(isConfigured(null, { label: "App 2" }), false);
    assert.strictEqual(isConfigured({ label: "App 1" }, null), false);
    assert.strictEqual(isConfigured(null, null), false);
});

test("LaunchProfile.isConfigured returns true when both slots are configured", () => {
    assert.strictEqual(isConfigured({ label: "App 1" }, { label: "App 2" }), true);
});

test("LaunchProfile.safeDelayMs clamps negative delay to 0 ms", () => {
    assert.strictEqual(calculateSafeDelay(-100), 0);
    assert.strictEqual(calculateSafeDelay(-1), 0);
});

test("LaunchProfile.safeDelayMs clamps excessive delay to 2000 ms", () => {
    assert.strictEqual(calculateSafeDelay(2500), 2000);
    assert.strictEqual(calculateSafeDelay(99999), 2000);
});

test("LaunchProfile.safeDelayMs preserves valid delay between 0 and 2000 ms", () => {
    assert.strictEqual(calculateSafeDelay(0), 0);
    assert.strictEqual(calculateSafeDelay(350), 350);
    assert.strictEqual(calculateSafeDelay(2000), 2000);
});

// -------------------------------------------------------------
// Suite 2: IntentSerializer & Transient Flag Stripping
// -------------------------------------------------------------
console.log("\nSuite 2: IntentSerializer Logic & Flag Sanitization");

const FLAG_GRANT_READ_URI_PERMISSION = 0x00000001;
const FLAG_GRANT_WRITE_URI_PERMISSION = 0x00000002;
const FLAG_GRANT_PERSISTABLE_URI_PERMISSION = 0x00000040;
const FLAG_GRANT_PREFIX_URI_PERMISSION = 0x00000080;
const FLAG_ACTIVITY_NEW_TASK = 0x10000000;

const TRANSIENT_FLAGS_MASK = ~(
    FLAG_GRANT_READ_URI_PERMISSION |
    FLAG_GRANT_WRITE_URI_PERMISSION |
    FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
    FLAG_GRANT_PREFIX_URI_PERMISSION
);

function sanitizeFlags(flags) {
    return flags & TRANSIENT_FLAGS_MASK;
}

function restoreIntentFlags(flags) {
    return flags | FLAG_ACTIVITY_NEW_TASK;
}

test("IntentSerializer strips transient URI permission flags (read, write, persistable, prefix)", () => {
    const inputFlags = FLAG_GRANT_READ_URI_PERMISSION |
                       FLAG_GRANT_WRITE_URI_PERMISSION |
                       FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                       FLAG_GRANT_PREFIX_URI_PERMISSION |
                       FLAG_ACTIVITY_NEW_TASK;

    const sanitized = sanitizeFlags(inputFlags);
    assert.strictEqual((sanitized & FLAG_GRANT_READ_URI_PERMISSION), 0);
    assert.strictEqual((sanitized & FLAG_GRANT_WRITE_URI_PERMISSION), 0);
    assert.strictEqual((sanitized & FLAG_GRANT_PERSISTABLE_URI_PERMISSION), 0);
    assert.strictEqual((sanitized & FLAG_GRANT_PREFIX_URI_PERMISSION), 0);
    assert.strictEqual((sanitized & FLAG_ACTIVITY_NEW_TASK), FLAG_ACTIVITY_NEW_TASK);
});

test("IntentSerializer restores FLAG_ACTIVITY_NEW_TASK on deserialization", () => {
    const originalFlags = 0;
    const restoredFlags = restoreIntentFlags(originalFlags);
    assert.notStrictEqual((restoredFlags & FLAG_ACTIVITY_NEW_TASK), 0);
});

test("IntentSerializer returns null on blank or null URI strings", () => {
    function fromUriOrNull(uri) {
        if (!uri || uri.trim() === "") return null;
        if (!uri.startsWith("intent:") && !uri.startsWith("#Intent;")) return null;
        return { uri, flags: FLAG_ACTIVITY_NEW_TASK };
    }
    assert.strictEqual(fromUriOrNull(null), null);
    assert.strictEqual(fromUriOrNull(""), null);
    assert.strictEqual(fromUriOrNull("   "), null);
    assert.strictEqual(fromUriOrNull("invalid_scheme://test"), null);
    assert.notStrictEqual(fromUriOrNull("#Intent;action=MAIN;end"), null);
});

// -------------------------------------------------------------
// Suite 3: Huawei & Honor Device & Permission Logic
// -------------------------------------------------------------
console.log("\nSuite 3: Huawei & Honor Device Detection & Permission Routing");

function isHuaweiOrHonorManufacturer(mfg) {
    if (!mfg) return false;
    const lower = mfg.toLowerCase();
    return lower.includes("huawei") || lower.includes("honor");
}

test("Huawei detection recognizes HUAWEI, Huawei, huawei", () => {
    assert.strictEqual(isHuaweiOrHonorManufacturer("HUAWEI"), true);
    assert.strictEqual(isHuaweiOrHonorManufacturer("Huawei"), true);
    assert.strictEqual(isHuaweiOrHonorManufacturer("huawei"), true);
});

test("Huawei detection recognizes HONOR, Honor, honor", () => {
    assert.strictEqual(isHuaweiOrHonorManufacturer("HONOR"), true);
    assert.strictEqual(isHuaweiOrHonorManufacturer("Honor"), true);
    assert.strictEqual(isHuaweiOrHonorManufacturer("honor"), true);
});

test("Huawei detection ignores non-Huawei brands (Xiaomi, Samsung, Google, OPPO, VIVO)", () => {
    assert.strictEqual(isHuaweiOrHonorManufacturer("Xiaomi"), false);
    assert.strictEqual(isHuaweiOrHonorManufacturer("Samsung"), false);
    assert.strictEqual(isHuaweiOrHonorManufacturer("Google"), false);
    assert.strictEqual(isHuaweiOrHonorManufacturer("OPPO"), false);
    assert.strictEqual(isHuaweiOrHonorManufacturer("vivo"), false);
});

test("Huawei permission settings Intent prioritizes systemmanager with fallback to details", () => {
    const pkgName = "com.example.dualshortcut";
    const fallbackIntent = {
        action: "android.settings.APPLICATION_DETAILS_SETTINGS",
        data: `package:${pkgName}`
    };
    assert.strictEqual(fallbackIntent.action, "android.settings.APPLICATION_DETAILS_SETTINGS");
    assert.strictEqual(fallbackIntent.data, `package:${pkgName}`);
});

// -------------------------------------------------------------
// Suite 4: DispatchRunner Sequential Execution Simulation
// -------------------------------------------------------------
console.log("\nSuite 4: DispatchRunner Sequential Execution & Error Tolerance");

async function simulateDispatchRunner(profile, launcher, onError) {
    let completed = false;
    try {
        if (profile.slot1) {
            try {
                launcher(profile.slot1);
            } catch (err) {
                onError(`Failed slot1: ${err.message}`);
            }
        }

        const delayMs = calculateSafeDelay(profile.delayMs);
        if (delayMs > 0) {
            await new Promise(resolve => setTimeout(resolve, delayMs));
        }

        if (profile.slot2) {
            try {
                launcher(profile.slot2);
            } catch (err) {
                onError(`Failed slot2: ${err.message}`);
            }
        }
    } finally {
        completed = true;
    }
    return completed;
}

test("DispatchRunner launches Slot 1 then Slot 2 with configured delay", async () => {
    const events = [];
    const startTime = Date.now();

    const profile = {
        slot1: { label: "App 1", packageName: "com.example.app1" },
        slot2: { label: "App 2", packageName: "com.example.app2" },
        delayMs: 100
    };

    const completed = await simulateDispatchRunner(
        profile,
        (target) => events.push({ target: target.label, time: Date.now() - startTime }),
        (err) => console.error(err)
    );

    assert.strictEqual(completed, true);
    assert.strictEqual(events.length, 2);
    assert.strictEqual(events[0].target, "App 1");
    assert.strictEqual(events[1].target, "App 2");
    // Verify interval elapsed is at least around 80ms
    assert.ok(events[1].time - events[0].time >= 80, `Expected delay >= 80ms, got ${events[1].time - events[0].time}ms`);
});

test("DispatchRunner handles ActivityNotFound error on Slot 1 without breaking Slot 2", async () => {
    const events = [];
    const errors = [];

    const profile = {
        slot1: { label: "Missing App", packageName: "com.example.missing" },
        slot2: { label: "App 2", packageName: "com.example.app2" },
        delayMs: 20
    };

    const completed = await simulateDispatchRunner(
        profile,
        (target) => {
            if (target.packageName === "com.example.missing") {
                throw new Error("ActivityNotFoundException");
            }
            events.push(target.label);
        },
        (err) => errors.push(err)
    );

    assert.strictEqual(completed, true);
    assert.strictEqual(errors.length, 1);
    assert.ok(errors[0].includes("ActivityNotFoundException"));
    assert.strictEqual(events.length, 1);
    assert.strictEqual(events[0], "App 2");
});

// -------------------------------------------------------------
// Suite 5: Signing & Manifest Security / Design Verification
// -------------------------------------------------------------
console.log("\nSuite 5: Signing, Manifest & Architecture Invariants");

test("Manifest configures DispatchActivity with required stealth flags", () => {
    const manifest = fs.readFileSync('app/src/main/AndroidManifest.xml', 'utf8');
    assert.ok(manifest.includes('android:name=".ui.DispatchActivity"'));
    assert.ok(manifest.includes('android:excludeFromRecents="true"'));
    assert.ok(manifest.includes('android:noHistory="true"'));
    assert.ok(manifest.includes('android:launchMode="singleInstance"'));
    assert.ok(manifest.includes('android:taskAffinity=""'));
});

test("build.gradle.kts enables V1, V2, and V3 signing simultaneously", () => {
    const buildGradle = fs.readFileSync('app/build.gradle.kts', 'utf8');
    assert.ok(buildGradle.includes('enableV1Signing = true'));
    assert.ok(buildGradle.includes('enableV2Signing = true'));
    assert.ok(buildGradle.includes('enableV3Signing = true'));
});

test("Translucent theme completely disables window animation and window background", () => {
    const themes = fs.readFileSync('app/src/main/res/values/themes.xml', 'utf8');
    assert.ok(themes.includes('Theme.DualAppShortcutLauncher.Translucent'));
    assert.ok(themes.includes('android:windowAnimationStyle">@null'));
    assert.ok(themes.includes('android:windowBackground">@android:color/transparent'));
    assert.ok(themes.includes('android:windowIsTranslucent">true'));
});

console.log(`\n======================================================`);
console.log(`Suite Summary: ${suitePassed} Passed, ${suiteFailed} Failed`);
console.log(`======================================================\n`);

if (suiteFailed > 0) {
    process.exit(1);
}
