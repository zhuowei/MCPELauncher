#include "signalhandler_machine.h"
void dump_registers(FILE* file, ucontext_t* context) {
  mcontext_t* m = &context->uc_mcontext;

  fprintf(file, "    r0 %08x  r1 %08x  r2 %08x  r3 %08x\n",
       static_cast<uint32_t>(m->arm_r0), static_cast<uint32_t>(m->arm_r1),
       static_cast<uint32_t>(m->arm_r2), static_cast<uint32_t>(m->arm_r3));
  fprintf(file, "    r4 %08x  r5 %08x  r6 %08x  r7 %08x\n",
       static_cast<uint32_t>(m->arm_r4), static_cast<uint32_t>(m->arm_r5),
       static_cast<uint32_t>(m->arm_r6), static_cast<uint32_t>(m->arm_r7));
  fprintf(file, "    r8 %08x  r9 %08x  sl %08x  fp %08x\n",
       static_cast<uint32_t>(m->arm_r8), static_cast<uint32_t>(m->arm_r9),
       static_cast<uint32_t>(m->arm_r10), static_cast<uint32_t>(m->arm_fp));
  fprintf(file, "    ip %08x  sp %08x  lr %08x  pc %08x  cpsr %08x\n",
       static_cast<uint32_t>(m->arm_ip), static_cast<uint32_t>(m->arm_sp),
       static_cast<uint32_t>(m->arm_lr), static_cast<uint32_t>(m->arm_pc),
       static_cast<uint32_t>(m->arm_cpsr));
}
