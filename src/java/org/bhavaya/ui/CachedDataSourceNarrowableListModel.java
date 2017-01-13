package org.bhavaya.ui;

import org.bhavaya.util.Log;
import org.bhavaya.util.TaskScheduler;
import org.bhavaya.util.Utilities;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * For most uses of NarrowableComboBox there will some sort of time-consuming query that fetches data in response
 * to the user's typing.  This class handles the complicated task managing caching and query scheduling.  A query
 * interface is provided to allow any datasource to be used in the query.  Composition was preferred over inheritance
 * because of the life-cycle differences between the two (in composition, the parameter object is initialised first.  In
 * inheritance the subclass is initialised second).
 *
 * @author Brendon McLean
 * @version $Revision: 1.7.4.2 $
 */
public class CachedDataSourceNarrowableListModel extends NarrowableListModel {
    private static final Log log = Log.getCategory(CachedDataSourceNarrowableListModel.class);

    /**
     * This simple interface allows us to abstract away the rather simple task of data retrieval from the more
     * complicated task of caching.
     */
    public interface DataQuery {
        /**
         * @param searchString String used in a query for returning a collection of objects (eg. 'SELECT * FROM table WHERE x LIKE 'searchString%')
         * @return a collection of objects.  (Typically the ListRenderers will know how render these, otherwise toString() will be used).
         */
        Collection execAndGetCollection(String searchString);

        /**
         * Each data query has key that makes it unique.  In a SQL context this would simply be the sql query string.  This is used to cache
         * results with similar queries for faster usage on second usage.
         * @return
         */
        Object getQueryKey();
    }

    private DataQuery dataSource;
    private NamedDatasetCache cache;
    private Object currentCacheKey = null;
    private TaskScheduler taskScheduler;
    private boolean loadingData = false;

    private DataQuery favouriteDataSource;

    private static final ConcurrentMap<Object, List> FAVOURITES = new ConcurrentHashMap<Object, List>();
    private static final List PLACEHOLDER = Collections.unmodifiableList(new ArrayList(0));


    public CachedDataSourceNarrowableListModel(DataQuery dataSource) {
        this(dataSource, false);
    }

    public CachedDataSourceNarrowableListModel(DataQuery dataSource, DataQuery favouriteDataSource) {
        super(false);
        this.dataSource = dataSource;
        this.favouriteDataSource = favouriteDataSource;
        this.taskScheduler = new TaskScheduler(500, true);
        this.cache = NamedDatasetCache.getInstance(dataSource.getQueryKey());
        taskScheduler.setTask(new InitFavouritesTask());
    }

    public CachedDataSourceNarrowableListModel(DataQuery dataSource, boolean emptyStringIsAllData) {
        super(emptyStringIsAllData);
        this.dataSource = dataSource;
        this.taskScheduler = new TaskScheduler(500, true);
        this.cache = NamedDatasetCache.getInstance(dataSource.getQueryKey());
        if (isEmptyStringIsAllData()) narrow("", false); // TODO: think about how to handle lazy initialisation
    }

    public synchronized boolean isLoadingData() {
        return loadingData;
    }

    public synchronized void narrow(String narrowText, boolean scheduleLoad) {
        setLastNarrowText(narrowText);
        if (!narrowFromFavourites(narrowText) && cache != null && (narrowText.length() != 0 || isEmptyStringIsAllData())) {
            if (log.isDebug()) log.debug("Narrowing for: " + narrowText);
            // Narrow on what we have already
            Object cacheKeyForText = cache.getSuperKeyOf(narrowText);
            if (log.isDebug()) log.debug("Narrow will be using cache for: " + cacheKeyForText);

            if (cacheKeyForText == null) {
                //todo - at this point we should try and hide the current data set in case it was previously on the favourites
                //not in cache, so load the data into cache then set this model and narrow once it is loaded
                if (scheduleLoad) {
                    loadingData = true;
                    taskScheduler.setTask(new QueryToModelUpdater(narrowText, scheduleLoad));
                } else {
                    new QueryToModelUpdater(narrowText, scheduleLoad).run();
                }
            } else {
                if (log.isDebug()) log.debug("Current cache key is:" + currentCacheKey);
                //if we want to be looking at the same dataset as last time, just narrow
                if (Utilities.equals(currentCacheKey, cacheKeyForText)) {
                    narrow();
                } else {
                    if (log.isDebug()) log.debug("Changing data model for ListModel to " + cacheKeyForText);
                    clear();
                    currentCacheKey = cacheKeyForText;
                    addData((Collection) cache.get(cacheKeyForText));
                    narrow();
                }
            }
        } else {
            super.narrow();
        }
    }

