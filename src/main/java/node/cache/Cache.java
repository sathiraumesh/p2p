package node.cache;

public interface Cache {

    public void addCacheItem(String ip, int port, String command, String filename);
    public void removeCacheItem(CacheItem item);
}
