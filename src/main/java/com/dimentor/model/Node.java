package com.dimentor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class Node {
    private long id;

    @NonNull
    private String name;

    private String checksum;

    @NonNull
    private long version;

    private Node parent;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ToString.Exclude
    private List<Node> children = new ArrayList<>();

    public String generateUri(){
        Node n = this;
        StringBuilder s = new StringBuilder();
        while (n!= null){
            s.insert(0, n.getName() + "\\");
            n = n.getParent();
            //System.out.println(s.toString());
        }
        return s.toString();
    }
}

