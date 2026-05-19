#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <KittyMemory/KittyMemory.h>
#include <KittyMemory/MemoryPatch.h>
#include <KittyMemory/KittyScanner.h>
#include <KittyMemory/KittyUtils.h>
#include "Includes/obfuscate.h"
#include "Includes/Logger.h"
#include "Includes/Macros.h"
#include "Includes/JNIStuff.h"
#include "Includes/Utils.h"
#include "STARCOOLX/Call_Me.h"
#include "STARCOOL.h"
#include "SocketControl.h"

bool hackjump = false;
bool hackscore = false;
float speedValue = 1.0f;
bool hackcoins = false;
bool hacknoclip = false;

int (*old_jump)(void *instance);
int jump(void *instance) {
    if (instance != NULL && hackjump) {
        return 9999999;
    }
    return old_jump(instance);
}

int (*old_score)(void *instance);
int score(void *instance) {
    if (instance != NULL && hackscore) {
        return 9999999;
    }
    return old_score(instance);
}

float (*old_speed)(void *instance);
float speed(void *instance) {
    if (instance != NULL) {
        return speedValue;
    }
    return old_speed(instance);
}

MemoryPatch Coins;
MemoryPatch noclipPatch;

void *Init_Thread(void *) {
    uintptr_t il2cppMap = 0;
    for (int i = 0; i < 15; i++) {
        il2cppMap = Tools::GetBaseAddress("libil2cpp.so");
        if (il2cppMap != 0) break;
        sleep(2);
    }
    if (il2cppMap == 0) return nullptr;
    DobbyHook((void *)(il2cppMap + 0x1EAB7FC), (void *) jump, (void **) &old_jump);
    DobbyHook((void *)(il2cppMap + 0x1234567), (void *) score, (void **) &old_score);
    DobbyHook((void *)(il2cppMap + 0x1357924), (void *) speed, (void **) &old_speed);
    Coins = MemoryPatch::createWithHex(il2cppMap + 0x3C7284C, "E0 1F 9C D2 A0 BE A0 F2 C0 03 5F D6");
    noclipPatch = MemoryPatch::createWithHex(il2cppMap + 0x2468135, "00 00 A0 E3 1E FF 2F E1");
    while (true) {
        if (hackcoins) {
            Coins.Modify();
        } else {
            Coins.Restore();
        }
        if (hacknoclip) {
            noclipPatch.Modify();
        } else {
            noclipPatch.Restore();
        }
        sleep(1);
    }
    return nullptr;
}

__attribute__((constructor))
void lib_main() {
    pthread_t ptid;
    pthread_create(&ptid, NULL, socket_server_thread, NULL);
    pthread_t myThread;
    pthread_create(&myThread, NULL, Init_Thread, NULL);
}

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    return JNI_VERSION_1_6;
}
