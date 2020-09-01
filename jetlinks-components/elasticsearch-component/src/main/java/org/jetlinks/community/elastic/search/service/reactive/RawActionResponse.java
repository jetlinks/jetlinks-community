package org.jetlinks.community.elastic.search.service.reactive;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.client.ClientResponse;

import java.io.IOException;

/**
 * Extension to {@link ActionResponse} that also delegates to {@link ClientResponse}.
 *
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 * @author Mark Paluch
 * @since 3.2
 */
class RawActionResponse extends ActionResponse {

	private final ClientResponse delegate;

	private RawActionResponse(ClientResponse delegate) {
		this.delegate = delegate;
	}

	static RawActionResponse create(ClientResponse response) {
		return new RawActionResponse(response);
	}

	public HttpStatus statusCode() {
		return delegate.statusCode();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.reactive.function.client.ClientResponse#headers()
	 */
	public ClientResponse.Headers headers() {
		return delegate.headers();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.reactive.function.client.ClientResponse#body(org.springframework.web.reactive.function.BodyExtractor)
	 */
	public <T> T body(BodyExtractor<T, ? super ClientHttpResponse> extractor) {
		return delegate.body(extractor);
	}

	/*
	 * (non-Javadoc)
	 * until Elasticsearch 7.4 this empty implementation was available in the abstract base class
	 */
	@Override
	public void writeTo(StreamOutput out) throws IOException {
	}
}