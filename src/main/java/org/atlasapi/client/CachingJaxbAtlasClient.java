/* Copyright 2009 Meta Broadcast Ltd

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. */

package org.atlasapi.client;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.atlasapi.client.query.AtlasQuery;
import org.atlasapi.media.entity.simple.ContentQueryResult;
import org.atlasapi.media.entity.simple.Description;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.url.UrlEncoding;

/**
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class CachingJaxbAtlasClient implements AtlasClient {
		
	private QueryStringBuilder queryStringBuilder = new QueryStringBuilder();
	private StringQueryClient queryClient;

	private final MapMaker cacheTemplate = new MapMaker().softValues().expiration(10, TimeUnit.MINUTES);
	
    private ConcurrentMap<String, ContentQueryResult> queryCache = cacheTemplate.makeComputingMap(new Function<String, ContentQueryResult>() {

		@Override
		public ContentQueryResult apply(String query) {
			return queryClient.query(query);
		}
    });
 
	
	private ConcurrentMap<String, Maybe<Description>> identifierQueryCache = cacheTemplate.makeMap();

	private final String baseUri;

	public CachingJaxbAtlasClient(String baseUri, StringQueryClient queryClient) {
		this.baseUri = baseUri;
		this.queryClient = queryClient;
	}
	
	public CachingJaxbAtlasClient() {
		this("http://atlasapi.org/2.0", new JaxbStringQueryClient());
	}
	
	public CachingJaxbAtlasClient(String baseUri) {
	    this(baseUri, new JaxbStringQueryClient());
	}
	
	@Override
	public <T> List<T> query(AtlasQuery<T> query) {
		return query.extractFrom(queryCache.get(baseUri + "/" + query.urlPrefix() + ".xml?" +  queryStringBuilder.build(query.build())));
	}
	
	@Override
	public ImmutableMap<String, Description> any(Iterable<String> ids) {
		Map<String, Description> results = Maps.newHashMap();
		List<String> toFetch = Lists.newArrayList();
		
		for (String id : ids) {
			if (identifierQueryCache.containsKey(id)) {
				Maybe<Description> description = identifierQueryCache.get(id);
				
				if (description.hasValue()) {
					results.put(description.requireValue().getUri(), description.requireValue());
				}
			}
			else {
				toFetch.add(id);
			}
		}
		if (!toFetch.isEmpty()) {
			results.putAll(fetchIdentifierQuery(toFetch));
		}
		return ImmutableMap.copyOf(results);
	}

	private Map<String, Description> fetchIdentifierQuery(Iterable<String> uris) {
		ContentQueryResult result = queryClient.query(baseUri + "/any.xml?uri=" +  Joiner.on(",").join(UrlEncoding.encode(uris)));
		Set<Description> all = Sets.newHashSet();
		all.addAll(result.getItems());
		all.addAll(result.getPlaylists());
		return cacheResults(all, Sets.newHashSet(uris));
	}

	private Map<String, Description> cacheResults(Set<Description> all, Set<String> uris) {
		Map<String, Description> results = Maps.newHashMap();
		Set<String> allIdentifiersReturned = Sets.newHashSet();
		for (Description description : all) {
			results.put(description.getUri(), description);
			putInCache(identifierQueryCache, description);
			allIdentifiersReturned.addAll(description.identifiers());
		}
		// Make sure negative results are cached
		Set<String> notFound = Sets.difference(uris, allIdentifiersReturned);
		for (String notfound : notFound) {
			identifierQueryCache.put(notfound, Maybe.<Description>nothing());
		}
		return results;
	}

	private void putInCache(ConcurrentMap<String, Maybe<Description>> cache, Description content) {
		cache.put(content.getUri(), Maybe.just(content));
		if (content.getCurie() != null) {
			cache.put(content.getCurie(),  Maybe.just(content));
		}
	}
}