#include <jni.h>
#include <cstdint>
#include <cstdio>
#include <string>

namespace {
constexpr std::uint64_t MAX_PACK_SIZE = 512ULL * 1024ULL * 1024ULL;

std::string toUtf8(JNIEnv* env, jstring value) {
    if (value == nullptr) return {};
    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) return {};
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

bool hasMcpackExtension(const std::string& path) {
    constexpr char suffix[] = ".mcpack";
    constexpr std::size_t n = sizeof(suffix) - 1;
    if (path.size() < n) return false;
    const std::size_t start = path.size() - n;
    for (std::size_t i = 0; i < n; ++i) {
        char c = path[start + i];
        if (c >= 'A' && c <= 'Z') c = static_cast<char>(c - 'A' + 'a');
        if (c != suffix[i]) return false;
    }
    return true;
}

bool looksLikeZip(const std::string& path) {
    FILE* file = std::fopen(path.c_str(), "rb");
    if (!file) return false;
    unsigned char header[4] = {};
    const std::size_t count = std::fread(header, 1, sizeof(header), file);
    std::fclose(file);
    return count == 4 && header[0] == 0x50 && header[1] == 0x4B &&
           (header[2] == 0x03 || header[2] == 0x05 || header[2] == 0x07);
}

bool hasAcceptableSize(const std::string& path) {
    FILE* file = std::fopen(path.c_str(), "rb");
    if (!file) return false;
    if (std::fseek(file, 0, SEEK_END) != 0) {
        std::fclose(file);
        return false;
    }
    const long size = std::ftell(file);
    std::fclose(file);
    return size > 0 && static_cast<std::uint64_t>(size) <= MAX_PACK_SIZE;
}
} // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_net_nexlauncher_MainActivity_nativeValidateMcpack(
    JNIEnv* env, jobject /* thiz */, jstring javaPath) {
    const std::string path = toUtf8(env, javaPath);
    const char* result = nullptr;
    if (path.empty()) {
        result = "ERROR: File path is empty.";
    } else if (!hasMcpackExtension(path)) {
        result = "ERROR: Select a file ending in .mcpack.";
    } else if (!hasAcceptableSize(path)) {
        result = "ERROR: File is missing, empty, unreadable, or larger than 512 MiB.";
    } else if (!looksLikeZip(path)) {
        result = "ERROR: File does not have a recognized ZIP header.";
    } else {
        result = "OK: Basic ZIP checks passed. Full archive and manifest validation is still required.";
    }
    return env->NewStringUTF(result);
}

extern "C" JNIEXPORT jint JNICALL
Java_net_nexlauncher_MainActivity_nativeGetAbiVersion(
    JNIEnv* /* env */, jobject /* thiz */) {
    return 1;
}
