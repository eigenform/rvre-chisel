.PHONY: clean verilog
clean:
	rm verilog/*
test:
	sbt test
verilog:
	sbt 'runMain rvre.elaborate.VerilogEmitter'
