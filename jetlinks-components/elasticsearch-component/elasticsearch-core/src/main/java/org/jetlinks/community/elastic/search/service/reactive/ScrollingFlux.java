/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.elastic.search.service.reactive;

import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch.core.ScrollResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Operators;
import reactor.util.concurrent.Queues;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;
import java.util.function.Function;

public class ScrollingFlux<T> extends Flux<T> {

    final Function<Map<String, Object>, T> mapper;
    final ReactiveElasticsearchClient client;
    final Time timeout;
    final Consumer<SearchRequest.Builder> request;

    public ScrollingFlux(Function<Map<String, Object>, T> mapper,
                         ReactiveElasticsearchClient client,
                         Time timeout,
                         Consumer<SearchRequest.Builder> request) {
        this.mapper = mapper;
        this.client = client;
        this.request = request;
        this.timeout = timeout;
    }


    @Override
    public void subscribe(CoreSubscriber<? super T> actual) {
        actual.onSubscribe(new ScrollingFluxSubscriber<>(this, actual));
    }

    @RequiredArgsConstructor
    static class ScrollingFluxSubscriber<T> implements Subscription {
        static final Disposable COMPLETED = Disposables.disposed();

        final ScrollingFlux<T> parent;
        final CoreSubscriber<? super T> actual;
        final Queue<T> queue = Queues.<T>unboundedMultiproducer().get();

        @SuppressWarnings("all")
        static final AtomicIntegerFieldUpdater<ScrollingFluxSubscriber> WIP =
            AtomicIntegerFieldUpdater.newUpdater(ScrollingFluxSubscriber.class, "wip");
        volatile int wip;

        @SuppressWarnings("all")
        static final AtomicLongFieldUpdater<ScrollingFluxSubscriber> REQUESTED =
            AtomicLongFieldUpdater.newUpdater(ScrollingFluxSubscriber.class, "requested");

        volatile long requested;

        volatile String scrollId;

        volatile boolean cancelled;

        @SuppressWarnings("all")
        static final AtomicReferenceFieldUpdater<ScrollingFluxSubscriber, Disposable>
            FETCHING = AtomicReferenceFieldUpdater.newUpdater(ScrollingFluxSubscriber.class, Disposable.class, "fetching");

        volatile Disposable fetching;

        Disposable doRequest() {
            if (scrollId != null) {
                return parent
                    .client
                    .execute(elastic -> elastic
                        .scroll(scroll -> {
                            scroll.scroll(parent.timeout)
                                  .scrollId(scrollId);
                            return scroll;
                        }, Map.class))
                    .subscribe(this::handleResponse,
                               this::onError);
            }
            return parent
                .client
                .execute(elastic -> elastic
                    .search(scroll -> {
                        scroll.scroll(parent.timeout);
                        parent.request.accept(scroll);
                        return scroll;
                    }, Map.class))
                .subscribe(this::handleResponse,
                           this::onError);
        }

        void onError(Throwable error) {
            actual.onError(error);

        }

        void handleResponse(SearchResponse<Map> response) {
            this.scrollId = response.scrollId();
            handleResponse(response.hits());
        }

        void handleResponse(ScrollResponse<Map> response) {
            this.scrollId = response.scrollId();
            handleResponse(response.hits());
        }

        @SuppressWarnings("all")
        void handleResponse(HitsMetadata<Map> hits) {
            try {
                List<Hit<Map>> hitList = hits.hits();
                if (CollectionUtils.isEmpty(hitList)) {
                    FETCHING.set(this, COMPLETED);
                    drain();
                } else {
                    for (Hit<Map> hit : hitList) {
                        Map<String, Object> map = hit.source();
                        if (map.get("id") == null) {
                            map.put("id", hit.id());
                        }

                        T res = parent.mapper.apply(map);
                        if (res != null) {
                            if (!queue.offer(res)) {
                                Operators.onDiscard(res, actual.currentContext());
                            }
                        }
                    }
                    FETCHING.set(this, null);
                    drain();
                }
            } catch (Throwable error) {
                onError(error);
            }


        }

        void fetch() {
            if (FETCHING.get(this) != null && WIP.getAndIncrement(this) == 0) {
                drain();
                return;
            }
            Disposable.Swap swap = Disposables.swap();
            if (FETCHING.compareAndSet(this, null, swap)) {
                swap.update(doRequest());
            } else if (WIP.getAndIncrement(this) == 0){
                drain();
            }
        }

        void drain() {
            if (WIP.getAndIncrement(this) != 0) {
                return;
            }

            int missed = 1;

            do {
                long r = requested;
                long count = 0;

                while (count != r) {
                    if (cancelled) {
                        return;
                    }
                    T data = queue.poll();
                    if (data == null) {
                        //complete
                        if (FETCHING.get(this) == COMPLETED) {
                            actual.onComplete();
                            return;
                        }
                        break;
                    }
                    actual.onNext(data);
                    count++;
                }

                if (cancelled) {
                    return;
                }
                if (count != 0L) {
                    if (r != Long.MAX_VALUE) {
                        Operators.produced(REQUESTED, this, count);
                    }
                }

                missed = WIP.addAndGet(this, -missed);

            } while (missed != 0);
            if (queue.isEmpty() && FETCHING.get(this) == null) {
                fetch();
            } else if (queue.isEmpty() && FETCHING.get(this) == COMPLETED) {
                actual.onComplete();
            }

        }


        @Override
        public void request(long n) {
            if (Operators.validate(n)) {
                Operators.addCap(REQUESTED, this, n);
                fetch();
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
            Disposable fetching = FETCHING.getAndSet(this, COMPLETED);
            if (fetching != null) {
                fetching.dispose();
            }
        }
    }
}
