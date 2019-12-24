#include <elf.h>
#ifdef __cplusplus
extern "C" {
#endif
void* dobby_dlsym(void* handle, const char* symbol);
//Elf_Sym* dobby_elfsym(void* si, const char* name);
#ifdef __cplusplus
}
#endif
