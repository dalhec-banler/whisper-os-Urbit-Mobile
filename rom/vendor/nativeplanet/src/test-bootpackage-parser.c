/*
 * test-bootpackage-parser.c
 *
 * Host-side test harness for BootPackage parsing.
 * Compile with: gcc -o test-bootpackage-parser test-bootpackage-parser.c -DTEST_MODE
 *
 * Run: ./test-bootpackage-parser
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <ctype.h>
#include <sys/stat.h>
#include <errno.h>

#define TEST_MODE 1

#define MAX_JSON_SIZE       8192
#define MAX_PATH_LEN        512
#define MAX_SHIP_LEN        64

#define PILL_PATH_PREFIX    "/system_ext/etc/nativeplanet/"
#define PIER_PATH_PREFIX    "/data/nativeplanet/ships/"

#define BOOT_MODE_FAKE_TEST 1
#define BOOT_MODE_MOON      2
#define BOOT_MODE_UNKNOWN   0

#define SUPPORTED_PACKAGE_VERSION 1

typedef struct {
    char ship[MAX_SHIP_LEN];
    char pillPath[MAX_PATH_LEN];
    char pierPath[MAX_PATH_LEN];
    char keyMaterialRef[128];
    int bootMode;
    int packageVersion;
    int valid;
} BootPackage;

static char last_error[512] = {0};

static void log_error(const char *msg) {
    strncpy(last_error, msg, sizeof(last_error) - 1);
    last_error[sizeof(last_error) - 1] = '\0';
}

static void log_error_fmt(const char *fmt, const char *arg) {
    snprintf(last_error, sizeof(last_error), fmt, arg);
}

static const char *skip_whitespace(const char *p) {
    while (*p && isspace((unsigned char)*p)) p++;
    return p;
}

static const char *parse_string_value(const char *p, char *out, size_t out_size) {
    p = skip_whitespace(p);
    if (*p != '"') return NULL;
    p++;

    size_t i = 0;
    while (*p && *p != '"' && i < out_size - 1) {
        if (*p == '\\' && *(p+1)) {
            p++;
            switch (*p) {
                case 'n': out[i++] = '\n'; break;
                case 't': out[i++] = '\t'; break;
                case '\\': out[i++] = '\\'; break;
                case '"': out[i++] = '"'; break;
                default: out[i++] = *p; break;
            }
        } else {
            out[i++] = *p;
        }
        p++;
    }
    out[i] = '\0';

    if (*p != '"') return NULL;
    return p + 1;
}

static const char *parse_int_value(const char *p, int *out) {
    p = skip_whitespace(p);

    char *end;
    long val = strtol(p, &end, 10);
    if (end == p) return NULL;
    *out = (int)val;
    return end;
}

static const char *find_key(const char *json, const char *key) {
    char search_key[128];
    snprintf(search_key, sizeof(search_key), "\"%s\"", key);

    const char *p = strstr(json, search_key);
    if (!p) return NULL;

    p += strlen(search_key);
    p = skip_whitespace(p);
    if (*p != ':') return NULL;
    return p + 1;
}

static int parse_boot_mode(const char *mode_str) {
    if (strcmp(mode_str, "FAKE_TEST") == 0) return BOOT_MODE_FAKE_TEST;
    if (strcmp(mode_str, "MOON") == 0) return BOOT_MODE_MOON;
    return BOOT_MODE_UNKNOWN;
}

static int validate_path_safe(const char *path) {
    if (!path || strlen(path) == 0) return 0;
    if (strstr(path, "..") != NULL) return 0;
    for (const char *p = path; *p; p++) {
        unsigned char c = (unsigned char)*p;
        if (c < 0x20 || c == 0x7f) return 0;
    }
    return 1;
}

static int validate_pill_path(const char *path) {
    if (!validate_path_safe(path)) {
        log_error("BootPackage invalid: pillPath contains invalid characters");
        return 0;
    }
    if (strncmp(path, PILL_PATH_PREFIX, strlen(PILL_PATH_PREFIX)) != 0) {
        log_error_fmt("BootPackage invalid: pillPath must start with %s", PILL_PATH_PREFIX);
        return 0;
    }
    return 1;
}

static int validate_pier_path(const char *path) {
    if (!validate_path_safe(path)) {
        log_error("BootPackage invalid: pierPath contains invalid characters");
        return 0;
    }
    if (strncmp(path, PIER_PATH_PREFIX, strlen(PIER_PATH_PREFIX)) != 0) {
        log_error_fmt("BootPackage invalid: pierPath must start with %s", PIER_PATH_PREFIX);
        return 0;
    }
    return 1;
}

static int looks_like_secret(const char *value) {
    size_t len = strlen(value);
    if (len > 50 && strspn(value, "0123456789abcdefABCDEF") == len) return 1;
    if (strstr(value, "~") != NULL && len > 20) return 1;
    if ((strncmp(value, "0x", 2) == 0 || strncmp(value, "0X", 2) == 0) && len > 20) return 1;
    return 0;
}

static int validate_ship_name(const char *ship) {
    if (!ship || strlen(ship) == 0) return 0;
    if (strlen(ship) > 60) return 0;
    for (const char *p = ship; *p; p++) {
        char c = *p;
        if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '~')) return 0;
    }
    return 1;
}

static int parse_bootpackage(const char *json, BootPackage *pkg) {
    memset(pkg, 0, sizeof(*pkg));
    pkg->valid = 0;
    last_error[0] = '\0';

    const char *p;

    p = find_key(json, "packageVersion");
    if (!p) { log_error("BootPackage invalid: missing 'packageVersion'"); return -1; }
    if (!parse_int_value(p, &pkg->packageVersion)) {
        log_error("BootPackage invalid: 'packageVersion' is not a valid integer");
        return -1;
    }
    if (pkg->packageVersion != SUPPORTED_PACKAGE_VERSION) {
        char buf[128];
        snprintf(buf, sizeof(buf), "BootPackage invalid: version %d not supported (expected %d)",
                 pkg->packageVersion, SUPPORTED_PACKAGE_VERSION);
        log_error(buf);
        return -1;
    }

    p = find_key(json, "ship");
    if (!p) { log_error("BootPackage invalid: missing 'ship'"); return -1; }
    if (!parse_string_value(p, pkg->ship, sizeof(pkg->ship))) {
        log_error("BootPackage invalid: 'ship' is not a valid string");
        return -1;
    }
    if (!validate_ship_name(pkg->ship)) {
        log_error("BootPackage invalid: 'ship' contains invalid characters");
        return -1;
    }

    p = find_key(json, "pillPath");
    if (!p) { log_error("BootPackage invalid: missing 'pillPath'"); return -1; }
    if (!parse_string_value(p, pkg->pillPath, sizeof(pkg->pillPath))) {
        log_error("BootPackage invalid: 'pillPath' is not a valid string");
        return -1;
    }
    if (!validate_pill_path(pkg->pillPath)) return -1;

    p = find_key(json, "pierPath");
    if (!p) { log_error("BootPackage invalid: missing 'pierPath'"); return -1; }
    if (!parse_string_value(p, pkg->pierPath, sizeof(pkg->pierPath))) {
        log_error("BootPackage invalid: 'pierPath' is not a valid string");
        return -1;
    }
    if (!validate_pier_path(pkg->pierPath)) return -1;

    p = find_key(json, "bootMode");
    if (!p) { log_error("BootPackage invalid: missing 'bootMode'"); return -1; }
    char mode_str[32];
    if (!parse_string_value(p, mode_str, sizeof(mode_str))) {
        log_error("BootPackage invalid: 'bootMode' is not a valid string");
        return -1;
    }
    pkg->bootMode = parse_boot_mode(mode_str);
    if (pkg->bootMode == BOOT_MODE_UNKNOWN) {
        log_error_fmt("BootPackage invalid: unknown bootMode '%s'", mode_str);
        return -1;
    }
    if (pkg->bootMode == BOOT_MODE_MOON) {
        log_error("BootPackage invalid: bootMode 'MOON' not supported in v0");
        return -1;
    }

    p = find_key(json, "keyMaterialRef");
    if (!p) { log_error("BootPackage invalid: missing 'keyMaterialRef'"); return -1; }
    if (!parse_string_value(p, pkg->keyMaterialRef, sizeof(pkg->keyMaterialRef))) {
        log_error("BootPackage invalid: 'keyMaterialRef' is not a valid string");
        return -1;
    }
    if (looks_like_secret(pkg->keyMaterialRef)) {
        log_error("BootPackage invalid: keyMaterialRef appears to contain raw key material");
        return -1;
    }
    if (pkg->bootMode == BOOT_MODE_FAKE_TEST && strcmp(pkg->keyMaterialRef, "none") != 0) {
        log_error("BootPackage invalid: FAKE_TEST mode requires keyMaterialRef='none'");
        return -1;
    }

    pkg->valid = 1;
    return 0;
}

/* Test infrastructure */
static int tests_run = 0;
static int tests_passed = 0;

