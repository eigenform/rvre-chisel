package rvre.core

import chisel3._
import chisel3.util._
import rvre.uarch._

class RetireUnit extends RVREModule {
  val io = IO(new Bundle {
    val uop     = Flipped(Decoupled(new DecodeBundle))
    val alu_res = Flipped(Valid(UInt(XLEN.W)))
    val bcu_res = Flipped(Valid(new BCUResult))
    val lsu_res = Flipped(Valid(new LSUResult))

    val rf_wp   = Vec(1, Valid(new RFWritePortBundle))
    val cf_out  = Output(Valid(new ControlFlowBundle))
  })

  io.uop.ready := true.B

  // Operations are complete when the 'valid' bit is asserted
  val alu_comp = io.uop.valid && io.alu_res.valid
  val lsu_comp = io.uop.valid && io.lsu_res.valid
  val bcu_comp = io.uop.valid && io.bcu_res.valid

  // Data-flow:
  // send result data to register file write port
  val rd_en  = io.uop.bits.rd_en
  val alu_wb = alu_comp && rd_en
  val lsu_wb = lsu_comp && rd_en && ~io.lsu_res.bits.store
  io.rf_wp(0).valid     := alu_wb || lsu_wb
  io.rf_wp(0).bits.addr := io.uop.bits.rd
  io.rf_wp(0).bits.data := MuxCase(0.U, List(
    alu_wb -> io.alu_res.bits,
    lsu_wb -> io.lsu_res.bits.data,
  ))

  when (alu_wb) {
    printf("Retire: alu_wb rd=%d <- %x\n", 
      io.uop.bits.rd, io.alu_res.bits)
  }
  when (lsu_wb) {
    printf("Retire: lsu_wb rd=%d <- %x\n", 
      io.uop.bits.rd, io.lsu_res.bits.data)
  }

  // Control-flow: 
  // Increment the program counter only when an instruction has completed.
  //assert( alu_comp ^ lsu_comp ^ bcu_comp )
  val npc_wb          = bcu_comp && io.bcu_res.bits.taken
  io.cf_out.valid    := alu_comp || lsu_comp || bcu_comp
  io.cf_out.bits.tgt := Mux(npc_wb, io.bcu_res.bits.pc, 0.U)
  io.cf_out.bits.op  := Mux(npc_wb, 
    ControlFlowOp.CF_BR, ControlFlowOp.CF_SEQ
  )

  when (io.cf_out.valid) {
    val cf = io.cf_out.bits
    printf("Retire: cf_out tgt=%x\n", cf.tgt)
  } .otherwise {
    printf("Retire: no operations retired\n")
  }

}
