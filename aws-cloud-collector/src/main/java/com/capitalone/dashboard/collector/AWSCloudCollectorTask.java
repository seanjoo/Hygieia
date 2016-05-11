/*************************
 * DA-BOARD-LICENSE-START*********************************
 * Copyright 2014 CapitalOne, LLC.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ************************DA-BOARD-LICENSE-END
 *********************************/

package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.config.collector.CloudConfig;
import com.capitalone.dashboard.model.CloudInstance;
import com.capitalone.dashboard.model.CloudInstanceHistory;
import com.capitalone.dashboard.model.CloudSubNetwork;
import com.capitalone.dashboard.model.CloudVirtualNetwork;
import com.capitalone.dashboard.model.CloudVolumeStorage;
import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.repository.AWSConfigRepository;
import com.capitalone.dashboard.repository.BaseCollectorRepository;
import com.capitalone.dashboard.repository.CloudInstanceHistoryRepository;
import com.capitalone.dashboard.repository.CloudInstanceRepository;
import com.capitalone.dashboard.repository.CloudSubNetworkRepository;
import com.capitalone.dashboard.repository.CloudVirtualNetworkRepository;
import com.capitalone.dashboard.repository.CloudVolumeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Collects {@link AWSCloudCollector} data from feature content source system.
 */
@Component
public class AWSCloudCollectorTask extends CollectorTask<AWSCloudCollector> {

    private final CloudInstanceRepository cloudInstanceRepository;
    private final CloudVirtualNetworkRepository cloudVirtualNetworkRepository;
    private final CloudSubNetworkRepository cloudSubNetworkRepository;
    private final CloudVolumeRepository cloudVolumeRepository;
    private final CloudInstanceHistoryRepository cloudInstanceHistoryRepository;


    private final AWSCloudSettings awsSetting;
    private final AWSCloudClient awsClient;
    private final AWSConfigRepository awsConfigRepository;
    private final BaseCollectorRepository<AWSCloudCollector> collectorRepository;


    /**
     * @param taskScheduler Task Scheduler
     * @param collectorRepository Collector Repository
     * @param cloudSettings Cloud Settings
     * @param cloudClient Cloud Client
     * @param awsConfigRepository Config Repository
     * @param cloudInstanceRepository Cloud Instance Repository
     * @param cloudVirtualNetworkRepository Cloud Virtual Network Repository
     * @param cloudSubNetworkRepository Cloud Subnet Repository
     * @param cloudVolumeRepository Cloud Volume Repository
     * @param cloudInstanceHistoryRepository Cloud Instance History Repository
     */
    @Autowired
    public AWSCloudCollectorTask(TaskScheduler taskScheduler,
                                 BaseCollectorRepository<AWSCloudCollector> collectorRepository,
                                 AWSCloudSettings cloudSettings, AWSCloudClient cloudClient,
                                 AWSConfigRepository awsConfigRepository,
                                 CloudInstanceRepository cloudInstanceRepository,
                                 CloudVirtualNetworkRepository cloudVirtualNetworkRepository,
                                 CloudSubNetworkRepository cloudSubNetworkRepository,
                                 CloudVolumeRepository cloudVolumeRepository,
                                 CloudInstanceHistoryRepository cloudInstanceHistoryRepository) {
        super(taskScheduler, "AWSCloud");
        this.collectorRepository = collectorRepository;
        this.awsClient = cloudClient;
        this.awsSetting = cloudSettings;
        this.awsConfigRepository = awsConfigRepository;
        this.cloudInstanceRepository = cloudInstanceRepository;
        this.cloudVirtualNetworkRepository = cloudVirtualNetworkRepository;
        this.cloudSubNetworkRepository = cloudSubNetworkRepository;
        this.cloudVolumeRepository = cloudVolumeRepository;
        this.cloudInstanceHistoryRepository = cloudInstanceHistoryRepository;
    }

    public AWSCloudCollector getCollector() {
        return AWSCloudCollector.prototype();
    }

    /**
     * Accessor method for the current chronology setting, for the scheduler
     */
    public String getCron() {
        return awsSetting.getCron();
    }

