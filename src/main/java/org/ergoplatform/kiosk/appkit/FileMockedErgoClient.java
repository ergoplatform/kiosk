package org.ergoplatform.kiosk.appkit;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.ergoplatform.appkit.*;
import org.ergoplatform.appkit.impl.BlockchainContextBuilderImpl;
import org.ergoplatform.appkit.impl.NodeAndExplorerDataSourceImpl;
import org.ergoplatform.explorer.client.ExplorerApiClient;
import org.ergoplatform.restapi.client.ApiClient;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;

/**
 * MockedRunner using given files to provide BlockchainContext information.
 */
public class FileMockedErgoClient implements MockedErgoClient {

    private final List<String> _nodeResponses;
    private final List<String> _explorerResponses;
    private final MockWebServer _node;
    private final MockWebServer _explorer;
    private final NodeAndExplorerDataSourceImpl _dataSource;

    public FileMockedErgoClient(List<String> nodeResponses, List<String> explorerResponses) {
        _nodeResponses = nodeResponses;
        _explorerResponses = explorerResponses;
        _node = new MockWebServer();
        _explorer = new MockWebServer();

        enqueueResponses(_node, _nodeResponses);
        enqueueResponses(_explorer, _explorerResponses);

        HttpUrl baseUrl = _node.url("/");
        ApiClient client = new ApiClient(baseUrl.toString());
        HttpUrl explorerBaseUrl = _explorer.url("/");
        ExplorerApiClient explorerClient = new ExplorerApiClient(explorerBaseUrl.toString());
        _dataSource = new NodeAndExplorerDataSourceImpl(client, explorerClient) {
            @Override
            public BlockchainParameters getParameters() {
                return new BlockchainParameters() {
                    @Override
                    public NetworkType getNetworkType() {
                        return NetworkType.MAINNET;
                    }

                    @Override
                    public int getStorageFeeFactor() {
                        return 0;
                    }

                    @Override
                    public int getMinValuePerByte() {
                        return 0;
                    }

                    @Override
                    public int getMaxBlockSize() {
                        return 0;
                    }

                    @Override
                    public int getTokenAccessCost() {
                        return 0;
                    }

                    @Override
                    public int getInputCost() {
                        return 0;
                    }

                    @Override
                    public int getDataInputCost() {
                        return 0;
                    }

                    @Override
                    public int getOutputCost() {
                        return 0;
                    }

                    @Override
                    public int getMaxBlockCost() {
                        return 49000000;
                    }

                    @Override
                    public byte getBlockVersion() {
                        return 3;
                    }
                };
            }
        };
    }

    @Override
    public List<String> getNodeResponses() {
        return _nodeResponses;
    }

    @Override
    public List<String> getExplorerResponses() {
        return _explorerResponses;
    }

    void enqueueResponses(MockWebServer server, List<String> rs) {
        for (String r : rs) {
            server.enqueue(new MockResponse()
                    .addHeader("Content-Type", "application/json; charset=utf-8")
                    .setBody(r));
        }
    }

    @Override
    public BlockchainDataSource getDataSource() {
        return _dataSource;
    }

    @Override
    public <T> T execute(Function<BlockchainContext, T> action) {
        BlockchainContext ctx =
                new BlockchainContextBuilderImpl(_dataSource, NetworkType.MAINNET).build();
        T res = action.apply(ctx);

        try {
            _explorer.close();
            _node.close();
        } catch (IOException e) {
            throw new ErgoClientException("Cannot shutdown server " + _node.toString(), e);
        }

        return res;
    }
}

