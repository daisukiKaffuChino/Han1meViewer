#include "chino.h"
#include "kaffu.h"
#include <fcntl.h>
#include <unistd.h>
#include <cstring>
#include <cstdio>

static inline long svc_openat(int dirfd, const char *path, int flags, mode_t mode) {
    register long x0 __asm__("x0") = (long) dirfd;
    register long x1 __asm__("x1") = (long) path;
    register long x2 __asm__("x2") = (long) flags;
    register long x3 __asm__("x3") = (long) mode;
    register long x8 __asm__("x8") = MY_NR_OPENAT;
    __asm__ __volatile__ ("svc #0" : "+r"(x0) : "r"(x1), "r"(x2), "r"(x3), "r"(x8) : "memory");
    return x0;
}

static inline long svc_read(int fd, void *buf, size_t len) {
    register long x0 __asm__("x0") = (long) fd;
    register long x1 __asm__("x1") = (long) buf;
    register long x2 __asm__("x2") = (long) len;
    register long x8 __asm__("x8") = MY_NR_READ;
    __asm__ __volatile__ ("svc #0" : "+r"(x0) : "r"(x1), "r"(x2), "r"(x8) : "memory");
    return x0;
}

static inline long svc_lseek(int fd, off_t offset, int whence) {
    register long x0 __asm__("x0") = (long) fd;
    register long x1 __asm__("x1") = (long) offset;
    register long x2 __asm__("x2") = (long) whence;
    register long x8 __asm__("x8") = MY_NR_LSEEK;
    __asm__ __volatile__ ("svc #0" : "+r"(x0) : "r"(x1), "r"(x2), "r"(x8) : "memory");
    return x0;
}

static inline void svc_close(int fd) {
    register long x0 __asm__("x0") = (long) fd;
    register long x8 __asm__("x8") = MY_NR_CLOSE;
    __asm__ __volatile__ ("svc #0" : "+r"(x0) : "r"(x8) : "memory");
}

static bool svc_read_exact(int fd, void *buf, size_t len) {
    size_t total_read = 0;
    uint8_t *p = (uint8_t *) buf;
    while (total_read < len) {
        long bytes = svc_read(fd, p + total_read, len - total_read);
        if (bytes <= 0) return false;
        total_read += (size_t) bytes;
    }
    return true;
}

void xor_decrypt(uint8_t *data, size_t len) {
    for (size_t i = 0; i < len; i++) data[i] ^= XOR_KEY;
}

#pragma pack(push, 1)
struct EOCD {
    uint32_t signature;
    uint16_t diskNum;
    uint16_t cdStartDisk;
    uint16_t cdRecordOnDisk;
    uint16_t cdTotalRecord;
    uint32_t cdSize;
    uint32_t cdOffset;
    uint16_t commentLen;
};
#pragma pack(pop)

bool find_eocd(int fd, EOCD &eocd_out) {
    off_t file_size = svc_lseek(fd, 0, SEEK_END);
    if (file_size < 22) return false;

    size_t max_search = 65535 + 22;
    size_t search_range = (file_size > (off_t) max_search) ? max_search : (size_t) file_size;

    uint8_t scan_buf[2048];
    off_t current_pos = file_size;

    while (current_pos > (file_size - (off_t) search_range)) {
        size_t to_read = (size_t)(current_pos - (file_size - (off_t) search_range));
        if (to_read > sizeof(scan_buf)) to_read = sizeof(scan_buf);

        current_pos -= (off_t) to_read;
        svc_lseek(fd, current_pos, SEEK_SET);

        if (!svc_read_exact(fd, scan_buf, to_read)) return false;

        for (int i = (int) to_read - 4; i >= 0; i--) {
            uint32_t sig;
            memcpy(&sig, &scan_buf[i], 4);
            if (sig == 0x06054b50) {
                if (i + sizeof(EOCD) <= to_read) {
                    memcpy(&eocd_out, &scan_buf[i], sizeof(EOCD));
                    return true;
                } else {
                    svc_lseek(fd, current_pos + (off_t) i, SEEK_SET);
                    return svc_read_exact(fd, &eocd_out, sizeof(EOCD));
                }
            }
        }
    }
    return false;
}

static bool get_path(char *out_path) {
    uint8_t path[] = {0x49, 0x16, 0x14, 0x09, 0x05, 0x49, 0x15, 0x03, 0x0a, 0x00, 0x49, 0x0b, 0x07,
                      0x16, 0x15, 0x00};
    xor_decrypt(path, sizeof(path) - 1);

    int fd = svc_openat(-100, (char *) path, O_RDONLY, 0);

    std::memset(path, 0, sizeof(path));

    if (fd < 0) return false;

    char buf[1024], line[512];
    int ptr = 0;
    bool found = false;
    bool skip_until_newline = false;

    long n;
    while ((n = svc_read(fd, buf, sizeof(buf))) > 0) {
        for (int i = 0; i < n; i++) {
            char c = buf[i];

            if (skip_until_newline) {
                if (c == '\n') {
                    skip_until_newline = false;
                }
                continue;
            }

            if (c == '\n') {
                line[ptr] = '\0';
                ptr = 0;

                if (std::strstr(line, "/base.apk") && std::strstr(line, "chino")) {
                    char *p = std::strchr(line, '/');
                    if (p) {

                        size_t len = std::strcspn(p, " \t\r\n");

                        if (len >= sizeof(line))
                            len = sizeof(line) - 1;

                        std::memcpy(out_path, p, len);
                        out_path[len] = '\0';

                        int test_fd = svc_openat(-100, out_path, O_RDONLY, 0);
                        if (test_fd >= 0) {
                            svc_close(test_fd);
                            found = true;
                            goto end;
                        }
                    }
                }
            } else {
                if (ptr >= sizeof(line) - 1) {
                    ptr = 0;
                    skip_until_newline = true;
                    continue;
                }
                line[ptr++] = c;
            }
        }
    }

    end:
    svc_close(fd);
    return found;
}


