#ifdef __cplusplus
extern "C" {
#endif

int mcpelauncher_get_version();
void mcpelauncher_hook(void *orig_fcn, void* new_fcn, void **orig_fcn_ptr);

#ifdef __cplusplus
}
#endif
