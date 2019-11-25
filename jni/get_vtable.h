#pragma once
static void** getVtable(void* obj) {
	return *((void***)obj);
}
