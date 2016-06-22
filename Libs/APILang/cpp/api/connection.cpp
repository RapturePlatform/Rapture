#include <iostream>
#include <cstdlib>
#include <boost/network/protocol/http/client.hpp>
#include <boost/network/uri.hpp>

#include "connection.hpp"
#include "md5.h"

namespace http = boost::network::http;
namespace uri = boost::network::uri;

json RaptureConnection::performCall(std::string area, std::string func, json &params) {
    std::string paramString = params.dump();

    http::client client;
    http::client::request request(_hostname + "/" + area);

    json req;
    req["FUNCTION"] = func;
    req["PARAMS"] = paramString;

    std::string content = req.dump();
    std::cout << "Request json is " << content << std::endl;
    request << boost::network::header("Content-Type", "application/json");
    char body_str_len[8];
    sprintf(body_str_len, "%lu", content.length());
    request << boost::network::header("Content-Length", body_str_len);
    if (_isConnected) {
      request << boost::network::header("x-rapture", _connectionToken);
    }
    request << boost::network::body(req.dump());
    http::client::response response = client.post(request);
    std::string respJson = static_cast<std::string>(body(response));
    return json::parse(respJson);
}

void RaptureConnection::login(std::string password) {
    // Login to Rapture (eeeeek)
    std::cout << "Logging in to Rapture" << std::endl;

    json params;
    params["user"] = _username;
    json resp = performCall("login","CONTEXT", params);
    // inner part

    std::cout << resp["response"] << std::endl;

    // Now resp["response"]["contextId"] and salt are used to construct a login request
    json loginParams;
    loginParams["user"] = _username;
    loginParams["context"] = resp["response"]["contextId"];

    std::string salt = resp["response"]["salt"];
    std::string hashpassword = md5(md5(password) + ":" + salt);
    loginParams["digest"] = hashpassword;

    json loginResp = performCall("login", "LOGIN", loginParams);
    std::cout << loginResp << std::endl;
    _connectionToken = loginResp["response"]["context"];
    _isConnected = true;
}
