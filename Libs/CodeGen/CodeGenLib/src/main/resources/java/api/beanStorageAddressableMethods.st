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
<if(fts)>
	ObjectStorageSearchPublisher.publishDeleteMessage(addressURI);
	
<endif>
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

public DocumentWithMeta i_getDocumentWithMeta(RaptureURI addressURI) {
   return getDocumentWithMeta(addressURI);
}

/**
 * Tags
 */
 
public static DocumentMetadata applyTag(RaptureURI addressURI, String user, String tagUri, String tagValue) {
    RaptureURI storageLocation = StorageLocationFactory.createStorageURI(<name>PathBuilder.getRepoName(), <name>PathBuilder.getPrefix(), addressURI);
	DocumentWithMeta dwm = ObjectStorage.applyTag(storageLocation,  storableInfo.getIndexInfo(), user, tagUri, tagValue);
	if (dwm != null) {
<if(fts)>
	   	dwm.getMetaData().setSemanticUri(addressURI.toString());
		ObjectStorageSearchPublisher.publishCreateMessage(dwm);
<endif>
		return dwm.getMetaData();
	}
	return null;
}

public static DocumentMetadata applyTags(RaptureURI addressURI, String user, Map\<String, String\> tagMap) {
    RaptureURI storageLocation = StorageLocationFactory.createStorageURI(<name>PathBuilder.getRepoName(), <name>PathBuilder.getPrefix(), addressURI);
	DocumentWithMeta dwm = ObjectStorage.applyTags(storageLocation,  storableInfo.getIndexInfo(), user, tagMap);
	if (dwm != null) {
<if(fts)>
	   	dwm.getMetaData().setSemanticUri(addressURI.toString());
		ObjectStorageSearchPublisher.publishCreateMessage(dwm);
<endif>
		return dwm.getMetaData();
	}
	return null;
}

public static DocumentMetadata removeTag(RaptureURI addressURI, String user, String tagUri) {
    RaptureURI storageLocation = StorageLocationFactory.createStorageURI(<name>PathBuilder.getRepoName(), <name>PathBuilder.getPrefix(), addressURI);
	DocumentWithMeta dwm = ObjectStorage.removeTag(storageLocation,  storableInfo.getIndexInfo(), user, tagUri);
	if (dwm != null) {
<if(fts)>
	   	dwm.getMetaData().setSemanticUri(addressURI.toString());
		ObjectStorageSearchPublisher.publishCreateMessage(dwm);
<endif>
		return dwm.getMetaData();
	}
	return null;
}

public static DocumentMetadata removeTags(RaptureURI addressURI, String user, List\<String\> tags) {
    RaptureURI storageLocation = StorageLocationFactory.createStorageURI(<name>PathBuilder.getRepoName(), <name>PathBuilder.getPrefix(), addressURI);
	DocumentWithMeta dwm = ObjectStorage.removeTags(storageLocation,  storableInfo.getIndexInfo(), user, tags);
	if (dwm != null) {
<if(fts)>
	   	dwm.getMetaData().setSemanticUri(addressURI.toString());
		ObjectStorageSearchPublisher.publishCreateMessage(dwm);
<endif>
		return dwm.getMetaData();
	}
	return null;
}

