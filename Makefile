.PHONY: irom test clean verilog
irom:
	make -C irom/
test:
	sbt test
clean:
	rm verilog/*
verilog:
	sbt 'runMain rvre.elaborate.VerilogEmitter'
