package com.jvmd.digitalurpaq_ai_agent.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Document(indexName = "document")
public class RetrievalDocument {

    @Id
    private String id;
    private String name;
    private String text;

}
