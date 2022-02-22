
package rvre.bus

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import rvre.uarch._

// A simple bus for memory accesses traveling around the core.
// Flying without caches for a while I think, so this should be sufficient.
class RVREBusIO extends RVREBundle {

  class RVREBusRequest extends RVREBundle {
    val addr     = Output(UInt(XLEN.W))
    val signed   = Output(Bool())
    // log2 number of bytes (1 << n) in this request
    val bytes    = Output(UInt(log2Ceil(XLEN / 8).W))
    val st_en    = Output(Bool())
    val st_data  = Output(UInt(XLEN.W))
  }

  class RVREBusResponse extends RVREBundle {
    val data = Output(UInt(XLEN.W))
    val err  = Output(Bool())
  }

  val req  = Decoupled(new RVREBusRequest)
  val resp = Flipped(Decoupled(new RVREBusResponse))
}

object RVREBusPort {
  def source() = new RVREBusIO
  def sink() = Flipped(new RVREBusIO)
}
