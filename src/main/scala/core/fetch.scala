
package rvre.core

import chisel3._
import chisel3.util._

import rvre.uarch._
import rvre.bus._

class FetchUnit extends RVREModule {
  val io = IO(new Bundle {
    val cf_in  = Input(Valid(new ControlFlowBundle))
    val out    = Decoupled(new FetchBundle)
    val bus    = RVREBusPort.source()
  })

  val pc      = RegInit(0.U(XLEN.W))
  val npc_seq = pc + 4.U
  val npc     = MuxCase(npc_seq, Array(
    (io.cf_in.bits.op === ControlFlowOp.CF_SEQ) -> npc_seq, 
    (io.cf_in.bits.op === ControlFlowOp.CF_BR ) -> io.cf_in.bits.tgt, 
  ))

  when (io.cf_in.valid) {
    printf("FetchUnit: pc=%x -> pc=%x\n", pc, npc)
    pc := npc 
  }

  io.bus.req.bits.addr     := pc
  io.bus.req.bits.bytes    := "b10".U
  io.bus.req.bits.signed   := false.B
  io.bus.req.bits.st_en    := false.B
  io.bus.req.bits.st_data  := 0.U
  when (io.bus.req.fire) {
    printf("FetchUnit: req addr=%x\n", io.bus.req.bits.addr)
  }

  io.bus.req.valid  := io.out.ready && io.cf_in.valid
  io.bus.resp.ready := io.out.ready && io.cf_in.valid
  io.out.bits.inst  := io.bus.resp.bits.data
  io.out.bits.err   := io.bus.resp.bits.err
  io.out.bits.pc    := pc
  io.out.valid      := io.bus.resp.valid


}


