package rvre.core

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import rvre.uarch._
import rvre.uarch.LSUOp._

class LoadStoreUnit extends RVREModule {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new LSUPacket))
    val out = Output(Valid(UInt(XLEN.W)))
    val bus = rvre.bus.RVREBusPort.source()
  })

  object State extends ChiselEnum { 
    val IDLE, BUSY = Value
  }

  val state = RegInit(State.IDLE)
  val addr  = io.in.bits.base + io.in.bits.off
  val store = ( (io.in.bits.op === LSUOp.LSU_SB) 
    || (io.in.bits.op === LSUOp.LSU_SH) 
    || (io.in.bits.op === LSUOp.LSU_SW)
  )
  val mask = Mux1H(Seq(
    ((io.in.bits.op === LSU_SB) || (io.in.bits.op === LSU_LB)) -> "b0001".U,
    ((io.in.bits.op === LSU_SH) || (io.in.bits.op === LSU_LH)) -> "b0011".U,
    ((io.in.bits.op === LSU_SW) || (io.in.bits.op === LSU_LW)) -> "b1111".U,
  ))

  switch (state) {
    is (State.IDLE) {
      when (io.bus.req.fire) {
        state := State.BUSY
      }
    }
    is (State.BUSY) {
      when (io.bus.resp.fire) {
        state := State.IDLE
      }
    }
  }

  io.bus.req.bits.addr    := addr
  io.bus.req.bits.mask    := mask
  io.bus.req.bits.st_en   := store
  io.bus.req.bits.st_data := io.in.bits.data

  io.out.valid      := io.bus.resp.fire
  io.in.ready       := state === State.IDLE
  io.bus.req.valid  := io.in.valid
  io.bus.resp.ready := true.B

  io.out.bits       := io.bus.resp.bits.data

}
