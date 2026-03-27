package com.example.product.model;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class AddImagesRequest {

	@NotEmpty(message = "mediaIds must not be empty")
	private List<String> mediaIds;

	public List<String> getMediaIds() {
		return mediaIds;
	}

	public void setMediaIds(List<String> mediaIds) {
		this.mediaIds = mediaIds;
	}
}
