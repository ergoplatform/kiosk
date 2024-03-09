package org.ergoplatform.kiosk.oraclepool.v5

import org.ergoplatform.kiosk.ergo._
import scorex.crypto.hash.Blake2b256
import org.scalatest.matchers.should.Matchers
import org.scalatest.propspec.AnyPropSpec

class OraclePoolLiveAddressSpec extends AnyPropSpec with Matchers {
  lazy val minBoxValue = 2000000 // min value to remain AFTER paying rewards. For min value to keep BEFORE rewards, see minPoolBoxValue
  val epochPoolLive = new OraclePoolLive {}

  import epochPoolLive._

  property("Display Addresses") {
    println("minPoolBoxValue " + minPoolBoxValue)
    println(s"Live Epoch script length       : ${liveEpochErgoTree.bytes.length}")
    println(s"Live Epoch script complexity   : ${liveEpochErgoTree.complexity}")
    println(s"Epoch prep script length       : ${epochPrepErgoTree.bytes.length}")
    println(s"Epoch prep script complexity   : ${epochPrepErgoTree.complexity}")
    println(s"DataPoint script length        : ${dataPointErgoTree.bytes.length}")
    println(s"DataPoint script complexity    : ${dataPointErgoTree.complexity}")
    println(s"PoolDeposit script length      : ${poolDepositErgoTree.bytes.length}")
    println(s"PoolDeposit script complexity  : ${poolDepositErgoTree.complexity}")

    println("liveEpochAddress: " + liveEpochAddress)
    println("epochPrepAddress: " + epochPrepAddress)
    println("dataPointAddress: " + dataPointAddress)
    println("poolDepositAddress: " + poolDepositAddress)
    println("updateAddress: " + updateAddress)
    println("EpochPrepScriptHash: (for R6 of LiveEpochBox) " + Blake2b256(KioskErgoTree(epochPrepErgoTree).serialize).encodeHex)
    println("LiveEpochScriptHash: (for hard-coding in Datapoint script) " + Blake2b256(liveEpochErgoTree.bytes).encodeHex)
    println("Min box value: " + minPoolBoxValue)

    liveEpochAddress shouldEqual "8D6pdYVRvF6PeiuSWVBR4x2RqKKCjPPqMyTpmRho6EpfWMqnwN4uedKYYTkniyKPEjnjHXpLFLD9HaVCYV7vkSNeQkjLXkQyvK8UvQuACZQw1BjpgmJc8gpkT5Ckd9X6XCpEzxQAgDzoDC4Kyq3XQtKmwap4htAk9RAcnGEbKWqyWdkLV8iSqETLj5f9ARBrU8CZwpCNZHxxpbWLZ9wPrxQMwenyQce23v7qcWkKsHkxSvQbfeyjV2wcXUGQchbwEUrtzBYLUQNBDqctQoK1mAt1V69TQDs7NxxrQcdaNmHoNkp6EVbioYNdezbjascXzfEcwXkzMmwFBhHCHwA51yHGQ66fVkLtU23iWsmkn6y14FdmQFauV23MFY5D4QTdDzExT8h7J2gaqxqqSUDXbmqcuKpoCRAisEKtSStFCHgRFeHCeRsJXNPHe7CXXutVKJpopcJdNXRtuGDw6vzoXBKJ1W7HHbF5GmQcHHBdRMMysKFZo7gwmAXB9DyUKH4UowQeaQ5R3hWzy1o4ES1fj2VPSTo7ypEJpk9Gpf8Vq6ybpeFb5ZFpsyvY3fpW3"
    epochPrepAddress shouldEqual "7DY5qpcRgHc59umTpZQT7MjtbdgEBE3nbGCNmqTd7w3Tk4jhm3Su661UAGWajoUMtBZDsc8PcmvbU4EuJUdW1dRcyXYZrQ4RpUNikekBXHCCp5vtSqzbq56DpUkcB3j1g3Fg8NmZKyciEJzqTPbnYAfRsip3e1DAiyuPCJTk1vbPkSqdEg9TpmmzBB5e4sAN4HqhDpsFRxvGuKM78uHrhVsZ8be9WgmNvmk4XeWrFMzwDxYa4dK1Gr679x86jdgknuJQoB4cq9TdQ1tCDbsCNQhBBa7ajzgLyXfDuP1oWGg9WHYfm9wEHAxKHxtRgmWL8Et9RryNdqbUWndREAkjbda5HED52Vhtvf91D9ryUjLYeefeBmJY2g6D4sWJGgoVKmYMrvB3phBb84Ng8epKcnRX9csdPoEJzijW7YfLVJis3p4KUeMfoKuXoLh8MBa2dF2D613E2dQbGgbFibpymGycvsMCBM1g2vdCtvw"
    dataPointAddress shouldEqual "jL2aaqw6XU61SZznwZeLEryve79s26LKMtJDCG5AMu3jQ8oUxQ8wREoWLPepqTWUu1XBYJYPXa3pUepjFfARG6eiXCa5sRD8wangJCdFnBMXprkF4mWxKGGWkFLyNepe5VrRcKM9HYnWuMmDs6xDe8u2tzC1meErUzJZdNoXNULTdAxHojV5GfmK2oRrKA6pMNaiHJuFzKKXFLWKb862vRqn83vB3uZpYt99K8tywNtbhig4q9XGcXsNsAMLH"
    poolDepositAddress shouldEqual "zLSQDVBaKjE8uosAMWUTQonH8AMqYM78fhCrLAGvgNHenjS4Wf7yvk2kpaPu5fZtJs9GLxCENX74EtPG28zr6SR4YCJ75oZc5eXusJ6Nxbftbg5yt9ZAhcUEYBPbvyKATUFASVx8x2JZMv6g73KFbSThVYSrCLLGwhh7KTQKf4cQkGywDV7yaBaaRqBH2XQgappKxBcVq8kT5Bx34unh7R7PuD2T8a61UwMR2RRSh2JXJVD6kx"
    updateAddress shouldEqual "5SexodN85jTRoFtPcFXmfpvF5Lp2Mcufo9RVA1v1t4xSYswQVepxayNgfTfAXbNyAmwX7r2bHpyXrUKUb3S92KdGFVH6P1q2nzfusukLMbijuUtCKcvkzHJe4iR8fVBvvJEPWhGVrRqRSLXyPEoQmVBUqiVQtWf1fLww8RfbJ8RihvPMk5ycZoTRTS1jqvMvmh9pwZE1QoyxnQS9GCfE63QvZ6PDPWPB2zYXrd6M7hEDKDGp9sh2FCXtAKyGKXwrsgwXfpcAxAb4gZvTME1TRwNUY58sB7yumhAr3rTePLHfZ68hun4R71pLLhNqqQNVuvp3VmtU3rRjfbK3Z4XkGMwmzvv4dPXsGo7nQxbbutaXFqdbFFuZYnm1gkYBz3Mws3gbDaxxPE64J3qUFj1M1u5dy33dAVkB2S3ZRbG1KHPcKbqzgzKAYQtD8ziAZRgicXMcGgkcRb3doniUVUShYoGWHcYowAGjecbyc2WCr3AVKibT7r5LH2xJP1b9VRxdakQVEnFdxTwjtSdcXkXLQ6VaQFjTJMTx5MCfBYe9iN"
    Blake2b256(epochPrepErgoTree.bytes).encodeHex shouldEqual "e3aacf50096c2512535eaabfce3cc6c63af51b25d2a7fa2672278ab8a53a1a72"
    Blake2b256(liveEpochErgoTree.bytes).encodeHex shouldEqual "61ef15b34f00ee0b85b9646ba3272940da5f164f16d659136e66abb01536fd81"
    minPoolBoxValue shouldEqual 262000000
  }
}
