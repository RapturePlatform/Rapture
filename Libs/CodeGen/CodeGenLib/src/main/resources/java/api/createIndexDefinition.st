public static IndexDefinition createIndex_<name>() {
    IndexDefinition indexDefinition = new IndexDefinition();
    indexDefinition.setIndexName("<name>");
    FieldDefinition field;
    FieldLocator fieldLocator;
    DocLocator docLocator;

    <fields; separator="\n">
    return indexDefinition;
}
