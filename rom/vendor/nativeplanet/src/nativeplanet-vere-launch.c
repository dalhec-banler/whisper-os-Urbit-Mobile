/*
 * nativeplanet-vere-launch.c
 *
 * Native launcher for vere on Android.
 * Detects pier state and execs vere with appropriate arguments.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/stat.h>
#include <time.h>
#include <errno.h>

#define VERE_PATH           "/system_ext/bin/vere"
#define PILL_PATH           "/system_ext/etc/nativeplanet/urbit-v4.3.pill"
#define PIER_PATH           "/data/nativeplanet/pier"
#define PIER_URB_PATH       "/data/nativeplanet/pier/.urb"
#define LOG_PATH            "/data/nativeplanet/logs/nativeplanet-vere-launch.log"
#define LOG_DIR             "/data/nativeplanet/logs"

static void log_status(const char *status) {
    struct stat st;
    if (stat(LOG_DIR, &st) != 0) {
        mkdir(LOG_DIR, 0700);
    }

    FILE *f = fopen(LOG_PATH, "a");
    if (f) {
        time_t now = time(NULL);
        struct tm *tm = localtime(&now);
        char timebuf[64];
        strftime(timebuf, sizeof(timebuf), "%Y-%m-%d %H:%M:%S", tm);
        fprintf(f, "[%s] %s\n", timebuf, status);
        fclose(f);
    }
}

static int pier_exists(void) {
    struct stat st;
    return (stat(PIER_URB_PATH, &st) == 0 && S_ISDIR(st.st_mode));
}

int main(int argc, char *argv[]) {
    (void)argc;
    (void)argv;

    if (pier_exists()) {
        log_status("existing pier detected, resuming (foreground)");

        char *args[] = {
            VERE_PATH,
            "-t",              // terminal mode (no fork)
            "--no-dock",
            PIER_PATH,
            NULL
        };

        execv(VERE_PATH, args);
        log_status("execv failed for existing pier boot");
        perror("execv");
        return 1;
    }

    log_status("no pier found, creating new pier with -c (foreground)");

    char *args[] = {
        VERE_PATH,
        "-t",                  // terminal mode (no fork)
        "-F", "zod",
        "--no-dock",
        "-B", PILL_PATH,
        "-c", PIER_PATH,
        NULL
    };

    execv(VERE_PATH, args);
    log_status("execv failed for new pier creation");
    perror("execv");
    return 1;
}
