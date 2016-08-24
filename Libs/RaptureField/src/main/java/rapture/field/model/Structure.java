package rapture.field.model;

import java.util.List;

/**
 * A structure is a definition of some or part of a document (or an array of fields)
 * It lists a set of fields with key names, positions, and field definitions.
 */
 
public class Structure {
    private String uri;
    private String description;
    private List<StructureField> fields; // Order can be important here if the fields are passed from, say a result set...   
}