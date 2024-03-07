package kiosk.appkit

import org.ergoplatform.appkit.{BlockchainContext, ErgoClient, NetworkType, RestApiErgoClient}

object Client {
  val client: String => Client = (url: String) => new Client(s"http://$url")

  def usingContext[T](url: String)(f: BlockchainContext => T): T = {
    client(url).usingContext(f)
  }
}

class Client(url: String, networkType: NetworkType = NetworkType.MAINNET) {
  private val explorer = if (networkType == NetworkType.MAINNET)
    RestApiErgoClient.defaultMainnetExplorerUrl
  else
    RestApiErgoClient.defaultTestnetExplorerUrl
  private val restApiErgoClient: ErgoClient = RestApiErgoClient.create(url, networkType, "", explorer)

  private def usingContext[T](f: BlockchainContext => T): T = {
    restApiErgoClient.execute { ctx =>
      f(ctx)
    }
  }
}
