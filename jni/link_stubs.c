// reexport C2/D2 versions of functions as C1/D1

extern void* _ZN16ResourceLocationD2Ev(void* a);
void* _ZN16ResourceLocationD1Ev(void* a) {
	return _ZN16ResourceLocationD2Ev(a);
}

extern void* _ZN4Json6ReaderD2Ev(void* a);
void* _ZN4Json6ReaderD1Ev(void* a) {
	return _ZN4Json6ReaderD2Ev(a);
}

extern void* _ZN12ItemInstanceD2Ev(void* a);
void* _ZN12ItemInstanceD1Ev(void* a) {
	return _ZN12ItemInstanceD2Ev(a);
}

/*
extern void* _ZN9ItemStackD2Ev(void* a);
void* _ZN9ItemStackD1Ev(void* a) {
	return _ZN9ItemStackD2Ev(a);
}
*/