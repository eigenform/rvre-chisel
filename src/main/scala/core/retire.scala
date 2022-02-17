package rvre.core

import chisel3._
import chisel3.util._
import rvre.uarch._

class RetireUnit extends RVREModule {
  val io = IO(new Bundle {
    val uop     = Flipped(Decoupled(new DecodeBundle))
    val alu_res = Flipped(Valid(UInt(XLEN.W)))
    val bcu_res = Flipped(Valid(new BCUResult))
    val lsu_res = Flipped(Valid(UInt(XLEN.W)))

    val rf_wp = Vec(1, Valid(new RFWritePortBundle))
    val npc_out = Output(Valid(UInt(XLEN.W)))
  })

  io.uop.ready := true.B

  val rd_en = io.uop.bits.rd_en

  val alu_wb = io.uop.valid && io.alu_res.valid && rd_en
  val lsu_wb = io.uop.valid && io.lsu_res.valid && rd_en
  val npc_wb = io.uop.valid && io.bcu_res.valid && io.bcu_res.bits.taken

  io.rf_wp(0).valid     := io.alu_res.valid || io.lsu_res.valid
  io.rf_wp(0).bits.addr := io.uop.bits.rd
  io.rf_wp(0).bits.data := MuxCase(0.U, List(
    alu_wb -> io.alu_res.bits,
    lsu_wb -> io.lsu_res.bits,
  ))

  io.npc_out.valid := npc_wb
  io.npc_out.bits  := io.bcu_res.bits.pc

  when (alu_wb) {
    printf("Retire: alu_wb rd=%d <- %x\n", io.uop.bits.rd, io.alu_res.bits)
  }
  when (lsu_wb) {
    printf("Retire: lsu_wb rd=%d <- %x\n", io.uop.bits.rd, io.lsu_res.bits)
  }
  when (npc_wb) {
    printf("Retire: npc_wb pc=%x\n", io.bcu_res.bits.pc)
  }
  when (~alu_wb && ~lsu_wb && ~npc_wb) {
    printf("Retire: nothing to retire\n")
  }


}
