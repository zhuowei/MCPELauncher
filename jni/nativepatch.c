#include <jni.h>
#include <sys/mman.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <malloc.h>
#include <unistd.h>
#include <android/log.h>

#include <mcpelauncher.h>

JNIEXPORT jint JNICALL Java_net_zhuoweizhang_pokerface_PokerFace_mprotect
  (JNIEnv *env, jclass clazz, jlong addr, jlong len, jint prot) {
	return mprotect((void *)(uintptr_t) addr, len, prot);
}

JNIEXPORT jlong JNICALL Java_net_zhuoweizhang_pokerface_PokerFace_sysconf
  (JNIEnv *env, jclass clazz, jint name) {
	long result = sysconf(name);
	return result;
}

void *__memcpy(void *d, void *s, int n)
{
    int i;

    for( i = 0; i < n; i++)
        ((unsigned char *)d)[i] = ((unsigned char *)s)[i];

    return d;
}

int __mprotect_no_errno_set(void * a, int n, int p)
{
    return mprotect(a, n, p);
}

void mcpelauncher_hook(void *orig_fcn, void* new_fcn, void **orig_fcn_ptr)
{
    __android_log_print(ANDROID_LOG_INFO, "andhook_embed", "andhook: placing hook (orig_fcn=%p, new_fcn=%p, orig_fcn_ptr=%p)\n", 
            orig_fcn,
            new_fcn,
            orig_fcn_ptr );

#ifdef __arm__

    int thumbMode = (int) orig_fcn & 0x1;

    if (thumbMode) {
        orig_fcn = (void*) ((int) orig_fcn - 1);
        
    }

    unsigned char *hook = malloc( sysconf( _SC_PAGESIZE ) );

    __memcpy( hook, (unsigned char *)orig_fcn, 8 );    /* save 1st 8 bytes of orig fcn */
    *(int *)(hook + 8) = 0xf000f85f;                   /* ldr pc, [pc] */
    *(int *)(hook + 12) = (int)orig_fcn + 9;           /* ptr to orig fcn offset */

    if( __mprotect_no_errno_set( (void *)(int)hook - ((int)hook % sysconf( _SC_PAGESIZE )),
                                 sysconf( _SC_PAGESIZE ),
                                 PROT_EXEC|PROT_READ|PROT_WRITE ) == 0 ) {
        if( 1 ) {
            *((unsigned int*)orig_fcn) = 0xf000f85f; //f85ff004, actually, but half-endian swap
            *((unsigned int*)((int)orig_fcn + 4)) = (int)new_fcn;

            if( 1 ) {
                *orig_fcn_ptr = (void*) (hook + 1) ;
            }
        }
    }

#endif
}

int mcpelauncher_get_version() {
	return 1;
}

