#include <iostream>
#include <cstdlib>

#include "connection.hpp"
#include "generated/User.h"
#include "generated/Doc.h"
#include "generated/Script.h"

/**
  * A test file for playing with the Rapture API
  */

int main(int argc, char* argv[])
{
   std::cout << "Hello world" << std::endl;
   RaptureConnection conn("http://localhost:8665/rapture", "rapture");
   conn.login("rapture");

   UserApi userApi(conn);
   DocApi docApi(conn);
   ScriptApi scriptApi(conn);

   std::cout << userApi.GetWhoAmI() << std::endl;

   std::string docUri = "//testSearch/One";

   std::cout << "Retrieving the document " << docUri << std::endl;

   json r = docApi.GetDocAndMeta(docUri);

   std::cout << r << std::endl;

   std::string scriptUri = "//curtis/createScript";
   std::cout << "Retrieving a script - " << scriptUri << std::endl;
   json r2 = scriptApi.GetScript(scriptUri);
   std::cout << r2 << std::endl;

   return 0;
}
