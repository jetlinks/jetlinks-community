package org.jetlinks.community.utils.math;

import lombok.Generated;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Operators;

import javax.annotation.Nonnull;

@Generated
abstract class MathSubscriber<T, R> extends Operators.MonoSubscriber<T, R> {

	Subscription s;

	boolean done;

	public MathSubscriber(CoreSubscriber<? super R> actual) {
		super(actual);
	}

	@Override
	public void onSubscribe(@Nonnull Subscription s) {
		if (Operators.validate(this.s, s)) {
			this.s = s;
			actual.onSubscribe(this);
			s.request(Long.MAX_VALUE);
		}
	}

	@Override
	public void onNext(@Nonnull T t) {
		if (done) {
			Operators.onNextDropped(t, actual.currentContext());
			return;
		}

		try {
			updateResult(t);
		}
		catch (Throwable ex) {
			reset();
			done = true;
			actual.onError(Operators.onOperatorError(s, ex, t, actual.currentContext()));
			return;
		}
	}

	@Override
	public void onError(@Nonnull Throwable t) {
		if (done) {
			Operators.onErrorDropped(t, actual.currentContext());
			return;
		}
		done = true;
		reset();
		actual.onError(t);
	}

	@Override
	public void onComplete() {
		if (done) {
			return;
		}
		done = true;
		R r = result();
		if (r != null) {
			complete(r);
		}
		else {
			actual.onComplete();
		}
	}

	@Override
	public void cancel() {
		super.cancel();
		s.cancel();
	}

	protected abstract void reset();

	protected abstract R result();

	protected abstract void updateResult(T newValue);
}
