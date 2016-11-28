// reexport C2/D2 versions of functions as C1/D1
extern void _ZN16ResourceLocationC2ERKSs(void* a, void* b);
void _ZN16ResourceLocationC1ERKSs(void* a, void* b) {
	_ZN16ResourceLocationC2ERKSs(a, b);
}

extern void _ZN16ResourceLocationC2Ev(void* a);
void _ZN16ResourceLocationC1Ev(void* a) {
	_ZN16ResourceLocationC2Ev(a);
}

extern void _ZN16ResourceLocationD2Ev(void* a);
void _ZN16ResourceLocationD1Ev(void* a) {
	_ZN16ResourceLocationD2Ev(a);
}

extern void _ZN4Json6ReaderD2Ev(void* a);
void _ZN4Json6ReaderD1Ev(void* a) {
	_ZN4Json6ReaderD2Ev(a);
}
