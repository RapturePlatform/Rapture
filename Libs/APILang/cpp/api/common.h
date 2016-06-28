#ifndef COMMON_H
#define COMMON_H

typedef struct {
    std::string scheme;
    std::string authority;
    std::string docPath;
    std::string version;
    std::string asOfTime;
    std::string attribute;
    std::string element;
} RaptureURI ;

#endif
