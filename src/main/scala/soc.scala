package rvre.soc

import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFileInline
import chisel3.experimental.{annotate, ChiselAnnotation}
import firrtl.annotations.MemorySynthInit
import rvre.uarch._

class IROMWrapper(rom_file: String) extends RVREModule {
  val ROM_SIZE: Int = 256
  val io = IO(new Bundle {
    val bus = rvre.bus.RVREBusPort.sink()
  })

  annotate(new ChiselAnnotation { override def toFirrtl = MemorySynthInit })
  val rom = SyncReadMem(ROM_SIZE, UInt(XLEN.W))
  if (rom_file.trim().nonEmpty) {
    loadMemoryFromFileInline(rom, rom_file)
  }

  val addr = io.bus.req.bits.addr((log2Ceil(ROM_SIZE) + 2), 2)
  val aligned = (io.bus.req.bits.addr & "b11".U) === 0.U
  val err = io.bus.req.valid && ( 
    ~aligned || io.bus.req.bits.st_en
    || io.bus.req.bits.mask =/= "b1111".U
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

class RAMWrapper(ram_file: String = "") extends RVREModule {
  val RAM_SIZE: Int = 1024
  val io = IO(new Bundle {
    val bus = rvre.bus.RVREBusPort.sink()
  })

  // FIXME: You can't initialize this because it's a vector of bytes.
  // How do we deal with this?
  annotate(new ChiselAnnotation { override def toFirrtl = MemorySynthInit })
  val ram  = SyncReadMem(RAM_SIZE / 4, Vec(4, UInt(8.W)))
  if (ram_file.trim().nonEmpty) {
    loadMemoryFromFileInline(ram, ram_file)
  }

  val req  = io.bus.req.bits
  val resp = io.bus.resp.bits

  // Only use the bottom bits from requested addresses
  val addr = req.addr(log2Ceil(RAM_SIZE), 0)

  resp.data := 0.U
  resp.err  := false.B

  when (io.bus.req.fire) {
    printf("RAMWrapper: req addr=%x, mask=%b, st_en=%b, st_data=%x\n",
      io.bus.req.bits.addr, io.bus.req.bits.mask,
      io.bus.req.bits.st_en, io.bus.req.bits.st_data)

    when (req.st_en) {
      ram.write(
        addr, req.st_data.asTypeOf(Vec(4, UInt(8.W))), req.mask.asBools
      )
    } .otherwise {
      resp.data := Cat(ram.read(addr).reverse)
    }
  }

  io.bus.resp.valid := io.bus.req.fire
  io.bus.req.ready  := true.B

}



class RVRESoc extends RVREModule {
  val irom = Module(new IROMWrapper(rom_file = "irom/test.text.mem")) 
  val sram = Module(new RAMWrapper(ram_file = "irom/test.data.mem"))
  val hart = Module(new rvre.core.RVREHart)

  hart.io.ibus <> irom.io.bus
  hart.io.dbus <> sram.io.bus

}
