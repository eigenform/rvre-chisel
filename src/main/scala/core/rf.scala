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
    val rp = Vec(2, Flipped(new RFReadPortBundle))
    val wp = Vec(1, Flipped(Valid(new RFWritePortBundle)))
  })

  annotate(new ChiselAnnotation { override def toFirrtl = MemorySynthInit })
  val rf = Reg(Vec(32, UInt(XLEN.W)))

  printf("RF: x1=%x x2=%x x3=%x x4=%x\n", rf(1), rf(2), rf(3), rf(4))
  printf("RF: x5=%x x6=%x x7=%x x8=%x\n", rf(5), rf(6), rf(7), rf(8))

  // Generate hardware for read ports
  for (rp <- io.rp) {
    // Add bypasses from each read port to all write ports
    for (wp <- io.wp) {
      when ( wp.valid && (wp.bits.addr === rp.addr) ) {
        rp.data := wp.bits.data
      }
    }
    when (rp.addr === 0.U) {
      rp.data := 0.U
    } .otherwise {
      rp.data := rf(rp.addr)
    }
  }

  // Generate hardware for write ports
  for (wp <- io.wp) {
    when (wp.valid) {
      rf(wp.bits.addr) := wp.bits.data
    }
  }

}
