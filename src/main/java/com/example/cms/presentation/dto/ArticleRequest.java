package com.example.cms.presentation.dto;

import lombok.Data;

@Data
public class ArticleRequest {
    private String title;
    private String content;
    private Long categoryId;
    private String keywords;
}
