#pragma once

enum FMOD_RESULT {
  FMOD_OK,
  FMOD_ERR_BADCOMMAND,
  FMOD_ERR_CHANNEL_ALLOC,
  FMOD_ERR_CHANNEL_STOLEN,
  FMOD_ERR_DMA,
  FMOD_ERR_DSP_CONNECTION,
  FMOD_ERR_DSP_DONTPROCESS,
  FMOD_ERR_DSP_FORMAT,
  FMOD_ERR_DSP_INUSE,
  FMOD_ERR_DSP_NOTFOUND,
  FMOD_ERR_DSP_RESERVED,
  FMOD_ERR_DSP_SILENCE,
  FMOD_ERR_DSP_TYPE,
  FMOD_ERR_FILE_BAD,
  FMOD_ERR_FILE_COULDNOTSEEK,
  FMOD_ERR_FILE_DISKEJECTED,
  FMOD_ERR_FILE_EOF,
  FMOD_ERR_FILE_ENDOFDATA,
  FMOD_ERR_FILE_NOTFOUND,
};

typedef unsigned int FMOD_INITFLAGS;

#define F_CALLBACK

typedef FMOD_RESULT F_CALLBACK (*FMOD_FILE_OPEN_CALLBACK)(
  const char *name,
  unsigned int *filesize,
  void **handle,
  void *userdata
);

typedef FMOD_RESULT F_CALLBACK (*FMOD_FILE_CLOSE_CALLBACK)(
  void *handle,
  void *userdata
);

typedef FMOD_RESULT F_CALLBACK (*FMOD_FILE_READ_CALLBACK)(
  void *handle,
  void *buffer,
  unsigned int sizebytes,
  unsigned int *bytesread,
  void *userdata
);

typedef FMOD_RESULT F_CALLBACK (*FMOD_FILE_SEEK_CALLBACK)(
  void *handle,
  unsigned int pos,
  void *userdata
);

typedef FMOD_RESULT F_CALLBACK (*FMOD_FILE_ASYNCDONE)(
  struct FMOD_ASYNCREADINFO *info,
  FMOD_RESULT result
);

typedef struct FMOD_ASYNCREADINFO {
  void *handle;
  unsigned int offset;
  unsigned int sizebytes;
  int priority;
  void *userdata;
  void *buffer;
  unsigned int bytesread;
  FMOD_FILE_ASYNCDONE done;
} FMOD_ASYNCREADINFO;

typedef FMOD_RESULT F_CALLBACK FMOD_FILE_ASYNCREAD_CALLBACK(
  FMOD_ASYNCREADINFO *info,
  void *userdata
);

typedef FMOD_RESULT F_CALLBACK FMOD_FILE_ASYNCCANCEL_CALLBACK(
  FMOD_ASYNCREADINFO *info,
  void *userdata
);

namespace FMOD {

class System {
public:
	FMOD_RESULT setFileSystem(FMOD_FILE_OPEN_CALLBACK useropen,
		FMOD_FILE_CLOSE_CALLBACK userclose,
		FMOD_FILE_READ_CALLBACK userread,
		FMOD_FILE_SEEK_CALLBACK userseek,
		FMOD_FILE_ASYNCREAD_CALLBACK userasyncread,
		FMOD_FILE_ASYNCCANCEL_CALLBACK userasynccancel,
		int blockalign);
	FMOD_RESULT init(int maxchannels, FMOD_INITFLAGS flags, void *extradriverdata);
};

} // namespace
