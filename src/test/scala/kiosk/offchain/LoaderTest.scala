package kiosk.offchain

import kiosk.offchain.compiler.model._
import kiosk.offchain.compiler.{Dictionary, Loader, optSeq}
import kiosk.offchain.parser.Parser

object LoaderTest {
  val myLong1 = Constant("myLong1", DataType.Long, "1234")
  val myInt = Constant("myInt", DataType.Int, "1234")
  val myCollByte = Constant("myCollByte", DataType.CollByte, "506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7")
  val myTokenId = Constant("myTokenId", DataType.CollByte, "ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f")
  val myGroupElement = Constant("myGroupElement", DataType.GroupElement, "028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67")
  val myErgoTree1 = Constant("myErgoTree1", DataType.ErgoTree, "10010101D17300")
  val myAddress = Constant("myAddress", DataType.Address, "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")

  val myIntToLong = Conversion("myIntToLong", "myInt", UnaryConverter.ToLong)
  val myLong2 = BinaryOp("myLong2", "myLong1", BinaryOperator.Add, "myIntToLong")
  val myLong3 = BinaryOp("myLong3", "myLong2", BinaryOperator.Max, "myLong1")
  val myLong4 = BinaryOp("myLong4", "myLong2", BinaryOperator.Add, "myLong3")
  val myLong5 = BinaryOp("myLong5", "myLong4", BinaryOperator.Add, "myLong2")
  val myLong6 = BinaryOp("myLong6", "myLong5", BinaryOperator.Add, "myLong4")

  val myLong7 = UnaryOp("myLong7", "myLong2", UnaryOperator.Neg)
  val myLong8 = UnaryOp("myLong8", "myLong7", UnaryOperator.Neg)

  val myErgoTree2 = Conversion("myErgoTree2", "myGroupElement", UnaryConverter.ProveDlog)
  val myCollByte2 = Conversion("myCollByte2", "myErgoTree2", UnaryConverter.ToCollByte)

  val constants = Some(Seq(myLong1, myCollByte, myInt, myTokenId, myGroupElement, myErgoTree1, myAddress))
  val unaryOps = Some(Seq(myLong7, myLong8))
  val conversions = Some(Seq(myErgoTree2, myCollByte2, myIntToLong))
  val binaryOps = Some(Seq(myLong2, myLong3, myLong4, myLong5, myLong6))

  val myRegister1 = Register(Some("myRegister1"), value = None, RegNum.R4, DataType.CollByte)
  val myRegister2 = Register(Some("myRegister2"), value = None, RegNum.R4, DataType.CollByte)
  val myRegister3 = Register(Some("myRegister3"), value = None, RegNum.R4, DataType.CollByte)
  val myRegister4 = Register(Some("myRegister4"), value = None, RegNum.R4, DataType.CollByte)

  val myToken0 = Token(
    index = Some(1),
    id = Id(name = Some("myToken1ActualId"), value = None),
    amount = Long(
      name = Some("someLong1"),
      value = None,
      filter = None
    )
  )

  val myToken1 = Token(
    index = Some(1),
    id = Id(name = Some("myToken1Id"), value = None),
    amount = Long(
      name = Some("someLong1"),
      value = None,
      filter = None
    )
  )

  val myToken2 = Token(
    index = Some(1),
    id = Id(name = Some("unreferencedToken2Id"), value = None),
    amount = Long(
      name = None,
      value = Some("myLong1"),
      filter = Some(FilterOp.Gt)
    )
  )

  val myToken3 = Token(
    index = Some(1),
    id = Id(name = Some("randomName"), value = None),
    amount = Long(
      name = Some("someLong3"),
      value = None,
      filter = None
    )
  )

  val myInput1 = Input(
    id = Some(Id(name = None, value = Some("myCollByte"))),
    address = Some(Address(name = Some("myAddressName"), value = None)),
    registers = Some(Seq(myRegister3)),
    tokens = Some(Seq(myToken1)),
    nanoErgs = Some(Long(name = Some("input1NanoErgs"), value = None, filter = None)),
    options = None
  )

  val myInput2 = Input(
    None,
    Some(Address(name = None, value = Some("myAddress"))),
    registers = Some(Seq(myRegister4)),
    tokens = Some(Seq(myToken2)),
    nanoErgs = Some(Long(name = None, value = Some("input1NanoErgs"), filter = Some(FilterOp.Ne))),
    options = Some(Set(InputOptions.Strict))
  )

  val myInput3 = Input(
    id = None,
    address = Some(Address(name = None, value = Some("myAddress"))),
    registers = Some(Seq(myRegister1, myRegister2)),
    tokens = Some(Seq(myToken3)),
    nanoErgs = Some(Long(name = None, value = Some("someLong1"), filter = Some(FilterOp.Ge))),
    options = Some(Set(InputOptions.Strict))
  )

  val protocol = Protocol(
    constants,
    dataInputs = Some(Seq(myInput1, myInput2)),
    inputs = Seq(myInput3),
    outputs = Nil,
    fee = Some(10000L),
    binaryOps,
    unaryOps,
    conversions
  )

  def main(args: Array[String]): Unit = {
    // test parser
    val protocolToJson = Parser.unparse(protocol) // convert to json
    val protocolToJsonToProtocol = Parser.parse(protocolToJson.toString) // convert back to protocol
    println(protocolToJson)
    require(protocolToJsonToProtocol == protocol, "protocolToJsonToProtocol") // require both to be equal
    val str = scala.io.Source.fromFile("src/test/resources/TxBuilderLoaderTest.json").getLines.mkString
    val strToProtocol = Parser.parse(str)
    require(strToProtocol == protocol, "strToProtocol")

    implicit val dictionary = new Dictionary
    // Step 1. validate that constants are properly encoded
    optSeq(protocol.constants).map(_.getValue) foreach println
    // Step 2. load declarations (also does semantic validation)
    (new Loader).load(protocol)
  }
}
