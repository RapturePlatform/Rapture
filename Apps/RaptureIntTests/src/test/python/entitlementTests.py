import raptureAPI
import multipart
import json
import time
import base64

# TODO These need to be parameters

repo = '//nightly_python'

blobRepoUri = 'blob:'+repo
docRepoUri = 'document:'+repo
seriesRepoUri = 'series:'+repo

site = 'localhost:8665/rapture'
username = 'rapture'
password = 'rapture'
rapture = raptureAPI.raptureAPI(site, username, password)
user2 = "User2"

def test_entitlements():
    ent = "/data/write/"+repo[2:]
    group = "Group"
    documentUri = "document:"+repo+"/test/doc"

    if 'valid' in rapture.context and rapture.context['valid']:
        print 'Logged in successfully '
    else:
        print "Login unsuccessful"
        assert false

    config = " {} USING MONGODB {prefix=\"nightly\"}"
    if(rapture.doDoc_DocRepoExists(docRepoUri)):
        rapture.doDoc_DeleteDocRepo(docRepoUri)
    rapture.doDoc_CreateDocRepo(docRepoUri, "NREP "+config)

    if rapture.doAdmin_DoesUserExist(user2):
        print "user exists"
    else:
        rapture.doAdmin_AddUser(user2, "Another User", multipart.MD5(user2), "user@incapture.net");
    rapture.doAdmin_ResetUserPassword(user2, multipart.MD5(user2));
    rapture.doEntitlement_DeleteEntitlementGroup(group)
    rapture.doEntitlement_DeleteEntitlement(ent)
    rapture2 = raptureAPI.raptureAPI(site, user2, user2)

    permitted = rapture2.doUser_IsPermitted('doc.putDoc', documentUri)
    assert permitted, "Unrestricted"

    rapture.doEntitlement_AddEntitlement(ent, group)
    rapture.doEntitlement_AddEntitlementGroup(group)
    rapture.doEntitlement_AddGroupToEntitlement(ent, group)

    assert rapture.doEntitlement_GetEntitlementGroup(group)['users'] == []
    gg = rapture.doEntitlement_GetEntitlement(ent)['groups']
#    assert rapture.doEntitlement_GetEntitlement(ent).getGroups().contains(group)

    permitted = rapture2.doUser_IsPermitted('doc.putDoc', documentUri)
    assert not permitted, "Access not yet granted"

    rapture.doEntitlement_AddUserToEntitlementGroup(group, user2)
    permitted = rapture2.doUser_IsPermitted('doc.putDoc', documentUri)
    assert permitted, "Access is now granted"

    rapture.doEntitlement_RemoveUserFromEntitlementGroup(group, user2)
    permitted = rapture2.doUser_IsPermitted('doc.putDoc', documentUri)
    assert not permitted, "Access no longer granted"
    rapture.doEntitlement_DeleteEntitlementGroup(group)
    rapture.doEntitlement_DeleteEntitlement(ent)

    permitted = rapture2.doUser_IsPermitted('doc.putDoc', documentUri)
    assert permitted, "Unrestricted"
