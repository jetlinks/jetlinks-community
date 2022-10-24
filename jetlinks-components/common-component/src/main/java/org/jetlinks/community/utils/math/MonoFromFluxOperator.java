package org.jetlinks.community.utils.math;

import lombok.Generated;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

@Generated
abstract class MonoFromFluxOperator<I, O> extends Mono<O> implements Scannable {

	protected final Flux<? extends I> source;

	protected MonoFromFluxOperator(Flux<? extends I> source) {
		this.source = Objects.requireNonNull(source);
	}

	@Override
	@Nullable
	public Object scanUnsafe(@Nonnull Attr key) {
		if (key == Attr.PREFETCH) return Integer.MAX_VALUE;
		if (key == Attr.PARENT) return source;
		return null;
	}

}