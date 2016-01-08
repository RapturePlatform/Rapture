/**
 * Return a {@link <name>} based on its member fields. This is useful if you know the value of
 * the member fields of the object, but not the exact storage location or address.
 * 
 * @return The {@link <name>} object stored at this address
 */
public static <name> readByFields(<fields; separator=", ">) {
    RaptureURI storageLocation 
      = new <name>PathBuilder()
       <adders; separator="\n">
			        .buildStorageLocation();

        return ObjectStorage.read(storageLocation, <name>.class, storableInfo, getObjectMapper());
}

/**
 * Get the folders or {@link <name>} objects under parentFolderPath.
 * Searches the tree of objects of type {@link <name>}
 * 
 * @param parentFolderPath
 * @return
 */
public static List\<RaptureFolderInfo\> getChildren(String parentFolderPath) {
    return ObjectStorage.getChildren(<name>PathBuilder.getRepoName(), <name>PathBuilder.getPrefix(), storableInfo, parentFolderPath);
}

/**
 * Removes the documents and folders {@link <name>} objects under parentFolderPath.
 * 
 * @param parentFolderPath
 * @return
 */
public static List\<RaptureFolderInfo\> removeFolder(String parentFolderPath) {
    return ObjectStorage.removeFolder(<name>PathBuilder.getRepoName(), <name>PathBuilder.getPrefix(), storableInfo, parentFolderPath);
}

/**
 * Return all the {@link <name>} objects currently stored in Rapture
 * @return
 */
public static List\<<name>\> readAll() {
    return ObjectStorage.readAll(<name>.class, <name>PathBuilder.getRepoName(), <name>PathBuilder.getPrefix(), storableInfo, "", getObjectMapper());
}

/**
 * Return all the {@link <name>} objects currently stored in Rapture 
 * @param
 * filterPrefix The prefix of the storageLocation that you want to filter by
 * 
 * @return
 */
public static List\<<name>\> readAll(String filterPrefix) {
    return ObjectStorage.readAll(<name>.class, <name>PathBuilder.getRepoName(), <name>PathBuilder.getPrefix(), storableInfo, filterPrefix, getObjectMapper());
}

/**
 * Return all the {@link <name>} objects currently stored in Rapture which match an ObjectFilter
 * @param
 * filterPrefix The prefix of the storageLocation that you want to filter by
 * @param
 * filter An ObjectFilter
 *
 * @return
 */
public static List\<<name>\> filterAll(ObjectFilter\<<name>\> filter) {
    return ObjectStorage.filterAll(<name>.class, <name>PathBuilder.getRepoName(), <name>PathBuilder.getPrefix(), storableInfo, "", filter, getObjectMapper());
}


/**
 * Visit the repository storing {@link <name>} objects
 * 
 * @param visitor
 */
public static void visitAll(RepoVisitor visitor) {
    ObjectStorage.visitAll(<name>PathBuilder.getRepoName(), <name>PathBuilder.getPrefix(), "", storableInfo, visitor);
}


/**
 * Return a {@link <name>} based on its storage location
 * 
 * @param storageLocationURI The storage location for this <name>
 * @return The {@link <name>} object stored at this address
 */
public static <name> readByStorageLocation(RaptureURI storageLocation) {
    return ObjectStorage.read(storageLocation, <name>.class, storableInfo, getObjectMapper());
}

/**
 * Read a {@link <name>} from json
 * @param content The {@link JsonContent} to read
 * @return The {@link <name>} object constructed from the json
 */
public static <name> readFromJson(JsonContent content) {
    return ObjectStorage.read(content, <name>.class, getObjectMapper());
}


/**
 * Delete a <name> from the repo, using its member fields to find it.
 * @param user The user doing the delete
 * @param comment The comment associated with the delete
 * @return
 */
public static Boolean deleteByFields(<if(fields)><fields; separator=", ">,<endif> String user, String comment) {
    RaptureURI storageLocation 
      = new <name>PathBuilder()
       <adders; separator="\n">
              .buildStorageLocation();
    return ObjectStorage.delete(user, storageLocation, storableInfo.getIndexInfo(), comment);
}

/**
 * Delete a <name> from the repo.
 * @param storageLocation The storage location of the object in the repo
 * @param user The user doing the delete
 * @param comment The comment associated with the delete
 * @return
 */
public static Boolean deleteByStorageLocation(RaptureURI storageLocation, String user, String comment) {
        return ObjectStorage.delete(user, storageLocation, storableInfo.getIndexInfo(), comment);
}

/**
 * Store a <name> into the repo, and add a comment
 * 
 * @param doc
 *            The <name> that will be stored
 * @param user
 *            The user doing the storing
 * @param comment
 */
public static void add(<name> storable, String user, String comment) {
    ObjectStorage.write(storable, user, storableInfo, comment, getObjectMapper());
}

/**
 * Run a query on the repo where the index of <name> is stored
 * @param query
 * @return
 */
public static TableQueryResult queryIndex(String query) {
    return ObjectStorage.queryIndex(storableInfo, <name>PathBuilder.getRepoName(), query);
}
