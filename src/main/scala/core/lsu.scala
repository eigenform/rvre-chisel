package rvre.core

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import rvre.uarch._
import rvre.uarch.LSUOp._

class LoadStoreUnit extends RVREModule {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new LSUPacket))
    val out = Output(Valid(new LSUResult))
    val bus = rvre.bus.RVREBusPort.source()
  })

  object State extends ChiselEnum { 
    val IDLE, BUSY = Value
  }

  val op     = io.in.bits.op
  val addr   = io.in.bits.base + io.in.bits.off
  val store  = ( (op === LSU_SB) || (op === LSU_SH) || (op === LSU_SW) )
  val signed = Mux(((op === LSU_LB) || (op === LSU_LH)), true.B, false.B)

  val bytes = Mux1H(Seq(
    ((op === LSU_SB) || (op === LSU_LB) || (op === LSU_LBU)) -> "b00".U,
    ((op === LSU_SH) || (op === LSU_LH) || (op === LSU_LHU)) -> "b01".U,
    ((op === LSU_SW) || (op === LSU_LW)) -> "b10".U,
  ))

  // Using a register here means we cannot complete load/store requests
  // asynchronously (the latency is at least one clock cycle).
  val state = RegInit(State.IDLE)
  switch (state) {
    is (State.IDLE) {
      when (io.bus.req.fire) {
        printf("LSU: bus req addr=%x bytes=%b signed=%b st_en=%b st_data=%x\n",
          io.bus.req.bits.addr, io.bus.req.bits.bytes, io.bus.req.bits.signed,
          io.bus.req.bits.st_en, io.bus.req.bits.st_data)
        state := State.BUSY
      }
    }
    is (State.BUSY) {
      when (io.bus.resp.fire) {
        printf("LSU: bus resp data=%x err=%b\n",
          io.bus.resp.bits.data, io.bus.resp.bits.err)
        state := State.IDLE
      }
    }
  }

  io.bus.req.bits.addr    := addr
  io.bus.req.bits.bytes   := bytes
  io.bus.req.bits.signed  := signed
  io.bus.req.bits.st_en   := store
  io.bus.req.bits.st_data := Mux(store, io.in.bits.data, 0.U)

  io.out.valid      := io.bus.resp.fire
  io.in.ready       := state === State.IDLE
  io.bus.req.valid  := io.in.valid
  io.bus.resp.ready := true.B

  io.out.bits.data  := io.bus.resp.bits.data
  io.out.bits.store := store

  when (io.in.fire) {
    assert(io.in.bits.op =/= LSU_ILL)
    printf("LSU: packet op=%d base=%x off=%x data=%x\n", 
      io.in.bits.op.asUInt, io.in.bits.base, io.in.bits.off, io.in.bits.data)
  }
}

