/*
 * Copyright (C) 2008 The Android Open Source Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

#include <errno.h>
#include <inttypes.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/mman.h>
#include <sys/prctl.h>
#include <sys/un.h>
#include <unistd.h>

#include <time.h>
#include "signalhandler_machine.h"

struct sigaction orig_sig_handler[32];

#define DO_BACKTRACE

#ifdef DO_BACKTRACE
// from debuggerd/backtrace.cpp

#include <corkscrew/backtrace.h>

#define STACK_DEPTH 32

extern "C" ssize_t unwind_backtrace_signal(siginfo_t* siginfo, void* sigcontext, backtrace_frame_t* backtrace, size_t ignore_depth, size_t max_depth);

static void dump_thread(FILE* file, pid_t tid, siginfo_t* info, ucontext_t* context) {
    backtrace_frame_t backtrace[STACK_DEPTH];
    ssize_t frames = unwind_backtrace_signal(info, context, backtrace, 0, STACK_DEPTH);
    if (frames <= 0) {
        fprintf(file, "Could not obtain stack trace for thread.\n");
    } else {
        backtrace_symbol_t backtrace_symbols[STACK_DEPTH];
        get_backtrace_symbols(backtrace, frames, backtrace_symbols);
        for (size_t i = 0; i < (size_t)frames; i++) {
            char line[MAX_BACKTRACE_LINE_LENGTH];
            format_backtrace_line(i, &backtrace[i], &backtrace_symbols[i],
                    line, MAX_BACKTRACE_LINE_LENGTH);
            fprintf(file, "  %s\n", line);
        }
        free_backtrace_symbols(backtrace_symbols, frames);
    }

}
#endif


// From linker/debugger.cpp

#define MAX_TASK_NAME_LEN (16)

/*
 * Writes a summary of the signal to the log file.  We do this so that, if
 * for some reason we're not able to contact debuggerd, there is still some
 * indication of the failure in the log.
 *
 * We could be here as a result of native heap corruption, or while a
 * mutex is being held, so we don't want to use any libc functions that
 * could allocate memory or hold a lock.
 */

// not anymore, lol
static void log_signal_summary(int signum, const siginfo_t* info, FILE* file) {
  const char* signal_name = "???";
  bool has_address = false;
  switch (signum) {
    case SIGABRT:
      signal_name = "SIGABRT";
      break;
    case SIGBUS:
      signal_name = "SIGBUS";
      has_address = true;
      break;
    case SIGFPE:
      signal_name = "SIGFPE";
      has_address = true;
      break;
    case SIGILL:
      signal_name = "SIGILL";
      has_address = true;
      break;
    case SIGPIPE:
      signal_name = "SIGPIPE";
      break;
    case SIGSEGV:
      signal_name = "SIGSEGV";
      has_address = true;
      break;
#if defined(SIGSTKFLT)
    case SIGSTKFLT:
      signal_name = "SIGSTKFLT";
      break;
#endif
    case SIGTRAP:
      signal_name = "SIGTRAP";
      break;
  }

  char thread_name[MAX_TASK_NAME_LEN + 1]; // one more for termination
  if (prctl(PR_GET_NAME, (unsigned long)thread_name, 0, 0, 0) != 0) {
    strcpy(thread_name, "<name unknown>");
  } else {
    // short names are null terminated by prctl, but the man page
    // implies that 16 byte names are not.
    thread_name[MAX_TASK_NAME_LEN] = 0;
  }

  // "info" will be null if the siginfo_t information was not available.
  // Many signals don't have an address or a code.
  char code_desc[32]; // ", code -6"
  char addr_desc[32]; // ", fault addr 0x1234"
  addr_desc[0] = code_desc[0] = 0;
  if (info != nullptr) {
    // For a rethrown signal, this si_code will be right and the one debuggerd shows will
    // always be SI_TKILL.
    snprintf(code_desc, sizeof(code_desc), ", code %d", info->si_code);
    if (has_address) {
      snprintf(addr_desc, sizeof(addr_desc), ", fault addr %p", info->si_addr);
    }
  }
  fprintf(file,
                    "Fatal signal %d (%s)%s%s in tid %d (%s)",
                    signum, signal_name, code_desc, addr_desc, gettid(), thread_name);
}

/*
 * Returns true if the handler for signal "signum" has SA_SIGINFO set.
 */
static bool have_siginfo(int signum) {
  struct sigaction old_action, new_action;

  memset(&new_action, 0, sizeof(new_action));
  new_action.sa_handler = SIG_DFL;
  new_action.sa_flags = SA_RESTART;
  sigemptyset(&new_action.sa_mask);

  if (sigaction(signum, &new_action, &old_action) < 0) {
    //__libc_format_log(ANDROID_LOG_WARN, "libc", "Failed testing for SA_SIGINFO: %s",
    //                  strerror(errno));
    return false;
  }
  bool result = (old_action.sa_flags & SA_SIGINFO) != 0;

  if (sigaction(signum, &old_action, nullptr) == -1) {
    //__libc_format_log(ANDROID_LOG_WARN, "libc", "Restore failed in test for SA_SIGINFO: %s",
    //                  strerror(errno));
  }
  return result;
}

/*
 * Catches fatal signals so we can ask debuggerd to ptrace us before
 * we crash.
 */
static void debuggerd_signal_handler(int signal_number, siginfo_t* info, void* context) {
  // It's possible somebody cleared the SA_SIGINFO flag, which would mean
  // our "info" arg holds an undefined value.
  if (!have_siginfo(signal_number)) {
    info = nullptr;
  }

  signal(signal_number, SIG_DFL);

  char filepath[1000];
  snprintf(filepath, sizeof(filepath), "/sdcard/blocklauncher_crash_%ld.txt", time(NULL));

  FILE* file = fopen(filepath, "w");

  log_signal_summary(signal_number, info, file);
  fprintf(file, "\n");
  dump_registers(file, (ucontext_t*) context);
#ifdef DO_BACKTRACE
  dump_thread(file, gettid(), info, (ucontext_t*) context);
#endif

  fclose(file);

  // call through to the next signal handler
  orig_sig_handler[signal_number].sa_sigaction(signal_number, info, context);
}

extern "C" void bl_signalhandler_init() {
  struct sigaction action;
  memset(&action, 0, sizeof(action));
  sigemptyset(&action.sa_mask);
  action.sa_sigaction = debuggerd_signal_handler;
  action.sa_flags = SA_RESTART | SA_SIGINFO;

  // Use the alternate signal stack if available so we can catch stack overflows.
  action.sa_flags |= SA_ONSTACK;

  sigaction(SIGABRT, &action, &orig_sig_handler[SIGABRT]);
  sigaction(SIGBUS, &action, &orig_sig_handler[SIGBUS]);
  sigaction(SIGFPE, &action, &orig_sig_handler[SIGFPE]);
  sigaction(SIGILL, &action, &orig_sig_handler[SIGILL]);
  sigaction(SIGPIPE, &action, &orig_sig_handler[SIGPIPE]);
  sigaction(SIGSEGV, &action, &orig_sig_handler[SIGSEGV]);
#if defined(SIGSTKFLT)
  sigaction(SIGSTKFLT, &action, &orig_sig_handler[SIGSTKFLT]);
#endif
  sigaction(SIGTRAP, &action, &orig_sig_handler[SIGTRAP]);
}