#define TEST(name) static void test_##name(void)
#define RUN_TEST(name) do { \
    printf("  %s... ", #name); \
    tests_run++; \
    test_##name(); \
    tests_passed++; \
    printf("PASS\n"); \
} while(0)

#define ASSERT_PARSE_OK(json) do { \
    BootPackage pkg; \
    int result = parse_bootpackage(json, &pkg); \
    if (result != 0) { \
        printf("FAIL\n    Expected success, got error: %s\n", last_error); \
        exit(1); \
    } \
} while(0)

#define ASSERT_PARSE_FAIL(json, expected_substr) do { \
    BootPackage pkg; \
    int result = parse_bootpackage(json, &pkg); \
    if (result == 0) { \
        printf("FAIL\n    Expected failure, but parsing succeeded\n"); \
        exit(1); \
    } \
    if (strstr(last_error, expected_substr) == NULL) { \
        printf("FAIL\n    Expected error containing '%s', got: %s\n", expected_substr, last_error); \
        exit(1); \
    } \
} while(0)

/* Valid BootPackage */
static const char *VALID_BOOTPACKAGE =
    "{\n"
    "  \"ship\": \"zod\",\n"
    "  \"parent\": null,\n"
    "  \"pillPath\": \"/system_ext/etc/nativeplanet/satellite.pill\",\n"
    "  \"pierPath\": \"/data/nativeplanet/ships/zod\",\n"
    "  \"bootMode\": \"FAKE_TEST\",\n"
    "  \"keyMaterialRef\": \"none\",\n"
    "  \"networkConfig\": {},\n"
    "  \"delegationConfig\": {},\n"
    "  \"createdAtMs\": 0,\n"
    "  \"packageVersion\": 1\n"
    "}";

