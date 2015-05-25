	.arm
	.cpu cortex-a9
	.fpu softvfp
	.eabi_attribute 20, 1
	.eabi_attribute 21, 1
	.eabi_attribute 23, 3
	.eabi_attribute 24, 1
	.eabi_attribute 25, 1
	.eabi_attribute 26, 2
	.eabi_attribute 30, 2
	.eabi_attribute 18, 4
	.file	"tamperpanic.c"
	.text
	.align	2
	.global	bl_panicTamper
	.hidden	bl_panicTamper
	.type	bl_panicTamper, %function
bl_panicTamper:
	@ args = 0, pretend = 0, frame = 0
	@ frame_needed = 0, uses_anonymous_args = 0
	@ link register save eliminated.
#APP
@ 3 "tamperpanic.c" 1
	mov lr, #0
	mov sp, #0
@ 0 "" 2
	bx	lr
	.size	bl_panicTamper, .-bl_panicTamper
	.section	.note.GNU-stack,"",%progbits
