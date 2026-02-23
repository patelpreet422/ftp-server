#!/usr/bin/env bash
# =============================================================================
# release-checklist.sh — Pre-release verification script
# =============================================================================
#
# Usage:
#   ./scripts/release-checklist.sh [version]
#
# Example:
#   ./scripts/release-checklist.sh v1.2.0
#
# This script runs through a complete release checklist:
#   1. Verify a device/emulator is connected via ADB
#   2. Clean build debug variant
#   3. Build staging variant
#   4. Build release variant
#   5. Run instrumented tests on connected device (debug variant)
#   6. Verify APK outputs exist and report sizes
#
# Prerequisites:
#   - ADB device connected (USB debugging enabled)
#   - ANDROID_HOME set correctly
#   - Run from the project root directory
#
# =============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# Colors for output
# ---------------------------------------------------------------------------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
step=0
pass_count=0
fail_count=0

print_step() {
    step=$((step + 1))
    echo ""
    echo -e "${BLUE}══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  Step ${step}: $1${NC}"
    echo -e "${BLUE}══════════════════════════════════════════════════════════════${NC}"
}

pass() {
    pass_count=$((pass_count + 1))
    echo -e "${GREEN}  ✅ PASS: $1${NC}"
}

fail() {
    fail_count=$((fail_count + 1))
    echo -e "${RED}  ❌ FAIL: $1${NC}"
}

warn() {
    echo -e "${YELLOW}  ⚠️  $1${NC}"
}

# ---------------------------------------------------------------------------
# Parse version argument
# ---------------------------------------------------------------------------
VERSION="${1:-}"
if [ -z "$VERSION" ]; then
    echo -e "${YELLOW}Usage: $0 <version>${NC}"
    echo -e "${YELLOW}Example: $0 v1.2.0${NC}"
    exit 1
fi

echo ""
echo -e "${BLUE}══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}           Release Checklist for ${VERSION}                   ${NC}"
echo -e "${BLUE}══════════════════════════════════════════════════════════════${NC}"
echo ""
echo "  Started at: $(date)"

# ---------------------------------------------------------------------------
# Step 1: Verify ADB device
# ---------------------------------------------------------------------------
print_step "Verify ADB device is connected"

if adb devices 2>/dev/null | grep -q "device$"; then
    DEVICE_SDK=$(adb shell getprop ro.build.version.sdk 2>/dev/null || echo "unknown")
    pass "Device connected (API ${DEVICE_SDK})"
else
    fail "No ADB device connected"
    echo ""
    echo "  Please connect a device with USB debugging enabled and try again."
    exit 1
fi

# ---------------------------------------------------------------------------
# Step 2: Clean build — Debug variant
# ---------------------------------------------------------------------------
print_step "Clean build — Debug variant"

if ./gradlew clean assembleDebug 2>&1 | tail -5; then
    DEBUG_APK=$(find app/build/outputs/apk/debug -name "*.apk" -type f 2>/dev/null | head -1)
    if [ -n "$DEBUG_APK" ] && [ -f "$DEBUG_APK" ]; then
        DEBUG_SIZE=$(du -h "$DEBUG_APK" | cut -f1)
        pass "Debug APK built: $(basename "$DEBUG_APK") (${DEBUG_SIZE})"
    else
        fail "Debug APK not found after build"
    fi
else
    fail "Debug build failed"
fi

# ---------------------------------------------------------------------------
# Step 3: Build — Staging variant
# ---------------------------------------------------------------------------
print_step "Build — Staging variant"

if ./gradlew assembleStaging 2>&1 | tail -5; then
    STAGING_APK=$(find app/build/outputs/apk/staging -name "*.apk" -type f 2>/dev/null | head -1)
    if [ -n "$STAGING_APK" ] && [ -f "$STAGING_APK" ]; then
        STAGING_SIZE=$(du -h "$STAGING_APK" | cut -f1)
        pass "Staging APK built: $(basename "$STAGING_APK") (${STAGING_SIZE})"
    else
        fail "Staging APK not found after build"
    fi
else
    fail "Staging build failed"
fi

