if (<fieldName> == null) {
    if (other.<fieldName> != null) {
        return false;
    }
} else if (!<fieldName>.equals(other.<fieldName>)) {
    return false;
}
