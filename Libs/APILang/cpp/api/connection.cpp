#include <iostream>
#include <cstdlib>
#include <boost/network/protocol/http/client.hpp>
#include <boost/network/uri.hpp>
#include "json.hpp"

using json = nlohmann::json;

#include "connection.hpp"

namespace http = boost::network::http;
namespace uri = boost::network::uri;

void RaptureConnection::login(std::string password) {
    // Login to Rapture (eeeeek)
    std::cout << "Logging in to Rapture" << std::endl;

    http::client client;
    http::client::request request(_hostname + "/login");
    std::string params("{\"user\" : \"rapture\"}");
    std::string func("CONTEXT");

    json req;
    req["FUNCTION"] = func;
    req["PARAMS"] = params;

    std::string content = req.dump();
    std::cout << "Request json is " << content << std::endl;
    request << boost::network::header("Content-Type", "application/json");
    char body_str_len[8];
    sprintf(body_str_len, "%u", content.length());
    request << boost::network::header("Content-Length", body_str_len);

    request << boost::network::body(req.dump());
    http::client::response response = client.post(request);
    std::cout << static_cast<std::string>(body(response)) << std::endl;
}
