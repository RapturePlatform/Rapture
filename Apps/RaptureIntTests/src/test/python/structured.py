import raptureAPI
import multipart
import json
import time
import base64

# TODO These need to be parameters

repo = '//nightly_python'

strucRepoUri = 'structured:'+repo

site = 'localhost:8665/rapture'
username = 'rapture'
password = 'rapture'
rapture = raptureAPI.raptureAPI(site, username, password)
config = "STRUCTURED { } USING POSTGRES { marvin=\"paranoid\" }"

def test_structuredapi():
 
    # Create a repo
    try:
        rapture.doStructured_DeleteStructuredRepo(strucRepoUri)
    except:
        assert True
    assert not rapture.doStructured_StructuredRepoExists(strucRepoUri)
    rapture.doStructured_CreateStructuredRepo(strucRepoUri, config)
    assert rapture.doStructured_StructuredRepoExists(strucRepoUri)
    assert config == rapture.doStructured_GetStructuredRepoConfig(strucRepoUri)['config']

    # Create a table. Add and remove data
    table = repo + "/table"
    
    definition = {'id' : 'int', 'name' : 'varchar(255), PRIMARY KEY (id)'}
    rapture.doStructured_CreateTable(table, definition)

    row = {'id' : 42, 'name' : 'Don\'t Panic'}
    rapture.doStructured_InsertRow(table, row)

    contents = rapture.doStructured_SelectRows(table, None, None, None, None, -1)
    assert len(contents) == 1
    assert contents[0] == row

    batch = [ {'id' : 11, 'name' : 'Ford Prefect'}, {'id' : 33, 'name' : 'Zaphod Beeblebrox'}, {'id' : 55, 'name' : 'Arthur Dent'}, {'id' : 44, 'name' : 'Slartibartfast'}, {'id' : 22, 'name' : 'Trillian'} ]

    rapture.doStructured_InsertRows(table, batch)
    contents = rapture.doStructured_SelectRows(table, None, None, None, None, -1)
    assert len(contents) == len(batch) + 1

    rapture.doStructured_DeleteRows(table, "id=42")

    contents = rapture.doStructured_SelectRows(table, None, None, None, None, -1)
    assert len(contents) == len(batch)
    
    for f in contents:
      if f['id'] == 33:
         assert f['name'] == "Zaphod Beeblebrox"

    # Update a row
    rapture.doStructured_UpdateRows(table, {'id' : 33, 'name' : 'Zarniwoop'}, "id=33")
    contents = rapture.doStructured_SelectRows(table, None, None, None, None, -1)
    assert len(contents) == len(batch)
    
    for f in contents:
      if f['id'] == 33:
         assert f['name'] == "Zarniwoop"

    rapture.doStructured_DropTable(table)
    try:
        contents = rapture.doStructured_SelectRows(table, None, None, None, None, -1)
        assert False
    except:
        assert True

    # Good enough. Delete the repo.
    rapture.doStructured_DeleteStructuredRepo(strucRepoUri)
    assert not rapture.doStructured_StructuredRepoExists(strucRepoUri)

