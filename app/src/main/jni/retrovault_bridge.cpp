#include <jni.h>
#include <android/log.h>
#include <algorithm>
#include <atomic>
#include <cstdio>
#include <cstring>
#include <fstream>
#include <mutex>
#include <string>
#include <vector>
#include "libretro.h"

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "RetroVaultNative", __VA_ARGS__)

namespace {
std::mutex coreMutex;
std::vector<uint16_t> video565(256 * 240);
std::vector<int16_t> audioSamples;
std::atomic<uint32_t> inputMask{0};
std::string systemDirectory;
std::string saveDirectory;
unsigned frameWidth = 256;
unsigned frameHeight = 240;
double frameRate = 60.0988;
bool loaded = false;

bool environment(unsigned cmd, void *data) {
    switch (cmd) {
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT: {
            auto *format = static_cast<retro_pixel_format *>(data);
            return *format == RETRO_PIXEL_FORMAT_RGB565;
        }
        case RETRO_ENVIRONMENT_GET_CAN_DUPE:
            *static_cast<bool *>(data) = true;
            return true;
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
            *static_cast<const char **>(data) = systemDirectory.c_str();
            return true;
        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:
            *static_cast<const char **>(data) = saveDirectory.c_str();
            return true;
        case RETRO_ENVIRONMENT_SET_VARIABLES:
        case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS:
        case RETRO_ENVIRONMENT_SET_CONTROLLER_INFO:
        case RETRO_ENVIRONMENT_SET_MEMORY_MAPS:
        case RETRO_ENVIRONMENT_SET_SUPPORT_ACHIEVEMENTS:
        case RETRO_ENVIRONMENT_SET_GEOMETRY:
            return true;
        case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE:
            *static_cast<bool *>(data) = false;
            return true;
        case RETRO_ENVIRONMENT_GET_VARIABLE: {
            auto *variable = static_cast<retro_variable *>(data);
            variable->value = nullptr;
            // A true return value guarantees that value points to a valid
            // option string. Some cores (including FCEUmm's DIP switch path)
            // rely on that contract and call strcmp() without another null
            // check. We do not expose configurable core options yet, so report
            // the lookup as unsupported and let the core keep its defaults.
            return false;
        }
        default:
            return false;
    }
}

void video(const void *data, unsigned width, unsigned height, size_t pitch) {
    if (!data || width == 0 || height == 0) return;
    frameWidth = std::min(width, 256u);
    frameHeight = std::min(height, 240u);
    const auto *source = static_cast<const uint8_t *>(data);
    for (unsigned y = 0; y < frameHeight; ++y) {
        std::memcpy(video565.data() + y * 256, source + y * pitch, frameWidth * sizeof(uint16_t));
    }
}

void audio(int16_t left, int16_t right) {
    audioSamples.push_back(left);
    audioSamples.push_back(right);
}

size_t audioBatch(const int16_t *data, size_t frames) {
    audioSamples.insert(audioSamples.end(), data, data + frames * 2);
    return frames;
}

void inputPoll() {}

int16_t inputState(unsigned port, unsigned device, unsigned, unsigned id) {
    if (port != 0 || device != RETRO_DEVICE_JOYPAD || id > RETRO_DEVICE_ID_JOYPAD_R3) return 0;
    return (inputMask.load(std::memory_order_relaxed) & (1u << id)) ? 1 : 0;
}

bool readFile(const std::string &path, std::vector<uint8_t> &result) {
    std::ifstream stream(path, std::ios::binary | std::ios::ate);
    if (!stream) return false;
    const auto size = stream.tellg();
    if (size <= 0) return false;
    result.resize(static_cast<size_t>(size));
    stream.seekg(0);
    return static_cast<bool>(stream.read(reinterpret_cast<char *>(result.data()), size));
}

bool writeFile(const std::string &path, const void *data, size_t size) {
    const std::string temporary = path + ".tmp";
    std::ofstream stream(temporary, std::ios::binary | std::ios::trunc);
    if (!stream || !stream.write(static_cast<const char *>(data), static_cast<std::streamsize>(size))) return false;
    stream.close();
    std::remove(path.c_str());
    return std::rename(temporary.c_str(), path.c_str()) == 0;
}

std::string fromJString(JNIEnv *env, jstring value) {
    const char *chars = env->GetStringUTFChars(value, nullptr);
    std::string result(chars ? chars : "");
    if (chars) env->ReleaseStringUTFChars(value, chars);
    return result;
}
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_retrovault_nes_emulation_NativeBridge_nativeInit(JNIEnv *env, jobject, jstring romPath, jstring systemDir, jstring saveDir) {
    std::lock_guard<std::mutex> lock(coreMutex);
    if (loaded) { retro_unload_game(); retro_deinit(); loaded = false; }
    systemDirectory = fromJString(env, systemDir);
    saveDirectory = fromJString(env, saveDir);
    const std::string path = fromJString(env, romPath);
    std::vector<uint8_t> rom;
    if (!readFile(path, rom)) { LOGE("Unable to read ROM: %s", path.c_str()); return JNI_FALSE; }

    retro_set_environment(environment);
    retro_set_video_refresh(video);
    retro_set_audio_sample(audio);
    retro_set_audio_sample_batch(audioBatch);
    retro_set_input_poll(inputPoll);
    retro_set_input_state(inputState);
    retro_init();
    retro_set_controller_port_device(0, RETRO_DEVICE_JOYPAD);

    retro_game_info info{};
    info.path = path.c_str();
    info.data = rom.data();
    info.size = rom.size();
    if (!retro_load_game(&info)) { retro_deinit(); LOGE("Core rejected ROM"); return JNI_FALSE; }

    retro_system_av_info av{};
    retro_get_system_av_info(&av);
    frameRate = av.timing.fps > 1.0 ? av.timing.fps : 60.0988;
    loaded = true;
    return JNI_TRUE;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_retrovault_nes_emulation_NativeBridge_nativeRunFrame(JNIEnv *env, jobject, jintArray pixels, jshortArray samples) {
    std::lock_guard<std::mutex> lock(coreMutex);
    if (!loaded) return 0;
    audioSamples.clear();
    retro_run();

    const jsize pixelCount = env->GetArrayLength(pixels);
    std::vector<jint> converted(static_cast<size_t>(std::min(pixelCount, 256 * 240)));
    for (size_t i = 0; i < converted.size(); ++i) {
        const uint16_t p = video565[i];
        converted[i] = static_cast<jint>(0xff000000u | ((p & 0xf800u) << 8u) | ((p & 0x07e0u) << 5u) | ((p & 0x001fu) << 3u));
    }
    env->SetIntArrayRegion(pixels, 0, static_cast<jsize>(converted.size()), converted.data());
    const jsize count = std::min(static_cast<jsize>(audioSamples.size()), env->GetArrayLength(samples));
    if (count > 0) env->SetShortArrayRegion(samples, 0, count, audioSamples.data());
    return count;
}

extern "C" JNIEXPORT void JNICALL
Java_com_retrovault_nes_emulation_NativeBridge_nativeSetInput(JNIEnv *, jobject, jint mask) { inputMask.store(static_cast<uint32_t>(mask)); }

extern "C" JNIEXPORT jdouble JNICALL
Java_com_retrovault_nes_emulation_NativeBridge_nativeFrameRate(JNIEnv *, jobject) { return frameRate; }

extern "C" JNIEXPORT jboolean JNICALL
Java_com_retrovault_nes_emulation_NativeBridge_nativeSaveSram(JNIEnv *env, jobject, jstring pathValue) {
    std::lock_guard<std::mutex> lock(coreMutex);
    if (!loaded) return JNI_FALSE;
    void *data = retro_get_memory_data(RETRO_MEMORY_SAVE_RAM);
    const size_t size = retro_get_memory_size(RETRO_MEMORY_SAVE_RAM);
    return data && size && writeFile(fromJString(env, pathValue), data, size) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_retrovault_nes_emulation_NativeBridge_nativeLoadSram(JNIEnv *env, jobject, jstring pathValue) {
    std::lock_guard<std::mutex> lock(coreMutex);
    if (!loaded) return JNI_FALSE;
    void *target = retro_get_memory_data(RETRO_MEMORY_SAVE_RAM);
    const size_t expected = retro_get_memory_size(RETRO_MEMORY_SAVE_RAM);
    std::vector<uint8_t> data;
    if (!target || !expected || !readFile(fromJString(env, pathValue), data) || data.size() != expected) return JNI_FALSE;
    std::memcpy(target, data.data(), expected);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_retrovault_nes_emulation_NativeBridge_nativeSaveState(JNIEnv *env, jobject, jstring pathValue) {
    std::lock_guard<std::mutex> lock(coreMutex);
    if (!loaded) return JNI_FALSE;
    std::vector<uint8_t> state(retro_serialize_size());
    return !state.empty() && retro_serialize(state.data(), state.size()) && writeFile(fromJString(env, pathValue), state.data(), state.size()) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_retrovault_nes_emulation_NativeBridge_nativeLoadState(JNIEnv *env, jobject, jstring pathValue) {
    std::lock_guard<std::mutex> lock(coreMutex);
    if (!loaded) return JNI_FALSE;
    std::vector<uint8_t> state;
    return readFile(fromJString(env, pathValue), state) && retro_unserialize(state.data(), state.size()) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_retrovault_nes_emulation_NativeBridge_nativeReset(JNIEnv *, jobject) { std::lock_guard<std::mutex> lock(coreMutex); if (loaded) retro_reset(); }

extern "C" JNIEXPORT void JNICALL
Java_com_retrovault_nes_emulation_NativeBridge_nativeShutdown(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(coreMutex);
    if (loaded) { retro_unload_game(); retro_deinit(); loaded = false; }
}
