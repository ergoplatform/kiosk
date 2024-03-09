package org.ergoplatform.kiosk.oraclepool.v7

import org.ergoplatform.kiosk.ergo._
import scorex.crypto.hash.Blake2b256
import org.scalatest.matchers.should.Matchers
import org.scalatest.propspec.AnyPropSpec

class AddressSpec extends AnyPropSpec with Matchers {
  lazy val minBoxValue = 2000000 // min value to remain AFTER paying rewards. For min value to keep BEFORE rewards, see minPoolBoxValue
  val epochPoolLive = new OraclePoolParams {}

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

    liveEpochAddress shouldEqual "NTkuk55NdwCVifH8NTWdK7o6Z4S6RcZPmprheumFHoc537SDnupXc6VNPGTYm1p93TRoSA1pqMkwuobnJDoUwDnx553shYcwgboPrYtYe5Xit4fDfXAtxvqoEDq8zFxVUnu2qJmy9BH7vN9kVPTkZLEaGvpmQQqcvvqhaCcazd7iapmXu2HsfhMwWshqJwDvxD4miYCGWtmfkppcn3RReFEhEFVZuLhZCFMEEJ8aTMtKvpmir54jk9hiiEfHE6drKNw3HQZyPng2UJq3gAyyPcH7KbZLwAxwHqeUvZvMuSS2ZaFUGn65yGGpzr1vSYjHvbDxRcUfh8B4cxzqJ2on9B9thuzPUDcPrLRvYgbpWQUqvcakhvgMWUDiJyJjKb5Q7y6qX2tiwxKyXH6crSxgz6kuJRKLAPDMKBPhawU5skbAkmcgbuAcpRxABnGqmT5Y5ZLckqSfrsEHyktPpkhrivi1yr5zVG35vmK6kNJrcMDzrGn6GuJanoUzKvqm7DNgmZjVdGDdvafXF8r83kgizHMNP7yzVXeYD7gt58oLa5JpvyXhBdN3vcS2qR"
    epochPrepAddress shouldEqual "2Qgfhy6m7Exe9deFZcZBDbN83eQmLQNCcYhuTFMRKLNsrKHgcRsZ9Y2mdEyFumYY7CqRDpAVDrQ1L28vYQEt5yyVEabpRQfvukeKLaAXE4g4Rz67wUa7bCtNvyJDVsJebc1wKy37LwTktSUZKLaxUd1TQShu9jN3rD3h7xRJvuqznTwm3h3r2Hz8GRKu3L8pFUCGCXBZsGMCVyXPrhc3ztDSL9V8cWnKU7HSVrA4YS3BA38ATicDzsoFhLAN6H7jjEZxa1jBEc91ncVdR9zrzXPNJi683GAWShjFyLqfiNHLtsoj152vdrLDJgoUem1gPntLPd3LFZQgkHupqfRDgAkxDFArpdQi2LboaWPhg7yiDPsLWD5gKTgervQ619NKbYrMi2CUGqx4XVv7VGXsxXJHtQ7TzwWpx4K6g6tyFRfFAVB8Zr2nvu4Tt8SbRPXpPSGg1moVM8bkeCaxxfmW4TvdT5X5famkZwqAmF"
    dataPointAddress shouldEqual "AucEQEJ3Y5Uhmu4o8dnoztRiAKKTErrhugq6EyACQ4SrK7NFAds5u9B93Xvb7heGC9oGL88F8muu6T6MARfqKpHS2ce1jZ6x8Ju6j9n4AvWkcQxBaLUq36wHGKmiCqRDtKT5tbhZ7hQbK7WuMAejKD7aW91yTBrBNHAXDVmZznmYjzJqDQGuPMsRHQSYGGyW5H2p"
    poolDepositAddress shouldEqual "zLSQDVBaGtShacgCZGTpgHjdHu4VaHE9GvzZvuZjW51nh4MLk439ReAXKEyLXVaHtYm36HXZAPpiTm7on3DNGJpHiCdmNYypgDj1kXx2hdzaF2xehvtAAaEYDLfkmbqTVtiBcPaJvDaJtwNwM6xQ2m7xqyWMEbZEnJFkre2zuBnm363ukM7jzWnMF33WkcGkCgRyXfFadRVQGM4iWKjXK6FgEEpn4ArFwYKEfKqn2AFASJMi8A"
    updateAddress shouldEqual "5SexodN85jTR9ooTabXdR1PVz1x2XEcwS5kQfnAD2AM1wkuZ5kjH6LucpgDfDYHWMwvx164PvxRQjHGNqEBDdubhGge8VtGpAepH39jYBGWG9apbsesjkhyFiVXjfQxCcrMXPqkwNMhHKwivC4AoZqFFmUMR8KyXZX5NN9dtHACjmKTarVLzE6co2xFaPnEFHE9G21Q5WQG4kRMKZPE9J7EBu4xF7AD8g8RLQGs1FpB4Be758rJfCmZwAyA4xpderiwNTZVomrZUddaFwkoTa6HPfNhJgEmSKKSChUzwdcreCJWyAYPrRQ3jaZVYyquE2y9Kw4q5FUJ3erw2iYbdBLaM4QBkNwAJ39j1SWkDL35gpCpJWRq5S6tyr9CJ7YkdP3nowpfMeWcTa5uN15bFfR15gEjTwTDDhurjH8mY5XRUudhVsFYYCc13TgknX35fC82WoGbPgE1qa9X8VR7kpfqtCxp68eB3gSFZKLfyB5HhyVYZYoVzWaXReYyBvB5DVifdRnPTfz5FtBMxujeaCLk7ujtes7Ba3zfXXtbDGf"
    Blake2b256(epochPrepErgoTree.bytes).encodeHex shouldEqual "8e46a04822fb92c428e29148d0fb57b65820a5052dbddd8ba3fbc053874a85b0"
    Blake2b256(liveEpochErgoTree.bytes).encodeHex shouldEqual "ad0dace9ccd7e4d9af296225ea35764f9c80bc1dda5070edbeb53f5fa76bda6e"
    minPoolBoxValue shouldEqual 14000000
  }
}