TEST(valid_bootpackage) {
    ASSERT_PARSE_OK(VALID_BOOTPACKAGE);
}

TEST(missing_ship) {
    const char *json =
        "{\n"
        "  \"pillPath\": \"/system_ext/etc/nativeplanet/satellite.pill\",\n"
        "  \"pierPath\": \"/data/nativeplanet/ships/zod\",\n"
        "  \"bootMode\": \"FAKE_TEST\",\n"
        "  \"keyMaterialRef\": \"none\",\n"
        "  \"packageVersion\": 1\n"
        "}";
    ASSERT_PARSE_FAIL(json, "missing 'ship'");
}

TEST(missing_pillPath) {
    const char *json =
        "{\n"
        "  \"ship\": \"zod\",\n"
        "  \"pierPath\": \"/data/nativeplanet/ships/zod\",\n"
        "  \"bootMode\": \"FAKE_TEST\",\n"
        "  \"keyMaterialRef\": \"none\",\n"
        "  \"packageVersion\": 1\n"
        "}";
    ASSERT_PARSE_FAIL(json, "missing 'pillPath'");
}

TEST(missing_pierPath) {
    const char *json =
        "{\n"
        "  \"ship\": \"zod\",\n"
        "  \"pillPath\": \"/system_ext/etc/nativeplanet/satellite.pill\",\n"
        "  \"bootMode\": \"FAKE_TEST\",\n"
        "  \"keyMaterialRef\": \"none\",\n"
        "  \"packageVersion\": 1\n"
        "}";
    ASSERT_PARSE_FAIL(json, "missing 'pierPath'");
}

TEST(missing_bootMode) {
    const char *json =
        "{\n"
        "  \"ship\": \"zod\",\n"
        "  \"pillPath\": \"/system_ext/etc/nativeplanet/satellite.pill\",\n"
        "  \"pierPath\": \"/data/nativeplanet/ships/zod\",\n"
        "  \"keyMaterialRef\": \"none\",\n"
        "  \"packageVersion\": 1\n"
        "}";
    ASSERT_PARSE_FAIL(json, "missing 'bootMode'");
}

