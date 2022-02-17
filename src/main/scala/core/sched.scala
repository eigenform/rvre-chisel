package rvre.core

import chisel3._
import chisel3.util._
import rvre.uarch._

import rvre.core.{RFReadPortBundle}

class ScheduleUnit extends RVREModule {
  val io = IO(new Bundle {
    val in      = Flipped(Decoupled(new DecodeBundle))
    val out_alu = Decoupled(new ALUPacket)
    val out_bcu = Decoupled(new BCUPacket)
    val out_lsu = Decoupled(new LSUPacket)

    val rf_rp   = Vec(2, new RFReadPortBundle)
  })

  io.rf_rp(0).addr := io.in.bits.rs1
  io.rf_rp(1).addr := io.in.bits.rs2

  io.out_alu.bits.op := io.in.bits.alu_op
  io.out_alu.bits.x  := Mux(io.in.bits.pc_en, 
    io.in.bits.pc, io.rf_rp(0).data
  )
  io.out_alu.bits.y  := Mux(io.in.bits.imm_en,
    io.in.bits.imm.asUInt, io.rf_rp(1).data
  )

  io.out_bcu.bits.op := io.in.bits.bcu_op
  io.out_bcu.bits.x  := io.rf_rp(0).data
  io.out_bcu.bits.y  := io.rf_rp(1).data
  io.out_bcu.bits.pc := io.in.bits.pc

  io.out_lsu.bits.op   := io.in.bits.lsu_op
  io.out_lsu.bits.base := io.rf_rp(0).data
  io.out_lsu.bits.off  := io.in.bits.imm.asUInt
  io.out_lsu.bits.data := io.rf_rp(1).data

  io.out_alu.valid := (io.in.bits.eu === ExecutionUnit.EU_ALU)
  io.out_lsu.valid := (io.in.bits.eu === ExecutionUnit.EU_LSU)
  io.out_bcu.valid := (io.in.bits.eu === ExecutionUnit.EU_BCU)

  io.in.ready := false.B
  io.in.ready := Mux1H(Seq(
    io.out_alu.valid -> io.out_alu.ready,
    io.out_lsu.valid -> io.out_lsu.ready,
    io.out_bcu.valid -> io.out_bcu.ready,
  ))

  when (io.in.fire) {
    printf("Sched: out_alu=%b out_lsu=%b out_bcu=%b rp0(%d)=%x rp1(%d)=%x\n",
      io.out_alu.valid, io.out_lsu.valid, io.out_bcu.valid,
      io.in.bits.rs1, io.rf_rp(0).data,
      io.in.bits.rs2, io.rf_rp(1).data,
    )
  }

}
