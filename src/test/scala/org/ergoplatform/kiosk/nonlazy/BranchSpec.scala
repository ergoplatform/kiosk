package org.ergoplatform.kiosk.nonlazy

import org.ergoplatform.kiosk.ergo.{DhtData, KioskBox, KioskCollByte, KioskInt, KioskLong}
import Branch.branchBoxAddress
import org.ergoplatform.kiosk.tx.TxUtil
import org.ergoplatform.appkit.{BlockchainContext, ConstantsBuilder, InputBox, SignedTransaction}
import org.ergoplatform.kiosk.appkit.HttpClientTesting.createMockedErgoClient
import org.scalatest.matchers.should.Matchers
import org.scalatest.propspec.AnyPropSpec

import scala.util.Try

class BranchSpec extends AnyPropSpec with Matchers {

  val changeAddress = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK"
  val dummyTxId = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
  val dummyScript = "{sigmaProp(1 < 2)}"

  property("Not-so-lazy evaluation") {
    createMockedErgoClient().execute { implicit ctx: BlockchainContext =>
      assert(branchBoxAddress == "88dwYDNXcCq9UyA7VBcSdqJRgooKVqS8ixprCknxcm2sba4jbhQYGphjutEebtr3ZeC4tmT9oEWKS2Bq")

      val fee = 1500000

      val branchBoxToCreate = KioskBox(
        Branch.branchBoxAddress,
        value = 2000000,
        registers = Array(),
        tokens = Array()
      )

      // dummy custom input box for funding various transactions
      val customInputBox = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(10000000000L)
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .build()
        .convertToInputWith(dummyTxId, 0)

      val branchBoxCreationTx: SignedTransaction = TxUtil.createTx(
        inputBoxes = Array(customInputBox),
        dataInputs = Array[InputBox](),
        boxesToCreate = Array(branchBoxToCreate),
        fee,
        changeAddress,
        Array[String](),
        Array[DhtData](),
        false
      )

      val dataBoxWithLong = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(10000000L)
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .registers(KioskLong(1L).getErgoValue)
        .build()
        .convertToInputWith(dummyTxId, 0)

      val branchBox = branchBoxCreationTx.getOutputsToSpend.get(0)

      val dataBoxWithCollByte = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(10000000L)
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .registers(KioskCollByte("hello".getBytes()).getErgoValue)
        .build()
        .convertToInputWith(dummyTxId, 0)

      val longSelectionBox = KioskBox(
        changeAddress,
        value = 2000000,
        registers = Array(KioskInt(1)),
        tokens = Array()
      )

      val collByteSelectionBox = KioskBox(
        changeAddress,
        value = 2000000,
        registers = Array(KioskInt(2)),
        tokens = Array()
      )

      TxUtil.createTx(
        inputBoxes = Array(branchBox, customInputBox),
        dataInputs = Array[InputBox](dataBoxWithCollByte),
        boxesToCreate = Array(collByteSelectionBox),
        fee,
        changeAddress,
        Array[String](),
        Array[DhtData](),
        false
      )

      // below should work ideally (with truly lazy evaluation). However, it currently fails
      // it works
      assert(
        Try(
          TxUtil.createTx(
            inputBoxes = Array(branchBox, customInputBox),
            dataInputs = Array[InputBox](dataBoxWithLong),
            boxesToCreate = Array(longSelectionBox),
            fee,
            changeAddress,
            Array[String](),
            Array[DhtData](),
            false
          )
        ).isSuccess
      )
    }
  }
}
