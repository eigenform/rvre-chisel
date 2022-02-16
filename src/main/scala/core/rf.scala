package rvre.core

import chisel3._
import chisel3.util._
import chisel3.experimental.{annotate, ChiselAnnotation}
import firrtl.annotations.MemorySynthInit

import rvre.uarch._

class RFReadPortBundle extends RVREBundle {
    val addr = Output(UInt(AREG_W))
    val data = Input(UInt(XLEN.W))
}
class RFWritePortBundle extends RVREBundle {
    val addr = Output(UInt(AREG_W))
    val data = Output(UInt(XLEN.W))
}

class RegisterFile extends RVREModule {
  val io = IO(new Bundle {
    val rp = Flipped(Vec(2, new RFReadPortBundle))
    val wp = Flipped(Vec(1, Valid(new RFWritePortBundle)))
  })

  annotate(new ChiselAnnotation { override def toFirrtl = MemorySynthInit })
  val rf = SyncReadMem(32, UInt(XLEN.W))
  for (rp <- io.rp) {
    when (rp.addr === 0.U) {
      rp.data := 0.U
    } .otherwise {
      rp.data := rf(rp.addr)
    }
  }
  for (wp <- io.wp) {
    when (wp.valid) {
      rf(wp.bits.addr) := wp.bits.data
    }
  }
}