# ---------------------------------------------------------------------------
# Step 4: Build — Release variant
# ---------------------------------------------------------------------------
print_step "Build — Release variant"

if ./gradlew assembleRelease 2>&1 | tail -5; then
    RELEASE_APK=$(find app/build/outputs/apk/release -name "*.apk" -type f 2>/dev/null | head -1)
    if [ -n "$RELEASE_APK" ] && [ -f "$RELEASE_APK" ]; then
        RELEASE_SIZE=$(du -h "$RELEASE_APK" | cut -f1)
        pass "Release APK built: $(basename "$RELEASE_APK") (${RELEASE_SIZE})"
    else
        fail "Release APK not found after build"
    fi
else
    fail "Release build failed"
fi

# ---------------------------------------------------------------------------
# Step 5: Instrumented tests — Debug variant
# ---------------------------------------------------------------------------
print_step "Instrumented tests — Debug variant"

echo "  Running connectedDebugAndroidTest..."
TEST_OUTPUT=$(./gradlew connectedDebugAndroidTest 2>&1) || true
TEST_EXIT=${PIPESTATUS[0]:-$?}

if [ $TEST_EXIT -eq 0 ]; then
    # Extract test counts from output (strip device name for privacy)
    TEST_COUNTS=$(echo "$TEST_OUTPUT" | grep -oE "Tests [0-9]+/[0-9]+ completed\. \([0-9]+ skipped\) \([0-9]+ failed\)" | tail -1)
    FINISHED_COUNTS=$(echo "$TEST_OUTPUT" | grep -oE "Finished [0-9]+ tests" | tail -1)
    if [ -n "$TEST_COUNTS" ]; then
        pass "Instrumented tests passed: ${TEST_COUNTS}"
    else
        pass "Instrumented tests passed"
    fi
    if [ -n "$FINISHED_COUNTS" ]; then
        echo "       ${FINISHED_COUNTS}"
    fi
else
    fail "Instrumented tests failed"
    echo "$TEST_OUTPUT" | grep -E "(FAIL|Error|Exception)" | head -10
fi

# ---------------------------------------------------------------------------
# Step 6: Verify APK file names match version
# ---------------------------------------------------------------------------
print_step "Verify APK outputs"

for variant in debug staging release; do
    LABEL=$(echo "$variant" | awk '{print toupper(substr($0,1,1)) substr($0,2)}')
    echo "  ${LABEL} APKs:"
    find "app/build/outputs/apk/${variant}" -name "*.apk" -type f 2>/dev/null | while read -r apk; do
        SIZE=$(du -h "$apk" | cut -f1)
        echo "    📦 $(basename "$apk") (${SIZE})"
    done
done

# Check version string appears in APK names
if find app/build/outputs/apk -name "*.apk" -type f 2>/dev/null | grep -q "${VERSION}"; then
    pass "APK file names contain version string '${VERSION}'"
else
    warn "APK file names do not contain version string '${VERSION}' — check versionName in build.gradle.kts"
fi

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo ""
echo -e "${BLUE}══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}                    Release Checklist Summary                ${NC}"
echo -e "${BLUE}══════════════════════════════════════════════════════════════${NC}"
echo ""
echo "  Version:  ${VERSION}"
echo "  Device:   API ${DEVICE_SDK:-unknown}"
echo "  Finished: $(date)"
echo ""
echo -e "  ${GREEN}Passed: ${pass_count}${NC}"
echo -e "  ${RED}Failed: ${fail_count}${NC}"
echo ""

if [ "$fail_count" -eq 0 ]; then
    echo -e "${GREEN}  ✅ All checks passed! Ready to release ${VERSION}.${NC}"
    echo ""
    echo "  Next steps:"
    echo "    1. git tag ${VERSION}"
    echo "    2. git push origin ${VERSION}"
    echo "    3. gh release create ${VERSION} <apk-path> --title \"${VERSION}\" --notes \"...\""
    echo ""
    exit 0
else
    echo -e "${RED}  ❌ ${fail_count} check(s) failed. Please fix issues before releasing.${NC}"
    echo ""
    exit 1
fi