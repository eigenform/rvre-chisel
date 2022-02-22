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
  val ENC_ILL = Value(0.U)
  val ENC_R   = Value(1.U)
  val ENC_I   = Value(2.U)
  val ENC_S   = Value(3.U)
  val ENC_B   = Value(4.U)
  val ENC_U   = Value(5.U)
  val ENC_J   = Value(6.U)
}

// Set of execution units in the machine.
object ExecutionUnit extends ChiselEnum {
  val EU_ILL = Value(0.U)
  val EU_ALU = Value(1.U) // Arithemetic/logic unit
  val EU_LSU = Value(2.U) // Load/store unit
  val EU_BCU = Value(3.U) // Branch comparison unit (?)
}

// ALU operations
object ALUOp extends ChiselEnum {
  val ALU_ILL  = Value(0.U)
  val ALU_ADD  = Value(1.U)
  val ALU_SUB  = Value(2.U)
  val ALU_SLL  = Value(3.U)
  val ALU_SLT  = Value(4.U)
  val ALU_SLTU = Value(5.U)
  val ALU_XOR  = Value(6.U)
  val ALU_SRL  = Value(7.U)
  val ALU_SRA  = Value(8.U)
  val ALU_OR   = Value(9.U)
  val ALU_AND  = Value(10.U)
}

object BCUOp extends ChiselEnum {
  val BCU_ILL  = Value(0.U)
  val BCU_EQ   = Value(1.U)
  val BCU_NEQ  = Value(2.U)
  val BCU_LT   = Value(3.U)
  val BCU_LTU  = Value(4.U)
  val BCU_GE   = Value(5.U)
  val BCU_GEU  = Value(6.U)
}

object LSUOp extends ChiselEnum {
  val LSU_ILL  = Value(0.U)
  val LSU_LB   = Value(1.U)
  val LSU_LH   = Value(2.U)
  val LSU_LW   = Value(3.U)
  val LSU_LBU  = Value(4.U)
  val LSU_LHU  = Value(5.U)
  val LSU_SB   = Value(6.U)
  val LSU_SH   = Value(7.U)
  val LSU_SW   = Value(8.U)
}


object ControlFlowOp extends ChiselEnum {
  val CF_SEQ = Value(0.U)
  val CF_BR  = Value(1.U)
}

class ControlFlowBundle extends RVREBundle {
  val op  = Output(ControlFlowOp())
  val tgt = Output(UInt(XLEN.W))
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
  val op     = Output(BCUOp())
  val x      = Output(UInt(XLEN.W))
  val y      = Output(UInt(XLEN.W))
  val pc     = Output(UInt(XLEN.W))
  val pc_off = Output(UInt(XLEN.W))
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
  val data  = Output(UInt(XLEN.W))
  val store = Output(Bool())
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


