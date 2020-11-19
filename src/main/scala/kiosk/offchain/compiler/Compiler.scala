package kiosk.offchain.compiler

import kiosk.encoding.ScalaErgoConverters
import kiosk.ergo
import kiosk.ergo.{KioskBox, KioskCollByte, KioskErgoTree, KioskLong}
import kiosk.offchain.model._
import org.ergoplatform.ErgoAddress

object Compiler {
  def compile(protocol: Protocol) = {
    val dictionary = new Dictionary
    // validate that constants are properly encoded
    optSeq(protocol.constants).map(value => value.getValue(dictionary))
    // load declarations and valid then semantically
    Loader.load(protocol)(dictionary)
    // load on-chain declarations
    OnChainLoader.load(protocol)(dictionary)
    // create outputs
    val outputs: Seq[KioskBox] = protocol.outputs.map(createOutput(_)(dictionary))
    Printer.print(dictionary)
    outputs foreach println
    // print values
  }

  def createOutput(output: Output)(dictionary: Dictionary): KioskBox = {
    val ergoAddress: ErgoAddress = ScalaErgoConverters.getAddressFromErgoTree(dictionary.getValue(output.address.value.get).asInstanceOf[KioskErgoTree].value)
    val address: String = ScalaErgoConverters.getStringFromAddress(ergoAddress)
    val registers: Seq[ergo.KioskType[_]] = optSeq(output.registers)
      .map { register =>
        val value: ergo.KioskType[_] = dictionary.getValue(register.value.get)
        val index: Int = RegNum.getIndex(register.num)
        val regType: DataType.Type = register.`type`
        require(DataType.isValid(value, regType), s"Invalid type ${value.typeName} for register ${register.num}. Expected $regType")
        (index, value)
      }
      .sortBy(_._1)
      .map(_._2)

    val tokens: Seq[(String, scala.Long)] = optSeq(output.tokens)
      .map { token =>
        val tokenId: KioskCollByte = dictionary.getValue(token.tokenId.value.get).asInstanceOf[KioskCollByte]
        val index: Int = token.index
        val amount: KioskLong = dictionary.getValue(token.amount.value.get).asInstanceOf[KioskLong]
        (index, (tokenId.toString, amount.value))
      }
      .sortBy(_._1)
      .map(_._2)
    val nanoErgs: scala.Long = dictionary.getValue(output.nanoErgs.value.get).asInstanceOf[KioskLong].value
    KioskBox(address, nanoErgs, registers.toArray, tokens.toArray)
  }
}