TEST(missing_keyMaterialRef) {
    const char *json =
        "{\n"
        "  \"ship\": \"zod\",\n"
        "  \"pillPath\": \"/system_ext/etc/nativeplanet/satellite.pill\",\n"
        "  \"pierPath\": \"/data/nativeplanet/ships/zod\",\n"
        "  \"bootMode\": \"FAKE_TEST\",\n"
        "  \"packageVersion\": 1\n"
        "}";
    ASSERT_PARSE_FAIL(json, "missing 'keyMaterialRef'");
}

TEST(missing_packageVersion) {
    const char *json =
        "{\n"
        "  \"ship\": \"zod\",\n"
        "  \"pillPath\": \"/system_ext/etc/nativeplanet/satellite.pill\",\n"
        "  \"pierPath\": \"/data/nativeplanet/ships/zod\",\n"
        "  \"bootMode\": \"FAKE_TEST\",\n"
        "  \"keyMaterialRef\": \"none\"\n"
        "}";
    ASSERT_PARSE_FAIL(json, "missing 'packageVersion'");
}

TEST(unsupported_bootMode_MOON) {
    const char *json =
        "{\n"
        "  \"ship\": \"zod\",\n"
        "  \"pillPath\": \"/system_ext/etc/nativeplanet/satellite.pill\",\n"
        "  \"pierPath\": \"/data/nativeplanet/ships/zod\",\n"
        "  \"bootMode\": \"MOON\",\n"
        "  \"keyMaterialRef\": \"none\",\n"
        "  \"packageVersion\": 1\n"
        "}";
    ASSERT_PARSE_FAIL(json, "MOON' not supported");
}

TEST(unknown_bootMode) {
    const char *json =
        "{\n"
        "  \"ship\": \"zod\",\n"
        "  \"pillPath\": \"/system_ext/etc/nativeplanet/satellite.pill\",\n"
        "  \"pierPath\": \"/data/nativeplanet/ships/zod\",\n"
        "  \"bootMode\": \"INVALID\",\n"
        "  \"keyMaterialRef\": \"none\",\n"
        "  \"packageVersion\": 1\n"
        "}";
    ASSERT_PARSE_FAIL(json, "unknown bootMode");
}

TEST(keyMaterialRef_not_none_in_FAKE_TEST) {
    const char *json =
        "{\n"
        "  \"ship\": \"zod\",\n"
        "  \"pillPath\": \"/system_ext/etc/nativeplanet/satellite.pill\",\n"
        "  \"pierPath\": \"/data/nativeplanet/ships/zod\",\n"
        "  \"bootMode\": \"FAKE_TEST\",\n"
        "  \"keyMaterialRef\": \"keystore:moon-key\",\n"
        "  \"packageVersion\": 1\n"
        "}";
    ASSERT_PARSE_FAIL(json, "FAKE_TEST mode requires keyMaterialRef='none'");
}

TEST(keyMaterialRef_looks_like_secret) {
    const char *json =
        "{\n"
        "  \"ship\": \"zod\",\n"
        "  \"pillPath\": \"/system_ext/etc/nativeplanet/satellite.pill\",\n"
        "  \"pierPath\": \"/data/nativeplanet/ships/zod\",\n"
        "  \"bootMode\": \"FAKE_TEST\",\n"
        "  \"keyMaterialRef\": \"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef\",\n"
        "  \"packageVersion\": 1\n"
        "}";
    ASSERT_PARSE_FAIL(json, "raw key material");
}

TEST(path_traversal_pierPath) {
    const char *json =
        "{\n"
        "  \"ship\": \"zod\",\n"
        "  \"pillPath\": \"/system_ext/etc/nativeplanet/satellite.pill\",\n"
        "  \"pierPath\": \"/data/nativeplanet/ships/../../../tmp/evil\",\n"
        "  \"bootMode\": \"FAKE_TEST\",\n"
        "  \"keyMaterialRef\": \"none\",\n"
        "  \"packageVersion\": 1\n"
        "}";
    ASSERT_PARSE_FAIL(json, "invalid characters");
}

TEST(path_traversal_pillPath) {
    const char *json =
        "{\n"
        "  \"ship\": \"zod\",\n"
        "  \"pillPath\": \"/system_ext/etc/nativeplanet/../../../etc/passwd\",\n"
        "  \"pierPath\": \"/data/nativeplanet/ships/zod\",\n"
        "  \"bootMode\": \"FAKE_TEST\",\n"
        "  \"keyMaterialRef\": \"none\",\n"
        "  \"packageVersion\": 1\n"
        "}";
    ASSERT_PARSE_FAIL(json, "invalid characters");
}

