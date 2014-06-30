/*
 * #%L
 * ACS AEM Tools Package
 * %%
 * Copyright (C) 2013 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

/*global angular: false */

angular.module('findAndReplace', []).controller('MainCtrl', function($scope, $http, $timeout) {
    $scope.app = {
        running: false,
        uri: ''
    };

    $scope.form = {
        searchPath: '',
        updateReferences: 'dryrun'
    };

	$scope.notifications = [];

    $scope.findAndReplace = function() {

        if(!$scope.form.searchPath
                //|| !$scope.form.searchComponent
                || !$scope.form.searchString
                || !$scope.form.searchElement) {

            $scope.addNotification('help', 'HELP', 'Please complete all required fields');

        } else {
            $scope.app.running = true;
            $scope.result = {};

            $http({
                method: 'POST',
                url: $scope.app.uri,
                data: $.param({
                    search_element : $scope.form.searchElement || 'nt:ignore',
                    search_path : $scope.form.searchPath || '/dev/null',
                    search_string : $scope.form.searchString,
                    replace_string : $scope.form.replaceString,
                    search_component : $scope.form.searchComponent,
                    update_references: $scope.form.updateReferences || 'dryrun'
                }),
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
            }).success(function(data, status, headers, config) {
                $scope.result = data;
                $scope.app.running = false;
            }).error(function(data, status, headers, config) {
                $scope.result = data;
                $scope.app.running = false;
            });
        }
   };

    $scope.addNotification = function (type, title, message) {
        var timeout = 100000;

        if(type === 'success') {
            timeout = timeout / 2;
        }

        $scope.notifications.unshift({
            type: type,
            title: title,
            message: message
        });

        $timeout(function() {
            $scope.notifications.shift();
        }, timeout);
    };
});

