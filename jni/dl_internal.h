#pragma once
#include <elf.h>
#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
  char name[128];
  const Elf_Phdr* phdr;
  int phnum;
  unsigned entry;
  unsigned base;
  unsigned size;
} soinfo2;

#ifdef __cplusplus
}
#endif
