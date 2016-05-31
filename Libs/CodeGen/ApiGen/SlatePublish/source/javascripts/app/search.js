(function (global) {
    'use strict';

    var $global = $(global);
    var content, darkBox, searchResults, toc;
    var highlightOpts = {
        element: 'span',
        className: 'search-highlight'
    };

    var index = new lunr.Index();

    index.ref('id');
    index.field('title', {
        boost: 10
    });
    index.field('body');
    index.pipeline.add(lunr.trimmer, lunr.stopWordFilter);

    $(populate);
    $(bind);

    function populate() {
        $('h1, h2').each(function () {
            var title = $(this);
            var body = title.nextUntil('h1, h2');
            index.add({
                id: title.prop('id'),
                title: title.text(),
                body: body.text()
            });
        });
    }

    function bind() {
        content = $('.content');
        darkBox = $('.dark-box');
        searchResults = $('.search-results');
        toc = $('#toc');

        $('#input-search').on('keyup', search);
    }

    var timeout;

    function search(event) {
        var searchText = this.value;
        if (searchText.length == 1) {
            return;
        } else {

            if (timeout) {
                clearTimeout(timeout);
            }
            timeout = setTimeout(function () {
                unhighlight();
                searchResults.addClass('visible');
                toc.addClass('hidden');

                // ESC clears the field
                if (event.keyCode === 27) searchText = '';

                if (searchText) {
                    var results = index.search(searchText).filter(function (r) {
                        return r.score > 0.0001;
                    });

                    if (results.length) {
                        searchResults.empty();
                        $.each(results, function (index, result) {
                            var header = $("#" + result.ref);
                            var elem = header[0];
                            searchResults.append("<li><a href='#" + result.ref + "'>" + $(elem).text() + "</a></li>");
                        });
                        highlight(searchText);
                    } else {
                        searchResults.html('<li></li>');
                        $('.search-results li').text('No Results Found for "' + searchText + '"');
                    }
                } else {
                    unhighlight();
                    searchResults.removeClass('visible');
                    toc.removeClass('hidden');
                }
            }, 500);
        }
    }

    function highlight(searchText) {
        if (searchText) content.highlight(searchText, highlightOpts);
    }

    function unhighlight() {
        content.unhighlight(highlightOpts);
    }

})(window);