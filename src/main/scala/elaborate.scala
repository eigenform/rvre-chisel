package rvre.elaborate

import chisel3._
import chisel3.util._
import rvre.uarch._

// Emit the verilog/firrtl for various modules.
// Writes to a 'verilog/' directory in the project root.
//
// You can run these with 'sbt run'.
object VerilogEmitter extends App {

  println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
  println("!! Microarchitectural Parameters                   ")
  println("!!   Micro-op size: " + (new Uop).getWidth + " bits")
  println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")

  val emitter_args = Array("-td", "verilog")
  (new chisel3.stage.ChiselStage)
    .emitVerilog(new rvre.core.RVREHart, emitter_args)

  (new chisel3.stage.ChiselStage)
    .emitVerilog(new rvre.soc.RVRESoc, emitter_args)
  //(new chisel3.stage.ChiselStage)
  //  .emitVerilog(new rvre.soc.IROMWrapper("irom/test.mem"), emitter_args)
  //(new chisel3.stage.ChiselStage)
  //  .emitVerilog(new rvre.soc.RAMWrapper, emitter_args)



}


