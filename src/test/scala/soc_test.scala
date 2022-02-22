import chisel3._
import chiseltest._
import chiseltest.experimental._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.BundleLiterals._

class SocSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "RVRESoc"
  it should "work" in {
    test(new rvre.soc.RVRESoc) { dut => 
      for (i <- 1 to 32) {
        dut.clock.step()
        println("------- step " + i + " -------")
      }
    }
  }
}

