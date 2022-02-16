package rvre.core

import chisel3._
import chisel3.util._
import rvre.uarch._
import rvre.uarch.ALUOp._

class ArithmeticLogicUnit extends RVREModule {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new ALUPacket))
    val out = Output(Valid(UInt(XLEN.W)))
  })
  val op = io.in.bits.op
  val x = io.in.bits.x
  val y = io.in.bits.y
  val shamt = y(4, 0).asUInt

  io.in.ready := true.B
  io.out.valid := io.in.fire
  io.out.bits := 0.U
  switch (op) {
    is (ALU_ILL)  { io.out.bits := 0.U }
    is (ALU_AND)  { io.out.bits := x & y }
    is (ALU_OR)   { io.out.bits := x | y }
    is (ALU_ADD)  { io.out.bits := x + y }
    is (ALU_SUB)  { io.out.bits := x - y }
    is (ALU_SRA)  { io.out.bits := (x.asSInt >> shamt).asUInt }
    is (ALU_SLTU) { io.out.bits := x < y }
    is (ALU_XOR)  { io.out.bits := x ^ y }
    is (ALU_SRL)  { io.out.bits := x >> shamt }
    is (ALU_SLT)  { io.out.bits := (x.asSInt < y.asSInt).asUInt }
    is (ALU_SLL)  { io.out.bits := x << shamt }
  }
}


