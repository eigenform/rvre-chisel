.PHONY: clean verilog
clean:
	rm verilog/*

verilog:
	#sbt run
	sbt 'runMain rvre.elab.VerilogEmitter'
