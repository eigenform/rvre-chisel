
start:
	li  x1, 0x10000000
	li  x2, 0x20000000
	add x3, x1, x2
	add x4, x2, x2
	add x7, x3, x4
	sw  x7, 0x20(x0)
	lw  x8, 0x20(x0)

	
