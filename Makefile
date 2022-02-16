.PHONY: clean verilog
clean:
	rm verilog/*
verilog:
	sbt 'runMain rvre.elaborate.VerilogEmitter'
