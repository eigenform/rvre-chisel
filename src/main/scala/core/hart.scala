package rvre.core

import chisel3._
import chisel3.util._

import rvre.uarch._
import rvre.bus._

class RVFI extends RVREBundle {
  val valid    = Bool()
  val insn     = UInt(ILEN.W)
  val trap     = Bool()
  val order    = UInt(64.W)
  val halt     = Bool()
  val intr     = Bool()
  val rd_addr  = ArchReg()
  val rs1_addr = ArchReg()
  val rs2_addr = ArchReg()
  val rd_wdata = UInt(XLEN.W)
  val rs1_data = UInt(XLEN.W)
  val rs2_data = UInt(XLEN.W)
  val pc_rdata = UInt(XLEN.W)
  val pc_wdata = UInt(XLEN.W)
}

class RVREHart extends RVREModule {
  val io = IO(new Bundle {
    val ibus = RVREBusPort.source()
    val dbus = RVREBusPort.source()
  })

  val fetch  = Module(new rvre.core.FetchUnit)
  val decode = Module(new rvre.core.DecodeUnit)
  val sched  = Module(new rvre.core.ScheduleUnit)
  val retire = Module(new rvre.core.RetireUnit)
  val rf     = Module(new rvre.core.RegisterFile)
  val alu    = Module(new rvre.core.ArithmeticLogicUnit)
  val bcu    = Module(new rvre.core.BranchComparisonUnit)
  val lsu    = Module(new rvre.core.LoadStoreUnit)

  fetch.io.bus      <> io.ibus
  fetch.io.npc_in   <> retire.io.npc_out
  decode.io.in      <> fetch.io.out
  sched.io.in       <> decode.io.out
  rf.io.rp          <> sched.io.rf_rp
  rf.io.wp          <> retire.io.rf_wp
  alu.io.in         <> sched.io.out_alu
  bcu.io.in         <> sched.io.out_bcu
  lsu.io.in         <> sched.io.out_lsu
  lsu.io.bus        <> io.dbus
  retire.io.uop     <> decode.io.out
  retire.io.alu_res <> alu.io.out
  retire.io.bcu_res <> bcu.io.out
  retire.io.lsu_res <> lsu.io.out

}

