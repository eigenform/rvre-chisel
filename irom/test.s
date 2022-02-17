
.section .text
_start:
	la    x1, my_data
	lw    x2, 0x0(x1)
	lw    x3, 0x4(x1)
	lw    x4, 0x8(x1)
	lw    x5, 0xc(x1)

	#li  x1, 0x10000000
	#li  x2, 0x20000000
	#add x3, x1, x2
	#add x4, x2, x2
	#add x7, x3, x4
	#sw  x7, 0x20(x0)
	#lw  x8, 0x20(x0)

.section .data
my_data:
	.long 0x0000dead, 0x0001dead, 0x0002dead, 0x0003dead