TEST(pillPath_wrong_prefix) {
    const char *json =
        "{\n"
        "  \"ship\": \"zod\",\n"
        "  \"pillPath\": \"/data/local/tmp/evil.pill\",\n"
        "  \"pierPath\": \"/data/nativeplanet/ships/zod\",\n"
        "  \"bootMode\": \"FAKE_TEST\",\n"
        "  \"keyMaterialRef\": \"none\",\n"
        "  \"packageVersion\": 1\n"
        "}";
    ASSERT_PARSE_FAIL(json, "pillPath must start with");
}

TEST(pierPath_wrong_prefix) {
    const char *json =
        "{\n"
        "  \"ship\": \"zod\",\n"
        "  \"pillPath\": \"/system_ext/etc/nativeplanet/satellite.pill\",\n"
        "  \"pierPath\": \"/data/local/tmp/zod\",\n"
        "  \"bootMode\": \"FAKE_TEST\",\n"
        "  \"keyMaterialRef\": \"none\",\n"
        "  \"packageVersion\": 1\n"
        "}";
    ASSERT_PARSE_FAIL(json, "pierPath must start with");
}

TEST(invalid_ship_uppercase) {
    const char *json =
        "{\n"
        "  \"ship\": \"ZOD\",\n"
        "  \"pillPath\": \"/system_ext/etc/nativeplanet/satellite.pill\",\n"
        "  \"pierPath\": \"/data/nativeplanet/ships/zod\",\n"
        "  \"bootMode\": \"FAKE_TEST\",\n"
        "  \"keyMaterialRef\": \"none\",\n"
        "  \"packageVersion\": 1\n"
        "}";
    ASSERT_PARSE_FAIL(json, "ship' contains invalid characters");
}

TEST(invalid_ship_spaces) {
    const char *json =
        "{\n"
        "  \"ship\": \"zod evil\",\n"
        "  \"pillPath\": \"/system_ext/etc/nativeplanet/satellite.pill\",\n"
        "  \"pierPath\": \"/data/nativeplanet/ships/zod\",\n"
        "  \"bootMode\": \"FAKE_TEST\",\n"
        "  \"keyMaterialRef\": \"none\",\n"
        "  \"packageVersion\": 1\n"
        "}";
    ASSERT_PARSE_FAIL(json, "ship' contains invalid characters");
}

TEST(unsupported_packageVersion) {
    const char *json =
        "{\n"
        "  \"ship\": \"zod\",\n"
        "  \"pillPath\": \"/system_ext/etc/nativeplanet/satellite.pill\",\n"
        "  \"pierPath\": \"/data/nativeplanet/ships/zod\",\n"
        "  \"bootMode\": \"FAKE_TEST\",\n"
        "  \"keyMaterialRef\": \"none\",\n"
        "  \"packageVersion\": 99\n"
        "}";
    ASSERT_PARSE_FAIL(json, "not supported");
}

TEST(malformed_json_no_quotes) {
    const char *json = "{ ship: zod }";
    ASSERT_PARSE_FAIL(json, "missing");
}

TEST(empty_json) {
    const char *json = "{}";
    ASSERT_PARSE_FAIL(json, "missing");
}

TEST(valid_moon_ship_name) {
    const char *json =
        "{\n"
        "  \"ship\": \"~sampel-palnet\",\n"
        "  \"pillPath\": \"/system_ext/etc/nativeplanet/satellite.pill\",\n"
        "  \"pierPath\": \"/data/nativeplanet/ships/sampel-palnet\",\n"
        "  \"bootMode\": \"FAKE_TEST\",\n"
        "  \"keyMaterialRef\": \"none\",\n"
        "  \"packageVersion\": 1\n"
        "}";
    ASSERT_PARSE_OK(json);
}

TEST(control_char_in_path) {
    const char *json =
        "{\n"
        "  \"ship\": \"zod\",\n"
        "  \"pillPath\": \"/system_ext/etc/nativeplanet/sat\\tellite.pill\",\n"
        "  \"pierPath\": \"/data/nativeplanet/ships/zod\",\n"
        "  \"bootMode\": \"FAKE_TEST\",\n"
        "  \"keyMaterialRef\": \"none\",\n"
        "  \"packageVersion\": 1\n"
        "}";
    ASSERT_PARSE_FAIL(json, "invalid characters");
}

