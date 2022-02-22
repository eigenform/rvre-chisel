package rvre.soc

import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFileInline
import chisel3.experimental.{annotate, ChiselAnnotation}
import firrtl.annotations.MemorySynthInit
import rvre.uarch._

class IROMWrapper(rom_file: String, sync: Boolean) extends RVREModule {
  val ROM_SIZE: Int = 256
  val addr_width: Int = log2Ceil(ROM_SIZE)
  val io = IO(new Bundle {
    val bus = rvre.bus.RVREBusPort.sink()
  })

  annotate(new ChiselAnnotation { override def toFirrtl = MemorySynthInit })
  val rom = if (sync) {
    SyncReadMem(ROM_SIZE, UInt(XLEN.W))
  } else {
    Mem(ROM_SIZE, UInt(XLEN.W))
  }
  if (rom_file.trim().nonEmpty) {
    loadMemoryFromFileInline(rom, rom_file)
  }

  val addr = io.bus.req.bits.addr(addr_width - 1, 2)
  val aligned = (io.bus.req.bits.addr & "b11".U) === 0.U
  val err = io.bus.req.valid && ( 
    ~aligned || io.bus.req.bits.st_en
    || io.bus.req.bits.bytes =/= "b10".U
  )

  io.bus.resp.bits := DontCare
  when (io.bus.req.fire) {
    io.bus.resp.bits.err  := err
    io.bus.resp.bits.data := rom.read(addr)
    printf("IROMWrapper: resp data %x\n", io.bus.resp.bits.data)
  }
  io.bus.resp.valid     := io.bus.req.fire
  io.bus.req.ready      := true.B
}


// See 'src/main/scala/common/memory.scala' from `ucb-bar/riscv-sodor'.
class RAMModel(num_bytes: Int, sync: Boolean) extends RVREModule {
  val addr_width = log2Ceil(num_bytes)
  val io = IO(new Bundle {
    val bus = rvre.bus.RVREBusPort.sink()
  })

  annotate(new ChiselAnnotation { override def toFirrtl = MemorySynthInit })
  val ram  = if (sync) {
    SyncReadMem(num_bytes/4, Vec(4, UInt(8.W)))
  } else {
    Mem(num_bytes/4, Vec(4, UInt(8.W)))
  }

  def to_vec(x: UInt) = {
    VecInit(((x(31,0).asBools.reverse grouped 8) map 
      (bools => Cat(bools))).toSeq)
  }
  def to_mask_vec(bytes: UInt, off: UInt = 0.U) = {
      (("b00011111".U(8.W) << bytes)(7, 4) << off)(3,0).asBools.reverse
  }

  val req    = io.bus.req.bits
  val resp   = io.bus.resp.bits
  val signed = req.signed
  val addr   = req.addr(addr_width - 1, 2)
  val off    = req.addr(1, 0)
  val bytes  = (1.U << req.bytes) - 1.U

  resp.data := 0.U
  resp.err  := false.B

  when (io.bus.req.fire) {
    printf("RAMModel: req addr=%x, bytes=%b, signed=%b, st_en=%b, st_data=%x\n",
      io.bus.req.bits.addr, io.bus.req.bits.bytes, io.bus.req.bits.signed,
      io.bus.req.bits.st_en, io.bus.req.bits.st_data)

    when (req.st_en) {
      val data_shifted    = req.st_data << (off << 3)
      val data_shiftedvec = to_vec(data_shifted)
      val mask_vec        = to_mask_vec(bytes, off)
      ram.write(addr, data_shiftedvec, mask_vec)
    } .otherwise {
      val data_bytevec    = ram.read(addr)
      val data_uint       = Cat(data_bytevec)
      val data_shifted    = data_uint >> (off << 3)
      val data_shiftedvec = to_vec(data_shifted)
      val sign_bit        = data_shiftedvec(3.U - bytes)(7)
      val mask_vec        = to_mask_vec(bytes, 0.U)
      val data_maskedvec  = (data_shiftedvec zip mask_vec) map ({ 
        case (byte, mask) => Mux(sign_bit && signed, 
          byte | ~Fill(8, mask), byte & Fill(8, mask))
      })
      resp.data := Cat(data_maskedvec)
      printf("RAMModel: resp data=%x\n", resp.data)
    }
  }
  io.bus.resp.valid := io.bus.req.fire
  io.bus.req.ready  := true.B

}

// NOTE: Right now we're probably relying on the fact that all of these 
// memories can handle asynchronous reads.

class RVRESoc extends RVREModule {
  val irom = Module(new IROMWrapper(
    rom_file = "irom/test.text.mem", sync = false
  )) 
  val sram = Module(new RAMModel(
    num_bytes = 1024, sync = false
  ))
  val hart = Module(new rvre.core.RVREHart)

  hart.io.ibus <> irom.io.bus
  hart.io.dbus <> sram.io.bus
}





