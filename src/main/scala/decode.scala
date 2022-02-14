package rvre.decode

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import chisel3.util.experimental.decode

import rvre.uarch._
import rvre.uarch.ExecutionUnit._
import rvre.uarch.ALUOp._
import rvre.uarch.BCUOp._
import rvre.uarch.LSUOp._
import rvre.uarch.InstEnc._


// Map from matching instruction patterns to sets of control signals.
object DecoderTable {
  import rvre.isa.Instructions._

  // NOTE: The layout of the table has to match this bundle because we're
  // intending to cast the decoder output with .asTypeOf()
  class CtrlBundle extends Bundle {
    val eu     = ExecutionUnit()
    val enc    = InstEnc()
    val ill    = Bool()
    val rd_en  = Bool()
    val imm_en = Bool()
    var alu_op = ALUOp()
    var bcu_op = BCUOp()
    var lsu_op = LSUOp()
  }

  // Makes this table a bit less ugly
  def lit[T <: Data](n: T): UInt     = { n.litValue.U((n.getWidth).W) }
  def e[T <: Data](n: T): BitPat = { BitPat(lit(n)) }
  val N = BitPat.N()
  val Y = BitPat.Y()

  val matches = Array(
    BEQ     -> e(EU_BCU) ## e(ENC_B)   ## N ## N ## N ## e(ALU_ILL)  ## e(BCU_EQ)  ## e(LSU_ILL),   
    BNE     -> e(EU_BCU) ## e(ENC_B)   ## N ## N ## N ## e(ALU_ILL)  ## e(BCU_NEQ) ## e(LSU_ILL),
    BLT     -> e(EU_BCU) ## e(ENC_B)   ## N ## N ## N ## e(ALU_ILL)  ## e(BCU_LT)  ## e(LSU_ILL),
    BGE     -> e(EU_BCU) ## e(ENC_B)   ## N ## N ## N ## e(ALU_ILL)  ## e(BCU_GE)  ## e(LSU_ILL),
    BLTU    -> e(EU_BCU) ## e(ENC_B)   ## N ## N ## N ## e(ALU_ILL)  ## e(BCU_LTU) ## e(LSU_ILL),
    BGEU    -> e(EU_BCU) ## e(ENC_B)   ## N ## N ## N ## e(ALU_ILL)  ## e(BCU_GEU) ## e(LSU_ILL),
    JALR    -> e(EU_BCU) ## e(ENC_I)   ## N ## N ## N ## e(ALU_ILL)  ## e(BCU_ILL) ## e(LSU_ILL),
    JAL     -> e(EU_BCU) ## e(ENC_J)   ## N ## N ## N ## e(ALU_ILL)  ## e(BCU_ILL) ## e(LSU_ILL),
    LUI     -> e(EU_ALU) ## e(ENC_U)   ## N ## N ## N ## e(ALU_ILL)  ## e(BCU_ILL) ## e(LSU_ILL),
    AUIPC   -> e(EU_ALU) ## e(ENC_U)   ## N ## N ## N ## e(ALU_ILL)  ## e(BCU_ILL) ## e(LSU_ILL),
    ADDI    -> e(EU_ALU) ## e(ENC_I)   ## N ## Y ## Y ## e(ALU_ADD)  ## e(BCU_ILL) ## e(LSU_ILL),
    SLTI    -> e(EU_ALU) ## e(ENC_I)   ## N ## Y ## Y ## e(ALU_SLT)  ## e(BCU_ILL) ## e(LSU_ILL),
    SLTIU   -> e(EU_ALU) ## e(ENC_I)   ## N ## Y ## Y ## e(ALU_SLTU) ## e(BCU_ILL) ## e(LSU_ILL),
    XORI    -> e(EU_ALU) ## e(ENC_I)   ## N ## Y ## Y ## e(ALU_XOR)  ## e(BCU_ILL) ## e(LSU_ILL),
    ORI     -> e(EU_ALU) ## e(ENC_I)   ## N ## Y ## Y ## e(ALU_OR)   ## e(BCU_ILL) ## e(LSU_ILL),
    ANDI    -> e(EU_ALU) ## e(ENC_I)   ## N ## Y ## Y ## e(ALU_AND)  ## e(BCU_ILL) ## e(LSU_ILL),
    ADD     -> e(EU_ALU) ## e(ENC_R)   ## N ## Y ## N ## e(ALU_ADD)  ## e(BCU_ILL) ## e(LSU_ILL),
    SUB     -> e(EU_ALU) ## e(ENC_R)   ## N ## Y ## N ## e(ALU_SUB)  ## e(BCU_ILL) ## e(LSU_ILL),
    SLL     -> e(EU_ALU) ## e(ENC_R)   ## N ## Y ## N ## e(ALU_SLL)  ## e(BCU_ILL) ## e(LSU_ILL),
    SLT     -> e(EU_ALU) ## e(ENC_R)   ## N ## Y ## N ## e(ALU_SLT)  ## e(BCU_ILL) ## e(LSU_ILL),
    SLTU    -> e(EU_ALU) ## e(ENC_R)   ## N ## Y ## N ## e(ALU_SLTU) ## e(BCU_ILL) ## e(LSU_ILL),
    XOR     -> e(EU_ALU) ## e(ENC_R)   ## N ## Y ## N ## e(ALU_XOR)  ## e(BCU_ILL) ## e(LSU_ILL),
    SRL     -> e(EU_ALU) ## e(ENC_R)   ## N ## Y ## N ## e(ALU_SRL)  ## e(BCU_ILL) ## e(LSU_ILL),
    SRA     -> e(EU_ALU) ## e(ENC_R)   ## N ## Y ## N ## e(ALU_SRA)  ## e(BCU_ILL) ## e(LSU_ILL),
    OR      -> e(EU_ALU) ## e(ENC_R)   ## N ## Y ## N ## e(ALU_OR)   ## e(BCU_ILL) ## e(LSU_ILL),
    AND     -> e(EU_ALU) ## e(ENC_R)   ## N ## Y ## N ## e(ALU_AND)  ## e(BCU_ILL) ## e(LSU_ILL),
    LB      -> e(EU_LSU) ## e(ENC_I)   ## N ## Y ## N ## e(ALU_ILL)  ## e(BCU_ILL) ## e(LSU_LB),
    LH      -> e(EU_LSU) ## e(ENC_I)   ## N ## Y ## N ## e(ALU_ILL)  ## e(BCU_ILL) ## e(LSU_LH),
    LW      -> e(EU_LSU) ## e(ENC_I)   ## N ## Y ## N ## e(ALU_ILL)  ## e(BCU_ILL) ## e(LSU_LW),
    LBU     -> e(EU_LSU) ## e(ENC_I)   ## N ## Y ## N ## e(ALU_ILL)  ## e(BCU_ILL) ## e(LSU_ILL),
    LHU     -> e(EU_LSU) ## e(ENC_I)   ## N ## Y ## N ## e(ALU_ILL)  ## e(BCU_ILL) ## e(LSU_ILL),
    SB      -> e(EU_LSU) ## e(ENC_S)   ## N ## N ## N ## e(ALU_ILL)  ## e(BCU_ILL) ## e(LSU_SB),
    SH      -> e(EU_LSU) ## e(ENC_S)   ## N ## N ## N ## e(ALU_ILL)  ## e(BCU_ILL) ## e(LSU_SH),
    SW      -> e(EU_LSU) ## e(ENC_S)   ## N ## N ## N ## e(ALU_ILL)  ## e(BCU_ILL) ## e(LSU_SW),
    //FENCE   -> List(),
    //FENCE_I -> List(),
  )
  val default = {
               e(EU_ILL) ## e(ENC_ILL) ## Y ## N ## N ## e(ALU_ILL)  ## e(BCU_ILL) ## e(LSU_ILL) 
  }
}

