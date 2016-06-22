(function (global) {
    'use strict';

    var $global = $(global);


    function updateHeaderIds() {
        var headers = $(":header");
        var lastH1 = null;
        for (var i = 0; i < headers.length; i++) {
            var h = headers[i];
            if (h.tagName == "H1") {
                lastH1 = h.id;
            } else {
                if (lastH1 != null) {
                    h.id = createId(lastH1, h.id);
                }
            }
        }
    }

    function createId(apiName, methodName) {
        return apiName + "_" + methodName;
    }


    function scrollToAnchor() {
        var hash = window.location.hash;
        if (hash != null && hash.indexOf("#_") == 0) {
            var realHash = hash.substring(2);
            window.location.hash = realHash;
        }
    }

    $(function () {
        updateHeaderIds();
        scrollToAnchor();
    });

})(window);