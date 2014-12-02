package com.zimbra.cs.index.solr;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer.RemoteSolrException;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CollectionParams.CollectionAction;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.index.Indexer;
import com.zimbra.cs.index.ZimbraIndexSearcher;

//TODO let Solr control batching of documents instead of MailboxIndex class
public class SolrCloudIndex extends SolrIndexBase {
    private boolean solrCollectionProvisioned = false;
    private SolrCloudIndex(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public boolean indexExists() {
        if(!solrCollectionProvisioned) {
            //TODO switch to using collections?action=LIST when it is released (Solr 4.8 or 5). JIRA issue #SOLR-5466
            SolrQuery q = new SolrQuery().setParam("collection", accountId).setQuery("*:*").setRows(0);
            QueryRequest req = new QueryRequest(q);
            SolrServer solrServer = null;
            try {
                solrServer = getSolrServer();
                processRequest(solrServer, req);
                solrCollectionProvisioned = true;
            } catch (SolrServerException e) {
                ZimbraLog.index.error("Problem checking if Solr collection exists for account %s" ,accountId, e);
            } catch (SolrException e) {
                ZimbraLog.index.info("Solr collection for account %s does not exist", accountId);
            }  catch (ServiceException e) {
                ZimbraLog.index.error("Problem checking if Solr collection exists for account %s" ,accountId, e);
            } catch (IOException e) {
                ZimbraLog.index.error("Problem checking if Solr collection exists for account %s" ,accountId, e);
            } finally {
                shutdown (solrServer);
            }
        }
        return solrCollectionProvisioned;
    }

    @Override
    /**
     * Gets the latest commit version and generation from Solr
     */
    public long getLatestIndexGeneration(String accountId) throws ServiceException {
        long version = 0L;
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set(COMMAND, CMD_INDEX_VERSION);
        params.set(CommonParams.WT, "javabin");
        params.set(CommonParams.QT, "/replication");
        params.set("collection", accountId);
        QueryRequest req = new QueryRequest(params);
        SolrServer solrServer = getSolrServer();
        setupRequest(req, solrServer);
        @SuppressWarnings("rawtypes")
        NamedList rsp;
        try {
            ((CloudSolrServer)solrServer).setZkClientTimeout(60000);
            ((CloudSolrServer)solrServer).setZkConnectTimeout(15000);
            rsp = solrServer.request(req);
            version = (Long) rsp.get(GENERATION);
        } catch (SolrServerException | IOException e) {
          throw ServiceException.FAILURE(e.getMessage(),e);
        } finally {
            shutdown(solrServer);
        }
        return version;
    }

    /**
     * Fetches the list of index files from Solr using Solr Replication RequestHandler
     * See {@link https://cwiki.apache.org/confluence/display/solr/Index+Replication}
     * @param gen generation of index. Required by Replication RequestHandler
     * @throws BackupServiceException
     */
    @Override
    public List<Map<String, Object>> fetchFileList(long gen, String accountId) throws ServiceException {
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set(COMMAND, CMD_GET_FILE_LIST);
        params.set(GENERATION, String.valueOf(gen));
        params.set(CommonParams.WT, "javabin");
        params.set(CommonParams.QT, "/replication");
        params.set("collection", accountId);
        QueryRequest req = new QueryRequest(params);
        SolrServer solrServer = getSolrServer();
        setupRequest(req, solrServer);
        try {
            ((CloudSolrServer)solrServer).setZkClientTimeout(60000);
            ((CloudSolrServer)solrServer).setZkConnectTimeout(15000);
            @SuppressWarnings("rawtypes")
            NamedList response = solrServer.request(req);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> files = (List<Map<String, Object>>) response
                    .get(CMD_GET_FILE_LIST);
            if (files != null) {
                return files;
            } else {
                ZimbraLog.index.error("No files to download for index generation: "
                        + gen + " account: " + accountId);
                return Collections.emptyList();
            }
        } catch (SolrServerException | IOException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        } finally {
            shutdown(solrServer);
        }
    }
    @Override
    public void initIndex() throws IOException, ServiceException {
        if (!indexExists()) {
            SolrServer solrServer = getSolrServer();
            try {
                ModifiableSolrParams params = new ModifiableSolrParams();
                params.set("action", CollectionAction.CREATE.toString());
                params.set("name", accountId);
                //TODO: get global/server config for num shards, configName, replication factor and max shards per node
                params.set("numShards", 1);
                params.set("replicationFactor", 2);
                params.set("maxShardsPerNode", 1);
                params.set("collection.configName","zimbra");
                SolrRequest req = new QueryRequest(params);
                req.setPath("/admin/collections");
                processRequest(solrServer, req);
                //TODO check for errors
            } catch (SolrServerException e) {
                String errorMsg = String.format("Problem creating new Solr collection for account %s",accountId);
                ZimbraLog.index.error(errorMsg, e);
                throw new IOException(errorMsg,e);
            } catch (RemoteSolrException e) {
                String errorMsg = String.format("Problem creating new Solr collection for account %s",accountId);
                ZimbraLog.index.error(errorMsg, e);
                throw new IOException(errorMsg,e);
            } catch (SolrException e) {
                String errorMsg = String.format("Problem creating new Solr collection for account %s",accountId);
                ZimbraLog.index.error(errorMsg, e);
                throw new IOException(errorMsg,e);
            } catch (IOException e) {
                String errorMsg = String.format("Problem creating new Solr collection for account %s",accountId);
                ZimbraLog.index.error(errorMsg, e);
                throw new IOException(errorMsg,e);
            } finally {
                shutdown(solrServer);
            }

            //TODO: remove this test code. Added, to give ZooKeeper time to update cluster state when running multiple Solr instances on one laptop
            //wait for index to get created
            try {
                for(int i=0;i<5;i++) {
                    if(indexExists()) {
                        return;
                    }
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }

    @Override
    public Indexer openIndexer() throws IOException, ServiceException {
        if(!indexExists()) {
            initIndex();
        }
        return new SolrIndexer();
    }

    @Override
    public ZimbraIndexSearcher openSearcher() throws IOException, ServiceException {
        if(!indexExists()) {
            initIndex();
        }
        final SolrIndexReader reader = new SolrIndexReader();
        return new SolrIndexSearcher(reader);
    }

    @Override
    public void evict() {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteIndex() throws IOException, ServiceException {
        if (indexExists()) {
            SolrServer solrServer = getSolrServer();
            try {
                ModifiableSolrParams params = new ModifiableSolrParams();
                params.set("action", CollectionAction.DELETE.toString());
                params.set("name", accountId);
                SolrRequest req = new QueryRequest(params);
                req.setPath("/admin/collections");
                processRequest(solrServer, req);
                solrCollectionProvisioned = false;
                //TODO check for errors

            } catch (SolrServerException e) {
                ZimbraLog.index.error("Problem deleting Solr collection" , e);
            } catch (IOException e) {
                ZimbraLog.index.error("Problem deleting Solr collection" , e);
            } finally {
                shutdown (solrServer);
            }
        }

    }

    public static final class Factory implements IndexStore.Factory {

        public Factory() {
            ZimbraLog.index.info("Created SolrlIndexStore\n");
        }

        @Override
        public SolrIndexBase getIndexStore(String accountId) {
            return new SolrCloudIndex(accountId);
        }

        /**
         * Cleanup any caches etc associated with the IndexStore
         */
        @Override
        public void destroy() {
            //solrServer.shutdown();
        }
    }

    private class SolrIndexer extends SolrIndexBase.SolrIndexer {

        @Override
        public int maxDocs() {
            SolrServer solrServer = null;
            try {
                solrServer = getSolrServer();
                CoreAdminResponse resp = CoreAdminRequest.getStatus(null, solrServer);
                Iterator<Map.Entry<String, NamedList<Object>>> iter = resp.getCoreStatus().iterator();
                while(iter.hasNext()) {
                    Map.Entry<String, NamedList<Object>> entry = iter.next();
                    if(entry.getKey().indexOf(accountId, 0)==0) {
                        Object maxDocs = entry.getValue().findRecursive("index","maxDoc");
                        if(maxDocs != null && maxDocs instanceof Integer) {
                            return (int)maxDocs;
                        }
                    }
                }
            } catch (IOException e) {
                ZimbraLog.index.error("Cought IOException retrieving maxDocs for mailbox %s", accountId,e );
            } catch (SolrServerException e) {
                ZimbraLog.index.error("Cought SolrServerException retrieving maxDocs for mailbox %s", accountId,e);
            } catch (RemoteSolrException e) {
                ZimbraLog.index.error("Cought RemoteSolrException retrieving maxDocs for mailbox %s", accountId,e);
            } catch (ServiceException e) {
                ZimbraLog.index.error("Cought ServiceException retrieving maxDocs for mailbox %s", accountId,e );
            } finally {
                shutdown(solrServer);
            }
            return 0;
        }

    }

    public class SolrIndexReader extends SolrIndexBase.SolrIndexReader {
        @Override
        public int numDeletedDocs() {
            SolrServer solrServer = null;
            try {
                solrServer = getSolrServer();
                CoreAdminResponse resp = CoreAdminRequest.getStatus(null, solrServer);
                Iterator<Map.Entry<String, NamedList<Object>>> iter = resp.getCoreStatus().iterator();
                while(iter.hasNext()) {
                    Map.Entry<String, NamedList<Object>> entry = iter.next();
                    if(entry.getKey().indexOf(accountId, 0)==0) {
                        return (int)entry.getValue().findRecursive("index","deletedDocs");
                    }
                }
            } catch (IOException e) {
                ZimbraLog.index.error("Cought IOException retrieving number of deleted documents in mailbox %s", accountId,e);
            } catch (SolrServerException e) {
                ZimbraLog.index.error("Cought SolrServerException retrieving number of deleted documents in mailbox %s", accountId,e);
            } catch (RemoteSolrException e) {
                ZimbraLog.index.error("Cought SolrServerException retrieving number of deleted documents in mailbox %s", accountId,e);
            } catch (ServiceException e) {
                ZimbraLog.index.error("Cought ServiceException retrieving number of deleted documents in mailbox %s", accountId,e);
            } finally {
                shutdown(solrServer);
            }
            return 0;
        }
    }


    @Override
    public void setupRequest(Object obj, SolrServer solrServer) {
        if (obj instanceof UpdateRequest) {
            ((UpdateRequest) obj).setParam("collection", accountId);
        } else if (obj instanceof SolrQuery) {
            ((SolrQuery) obj).setParam("collection", accountId);
        }
    }

    @Override
    public SolrServer getSolrServer() throws ServiceException {
        return new CloudSolrServer(Provisioning.getInstance().getLocalServer().getAttr(Provisioning.A_zimbraSolrURLBase, true));
    }

    @Override
    public void shutdown(SolrServer server) {
        ((CloudSolrServer)server).shutdown();

    }
}