class DecodeUnit extends RVREModule {
  val io = IO(new Bundle {
    val inst  = Input(UInt(XLEN.W))
    val ctrl  = Output(new InstCtrlBundle)
  })

  val inst = io.inst
  val imm  = WireDefault(0.S(XLEN.W))

  // Use some logic minimization (with ESPRESSO) to map an instruction to its 
  // associated set of control signals. You probably need 'espresso' in your 
  // $PATH (see https://github.com/chipsalliance/espresso), otherwise we fall 
  // back on using Quine-McCluskey (although maybe it doesn't matter too much 
  // for what we're interested doing here)?

  var ctrl = chisel3.util.experimental.decode.decoder(inst,
    chisel3.util.experimental.decode.TruthTable(
      DecoderTable.matches, DecoderTable.default
    )
  ).asTypeOf(new DecoderTable.CtrlBundle)

  // Generate the appropriate immediate for the instruction encoding.
  switch (ctrl.enc) {
    is (ENC_I) { imm := inst(31, 20).asSInt }
    is (ENC_S) { imm := Cat(inst(31, 25), inst(11, 7)).asSInt }
    is (ENC_B) { 
      imm := Cat(inst(31), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W)).asSInt 
    }
    is (ENC_U) { imm := Cat(inst(31, 12), 0.U(12.W)).asSInt }
    is (ENC_J) { 
      imm := Cat(inst(31), inst(19, 12), inst(20), inst(30, 21), 0.U(1.W)).asSInt
    }
  }

  // Fields from the main decoder logic
  io.ctrl.ill    := ctrl.ill
  io.ctrl.eu     := ctrl.eu
  io.ctrl.enc    := ctrl.enc
  io.ctrl.rd_en  := ctrl.rd_en
  io.ctrl.imm_en := ctrl.imm_en
  io.ctrl.alu_op := ctrl.alu_op
  io.ctrl.bcu_op := ctrl.bcu_op
  io.ctrl.lsu_op := ctrl.lsu_op

  // Fields fixed in the instruction encoding
  io.ctrl.rd     := inst(11, 7)
  io.ctrl.rs1    := inst(19, 15)
  io.ctrl.rs2    := inst(24, 20)
  io.ctrl.imm    := imm
}


