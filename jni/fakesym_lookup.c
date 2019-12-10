#include <stdint.h>
void bl_fakeSyms_initOneAddress(void** ptr, uintptr_t baseAddr, uintptr_t offset) {
	*ptr = (void*)(baseAddr + offset);
}
