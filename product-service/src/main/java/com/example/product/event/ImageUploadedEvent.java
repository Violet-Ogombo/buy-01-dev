package com.example.product.event;

public class ImageUploadedEvent {

	private String mediaId;
	private String productId;
	private String userId;

	public ImageUploadedEvent() {
		/*
		 * No-arg constructor intentionally left empty.
		 *
		 * Reason: serialization frameworks (for example, Jackson) and some
		 * proxying/ORM tools require a public no-argument constructor to
		 * instantiate objects via reflection during deserialization or data
		 * mapping. Removing this constructor can break deserialization and
		 * framework integration.
		 *
		 * If you want to prevent direct instantiation in application code,
		 * replace the body with an explicit failure such as:
		 *     throw new UnsupportedOperationException("Use factory method instead");
		 */
	}

	public String getMediaId() {
		return mediaId;
	}

	public void setMediaId(String mediaId) {
		this.mediaId = mediaId;
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
}
