package rvre.uarch

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import chisel3.internal.firrtl.Width

// This trait carries constants for parameterizing the design.
trait RVREParams {
  val XLEN: Int = 32
  val ILEN: Int = 32
  val NUM_ARCH_REG: Int = 32

  val AREG_W: Width = log2Ceil(NUM_ARCH_REG).W
  def ArchReg() = UInt(AREG_W)
}

// Easier access to constants in modules/bundles with [RVREParams].
abstract class RVREBundle extends Bundle with RVREParams
abstract class RVREModule extends Module with RVREParams

// Set of RISC-V instruction encodings.
object InstEnc extends ChiselEnum {
  val ENC_ILL = Value
  val ENC_R   = Value
  val ENC_I   = Value
  val ENC_S   = Value
  val ENC_B   = Value
  val ENC_U   = Value
  val ENC_J   = Value
}

// Set of execution units in the machine.
object ExecutionUnit extends ChiselEnum {
  val EU_ILL = Value
  val EU_ALU = Value // Arithemetic/logic unit
  val EU_LSU = Value // Load/store unit
  val EU_BCU = Value // Branch comparison unit (?)
}

// ALU operations
object ALUOp extends ChiselEnum {
  val ALU_ILL  = Value
  val ALU_ADD  = Value
  val ALU_SUB  = Value
  val ALU_SLL  = Value
  val ALU_SLT  = Value
  val ALU_SLTU = Value
  val ALU_XOR  = Value
  val ALU_SRL  = Value
  val ALU_SRA  = Value
  val ALU_OR   = Value
  val ALU_AND  = Value
}

object BCUOp extends ChiselEnum {
  val BCU_ILL  = Value
  val BCU_EQ   = Value
  val BCU_NEQ  = Value
  val BCU_LT   = Value
  val BCU_LTU  = Value
  val BCU_GE   = Value
  val BCU_GEU  = Value
}

object LSUOp extends ChiselEnum {
  val LSU_ILL  = Value
  val LSU_LB   = Value
  val LSU_LH   = Value
  val LSU_LW   = Value
  val LSU_SB   = Value
  val LSU_SH   = Value
  val LSU_SW   = Value
}

class FetchBundle extends RVREBundle {
  val inst = Output(UInt(ILEN.W))
  val pc   = Output(UInt(XLEN.W))
  val err  = Output(Bool())
}

class ALUPacket extends RVREBundle {
  val op   = Output(ALUOp())
  val x    = Output(UInt(XLEN.W))
  val y    = Output(UInt(XLEN.W))
}

class BCUPacket extends RVREBundle {
  val op   = Output(BCUOp())
  val x    = Output(UInt(XLEN.W))
  val y    = Output(UInt(XLEN.W))
  val pc   = Output(UInt(XLEN.W))
}

class LSUPacket extends RVREBundle {
  val op   = Output(LSUOp())
  val base = Output(UInt(XLEN.W))
  val off  = Output(UInt(XLEN.W))
  val data = Output(UInt(XLEN.W))
}

class ALUResult extends RVREBundle {
  val res = Output(UInt(XLEN.W))
}
class LSUResult extends RVREBundle {
  val res = Output(UInt(XLEN.W))
}
class BCUResult extends RVREBundle {
  val taken = Output(Bool())
  val pc    = Output(UInt(XLEN.W))
}


object AccessWidth extends ChiselEnum {
  val BYTE, HALF, WORD = Value
}

// Output from the decode unit.
class DecodeBundle extends RVREBundle {
  val ill    = Output(Bool())
  val enc    = Output(InstEnc())
  val eu     = Output(ExecutionUnit())
  val alu_op = Output(ALUOp())
  val lsu_op = Output(LSUOp())
  val bcu_op = Output(BCUOp())
  val rd_en  = Output(Bool())
  val imm_en = Output(Bool())
  val pc_en  = Output(Bool())
  val rd     = Output(ArchReg())
  val rs1    = Output(ArchReg())
  val rs2    = Output(ArchReg())
  val imm    = Output(SInt(XLEN.W))
  val pc     = Output(UInt(XLEN.W))
}

class Uop extends RVREBundle {
  val ctrl = new DecodeBundle // Output from decode unit
}