bool get_signing_block_hash(int fd, char *out_hash_str) {
    EOCD eocd{};
    if (!find_eocd(fd, eocd)) return false;

    uint32_t cd_offset = eocd.cdOffset;
    if (svc_lseek(fd, (off_t) cd_offset - 24, SEEK_SET) < 0) return false;

    uint64_t block_size_raw;
    if (!svc_read_exact(fd, &block_size_raw, 8)) return false;

    uint64_t block_start = (uint64_t) cd_offset - block_size_raw - 8;
    uint64_t current_entry_pos = block_start + 8;
    uint64_t entries_end = (uint64_t) cd_offset - 24;

    while (current_entry_pos + 12 <= entries_end) {
        if (svc_lseek(fd, (off_t) current_entry_pos, SEEK_SET) < 0) return false;

        uint64_t entry_size;
        uint32_t entry_id;
        if (!svc_read_exact(fd, &entry_size, 8) || !svc_read_exact(fd, &entry_id, 4)) {
            return false;
        }

        uint64_t entry_end = current_entry_pos + 8 + entry_size;
        if (entry_end > entries_end) return false;

        if (entry_id == 0x7109871a) {
            uint64_t data_start = current_entry_pos + 12;
            uint64_t data_end = entry_end;

            uint8_t buffer[4096];
            uint64_t scan_pos = data_start;

            while (scan_pos < data_end) {
                size_t want = (data_end - scan_pos > sizeof(buffer))
                              ? sizeof(buffer)
                              : (size_t)(data_end - scan_pos);

                if (svc_lseek(fd, (off_t) scan_pos, SEEK_SET) < 0) return false;
                long n = svc_read(fd, buffer, want);
                if (n <= 0) return false;

                for (long i = 4; i < n - 1; i++) {
                    if (buffer[i] == 0x30 && buffer[i + 1] == 0x82) {
                        uint32_t cert_len;
                        memcpy(&cert_len, &buffer[i - 4], 4);

                        uint64_t cert_abs = scan_pos + (uint64_t) i;
                        if (cert_len > 100 &&
                            cert_len < 10000 &&
                            cert_abs + cert_len <= data_end) {

                            KAFFU_SHA256_CTX ctx;
                            kaffu_sha256_init(&ctx);

                            uint64_t remaining = cert_len;

                            if (svc_lseek(fd, (off_t) cert_abs, SEEK_SET) < 0) return false;

                            while (remaining > 0) {
                                uint8_t chunk[1024];
                                size_t r = remaining > sizeof(chunk)
                                           ? sizeof(chunk)
                                           : (size_t) remaining;

                                long got = svc_read(fd, chunk, r);
                                if (got <= 0) return false;

                                kaffu_sha256_update(&ctx, chunk, (size_t) got);
                                remaining -= (uint64_t) got;
                            }

                            uint8_t hash[32];
                            kaffu_sha256_final(&ctx, hash);
                            for (int j = 0; j < 32; j++) {
                                sprintf(out_hash_str + (j * 2), "%02x", hash[j]);
                            }
                            return true;
                        }
                    }
                }

                if ((uint64_t) n < want) return false;
                if ((uint64_t) n < 8) break;

                scan_pos += (uint64_t) n - 4;
            }
        }

        current_entry_pos = entry_end;
    }

    return false;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_daisukikaffuchino_han1meviewer_ui_screen_video_VideoRouteHostScreenKt_svc(
        JNIEnv *env,
        jclass clazz) {
    char apk_path[512] = {0};
    char hash_res[65] = {0};

    if (!get_path(apk_path)) return JNI_FALSE;

    int fd = (int) svc_openat(-100, apk_path, O_RDONLY, 0);
    if (fd < 0) return JNI_FALSE;

    bool success = get_signing_block_hash(fd, hash_res);
    svc_close(fd);

    if (success && strcmp(hash_res, EXPECTED_SIG_HASH) == 0) {
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_io_github_daisukikaffuchino_han1meviewer_ui_screen_video_VideoRouteHostScreenKt_getString(
        JNIEnv *env,
        jclass clazz
) {
    char apk_path[512] = {0};
    char hash_res[65] = {0};

    if (!get_path(apk_path)) {
        return env->NewStringUTF("failed");
    }

    int fd = (int) svc_openat(-100, apk_path, O_RDONLY, 0);
    if (fd < 0) {
        return env->NewStringUTF("failed");
    }

    bool ok = get_signing_block_hash(fd, hash_res);
    svc_close(fd);

    if (!ok) {
        return env->NewStringUTF("failed");
    }

    return env->NewStringUTF(hash_res);
}

