
import chisel3._
import chiseltest._
import chiseltest.experimental._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.BundleLiterals._

// I'm literally just using this for printf() debugging on the clock edges.
class SocSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "RVRESoc"
  it should "work" in {
    test(new rvre.soc.RVRESoc) { dut => 
      for (i <- 1 to 8) {
        dut.clock.step()
        println("------- step -------")
      }
    }
  }
}

