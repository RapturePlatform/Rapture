/**
 * Return a {@link <name>} based on its address
 * 
 * @param addressURI The address for this <name>
 * @return The {@link <name>} object stored at this address
 */
public static <name> readByAddress(RaptureURI addressURI) {
    RaptureURI storageLocation = StorageLocationFactory.createStorageURI(<name>PathBuilder.getRepoName(), <name>PathBuilder.getPrefix(), addressURI);
    return ObjectStorage.read(storageLocation, <name>.class, storableInfo, getObjectMapper());
}
  

/**
 * Delete a <name> from the repo, based on the object's Address.
 * @param addressURI The public address URI of the object to be deleted
 * @param user The user doing the delete
 * @param comment The comment associated with the delete
 * @return
 */
public static Boolean deleteByAddress(RaptureURI addressURI, String user, String comment) {
    RaptureURI storageLocation = StorageLocationFactory.createStorageURI(<name>PathBuilder.getRepoName(), <name>PathBuilder.getPrefix(), addressURI);
    return deleteByStorageLocation(storageLocation, user, comment);
}

/**
 * Return the storageLocation for this <name>, converted from the address
 * @param addressURI The public address URI of the <name>
 * @return
 */
public static RaptureURI addressToStorageLocation(RaptureURI addressURI) {
    return StorageLocationFactory.createStorageURI(<name>PathBuilder.getRepoName(), <name>PathBuilder.getPrefix(), addressURI);
}

/**
 * Retrieve the document and metadata for this entity
 */
 
public static DocumentWithMeta getDocumentWithMeta(RaptureURI addressURI) {
    RaptureURI storageLocation = StorageLocationFactory.createStorageURI(<name>PathBuilder.getRepoName(), <name>PathBuilder.getPrefix(), addressURI);
	return ObjectStorage.getDocumentWithMeta(storageLocation, storableInfo.getIndexInfo());
}
