
package rvre.core

import chisel3._
import chisel3.util._

import rvre.uarch._
import rvre.bus._

class FetchUnit extends RVREModule {
  val io = IO(new Bundle {
    val npc_in = Input(Valid(UInt(XLEN.W)))
    val bus    = RVREBusPort.source()
    val out    = Decoupled(new FetchBundle)
  })

  val pc      = RegInit(0.U(XLEN.W))
  val npc_seq = pc + 4.U
  val npc     = Mux(io.npc_in.valid, io.npc_in.bits, npc_seq)

  val pc_wren = io.npc_in.valid || io.bus.req.fire
  when (pc_wren) { 
    printf("FetchUnit: pc=%x -> pc=%x\n", pc, npc)
    pc := npc 
  }

  io.bus.req.bits.addr     := pc
  io.bus.req.bits.mask     := "b1111".U
  io.bus.req.bits.st_en    := false.B
  io.bus.req.bits.st_data  := 0.U

  when (io.bus.req.fire) {
    val req = io.bus.req
    printf("FetchUnit: req addr=%x\n", io.bus.req.bits.addr)
  }

  io.bus.req.valid  := io.out.ready
  io.bus.resp.ready := io.out.ready
  io.out.bits.inst  := io.bus.resp.bits.data
  io.out.bits.err   := io.bus.resp.bits.err
  io.out.bits.pc    := pc
  io.out.valid      := io.bus.resp.valid


}


