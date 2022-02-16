PREFIX 	:= riscv32-unknown-elf
CC 		:= $(PREFIX)-gcc
AS 		:= $(PREFIX)-as
OBJDUMP := $(PREFIX)-objdump
OBJCOPY := $(PREFIX)-objcopy

all:
	$(AS) -march=rv32i test.s -o test.o
	$(OBJCOPY) -O binary -j .text test.o test.bin
	$(OBJCOPY) -O ihex -j .text test.o test.hex
	hexdump -ve '1/4 "%08x\n"' test.bin > test.mem
dis:
	$(OBJDUMP) -Mnumeric -d test.o
clean:
	rm -rvf test.bin test.o
