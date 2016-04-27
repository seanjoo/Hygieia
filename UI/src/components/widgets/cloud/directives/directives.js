(function () {
    'use strict';

    var app = angular
        .module(HygieiaConfig.module);

    // simple way to add multiple directives with basic templates so we
    // can break apart the widget
    var directives = {
        metricCategory: {
            scope: { data: '='}
        },
        ipUtilization: {}
    };

    _(directives).forEach(function (obj, name) {
        app.directive(name, function () {
            obj = angular.extend({
                restrict: 'E',
                templateUrl: 'components/widgets/cloud/directives/' + name + '.html'
            }, obj);
            //console.log(obj);
            return obj;
        });
    });


})();