    private boolean narrowFromFavourites(String narrowText) {
        if(favouriteDataSource != null && narrowText.length() > 0) {
            List favouriteData = FAVOURITES.get(favouriteDataSource.getQueryKey());
            if(!favouriteData.isEmpty()) {
                int firstIndex = getFirstMatch(favouriteData, narrowText);
                if(firstIndex >= 0) {
                    int lastIndex = getLastMatchFor(favouriteData, narrowText);
                    if(lastIndex >= firstIndex) {
                        clear();
                        addData(favouriteData);
                        doNarrow(narrowText);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void clear() {
        super.clear();
        currentCacheKey = null;
    }

    /**
     * Allows transformation by subclass of search parameter used in query from narrow text.
     */
    protected String getSearchKey(String narrowText) {
        return narrowText;
    }


    protected void finalize() throws Throwable {
        taskScheduler.close();
        super.finalize();
    }

    private final class InitFavouritesTask implements Runnable {
        public void run() {
            List list = FAVOURITES.putIfAbsent(favouriteDataSource.getQueryKey(), PLACEHOLDER);
            if (list == null) {
                list = new ArrayList(favouriteDataSource.execAndGetCollection(""));
                Utilities.sort(list, OBJECT_TO_OBJECT_COMPARATOR);
                FAVOURITES.put(favouriteDataSource.getQueryKey(), list);
            }
        }
    }

    private final class QueryToModelUpdater implements Runnable {
        private String narrowText;
        private boolean scheduleLoad;

        protected QueryToModelUpdater(String narrowText, boolean scheduleLoad) {
            this.narrowText = narrowText;
            this.scheduleLoad = scheduleLoad;
        }

        public void run() {
            String superKey = cache.getSuperKeyOf(narrowText);
            String searchKey = getSearchKey(narrowText);
            if (superKey == null) {
                Collection data = dataSource.execAndGetCollection(searchKey.toUpperCase() + "%");
                if (data != null) {
                    // Add all this to the cache to avoid doing it again.
                    CachedDataSourceNarrowableListModel.this.cache.put(searchKey, data);

                    Runnable narrowTask = new Runnable() {
                        public void run() {
                            // We don't want to use the narrow text this thread was created with,
                            // as it could have changed in the meantime.
                            // Use the current narrowText instead.
                            synchronized (CachedDataSourceNarrowableListModel.this) {
                                CachedDataSourceNarrowableListModel.this.loadingData = false;
                            }
                            narrow(getLastNarrowText());
                        }
                    };

                    if (scheduleLoad) {
                        EventQueue.invokeLater(narrowTask);
                    } else {
                        narrowTask.run();
                    }
                }
            } else {
                if (log.isDebug()) log.debug("Not getting data for: " + narrowText + " as already got data for: " + superKey);
            }
        }

        public String toString() {
            return "QueryToModelUpdater-" + narrowText;
        }
    }

    /**
     * This class maps key.toString() to data.
     * However, it is quite different to a standard hashmap. If there is a mapping from string [str] to data
     * [data] then it is deemed that there is an identical mapping to [data] for all strings prefixed by [str].
     * All string comparisons are case insensitive.
     *
     * e.g.
     *      if "foo" maps to [Collection 1]
     *      then "FooBar", "football", etc also map to [Collection 1]
     *
     *      if we then add a new mapping from "f" to [Collection 2] then this overwrites these previous mappings
     *      and any string starting with "f" also maps to [Collection 2]
     *
     * if you delete a mapping, all submappings will also be deleted.
     */
    public static class NamedDatasetCache {
        private static final Log log = Log.getCategory(DBNarrowableListModel.NamedDatasetCache.class);
        private static Map cacheInstances = new HashMap();

        private HashMap data = new HashMap();

        public static synchronized NamedDatasetCache getInstance(Object selectStatement) {
            NamedDatasetCache cache = (NamedDatasetCache) cacheInstances.get(selectStatement);
            if (cache == null) {
                cache = new NamedDatasetCache();
                cacheInstances.put(selectStatement, cache);
            }
            return cache;
        }

        public static synchronized void clearAll() {
            for (Iterator iterator = cacheInstances.values().iterator(); iterator.hasNext();) {
                NamedDatasetCache namedDatasetCache = (NamedDatasetCache) iterator.next();
                namedDatasetCache.clear();
            }
            FAVOURITES.clear();
        }

        private void clearRedundantCacheData(String superKeyString) {
            Iterator iter = data.keySet().iterator();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                if (key.startsWith(superKeyString)) {
                    if (log.isDebug()) log.debug("removing data for key: " + key);
                    iter.remove();
                }
            }
        }

        /**
         * multiple keys map onto the same dataset. This returns the key that is a superkey of the given key
         * (i.e. returns the key of a dataset that is a superset of the data referenced by <code>key</code>)
         * (in practical terms if you pass a string, you will get back a prefix string, or null if no dataset
         * exists for the given key).
         */
        public synchronized String getSuperKeyOf(String key) {
            String keyStr = normaliseKeyString(key);

            Iterator keyIter = data.keySet().iterator();
            while (keyIter.hasNext()) {
                String superKeyStr = (String) keyIter.next();
                if (keyStr.startsWith(superKeyStr)) return superKeyStr;
            }
            return null;
        }

        private String normaliseKeyString(Object key) {
            return key.toString().toLowerCase();
        }

        public Object put(Object key, Object value) {
            return put(key.toString(), value);
        }

        public synchronized Object put(String key, Object value) {
            if (log.isDebug()) log.debug("call to cache.put " + key);
            String superKey = getSuperKeyOf(key);
            //only add it if there is no superset
            if (superKey == null) {
                String newKey = normaliseKeyString(key);
                //clear newly redundant data (i.e. sets that are subsets of this new one)
                clearRedundantCacheData(newKey);
                if (log.isDebug()) log.debug("Adding data to cache: key: " + newKey);
                return data.put(newKey, value);
            }
            return null;
        }

        public synchronized void clear() {
            data.clear();
        }

        public synchronized Object get(Object key) {
            String superKey = getSuperKeyOf(key.toString());
            return data.get(superKey);
        }

        public synchronized boolean containsKey(Object key) {
            Object superKey = getSuperKeyOf(key.toString());
            return data.containsKey(superKey);
        }
    }

}