    /**
     * The collection action. This is the task which will run on a schedule to
     * gather data from the feature content source system and update the
     * repository with retrieved .
     */
    public void collect(AWSCloudCollector collector) {
        log("Starting AWS collection...");
        log("Collecting AWS Cloud Data...");

        collectInstances();

        collectVolume();

        log("Finished Cloud collection.");
    }

    private void collectInstances() {
        Map<String, List<CloudInstance>> cloudInstanceMap = awsClient.getCloundInstances(cloudInstanceRepository);
        for (String account : cloudInstanceMap.keySet()) {
            Collection<CloudInstance> existing = cloudInstanceRepository.findByAccountNumber(account);
            if (!CollectionUtils.isEmpty(existing)) {
                cloudInstanceRepository.delete(existing);
            }
            cloudInstanceRepository.save(cloudInstanceMap.get(account));
            saveAggregatedHistory(account, cloudInstanceMap.get(account));
        }

    }

    private void saveAggregatedHistory(String account, List<CloudInstance> instances) {
        int unTaggedCount = 0;
        int stoppedCount = 0;
        int totalCount = 0;
        int expiredImageCount = 0;
//        double estimatedCharge = 0.0;
        double totalCpu = 0.0;
        double totalDiskRead = 0.0;
        double totalDiskWrite = 0.0;
        double totalNetworkIn = 0.0;
        double totalNetworkOut = 0.0;


        for (CloudInstance rd : instances) {
            totalCount = totalCount + 1;
            totalCpu = totalCpu + rd.getCpuUtilization();
            totalDiskRead = totalDiskRead + rd.getDiskRead();
            totalDiskWrite = totalDiskWrite + rd.getDiskWrite();
            totalNetworkIn = totalNetworkIn + rd.getNetworkIn();
            totalNetworkOut = totalNetworkOut + rd.getNetworkOut();

            if (rd.isStopped()) {
                stoppedCount = stoppedCount + 1;
            }
            if (!rd.isTagged()) {
                unTaggedCount = unTaggedCount + 1;
            }
            Date expirationDate = new Date(rd.getImageExpirationDate());
            Date today = new Date();
            if (today.after(expirationDate)) {
                expiredImageCount = expiredImageCount + 1;
            }
        }
        if (totalCount > 0) {
            CloudInstanceHistory history = new CloudInstanceHistory();
            history.setAccountNumber(account);
            history.setTime(System.currentTimeMillis());
            history.setTotal(totalCount);
            history.setStopped(stoppedCount);
            history.setCpu(totalCpu / totalCount);
            history.setDiskRead(totalDiskRead / totalCount);
            history.setDiskWrite(totalDiskWrite / totalCount);
            history.setNetworkOut(totalNetworkOut/totalCount);
            history.setNetworkIn(totalNetworkIn/totalCount);
            history.setNonTagged(unTaggedCount);
            cloudInstanceHistoryRepository.save(history);
        }

    }


    private void collectVPC() {
        CloudVirtualNetwork cloudVirtualNetwork = awsClient.getCloudVPC(cloudVirtualNetworkRepository);
        cloudVirtualNetwork.setLastUpdateDate(System.currentTimeMillis());
        cloudVirtualNetworkRepository.save(cloudVirtualNetwork);
    }

    private void collectSubNet() {
        CloudSubNetwork cloudSubNetwork = awsClient.getCloudSubnet(cloudSubNetworkRepository);
        cloudSubNetwork.setLastUpdateDate(System.currentTimeMillis());
        cloudSubNetworkRepository.save(cloudSubNetwork);
    }

    private void collectVolume() {
        Map<String, List<CloudVolumeStorage>> accountVolume = awsClient.getCloudVolumes();
        for (String account : accountVolume.keySet()) {
            Collection<CloudVolumeStorage> existing = cloudVolumeRepository.findByAccountNumber(account);
            if (!CollectionUtils.isEmpty(existing)) {
                cloudVolumeRepository.delete(existing);
            }
            cloudVolumeRepository.save(accountVolume.get(account));
        }
    }

    @Override
    public BaseCollectorRepository<AWSCloudCollector> getCollectorRepository() {
        return collectorRepository;
    }

    private List<CloudConfig> enabledConfigs(Collector collector) {
        return awsConfigRepository.findEnabledCloudConfig(collector.getId());
    }
}
