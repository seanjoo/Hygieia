
(function () {
    'use strict';
    console.log("module.js");
    var widget_state,
        config = {
            view: {
                defaults: {
                    title: 'Cloud' // widget title
                },
                controller: 'CloudWidgetViewController',
                controllerAs: 'cloudView',
                templateUrl: 'components/widgets/cloud/view.html'
            },
            config: {
                controller: 'CloudWidgetConfigController',
                controllerAs: 'cloudConfig',
                templateUrl: 'components/widgets/cloud/config.html'
            },
            getState: getState,
            collectors: ['cloud']
        };

    angular
        .module(HygieiaConfig.module)
        .config(register);

    register.$inject = ['widgetManagerProvider', 'WidgetState'];
    function register(widgetManagerProvider, WidgetState) {
        widget_state = WidgetState;
        widgetManagerProvider.register('cloud', config);
    }

    function getState(config) {
        return widget_state.READY;
        //return HygieiaConfig.local ?
        //    widget_state.READY :
        //    widget_state.CONFIGURE;
    }
})();