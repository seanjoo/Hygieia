(function () {

    console.log("cloud view ");

    'use strict';
    angular
        .module(HygieiaConfig.module)
        .controller('CloudWidgetViewController', CloudWidgetViewController);

    CloudWidgetViewController.$inject = ['$scope', 'cloudData', '$http', '$modal'];

    function CloudWidgetViewController($scope, cloudData, $http, $modal) {




        var ctrl = this;
        ctrl.tag = $scope.widgetConfig.options.tag || "";

        ctrl.load = function() {
            return cloudData.getEC2DataSummarizedByTag(ctrl.tag);
        }

        ctrl.viewSubnetUtilization = function(vpc) {
            $modal.open({
                controller: 'SubnetUtilizationController',
                controllerAs: 'subnetUtilization',                
                templateUrl: 'components/widgets/cloud/subnetUtilization.html',
                size: 'lg',
                resolve: {
                  vpc: function() {
                    return vpc;
                  }
                }

            });
        };

      $scope.vpcs = [];
      $scope.getVpcs = function() {
        var result = {};
        //,"vpc-e0750985"
        var requestBody = '{"percentUsedIps":"0","eventType":"API","tag":"ASV","subnetFilter":[{"name":"vpc-id","values":["vpc-45741f21","vpc-72c7f716","vpc-85f400e1","vpc-9ff33fa","vpc-f98ff19c","vpc-e0750985","vpc-1f8a7e7b","vpc-f98ff19c"]},{"name":"state","values":["available"]} ]}'; 
        
        result = $http.post('https://ogysssaa22.execute-api.us-east-1.amazonaws.com/dev/ctae-subnet-visigoth',  requestBody)
                    .success(function(data, status, headers, config) {
                      // this callback will be called asynchronously
                      // when the response is available
                      // $scope.subnets = ctrl.aggregateSubnetsByAz(data);})
                         $scope.vpcs = ctrl.groupByVpc(data);})
                    .error(function(data, status, headers, config) {
                      // called asynchronously if an error occurs
                      // or server returns response with an error status.
                      console.log(status)
                    });
        return result;
      }


        ctrl.calculateUtilization = function(subnet) {

            return subnet.usedIPCount/(subnet.usedIPCount + subnet.availableIPCount) * 100;
        }
        ctrl.groupByVpc = function(subnets) {
          var vpcMap = {};
          var vpcs = []
          angular.forEach(subnets, function(subnet) {
            if (!vpcMap[subnet.virtualNetworkId]) {
              var vpc = {};
              vpc.id = subnet.virtualNetworkId;
              vpc.subnets = [];
              vpc.countOfSubnetsByUtilization = {};
              vpc.countOfSubnetsByUtilization.high = 0;
              vpc.countOfSubnetsByUtilization.med = 0;
              vpc.countOfSubnetsByUtilization.low = 0;
              vpcs.push(vpc);
              vpcMap[subnet.virtualNetworkId] = vpc;              
            };
            vpcMap[subnet.virtualNetworkId].subnets.push(subnet);
            var utilization = ctrl.calculateUtilization(subnet);
            if (utilization > 70) {
              vpcMap[subnet.virtualNetworkId].countOfSubnetsByUtilization.high+=1;
            } else if (utilization <= 70 && utilization > 50) {
                vpcMap[subnet.virtualNetworkId].countOfSubnetsByUtilization.med+=1;
            } else {
                vpcMap[subnet.virtualNetworkId].countOfSubnetsByUtilization.low+=1;
            }
          });
          return vpcs;
        };

        ctrl.aggregateSubnetsByAz = function(subnets) {
          var azMap = {};
          var availabilityZones = []
          angular.forEach(subnets, function(subnet) {
            if (!azMap[subnet.subnet.availabilityZone]) {
              azMap[subnet.subnet.availabilityZone] = [];
              var az = {};
              az.name = subnet.subnet.availabilityZone;
              az.subnets = azMap[subnet.subnet.availabilityZone]
              availabilityZones.push(az);
            };
    
            azMap[subnet.subnet.availabilityZone].push(subnet);
          });
          return availabilityZones;
        };


    }

})();