TEST(single_line_json) {
    const char *json = "{\"ship\":\"zod\",\"pillPath\":\"/system_ext/etc/nativeplanet/satellite.pill\",\"pierPath\":\"/data/nativeplanet/ships/zod\",\"bootMode\":\"FAKE_TEST\",\"keyMaterialRef\":\"none\",\"packageVersion\":1}";
    ASSERT_PARSE_OK(json);
}

TEST(fields_different_order) {
    const char *json =
        "{\n"
        "  \"packageVersion\": 1,\n"
        "  \"keyMaterialRef\": \"none\",\n"
        "  \"bootMode\": \"FAKE_TEST\",\n"
        "  \"pierPath\": \"/data/nativeplanet/ships/zod\",\n"
        "  \"pillPath\": \"/system_ext/etc/nativeplanet/satellite.pill\",\n"
        "  \"ship\": \"zod\"\n"
        "}";
    ASSERT_PARSE_OK(json);
}

TEST(extra_unknown_fields) {
    const char *json =
        "{\n"
        "  \"ship\": \"zod\",\n"
        "  \"pillPath\": \"/system_ext/etc/nativeplanet/satellite.pill\",\n"
        "  \"pierPath\": \"/data/nativeplanet/ships/zod\",\n"
        "  \"bootMode\": \"FAKE_TEST\",\n"
        "  \"keyMaterialRef\": \"none\",\n"
        "  \"packageVersion\": 1,\n"
        "  \"futureField\": \"should be ignored\",\n"
        "  \"anotherFutureField\": 12345,\n"
        "  \"nestedFuture\": { \"a\": 1, \"b\": 2 }\n"
        "}";
    ASSERT_PARSE_OK(json);
}

TEST(trailing_whitespace) {
    const char *json =
        "{\n"
        "  \"ship\": \"zod\",\n"
        "  \"pillPath\": \"/system_ext/etc/nativeplanet/satellite.pill\",\n"
        "  \"pierPath\": \"/data/nativeplanet/ships/zod\",\n"
        "  \"bootMode\": \"FAKE_TEST\",\n"
        "  \"keyMaterialRef\": \"none\",\n"
        "  \"packageVersion\": 1\n"
        "}   \n\n\n";
    ASSERT_PARSE_OK(json);
}

int main(void) {
    printf("BootPackage Parser Tests\n");
    printf("========================\n\n");

    printf("Valid input:\n");
    RUN_TEST(valid_bootpackage);
    RUN_TEST(valid_moon_ship_name);
    RUN_TEST(single_line_json);
    RUN_TEST(fields_different_order);
    RUN_TEST(extra_unknown_fields);
    RUN_TEST(trailing_whitespace);

    printf("\nMissing required fields:\n");
    RUN_TEST(missing_ship);
    RUN_TEST(missing_pillPath);
    RUN_TEST(missing_pierPath);
    RUN_TEST(missing_bootMode);
    RUN_TEST(missing_keyMaterialRef);
    RUN_TEST(missing_packageVersion);

    printf("\nBoot mode validation:\n");
    RUN_TEST(unsupported_bootMode_MOON);
    RUN_TEST(unknown_bootMode);

    printf("\nKey material validation:\n");
    RUN_TEST(keyMaterialRef_not_none_in_FAKE_TEST);
    RUN_TEST(keyMaterialRef_looks_like_secret);

    printf("\nPath security:\n");
    RUN_TEST(path_traversal_pierPath);
    RUN_TEST(path_traversal_pillPath);
    RUN_TEST(pillPath_wrong_prefix);
    RUN_TEST(pierPath_wrong_prefix);
    RUN_TEST(control_char_in_path);

    printf("\nShip name validation:\n");
    RUN_TEST(invalid_ship_uppercase);
    RUN_TEST(invalid_ship_spaces);

    printf("\nVersion validation:\n");
    RUN_TEST(unsupported_packageVersion);

    printf("\nMalformed JSON:\n");
    RUN_TEST(malformed_json_no_quotes);
    RUN_TEST(empty_json);

    printf("\n========================\n");
    printf("Results: %d/%d tests passed\n", tests_passed, tests_run);

    return (tests_passed == tests_run) ? 0 : 1;
}
