package rvre.core

import chisel3._
import chisel3.util._
import rvre.uarch._
import rvre.uarch.BCUOp._

class BranchComparisonUnit extends RVREModule {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new BCUPacket))
    val out = Output(Valid(new BCUResult))
  })

  val x  = io.in.bits.x
  val y  = io.in.bits.y
  val op = io.in.bits.op
  val pc = io.in.bits.pc

  io.out.bits.taken := false.B
  io.in.ready       := true.B
  io.out.valid      := io.in.fire
  io.out.bits.pc    := pc

  switch (op) {
    is (BCU_EQ)  { io.out.bits.taken := (x === y) }
    is (BCU_NEQ) { io.out.bits.taken := (x =/= y) }
    is (BCU_LT)  { io.out.bits.taken := (x.asSInt < y.asSInt) }
    is (BCU_LTU) { io.out.bits.taken := (x < y) }
    is (BCU_GE)  { io.out.bits.taken := (x.asSInt > y.asSInt) }
    is (BCU_GEU) { io.out.bits.taken := (x > y) }
  }
}

