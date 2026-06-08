/*
 * nativeplanet-vere-launch.c
 *
 * Native launcher for vere on Android.
 * Reads BootPackage configuration and execs vere with appropriate arguments.
 *
 * NativePlanet Runtime Base v1 -> Satellite v0 transition
 *
 * SECURITY NOTES:
 * - Strict BootPackage validation, fail closed on any error
 * - Path allowlist enforced
 * - No shell commands, execv only
 * - No secret logging
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/stat.h>
#include <time.h>
#include <errno.h>
#include <fcntl.h>
#include <ctype.h>

#define VERE_PATH           "/system_ext/bin/vere"
#define BOOTPACKAGE_PATH    "/data/nativeplanet/boot-package.json"
#define LOG_PATH            "/data/nativeplanet/logs/nativeplanet-vere-launch.log"
#define LOG_DIR             "/data/nativeplanet/logs"

#define PILL_PATH_PREFIX    "/system_ext/etc/nativeplanet/"
#define PIER_PATH_PREFIX    "/data/nativeplanet/ships/"
#define KEY_PATH_PREFIX     "/data/nativeplanet/keys/"

#define MAX_JSON_SIZE       8192
#define MAX_PATH_LEN        512
#define MAX_SHIP_LEN        64

#define BOOT_MODE_FAKE_TEST 1
#define BOOT_MODE_MOON      2
#define BOOT_MODE_UNKNOWN   0

#define SUPPORTED_PACKAGE_VERSION 1

typedef struct {
    char ship[MAX_SHIP_LEN];
    char parent[MAX_SHIP_LEN];
    char pillPath[MAX_PATH_LEN];
    char pierPath[MAX_PATH_LEN];
    char keyMaterialRef[128];
    char keyFilePath[MAX_PATH_LEN];
    int bootMode;
    int packageVersion;
    int valid;
} BootPackage;

static FILE *log_file = NULL;

static void log_open(void) {
    struct stat st;
    if (stat(LOG_DIR, &st) != 0) {
        mkdir(LOG_DIR, 0700);
    }
    log_file = fopen(LOG_PATH, "a");
}

static void log_close(void) {
    if (log_file) {
        fclose(log_file);
        log_file = NULL;
    }
}

static void log_msg(const char *level, const char *msg) {
    if (!log_file) return;

    time_t now = time(NULL);
    struct tm *tm = localtime(&now);
    char timebuf[64];
    strftime(timebuf, sizeof(timebuf), "%Y-%m-%d %H:%M:%S", tm);
    fprintf(log_file, "[%s] [%s] %s\n", timebuf, level, msg);
    fflush(log_file);
}

static void log_info(const char *msg) { log_msg("INFO", msg); }
static void log_error(const char *msg) { log_msg("ERROR", msg); }

static void log_info_fmt(const char *fmt, const char *arg) {
    char buf[1024];
    snprintf(buf, sizeof(buf), fmt, arg);
    log_info(buf);
}

static void log_error_fmt(const char *fmt, const char *arg) {
    char buf[1024];
    snprintf(buf, sizeof(buf), fmt, arg);
    log_error(buf);
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
    if (!path || strlen(path) == 0) {
        return 0;
    }

    if (strstr(path, "..") != NULL) {
        return 0;
    }

    for (const char *p = path; *p; p++) {
        unsigned char c = (unsigned char)*p;
        if (c < 0x20 || c == 0x7f) {
            return 0;
        }
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

static int validate_key_file_ref(const char *ref, char *out_path, size_t out_size) {
    if (strncmp(ref, "file:", 5) != 0) {
        log_error("BootPackage invalid: keyMaterialRef must start with 'file:'");
        return 0;
    }

    const char *path = ref + 5;

    if (!validate_path_safe(path)) {
        log_error("BootPackage invalid: key file path contains invalid characters");
        return 0;
    }

    if (strncmp(path, KEY_PATH_PREFIX, strlen(KEY_PATH_PREFIX)) != 0) {
        log_error_fmt("BootPackage invalid: key file must be in %s", KEY_PATH_PREFIX);
        return 0;
    }

    if (strlen(path) >= out_size) {
        log_error("BootPackage invalid: key file path too long");
        return 0;
    }

    strncpy(out_path, path, out_size - 1);
    out_path[out_size - 1] = '\0';

    return 1;
}

static int validate_key_file_exists(const char *path) {
    struct stat st;
    if (stat(path, &st) != 0) {
        log_error("Key file not found");
        return 0;
    }
    if (!S_ISREG(st.st_mode)) {
        log_error("Key file is not a regular file");
        return 0;
    }
    if (access(path, R_OK) != 0) {
        log_error("Key file not readable");
        return 0;
    }
    return 1;
}

static int looks_like_secret(const char *value) {
    size_t len = strlen(value);

    if (len > 50 && strspn(value, "0123456789abcdefABCDEF") == len) {
        return 1;
    }

    if (strstr(value, "~") != NULL && len > 20) {
        return 1;
    }

    if (strncmp(value, "0x", 2) == 0 || strncmp(value, "0X", 2) == 0) {
        if (len > 20) return 1;
    }

    return 0;
}

static int validate_ship_name(const char *ship) {
    if (!ship || strlen(ship) == 0) {
        return 0;
    }

    if (strlen(ship) > 60) {
        return 0;
    }

    for (const char *p = ship; *p; p++) {
        char c = *p;
        if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '~')) {
            return 0;
        }
    }

    return 1;
}

static int parse_bootpackage(const char *json, BootPackage *pkg) {
    memset(pkg, 0, sizeof(*pkg));
    pkg->valid = 0;

    const char *p;

    p = find_key(json, "packageVersion");
    if (!p) {
        log_error("BootPackage invalid: missing 'packageVersion'");
        return -1;
    }
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
    if (!p) {
        log_error("BootPackage invalid: missing 'ship'");
        return -1;
    }
    if (!parse_string_value(p, pkg->ship, sizeof(pkg->ship))) {
        log_error("BootPackage invalid: 'ship' is not a valid string");
        return -1;
    }
    if (!validate_ship_name(pkg->ship)) {
        log_error("BootPackage invalid: 'ship' contains invalid characters");
        return -1;
    }

    p = find_key(json, "pillPath");
    if (!p) {
        log_error("BootPackage invalid: missing 'pillPath'");
        return -1;
    }
    if (!parse_string_value(p, pkg->pillPath, sizeof(pkg->pillPath))) {
        log_error("BootPackage invalid: 'pillPath' is not a valid string");
        return -1;
    }
    if (!validate_pill_path(pkg->pillPath)) {
        return -1;
    }

    p = find_key(json, "pierPath");
    if (!p) {
        log_error("BootPackage invalid: missing 'pierPath'");
        return -1;
    }
    if (!parse_string_value(p, pkg->pierPath, sizeof(pkg->pierPath))) {
        log_error("BootPackage invalid: 'pierPath' is not a valid string");
        return -1;
    }
    if (!validate_pier_path(pkg->pierPath)) {
        return -1;
    }

    p = find_key(json, "bootMode");
    if (!p) {
        log_error("BootPackage invalid: missing 'bootMode'");
        return -1;
    }
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

    p = find_key(json, "keyMaterialRef");
    if (!p) {
        log_error("BootPackage invalid: missing 'keyMaterialRef'");
        return -1;
    }
    if (!parse_string_value(p, pkg->keyMaterialRef, sizeof(pkg->keyMaterialRef))) {
        log_error("BootPackage invalid: 'keyMaterialRef' is not a valid string");
        return -1;
    }

    if (looks_like_secret(pkg->keyMaterialRef)) {
        log_error("BootPackage invalid: keyMaterialRef appears to contain raw key material");
        return -1;
    }

    if (pkg->bootMode == BOOT_MODE_FAKE_TEST) {
        if (strcmp(pkg->keyMaterialRef, "none") != 0) {
            log_error("BootPackage invalid: FAKE_TEST mode requires keyMaterialRef='none'");
            return -1;
        }
    }

    if (pkg->bootMode == BOOT_MODE_MOON) {
        p = find_key(json, "parent");
        if (!p) {
            log_error("BootPackage invalid: MOON mode requires 'parent' field");
            return -1;
        }
        if (!parse_string_value(p, pkg->parent, sizeof(pkg->parent))) {
            log_error("BootPackage invalid: 'parent' is not a valid string");
            return -1;
        }
        if (!validate_ship_name(pkg->parent)) {
            log_error("BootPackage invalid: 'parent' contains invalid characters");
            return -1;
        }

        if (!validate_key_file_ref(pkg->keyMaterialRef, pkg->keyFilePath, sizeof(pkg->keyFilePath))) {
            return -1;
        }
    }

    pkg->valid = 1;
    return 0;
}

static int read_bootpackage(BootPackage *pkg) {
    struct stat st;
    if (stat(BOOTPACKAGE_PATH, &st) != 0) {
        log_error_fmt("BootPackage not found: %s", BOOTPACKAGE_PATH);
        log_error("Create a valid boot-package.json before starting the service.");
        return -1;
    }

    if (st.st_size > MAX_JSON_SIZE) {
        log_error("BootPackage invalid: file too large");
        return -1;
    }

    FILE *f = fopen(BOOTPACKAGE_PATH, "r");
    if (!f) {
        log_error_fmt("Cannot open BootPackage: %s", strerror(errno));
        return -1;
    }

    char *json = malloc(st.st_size + 1);
    if (!json) {
        fclose(f);
        log_error("Memory allocation failed");
        return -1;
    }

    size_t read_size = fread(json, 1, st.st_size, f);
    fclose(f);
    json[read_size] = '\0';

    int result = parse_bootpackage(json, pkg);
    free(json);

    return result;
}

static int validate_pill(const char *pill_path) {
    struct stat st;
    if (stat(pill_path, &st) != 0) {
        log_error_fmt("Pill not found: %s", pill_path);
        return -1;
    }
    if (!S_ISREG(st.st_mode)) {
        log_error_fmt("Pill is not a regular file: %s", pill_path);
        return -1;
    }
    if (access(pill_path, R_OK) != 0) {
        log_error_fmt("Pill not readable: %s", pill_path);
        return -1;
    }
    return 0;
}

static int pier_exists(const char *pier_path) {
    char urb_path[MAX_PATH_LEN + 8];
    snprintf(urb_path, sizeof(urb_path), "%s/.urb", pier_path);

    struct stat st;
    return (stat(urb_path, &st) == 0 && S_ISDIR(st.st_mode));
}

static int ensure_parent_dirs(const char *path) {
    char *tmp = strdup(path);
    if (!tmp) return -1;

    char *p = tmp;
    while (*p == '/') p++;

    while ((p = strchr(p, '/')) != NULL) {
        *p = '\0';
        if (strlen(tmp) > 0) {
            struct stat st;
            if (stat(tmp, &st) != 0) {
                if (mkdir(tmp, 0700) != 0 && errno != EEXIST) {
                    log_error_fmt("Failed to create directory: %s", tmp);
                    free(tmp);
                    return -1;
                }
            }
        }
        *p = '/';
        p++;
    }

    free(tmp);
    return 0;
}

static void unlink_if_exists(const char *path) {
    if (unlink(path) == 0) {
        log_info_fmt("Removed stale runtime file: %s", path);
        return;
    }

    if (errno != ENOENT) {
        log_error_fmt("Failed to remove stale runtime file: %s", path);
    }
}

static void cleanup_stale_runtime_files(const char *pier_path) {
    char lock_path[MAX_PATH_LEN + 16];
    char sock_path[MAX_PATH_LEN + 32];

    snprintf(lock_path, sizeof(lock_path), "%s/.vere.lock", pier_path);
    snprintf(sock_path, sizeof(sock_path), "%s/.urb/conn.sock", pier_path);

    unlink_if_exists(lock_path);
    unlink_if_exists(sock_path);
}

static void exec_vere_new_pier(const BootPackage *pkg) {
    log_info_fmt("Creating new pier: %s", pkg->pierPath);
    log_info_fmt("Using pill: %s", pkg->pillPath);
    log_info_fmt("Ship: %s", pkg->ship);
    log_info("keyMaterialRef: [redacted]");

    char *args[] = {
        VERE_PATH,
        "-t",
        "-F", (char *)pkg->ship,
        "--no-dock",
        "-B", (char *)pkg->pillPath,
        "-c", (char *)pkg->pierPath,
        NULL
    };

    log_info("Executing vere (FAKE_TEST, new pier, foreground)");
    execv(VERE_PATH, args);

    log_error_fmt("execv failed: %s", strerror(errno));
}

static void exec_vere_existing_pier(const BootPackage *pkg) {
    log_info_fmt("Booting existing pier: %s", pkg->pierPath);
    log_info("keyMaterialRef: [redacted]");
    cleanup_stale_runtime_files(pkg->pierPath);

    char *args[] = {
        VERE_PATH,
        "-t",
        "--no-dock",
        (char *)pkg->pierPath,
        NULL
    };

    log_info("Executing vere (existing pier, foreground)");
    execv(VERE_PATH, args);

    log_error_fmt("execv failed: %s", strerror(errno));
}

static void exec_vere_new_pier_moon(const BootPackage *pkg) {
    log_info_fmt("Creating new moon pier: %s", pkg->pierPath);
    log_info_fmt("Using pill: %s", pkg->pillPath);
    log_info_fmt("Moon: %s", pkg->ship);
    log_info_fmt("Parent: %s", pkg->parent);
    log_info("keyMaterialRef: [redacted]");

    char *args[] = {
        VERE_PATH,
        "-t",
        "-w", (char *)pkg->ship,
        "-k", (char *)pkg->keyFilePath,
        "--no-dock",
        "-B", (char *)pkg->pillPath,
        "-c", (char *)pkg->pierPath,
        NULL
    };

    log_info("Executing vere (MOON, new pier, foreground)");
    execv(VERE_PATH, args);

    log_error_fmt("execv failed: %s", strerror(errno));
}

int main(int argc, char *argv[]) {
    (void)argc;
    (void)argv;

    log_open();
    log_info("=== nativeplanet-vere-launch v0 starting ===");

    BootPackage pkg;
    if (read_bootpackage(&pkg) != 0) {
        log_error("BootPackage validation failed - service will not start");
        log_close();
        return 1;
    }

    log_info_fmt("BootPackage loaded: ship=%s", pkg.ship);
    log_info_fmt("  pierPath=%s", pkg.pierPath);
    log_info_fmt("  pillPath=%s", pkg.pillPath);
    if (pkg.bootMode == BOOT_MODE_FAKE_TEST) {
        log_info("  bootMode=FAKE_TEST");
    } else if (pkg.bootMode == BOOT_MODE_MOON) {
        log_info("  bootMode=MOON");
        log_info_fmt("  parent=%s", pkg.parent);
    }
    log_info("  keyMaterialRef=[redacted]");

    if (pkg.bootMode == BOOT_MODE_MOON) {
        if (!validate_key_file_exists(pkg.keyFilePath)) {
            log_error("Key file validation failed - service will not start");
            log_close();
            return 1;
        }
    }

    if (pier_exists(pkg.pierPath)) {
        log_info("Existing pier detected");
        exec_vere_existing_pier(&pkg);
    } else {
        if (validate_pill(pkg.pillPath) != 0) {
            log_error("Pill validation failed - service will not start");
            log_close();
            return 1;
        }

        if (ensure_parent_dirs(pkg.pierPath) != 0) {
            log_error("Failed to create pier parent directories");
            log_close();
            return 1;
        }

        if (pkg.bootMode == BOOT_MODE_MOON) {
            exec_vere_new_pier_moon(&pkg);
        } else {
            exec_vere_new_pier(&pkg);
        }
    }

    log_error("vere exec failed - service terminating");
    log_close();
    return 1;
}
