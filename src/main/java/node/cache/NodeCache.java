package node.cache;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class NodeCache implements Cache {

    private List<CacheItem> cacheItemList;

    @Override
    public void addCacheItem(String ip, int port, String command, String params) {
        cacheItemList.add(new CacheItem(ip, port, command,  params));
    }

    @Override
    public void removeCacheItem(CacheItem item) {
        if (cacheItemList.size() > 10) {
            cacheItemList.remove(0);
        }

        cacheItemList.remove(item);
    }
}
