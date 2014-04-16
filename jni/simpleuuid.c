#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include "simpleuuid.h"

static void read_random(unsigned char* ptr, size_t count) {
	int devURandom = open("/dev/urandom", O_RDONLY);
	read(devURandom, ptr, count);
	close(devURandom);
}

void
uuid_generate_random(uuid_t out) {
	read_random(out, sizeof(uuid_t));

	out[6] = (out[6] & 0x0F) | 0x40;
	out[8] = (out[8] & 0x3F) | 0x80;
}

