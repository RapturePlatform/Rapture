#ifndef CONNECTION_HPP
#define CONNECTION_HPP

#include "json.hpp"
using json = nlohmann::json;

// Information about the connection to Rapture

class RaptureConnection {
private:
    std::string _hostname;
    std::string _username;
    int _portNumber;
    // Password is always passed in

    bool _isConnected;
    std::string _connectionToken;

public:
    RaptureConnection(std::string host, std::string username, int portNumber = 8665) :
        _hostname(host), _username(username), _portNumber(portNumber), _isConnected(false),
        _connectionToken("") {

        }

    // Attempt to login to Rapture using the saved configuration information. If
    // successful, store the connectionToken that can be used with subsequent calls.

    void login(std::string password);
    void logout() {
        _connectionToken = "";
        _isConnected = false;
    }

    json performCall(std::string area, std::string func, json &params);

};

#endif
