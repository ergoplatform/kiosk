package org.ergoplatform.kiosk.oraclepool.v2

import org.ergoplatform.kiosk.tx.TxUtil
import org.ergoplatform.kiosk.ergo._
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.appkit.{BlockchainContext, ConstantsBuilder, InputBox}
import org.ergoplatform.kiosk.appkit.MockErgoClient
import org.ergoplatform.sdk.ErgoToken

class OraclePoolFundingSpec extends MockErgoClient {
  property("Fund collection") { ergo =>
      ergo.client.execute { implicit ctx: BlockchainContext =>
      val pool = new OraclePool {
        val minBoxValue = 2000000
        override lazy val livePeriod = 4 // blocks
        override lazy val prepPeriod = 4 // blocks
        override lazy val buffer = 2 // blocks

        lazy val oracleToken = "12caaacb51c89646fac9a3786eb98d0113bd57d68223ccc11754a4f67281daed"
        lazy val poolToken = "961c8d498431664f4fb8a660b9a62618f092e34ef07370ba1a2fb7c278c5f57d"

        override lazy val oracleTokenId: Array[Byte] = oracleToken.decodeHex
        override lazy val poolTokenId: Array[Byte] = poolToken.decodeHex
        override lazy val oracleReward = 2000000 // Nano ergs. One reward per data point to be paid to oracle
        lazy val addresses = Seq(
          "9eiuh5bJtw9oWDVcfJnwTm1EHfK5949MEm5DStc2sD1TLwDSrpx", // private key is 37cc5cb5b54f98f92faef749a53b5ce4e9921890d9fb902b4456957d50791bd0
          "9f9q6Hs7vXZSQwhbrptQZLkTx15ApjbEkQwWXJqD2NpaouiigJQ", // private key is 5878ae48fe2d26aa999ed44437cffd2d4ba1543788cff48d490419aef7fc149d
          "9fGp73EsRQMpFC7xaYD5JFy2abZeKCUffhDBNbQVtBtQyw61Vym", // private key is 3ffaffa96b2fd6542914d3953d05256cd505d4beb6174a2601a4e014c3b5a78e
          "9fSqnSHKLzRz7sRkfwNW4Rqtmig2bHNaaspsQms1gY2sU6LA2Ng" // private key is 148bb91ada6ad5e6b1bba02fe70ecd96095e00cbaf0f1f9294f02fedf9855ea0
        ).toArray

        override lazy val minPoolBoxValue = oracleReward * (addresses.size + 1) + minBoxValue // how much min must exist in oracle pool box
      }

      val fee = 1500000

      val changeAddress = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK"
      val dummyTxId1 = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
      val dummyScript = "{sigmaProp(1 < 2)}"
      val poolToken = (pool.poolToken, 1L)

      // dummy custom input box for funding various transactions
      val customInputBox1 = ctx.newTxBuilder().outBoxBuilder.value(10000000000000L).contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript)).build().convertToInputWith(dummyTxId1, 0)

      // create funding boxes
      val fundingBox1ToCreate = KioskBox(pool.poolDepositAddress, value = 2000000000, registers = Array(), tokens = Array())
      val createNewFundingBox1Tx = TxUtil.createTx(Array(customInputBox1), Array[InputBox](), Array(fundingBox1ToCreate), fee, changeAddress, Array[String](), Array[DhtData](), false)
      val fundingBox1 = createNewFundingBox1Tx.getOutputsToSpend.get(0)

      // bootstrap pool (create EpochPrep box)
      val r4epochPrep = KioskLong(1) // dummy data point
      val r5epochPrep = KioskInt(20000) // end height of epoch

      // collect funds
      // first try without putting dummy value in R6

      // define box to spend
      val dummyEpochPrepBox = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(2000000000L)
        .tokens(new ErgoToken(poolToken._1, poolToken._2))
        .registers(
          r4epochPrep.getErgoValue,
          r5epochPrep.getErgoValue
        )
        .contract(new ErgoTreeContract(pool.epochPrepErgoTree, _networkType))
        .build()
        .convertToInputWith(dummyTxId1, 0)

      // define box to create
      val epochPrepBoxToCreate = KioskBox(
        pool.epochPrepAddress,
        value = 2000000000L + dummyEpochPrepBox.getValue,
        registers = Array(r4epochPrep, r5epochPrep),
        tokens = Array(poolToken)
      )

      val dummyInputBox = ctx.newTxBuilder().outBoxBuilder.value(10000000000000L).contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript)).build().convertToInputWith(dummyTxId1, 0)

      noException shouldBe thrownBy {
        TxUtil.createTx(
          Array(dummyEpochPrepBox, fundingBox1, dummyInputBox),
          Array[InputBox](),
          Array(epochPrepBoxToCreate),
          fee,
          changeAddress,
          Array[String](),
          Array[DhtData](),
          false
        )
      }

    }

  }
}
