#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <errno.h>
#include <stdbool.h>
#define BL_LOG(a, ...) printf(a "\n", ##__VA_ARGS__)

/*
0000000c <hook2>:
   c:	f8df f000 	ldr.w	pc, [pc]	; 10 <address2>

00000010 <address2>:
  10:	12345678 	.word	0x12345678
*/
const char kHookStart[] = {0xdf, 0xf8, 0x00, 0xf0};

static bool isThumb32Bit(uint16_t instr) {
    // according to QEMU:
    // "Top five bits 0b11101 / 0b11110 / 0b11111 : this is the first half of a 32-bit Thumb insn."
    uint16_t topFive = instr >> 11;
    return topFive == 0b11101 || topFive == 0b11110 || topFive == 0b11111;
}

static bool isEightBytesEnough(char* origFcnReadPtr) {
    int i;
    for (i = 0; i < 8; ) {
        uint16_t instr = *((uint16_t*)(origFcnReadPtr + i));
        if (isThumb32Bit(instr)) {
            i += 4;
        } else {
            i += 2;
        }
    }
    return i == 8;
}

void bl_silly_hook(void* origFcn, void* newFcn, void** origFcnPtr) {
    // a dumb function hook to use when TinySubstrate gets confused
    // this is just a rewrite of BlockLauncher's first hook.
    // Like the original, it:
    // replaces the first 8 bytes with
    // ldr pc, address
    // address: (hook)
    // and hope there's nothing PC-rel in the first 8-12 bytes of the function
    if (!origFcn) {
        BL_LOG("Original function is null");
        return;
    }
    char* page = mmap(NULL, 0x1000,
        PROT_READ | PROT_WRITE,
        MAP_PRIVATE | MAP_ANONYMOUS,
        -1, 0);
    if (page == MAP_FAILED) {
        BL_LOG("Failed to map page: %s", strerror(errno));
        abort();
    }
    char* origFcnReadPtr = (char*)(((uintptr_t)origFcn) & ~1);
    char* origFcnWritePtr = origFcnReadPtr; // should use marauder map function here

    // populate the origFcn:
    // 12 bytes of original function
    // 8 bytes of jumping code
    // 4 bytes of target address

    // there's a edge case here where copying 8 bytes is not enough:
    // 0: I1
    // 2: I2
    // 4: I3
    // 6: I5 // 32-bit!
    // 10: I6
    // we need to copy 10 bytes in this case, and pad an extra nop in the trampoline.
    int origFcnCopyAmount = isEightBytesEnough(origFcnReadPtr)? 8: 10;

    memcpy(page, origFcnReadPtr, origFcnCopyAmount);
    int align = 0;
    if (origFcnCopyAmount == 10) {
        align = 2;
        char* noptime = (page + origFcnCopyAmount);
        noptime[0] = 0x00;
        noptime[1] = 0xbf;
    }
    memcpy(page + origFcnCopyAmount + align, kHookStart, 4);
    ((void**)(page + origFcnCopyAmount + align + 4))[0] = (void*)(((uintptr_t)origFcn) + origFcnCopyAmount);

    // hook the function
    // 4 bytes of jumping code
    // 4 bytes of address
    memcpy(origFcnWritePtr, kHookStart, 4);
    ((void**)(origFcnWritePtr + 4))[0] = newFcn;

    if (mprotect(page, 0x1000, PROT_READ | PROT_EXEC) == -1) {
        BL_LOG("Failed to mprotect: %s", strerror(errno));
        abort();
    }
    BL_LOG("Hooked %p to %p, trampoline at %p, copy amount = %d", origFcn, newFcn, page, origFcnCopyAmount);
    *origFcnPtr = (void*)(((uintptr_t)page) | 1);
}